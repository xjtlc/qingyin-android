package local.qingyin.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.MediaMetadataRetriever;
import org.json.JSONObject;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlaybackService extends Service {
    private static final String CHANNEL = "qingyin_playback";
    private static final int NOTIFICATION_ID = 5042;
    private static final String LYRICS_CHANNEL = "qingyin_lock_lyrics";
    private static final int LYRICS_ID = 5043;
    private static final long FAILURE_SKIP_DELAY_MS = 650L;
    private static final long ONLINE_RESOLVE_TIMEOUT_MS = 55_000L;
    private static final long MEDIA_PREPARE_TIMEOUT_MS = 25_000L;
    private static final long PLAYBACK_CONFIRM_MS = 1_500L;
    public static final String[] AUDIO_EFFECTS={"原音","HIFI 高保真","超重低音","人声清晰","流行","摇滚","古典","爵士"};
    public static final String ACTION_PLAY = "local.qingyin.mobile.PLAY";
    public static final String ACTION_PAUSE = "local.qingyin.mobile.PAUSE";
    public static final String ACTION_PREV = "local.qingyin.mobile.PREV";
    public static final String ACTION_NEXT = "local.qingyin.mobile.NEXT";
    public static final String ACTION_STOP = "local.qingyin.mobile.STOP";
    private final Binder binder = new LocalBinder();
    private final List<Track> queue = new ArrayList<>();
    private final QueueNavigator navigator = new QueueNavigator();
    private final ExecutorService metadataWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService onlineWorker = Executors.newFixedThreadPool(2);
    private int onlineSerial;
    private String lastError = "";
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private MediaSession session;
    private MediaPlayer player;
    private Equalizer equalizer;
    private int effectPreset;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean playing, preparing, playWhenReady;
    private boolean started;
    private boolean noisyRegistered;
    private Runnable failureAdvance, prepareDeadline, playbackConfirmation;
    private int resumeAtMs;
    private boolean isClosing;
    private boolean lockLyricsEnabled;
    private Track lyricTrack, presentedTrack;
    private Bitmap mediaArtwork;
    private String artworkUrl = "";
    private List<LyricsParser.Line> lockLyrics = Collections.emptyList();
    private String lastLyric = "";
    private final Runnable lyricTicker = new Runnable() {
        @Override public void run() {
            if (isClosing || !lockLyricsEnabled) return;
            refreshLockLyric();
            mainThread.postDelayed(this, 750L);
        }
    };
    private final Runnable checkpointTask = new Runnable() {
        @Override public void run() {
            if (!playing || isClosing) return;
            savePlaybackPosition();
            mainThread.postDelayed(this, 15_000L);
        }
    };
    private long sleepDeadline;
    private final Runnable sleepTimeout = () -> {
        if (sleepDeadline > 0 && SystemClock.elapsedRealtime() >= sleepDeadline) {
            sleepDeadline = 0;
            pause();
        }
    };
    private OnChange listener;

    public interface OnChange { void changed(); }
    public final class LocalBinder extends Binder {
        public PlaybackService service() { return PlaybackService.this; }
    }

    @Override public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        effectPreset=getSharedPreferences("playback",MODE_PRIVATE).getInt("effect",0);
        if(effectPreset<0||effectPreset>=AUDIO_EFFECTS.length)effectPreset=0;
        String savedMode = getSharedPreferences("playback", MODE_PRIVATE).getString("mode", "LOOP");
        try { navigator.setMode(QueueNavigator.Mode.valueOf(savedMode)); }
        catch (IllegalArgumentException ignored) { navigator.setMode(QueueNavigator.Mode.LOOP); }
        PlaybackBookmark.Snapshot restored = PlaybackBookmark.load(this);
        if (restored != null) {
            queue.addAll(restored.songs);
            navigator.load(queue.size(), restored.index);
            Track previous = current();
            int safeDuration = previous == null ? 0 : (int)Math.min(Integer.MAX_VALUE, previous.durationMs);
            resumeAtMs = safeDuration > 1000 ? Math.min(restored.offsetMs, safeDuration - 1000) : 0;
        }
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL, "青音播放控制", NotificationManager.IMPORTANCE_LOW));
        NotificationChannel lyricChannel = new NotificationChannel(LYRICS_CHANNEL, "锁屏同步歌词", NotificationManager.IMPORTANCE_LOW);
        lyricChannel.setDescription("可选：在锁屏和下拉通知中显示当前同步歌词");
        lyricChannel.setSound(null, null);
        lyricChannel.enableVibration(false);
        lyricChannel.setShowBadge(false);
        notifications.createNotificationChannel(lyricChannel);
        lockLyricsEnabled = getSharedPreferences("playback", MODE_PRIVATE).getBoolean("lock_lyrics", false);
        if (lockLyricsEnabled) mainThread.post(lyricTicker);
        session = new MediaSession(this, "QingyinMobile");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { skip(1); }
            @Override public void onSkipToPrevious() { skip(-1); }
            @Override public void onSeekTo(long pos) { seekTo(pos); }
        });
        session.setActive(true);
        registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        noisyRegistered = true;
        publish();
    }

    private final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) pause();
        }
    };

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            started = true;
            startForeground(NOTIFICATION_ID, notification());
        }
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) resume();
            else if (ACTION_PAUSE.equals(action)) pause();
            else if (ACTION_PREV.equals(action)) skip(-1);
            else if (ACTION_NEXT.equals(action)) skip(1);
            else if (ACTION_STOP.equals(action)) {
                pause();
                stopForeground(STOP_FOREGROUND_REMOVE);
                started = false;
                stopSelfResult(startId);
            }
        }
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return binder; }
    public void setListener(OnChange next) { listener = next; if (next != null) next.changed(); }
    public Track current() { int i = navigator.index(); return i >= 0 && i < queue.size() ? queue.get(i) : null; }
    public List<Track> queueSnapshot() { return new ArrayList<>(queue); }
    public List<Integer> upcoming() { return navigator.upcoming(queue.size()); }
    public QueueNavigator.Mode mode() { return navigator.mode(); }
    public long sleepRemainingMs() { return sleepDeadline == 0 ? 0 : Math.max(0, sleepDeadline - SystemClock.elapsedRealtime()); }
    public void setSleepTimerMinutes(int minutes) {
        mainThread.removeCallbacks(sleepTimeout);
        if (minutes <= 0) { sleepDeadline = 0; publish(); return; }
        int safe = Math.min(minutes, 240);
        long duration = safe * 60_000L;
        sleepDeadline = SystemClock.elapsedRealtime() + duration;
        mainThread.postDelayed(sleepTimeout, duration);
        publish();
    }
    public void cycleMode() {
        QueueNavigator.Mode[] modes = QueueNavigator.Mode.values();
        navigator.setMode(modes[(navigator.mode().ordinal() + 1) % modes.length]);
        getSharedPreferences("playback", MODE_PRIVATE).edit().putString("mode", navigator.mode().name()).apply();
        publish();
    }
    public void selectQueue(int selected) {
        if (selected < 0 || selected >= queue.size()) return;
        navigator.choose(selected);
        resumeAtMs = 0;
        saveNewSongCheckpoint();
        open(true);
    }
    public String lastError() { return lastError; }
    public boolean isPlaying() { return playing; }
    public boolean isPreparing() { return preparing; }
    public int effectPreset(){return effectPreset;}
    public boolean setEffectPreset(int index){
        if(index<0||index>=AUDIO_EFFECTS.length)return false;
        int previous=effectPreset;
        effectPreset=index;
        if(player!=null&&!preparing&&!applyEqualizer()){
            effectPreset=0;
            applyEqualizer();
            getSharedPreferences("playback",MODE_PRIVATE).edit().putInt("effect",0).apply();
            publish();
            return false;
        }
        getSharedPreferences("playback",MODE_PRIVATE).edit().putInt("effect",effectPreset).apply();
        publish();
        return true;
    }
    private void releaseEqualizer(){
        if(equalizer!=null){
            try{equalizer.setEnabled(false);}catch(Exception ignored){}
            try{equalizer.release();}catch(Exception ignored){}
            equalizer=null;
        }
    }
    private static float targetDb(int preset,float hz){
        switch(preset){
            case 1: return hz<130?1.5f:hz<800?0.5f:hz<4000?0.8f:1.2f;
            case 2: return hz<120?6f:hz<280?3f:hz<1200?0f:-1f;
            case 3: return hz<200?-2f:hz<550?0f:hz<4500?4f:0f;
            case 4: return hz<180?3f:hz<800?1f:hz<5500?2.5f:1f;
            case 5: return hz<180?4f:hz<650?1f:hz<4000?3f:2f;
            case 6: return hz<220?1f:hz<1300?0f:hz<5000?1f:2.5f;
            case 7: return hz<200?2.5f:hz<900?1.5f:hz<4500?2f:1f;
            default:return 0f;
        }
    }
    private boolean applyEqualizer(){
        releaseEqualizer();
        if(effectPreset==0||player==null)return true;
        Equalizer next=null;
        try{
            next=new Equalizer(0,player.getAudioSessionId());
            short[] bounds=next.getBandLevelRange();
            for(short band=0;band<next.getNumberOfBands();band++){
                float hz=next.getCenterFreq(band)/1000f;
                int desired=Math.round(targetDb(effectPreset,hz)*100);
                short level=(short)Math.max(bounds[0],Math.min(bounds[1],desired));
                next.setBandLevel(band,level);
            }
            next.setEnabled(true);
            equalizer=next;
            return true;
        }catch(Exception unsupported){
            if(next!=null)try{next.release();}catch(Exception ignored){}
            return false;
        }
    }
    public int currentPosition() {
        try { return player != null && !preparing ? player.getCurrentPosition() : resumeAtMs; }
        catch (IllegalStateException ignored) { return resumeAtMs; }
    }
    private void saveCheckpoint() {
        if (isClosing && current() == null) return;
        PlaybackBookmark.save(this, queue, navigator.index(), currentPosition());
    }
    private void saveNewSongCheckpoint() {
        PlaybackBookmark.save(this, queue, navigator.index(), 0);
    }
    private void savePlaybackPosition() {
        if (current() != null) PlaybackBookmark.savePosition(this, currentPosition());
    }
    private void scheduleCheckpoint() {
        mainThread.removeCallbacks(checkpointTask);
        if (playing && !isClosing) mainThread.postDelayed(checkpointTask, 15_000L);
    }
    public int duration() {
        try { return player != null && !preparing ? Math.max(0, player.getDuration()) : (current() == null ? 0 : (int) current().durationMs); }
        catch (IllegalStateException ignored) { return current() == null ? 0 : (int) current().durationMs; }
    }

    public void playQueue(List<Track> songs, int index) {
        if (songs == null || index < 0 || index >= songs.size()) return;
        queue.clear();
        queue.addAll(songs);
        navigator.load(queue.size(), index);
        resumeAtMs = 0;
        saveNewSongCheckpoint();
        open(true);
    }

    /** Queue editing never resets the current MediaPlayer or changes pause state. */
    public boolean playNext(Track song) {
        if (isClosing || song == null) return false;
        Track selected = current();
        if (selected != null && selected.uri.equals(song.uri)) return false;
        if (selected == null) return selectFirstQueued(song);
        for (int i = queue.size() - 1; i >= 0; i--) {
            if (i != navigator.index() && queue.get(i).uri.equals(song.uri)) {
                navigator.removeQueued(i); queue.remove(i);
            }
        }
        int position = navigator.index() + 1;
        queue.add(position, song);
        navigator.insertQueued(position, true);
        saveCheckpoint(); publish();
        return true;
    }

    /** Append once; an empty queue selects the song without starting audio. */
    public boolean enqueue(Track song) {
        if (isClosing || song == null) return false;
        for (Track queued : queue) if (queued.uri.equals(song.uri)) return false;
        if (current() == null) return selectFirstQueued(song);
        int position = queue.size();
        queue.add(song); navigator.insertQueued(position, false);
        saveCheckpoint(); publish();
        return true;
    }

    private boolean selectFirstQueued(Track song) {
        releasePlayer();
        queue.clear(); queue.add(song); navigator.load(1, 0);
        resumeAtMs = 0; playWhenReady = false; lastError = "";
        giveUpAudioFocus(); saveCheckpoint(); publish();
        return true;
    }

    public boolean removeQueueItem(int position) {
        if (isClosing || !navigator.removeQueued(position)) return false;
        queue.remove(position);
        saveCheckpoint(); publish();
        return true;
    }

    /** Keep the current song playing, while removing every other queued song. */
    public int clearUpcoming() {
        if (isClosing || current() == null) return 0;
        Track selected = current();
        int removed = navigator.keepCurrentOnly();
        queue.clear(); queue.add(selected);
        saveCheckpoint(); publish();
        return removed;
    }

    /** Restore probe state without briefly playing or resolving its previous queue. */
    void restorePausedQueueForProbe(List<Track> songs, int index, int offsetMs) {
        if (isClosing || (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return;
        releasePlayer();
        queue.clear();
        if (songs != null) queue.addAll(songs);
        navigator.load(queue.size(), queue.isEmpty() ? 0 : Math.max(0, Math.min(queue.size() - 1, index)));
        resumeAtMs = Math.max(0, offsetMs);
        playWhenReady = false;
        lastError = "";
        giveUpAudioFocus();
        saveCheckpoint();
        publish();
    }

    private boolean gainAudioFocus() {
        if (focusRequest == null) focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(change -> {
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause();
                else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) safeVolume(0.3f);
                else if (change == AudioManager.AUDIOFOCUS_GAIN) safeVolume(1f);
            }).build();
        return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void giveUpAudioFocus() {
        if (focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
    }
    private void safeVolume(float level) {
        try { if (player != null && !preparing) player.setVolume(level, level); }
        catch (IllegalStateException ignored) { }
    }
    private void readSourceMetadata(Track song) {
        metadataWorker.execute(() -> {
            MediaMetadataRetriever reader = new MediaMetadataRetriever();
            int bitrate = 0, sampleRate = 0;
            try {
                reader.setDataSource(this, Uri.parse(song.uri));
                bitrate = parseSpec(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
                if (Build.VERSION.SDK_INT >= 29)
                    sampleRate = parseSpec(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE));
            } catch (Exception ignored) { }
            finally { try { reader.release(); } catch (Exception ignored) { } }
            final int resolvedBitrate = bitrate, resolvedRate = sampleRate;
            mainThread.post(() -> {
                if (isClosing) return;
                song.updateSourceFormat(resolvedBitrate, resolvedRate);
                if (current() == song) publish();
            });
        });
    }
    private static int parseSpec(String raw) {
        try { int value = Integer.parseInt(raw); return value > 0 ? value : 0; }
        catch (Exception ignored) { return 0; }
    }
    private void releasePlayer() {
        releaseEqualizer();
        onlineSerial++;
        cancelFailureAdvance();
        cancelPrepareDeadline();
        cancelPlaybackConfirmation();
        MediaPlayer previous = player;
        player = null;
        if (previous != null) {
            try { previous.reset(); } catch (RuntimeException ignored) {}
            try { previous.release(); } catch (RuntimeException ignored) {}
        }
        playing = false;
        preparing = false;
        mainThread.removeCallbacks(checkpointTask);
    }

    private boolean currentAttempt(Track song, int serial) {
        return !isClosing && serial == onlineSerial && current() == song;
    }
    private void cancelFailureAdvance() {
        if (failureAdvance != null) mainThread.removeCallbacks(failureAdvance);
        failureAdvance = null;
    }
    private void cancelPrepareDeadline() {
        if (prepareDeadline != null) mainThread.removeCallbacks(prepareDeadline);
        prepareDeadline = null;
    }
    private void cancelPlaybackConfirmation() {
        if (playbackConfirmation != null) mainThread.removeCallbacks(playbackConfirmation);
        playbackConfirmation = null;
    }
    private void prepareDeadline(Track song, int serial, MediaPlayer expected, long waitMs, String message) {
        cancelPrepareDeadline();
        prepareDeadline = () -> {
            if (!currentAttempt(song, serial) || !preparing ||
                (expected != null && expected != player)) return;
            failCurrent(song, serial, message);
        };
        mainThread.postDelayed(prepareDeadline, waitMs);
    }
    private void confirmPlayback(Track song, int serial, MediaPlayer expected, int startOffset) {
        cancelPlaybackConfirmation();
        if (song.isRadio()) return;
        final int initialPosition = Math.max(startOffset, currentPosition());
        playbackConfirmation = new Runnable() {
            @Override public void run() {
                if (!currentAttempt(song, serial) || expected != player || !playing || !playWhenReady) return;
                // Prepared/start alone is not success: corrupt media can fail immediately afterward.
                if (currentPosition() > initialPosition) {
                    navigator.resetFailurePass();
                    playbackConfirmation = null;
                } else mainThread.postDelayed(this, PLAYBACK_CONFIRM_MS);
            }
        };
        mainThread.postDelayed(playbackConfirmation, PLAYBACK_CONFIRM_MS);
    }
    private void failCurrent(Track song, int serial, String reason) {
        if (!currentAttempt(song, serial)) return;
        boolean advance = playWhenReady && !song.isRadio();
        String error = reason == null || reason.trim().isEmpty() ? "音源暂时不可用" : reason.trim();
        if (error.length() > 160) error = error.substring(0, 160);
        releasePlayer();
        resumeAtMs = 0;
        if (!advance) {
            lastError = error;
            playWhenReady = false;
            giveUpAudioFocus();
            saveNewSongCheckpoint();
            publish();
            return;
        }
        int next = navigator.nextAfterFailure();
        if (next < 0) {
            lastError = (navigator.mode() == QueueNavigator.Mode.ORDER ?
                "后续歌曲均无法播放，已停止" : "队列中没有可播放的歌曲，已停止") +
                "（跳过 " + navigator.failureCount() + " 首）：" + error;
            playWhenReady = false;
            giveUpAudioFocus();
            saveNewSongCheckpoint();
            publish();
            return;
        }
        lastError = "已跳过「" + song.title + "」：" + error;
        preparing = true; // The delayed next attempt is cancellable with the same pause action.
        saveNewSongCheckpoint();
        final int pendingSerial = onlineSerial;
        final Track pendingSong = current();
        failureAdvance = () -> {
            failureAdvance = null;
            if (!currentAttempt(pendingSong, pendingSerial) || !playWhenReady) return;
            open(true);
        };
        mainThread.postDelayed(failureAdvance, FAILURE_SKIP_DELAY_MS);
        publish();
    }

    private void open(boolean autoplay) {
        if (isClosing) return;
        Track song = current();
        releasePlayer();
        final int attemptSerial = onlineSerial;
        lastError = "";
        playWhenReady = autoplay;
        if (song == null) { publish(); return; }
        if (song.isOnline()) song.setCurrentSourceQuality(false, "", 0, false);
        if (autoplay && !gainAudioFocus()) { lastError="音频焦点暂时被其他应用占用，请关闭其他播放器后重试"; playWhenReady = false; publish(); return; }
        if(song.isRadio()){
            if(!RadioDirectory.trustedStream(song.uri)){
                failCurrent(song,attemptSerial,"电台直播链接不安全或不可用");return;
            }
            startResolved(song,song.uri,attemptSerial);
            return;
        }
        if (song.isOnline()) {
            if (!OnlineApi.paired(this)) {
                failCurrent(song,attemptSerial,"在线曲库服务不可用");
                return;
            }
            preparing = true;
            int requestSerial = attemptSerial;
            prepareDeadline(song,requestSerial,null,ONLINE_RESOLVE_TIMEOUT_MS,"在线音源解析超时");
            publish();
            onlineWorker.execute(() -> {
                try {
                    JSONObject stream=OnlineApi.call(this,"songUrl",OnlineApi.songParams(song,OnlineApi.quality(this)));
                    String url=stream.optString("url","").trim();
                    if(url.isEmpty())throw new java.io.IOException("歌曲暂无可用音源，可能需要账号播放权限");
                    Uri parsed=Uri.parse(url);
                    String host=parsed.getHost()==null?"":parsed.getHost().toLowerCase(java.util.Locale.ROOT);
                    boolean validHost=host.equals("kugou.com")||host.endsWith(".kugou.com")
                        ||host.equals("kugoucdn.com")||host.endsWith(".kugoucdn.com");
                    boolean validProtocol="https".equals(parsed.getScheme())||"http".equals(parsed.getScheme());
                    if(!validHost||!validProtocol||url.length()>8192||parsed.getUserInfo()!=null)
                        throw new java.io.IOException("在线音源地址安全校验失败");
                    JSONObject quality=stream.optJSONObject("quality");
                    String label=quality==null?"在线音源 · 未验证格式":quality.optString("label","在线音质待验证");
                    boolean verified=quality!=null && quality.optBoolean("streamVerified",false);
                    boolean preview=stream.optBoolean("preview");
                    String format=quality==null?"":quality.optString("format","").toLowerCase(java.util.Locale.ROOT);
                    final double sourceBitrateKbps=quality==null?0:quality.optDouble("bitrateKbps",0);
                    boolean actualLossless=verified&&!preview&&("flac".equals(format)||"wav".equals(format));
                    String preferred=quality==null?"":quality.optString("mode","");
                    boolean preferLossless="auto".equals(preferred)||"lossless".equals(preferred)||"lossless_only".equals(preferred);
                    boolean unencrypted="http".equals(parsed.getScheme());
                    String resolvedAudio = "音质待确认";
                    if (verified) {
                        StringBuilder actual = new StringBuilder(format.isEmpty() ? "音频" : format.toUpperCase(java.util.Locale.ROOT));
                        int bits = quality.optInt("bitDepth", 0);
                        int hz = quality.optInt("sampleRateHz", 0);
                        double bitrate = quality.optDouble("bitrateKbps", 0);
                        if (bits > 0) actual.append(" · ").append(bits).append(" bit");
                        if (hz > 0) actual.append(" · ").append(String.format(java.util.Locale.ROOT, "%.1f kHz", hz / 1000.0));
                        if (!"flac".equals(format) && !"wav".equals(format) && bitrate > 0)
                            actual.append(" · ").append(Math.round(bitrate)).append(" kbps");
                        if (preview) actual.append(" · 试听");
                        if (preferLossless && !actualLossless) actual.append(" · 已降级");
                        resolvedAudio = actual.toString();
                    }
                    final String actualFormat = resolvedAudio;
                    String text=(verified?"已验证 ":"待确认 ")+label+
                        (verified&&preferLossless&&!actualLossless?" · 已降级，非无损":"")+
                        (preview?" · 试听片段":"")+
                        (unencrypted?" · HTTP传输未加密":"");
                    mainThread.post(() -> {
                        if(!currentAttempt(song,requestSerial))return;
                        song.setOnlineSpec(text);
                        song.setActualAudioFormat(actualFormat);
                        song.setCurrentSourceQuality(verified,format,sourceBitrateKbps,preview);
                        preparing=false;
                        startResolved(song,url,requestSerial);
                    });
                }catch(Exception failure){
                    final String error=failure.getMessage()==null?"音源解析失败":failure.getMessage();
                    mainThread.post(()->{
                        failCurrent(song,requestSerial,error);
                    });
                }
            });
            return;
        }
        startResolved(song,song.uri,attemptSerial);
    }

    private void startResolved(Track song, String sourceUrl, int serial) {
        if (!currentAttempt(song, serial)) return;
        cancelPrepareDeadline();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            if (song.isOnline()) {
                java.util.Map<String,String> headers=new java.util.HashMap<>();
                headers.put("User-Agent","Mozilla/5.0 (Linux; Android) QingyinMobile/0.5");
                headers.put("Accept-Encoding","identity");
                player.setDataSource(this,Uri.parse(sourceUrl),headers);
            }
            else player.setDataSource(this, Uri.parse(sourceUrl));
            preparing = true;
            player.setOnPreparedListener(p -> {
                if (p != player || !currentAttempt(song, serial)) return;
                cancelPrepareDeadline();
                try {
                    preparing = false;
                    int requestedResume = resumeAtMs;
                    if (requestedResume > 0) {
                        try { p.seekTo(requestedResume); } catch (IllegalStateException ignored) { }
                        resumeAtMs = 0;
                    }
                    if(!applyEqualizer()){
                        effectPreset=0;
                        getSharedPreferences("playback",MODE_PRIVATE).edit().putInt("effect",0).apply();
                    }
                    if (playWhenReady) {
                        p.start(); playing = true;
                        confirmPlayback(song, serial, p, requestedResume);
                        if(!song.isRadio()){
                            rememberPlayed(song.id); OnlineLibraryStore.remember(this,song);
                            scheduleCheckpoint();
                        }
                    }
                    if (!song.isOnline()&&!song.isRadio()) readSourceMetadata(song);
                    if(!song.isRadio()){
                        if (requestedResume > 0)
                            PlaybackBookmark.save(this, queue, navigator.index(), requestedResume);
                        else saveCheckpoint();
                    }
                    publish();
                } catch (RuntimeException failure) {
                    failCurrent(song, serial, "音频无法开始播放（" + failure.getClass().getSimpleName() + "）");
                }
            });
            player.setOnCompletionListener(p -> {
                if (p != player || !currentAttempt(song, serial)) return;
                if (!playWhenReady) { playing=false;publish();return; }
                if (!song.isRadio() && currentPosition() <= 0) {
                    failCurrent(song,serial,"音频内容为空或未能正常播放");return;
                }
                navigator.resetFailurePass();
                if(song.isRadio()){
                    releasePlayer();playWhenReady=false;giveUpAudioFocus();publish();return;
                }
                int next = navigator.step(1, true);
                resumeAtMs = 0;
                if (next < 0) { releasePlayer(); playWhenReady = false; giveUpAudioFocus(); saveNewSongCheckpoint(); publish(); }
                else { saveNewSongCheckpoint(); open(true); }
            });
            player.setOnErrorListener((p, what, extra) -> {
                if (p != player || !currentAttempt(song, serial)) return true;
                failCurrent(song,serial,(song.isRadio()?"电台直播流":"音频")+
                    "无法解码或读取（媒体错误 " + what + "/" + extra + "）");
                return true;
            });
            prepareDeadline(song,serial,player,MEDIA_PREPARE_TIMEOUT_MS,"音频加载超时");
            player.prepareAsync();
            publish();
        } catch (Exception e) {
            failCurrent(song,serial,"音源无法初始化（"+e.getClass().getSimpleName()+"）");
        }
    }

    public void pause() {
        playWhenReady = false;
        boolean pendingSkip = failureAdvance != null;
        cancelFailureAdvance();
        cancelPlaybackConfirmation();
        if (pendingSkip) lastError = ""; // The selected next song has not failed or even been attempted.
        // Cancel both unresolved streams and decoder preparation. Late callbacks
        // then have an obsolete serial and cannot revive a paused/stopped queue.
        if (preparing) releasePlayer();
        if (player != null && playing) {
            try { player.pause(); } catch (IllegalStateException ignored) {}
        }
        playing = false;
        giveUpAudioFocus();
        scheduleCheckpoint();
        savePlaybackPosition();
        publish();
    }
    public void resume() {
        if (isClosing) return;
        if (current() == null) return;
        if (!gainAudioFocus()) return;
        navigator.resetFailurePass();
        cancelFailureAdvance();
        playWhenReady = true;
        if (player == null) { open(true); return; }
        if (!preparing) {
            int startOffset = currentPosition();
            try { player.start(); playing = true; scheduleCheckpoint();
                confirmPlayback(current(),onlineSerial,player,startOffset); }
            catch (RuntimeException failure) {
                failCurrent(current(),onlineSerial,"音频无法继续播放（"+failure.getClass().getSimpleName()+"）");return;
            }
        }
        publish();
    }
    public void toggle() { if (playing || (preparing && playWhenReady)) pause(); else resume(); }
    public void skip(int amount) {
        if (queue.isEmpty()) return;
        int next = navigator.step(amount, false);
        if (next < 0) return;
        navigator.resetFailurePass();
        resumeAtMs = 0;
        saveNewSongCheckpoint();
        open(true);
    }
    private void rememberPlayed(long id) {
        android.content.SharedPreferences prefs = getSharedPreferences("library", MODE_PRIVATE);
        String old = prefs.getString("history", "");
        ArrayList<String> ids = new ArrayList<>();
        String value = Long.toString(id);
        ids.add(value);
        for (String entry : old.split(",")) if (!entry.isEmpty() && !entry.equals(value) && ids.size() < 60) ids.add(entry);
        prefs.edit().putString("history", android.text.TextUtils.join(",", ids)).apply();
    }
    public void seekTo(long milliseconds) {
        if (player == null || preparing || (current()!=null&&current().isRadio())) return;
        int target = Math.max(0, Math.min(duration(), (int) milliseconds));
        player.seekTo(target);
        PlaybackBookmark.savePosition(this, target);
        publish();
    }

    public boolean lockscreenLyricsEnabled() { return lockLyricsEnabled; }

    public void setLockscreenLyricsEnabled(boolean enabled) {
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        lockLyricsEnabled = enabled;
        getSharedPreferences("playback", MODE_PRIVATE).edit().putBoolean("lock_lyrics", enabled).apply();
        mainThread.removeCallbacks(lyricTicker);
        if (enabled) {
            lyricTrack = null; lastLyric = "";
            prepareLockLyrics(current());
            mainThread.post(lyricTicker);
        } else {
            lockLyrics = Collections.emptyList(); lyricTrack = null; lastLyric = "";
            getSystemService(NotificationManager.class).cancel(LYRICS_ID);
        }
    }

    private void preparePresentation(Track song) {
        String nextUrl = song == null ? "" : song.coverUrl;
        if (song != presentedTrack || !nextUrl.equals(artworkUrl)) {
            presentedTrack = song; artworkUrl = nextUrl; mediaArtwork = null;
            if (song != null && nextUrl.isEmpty() && !song.isOnline() && !song.isRadio()) {
                metadataWorker.execute(() -> {
                    Bitmap embedded = null;
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(this, Uri.parse(song.uri));
                        byte[] picture = retriever.getEmbeddedPicture();
                        if (picture != null && picture.length > 0 && picture.length <= 2_000_000) {
                            BitmapFactory.Options bounds = new BitmapFactory.Options();
                            bounds.inJustDecodeBounds = true;
                            BitmapFactory.decodeByteArray(picture, 0, picture.length, bounds);
                            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                                BitmapFactory.Options opts = new BitmapFactory.Options();
                                int sample = 1;
                                while (bounds.outWidth / sample > 360 || bounds.outHeight / sample > 360) sample *= 2;
                                opts.inSampleSize = sample;
                                embedded = BitmapFactory.decodeByteArray(picture, 0, picture.length, opts);
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        try { retriever.release(); } catch (Exception ignored) { }
                    }
                    final Bitmap result = embedded;
                    mainThread.post(() -> {
                        if (isClosing || current() != song || !artworkUrl.isEmpty()) return;
                        mediaArtwork = Artwork.glassCover(result);
                        publish();
                    });
                });
            }
            if (song != null && !nextUrl.isEmpty()) {
                Artwork.loadBitmap(nextUrl, bitmap -> {
                    if (isClosing || song != current() || !nextUrl.equals(artworkUrl)) return;
                    if (bitmap != null) {
                        int width = bitmap.getWidth(), height = bitmap.getHeight();
                        if (width > 0 && height > 0) {
                            float factor = Math.min(1f, 280f / Math.max(width, height));
                            Bitmap scaled = factor < 1f ? Bitmap.createScaledBitmap(bitmap,
                                Math.max(1, Math.round(width * factor)), Math.max(1, Math.round(height * factor)), true) : bitmap;
                            mediaArtwork = Artwork.glassCover(scaled);
                        }
                    }
                    publish();
                });
            }
        }
        if (lockLyricsEnabled && song != lyricTrack) prepareLockLyrics(song);
    }

    private void prepareLockLyrics(Track song) {
        lyricTrack = song; lockLyrics = Collections.emptyList(); lastLyric = "";
        getSystemService(NotificationManager.class).cancel(LYRICS_ID);
        if (song == null || !song.isOnline()) return;
        onlineWorker.execute(() -> {
            List<LyricsParser.Line> lines = Collections.emptyList();
            try {
                JSONObject params = new JSONObject();
                params.put("hash", Uri.parse(song.uri).getLastPathSegment());
                params.put("title", song.title);
                params.put("duration", Math.max(0, song.durationMs / 1000));
                lines = LyricsParser.parse(OnlineApi.call(getApplicationContext(), "lyrics", params).optString("text", ""));
            } catch (Exception ignored) { }
            final List<LyricsParser.Line> result = lines;
            mainThread.post(() -> {
                if (isClosing || !lockLyricsEnabled || song != lyricTrack || song != current()) return;
                lockLyrics = result; refreshLockLyric();
            });
        });
    }

    private void refreshLockLyric() {
        if (!lockLyricsEnabled) return;
        Track song = current();
        if (song == null || song != lyricTrack || lockLyrics.isEmpty() ||
            (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            if (!lastLyric.isEmpty()) {
                lastLyric = ""; getSystemService(NotificationManager.class).cancel(LYRICS_ID);
            }
            return;
        }
        int index = LyricsParser.indexAt(lockLyrics, currentPosition());
        String line = index >= 0 ? lockLyrics.get(index).text : "";
        if (line.equals(lastLyric)) return;
        lastLyric = line;
        if (line.isEmpty()) { getSystemService(NotificationManager.class).cancel(LYRICS_ID); return; }
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, NowPlayingActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder card = new Notification.Builder(this, LYRICS_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_qingyin).setContentTitle(song.title).setContentText(line)
            .setStyle(new Notification.BigTextStyle().bigText(line)).setSubText("青音 · 同步歌词")
            .setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setContentIntent(open);
        getSystemService(NotificationManager.class).notify(LYRICS_ID, card.build());
    }

    private PendingIntent command(String action, int requestCode) {
        Intent intent = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private int artworkTint() {
        if (mediaArtwork == null) return -8366229;
        int width = mediaArtwork.getWidth(), height = mediaArtwork.getHeight();
        if (width < 1 || height < 1) return -8366229;
        long red = 0, green = 0, blue = 0;
        for (int y = 1; y <= 6; y++) for (int x = 1; x <= 6; x++) {
            int pixel = mediaArtwork.getPixel(Math.min(width - 1, x * width / 7), Math.min(height - 1, y * height / 7));
            red += Color.red(pixel); green += Color.green(pixel); blue += Color.blue(pixel);
        }
        return Color.rgb((int)Math.min(170, red / 36), (int)Math.min(170, green / 36), (int)Math.min(170, blue / 36));
    }

    private Notification notification() {
        Track song = current();
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, song == null ? IndexActivity.class : NowPlayingActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        boolean active = playing || (preparing && playWhenReady);
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_qingyin)
            .setContentTitle(song != null ? song.title : "青音")
            .setContentText(song != null ? song.artist : "准备好播放你的音乐")
            .setContentIntent(open)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(active)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setColor(artworkTint()).setColorized(mediaArtwork != null)
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous, "上一首", command(ACTION_PREV, 11)).build())
            .addAction(new Notification.Action.Builder(active ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                active ? "暂停" : "播放", command(active ? ACTION_PAUSE : ACTION_PLAY, 12)).build())
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next, "下一首", command(ACTION_NEXT, 13)).build())
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "停止", command(ACTION_STOP, 14)).build())
            .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2));
        if (song != null && mediaArtwork != null) b.setLargeIcon(mediaArtwork);
        return b.build();
    }

    private void publish() {
        Track song = current();
        preparePresentation(song);
        int state = playing ? PlaybackState.STATE_PLAYING : preparing ? PlaybackState.STATE_BUFFERING : PlaybackState.STATE_PAUSED;
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE |
                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO;
        session.setPlaybackState(new PlaybackState.Builder()
            .setActions(actions)
            .setState(current() == null ? PlaybackState.STATE_NONE : state, currentPosition(), playing ? 1f : 0f, SystemClock.elapsedRealtime())
            .build());
        MediaMetadata.Builder meta = new MediaMetadata.Builder();
        if (song != null) {
            meta.putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist).putLong(MediaMetadata.METADATA_KEY_DURATION, duration());
            if (mediaArtwork != null) meta.putBitmap(MediaMetadata.METADATA_KEY_ART, mediaArtwork)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mediaArtwork).putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mediaArtwork);
        }
        session.setMetadata(meta.build());
        if (started) getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification());
        if (listener != null) listener.changed();
    }

    @Override public void onDestroy() {
        savePlaybackPosition();
        isClosing = true;
        listener = null;
        releasePlayer();
        if (focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
        if (noisyRegistered) unregisterReceiver(noisyReceiver);
        metadataWorker.shutdownNow();
        onlineWorker.shutdownNow();
        mainThread.removeCallbacksAndMessages(null);
        getSystemService(NotificationManager.class).cancel(LYRICS_ID);
        sleepDeadline = 0;
        if (session != null) { session.setActive(false); session.release(); }
        super.onDestroy();
    }
}
