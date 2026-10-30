package local.qingyin.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** Actual-service regression fixtures, reachable only through the protected debug receiver. */
final class PlaybackFailureProbe {
    private static final String TAG = "QingyinProbe";
    private static boolean running;

    static void run(Context app, String requested) {
        String scenario = requested == null ? "mixed" : requested;
        if (!java.util.Arrays.asList("mixed", "all", "orderedEnd", "paused", "pendingPause", "superseded", "shuffle", "editing").contains(scenario)) return;
        if (running) { Log.w(TAG, "SKIP_PROBE_BUSY=true"); return; }
        running = true;
        app.startForegroundService(new Intent(app, PlaybackService.class));
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                PlaybackService service = ((PlaybackService.LocalBinder) binder).service();
                new Run(app, service, this, scenario).start();
            }
            @Override public void onServiceDisconnected(ComponentName name) { running = false; }
        };
        if (!app.bindService(new Intent(app, PlaybackService.class), connection, Context.BIND_AUTO_CREATE)) running = false;
    }

    private static final class Run {
        final Context app;
        final PlaybackService service;
        final ServiceConnection connection;
        final String scenario;
        final Handler ui = new Handler(Looper.getMainLooper());
        final List<Track> original;
        final int originalIndex, originalPosition;
        final QueueNavigator.Mode originalMode;
        final String originalHistory;
        final boolean wasPlaying;
        Track good, first, prioritySong, tailSong;
        int editingStage;
        long started, stableSince;
        int previousIndex = -2, transitions;
        boolean finished;

        Run(Context app, PlaybackService service, ServiceConnection connection, String scenario) {
            this.app = app; this.service = service; this.connection = connection; this.scenario = scenario;
            original = service.queueSnapshot(); originalIndex = original.indexOf(service.current());
            originalPosition = service.currentPosition(); originalMode = service.mode();
            wasPlaying = service.isPlaying();
            originalHistory = app.getSharedPreferences("library", Context.MODE_PRIVATE).getString("history", "");
        }

        void mode(QueueNavigator.Mode wanted) {
            for (int i = 0; i < QueueNavigator.Mode.values().length && service.mode() != wanted; i++) service.cycleMode();
        }

        void start() {
            // restorePausedQueueForProbe also restores an originally empty queue.
            try {
                File goodFile = new File(app.getCacheDir(), "skip-probe-good.wav");
                int samples = 8000 * 30, bytes = samples * 2;
                ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
                header.put(new byte[]{'R','I','F','F'}).putInt(36 + bytes).put(new byte[]{'W','A','V','E'});
                header.put(new byte[]{'f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1);
                header.putInt(8000).putInt(16000).putShort((short)2).putShort((short)16);
                header.put(new byte[]{'d','a','t','a'}).putInt(bytes);
                try (FileOutputStream out = new FileOutputStream(goodFile)) {
                    out.write(header.array()); byte[] silence = new byte[8000];
                    for (int remaining = bytes; remaining > 0; remaining -= silence.length) out.write(silence, 0, Math.min(remaining, silence.length));
                }
                File brokenFile = new File(app.getCacheDir(), "skip-probe-broken.wav");
                try (FileOutputStream out = new FileOutputStream(brokenFile)) { out.write(new byte[64]); }
                good = new Track(-71004, "自动跳过测试 · 有效音频", "静音测试", "audio/wav", 30000, Uri.fromFile(goodFile), 128000);
                if("editing".equals(scenario)){
                    File priorityFile=new File(app.getCacheDir(),"queue-probe-priority.wav");
                    File tailFile=new File(app.getCacheDir(),"queue-probe-tail.wav");
                    java.nio.file.Files.copy(goodFile.toPath(),priorityFile.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.copy(goodFile.toPath(),tailFile.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    prioritySong=new Track(-71005,"队列测试 · 下一首","静音测试","audio/wav",30000,Uri.fromFile(priorityFile),128000);
                    tailSong=new Track(-71006,"队列测试 · 队尾","静音测试","audio/wav",30000,Uri.fromFile(tailFile),128000);
                }
                Track missing = new Track(-71002, "自动跳过测试 · 缺失文件", "静音测试", "audio/wav", 0,
                    Uri.fromFile(new File(app.getCacheDir(), "skip-probe-nonexistent.wav")), 0);
                Track broken = new Track(-71003, "自动跳过测试 · 损坏音频", "静音测试", "audio/wav", 0, Uri.fromFile(brokenFile), 0);
                Track online = new Track(-71001, "自动跳过测试 · 解析拒绝", "静音测试", "audio/online", 0,
                    Uri.parse("qingyin-online://track/invalid-probe-hash?albumId=0&audioId=0"), 0);
                List<Track> fixture = new ArrayList<>();
                fixture.add(online); fixture.add(missing); fixture.add(broken);
                if (!"all".equals(scenario)) fixture.add(good);
                mode("shuffle".equals(scenario) ? QueueNavigator.Mode.SHUFFLE : QueueNavigator.Mode.ONE);
                if ("orderedEnd".equals(scenario)) {
                    fixture.clear(); fixture.add(good); fixture.add(missing); mode(QueueNavigator.Mode.ORDER);
                } else if ("pendingPause".equals(scenario)) {
                    fixture.clear(); fixture.add(missing); fixture.add(good);
                } else if ("paused".equals(scenario) || "superseded".equals(scenario)) {
                    fixture.clear(); fixture.add(online); fixture.add(good);
                }
                first = fixture.get("orderedEnd".equals(scenario) ? 1 : 0);
                if("editing".equals(scenario)){fixture.clear();fixture.add(good);first=good;}
                started = stableSince = SystemClock.elapsedRealtime();
                service.playQueue(fixture, "orderedEnd".equals(scenario) ? 1 : 0);
                if ("paused".equals(scenario) || "pendingPause".equals(scenario)) service.pause();
                if ("superseded".equals(scenario)) service.playQueue(java.util.Collections.singletonList(good), 0);
                Log.i(TAG, "SKIP_PROBE_START scenario=" + scenario);
                ui.postDelayed(this::poll, 200);
            } catch (Exception failure) { finish(false, "FIXTURE_" + failure.getClass().getSimpleName()); }
        }

        void poll() {
            if (finished) return;
            long now = SystemClock.elapsedRealtime(), elapsed = now - started;
            List<Track> queue = service.queueSnapshot();
            int index = queue.indexOf(service.current());
            if (index != previousIndex) {
                transitions++; previousIndex = index; stableSince = now;
                Log.i(TAG, "SKIP_PROBE_TRANSITION scenario=" + scenario + " index=" + index);
            }
            boolean stopped = !service.isPlaying() && !service.isPreparing();
            boolean error = !service.lastError().isEmpty();
            if("editing".equals(scenario)){
                if(editingStage==0&&service.current()==good&&service.isPlaying()&&service.currentPosition()>2200){
                    int position=service.currentPosition();
                    boolean added=service.enqueue(tailSong)&&!service.enqueue(tailSong)&&service.playNext(prioritySong);
                    boolean uninterrupted=service.current()==good&&service.isPlaying()&&service.currentPosition()>=position;
                    boolean removed=service.removeQueueItem(service.queueSnapshot().indexOf(prioritySong));
                    boolean cleared=service.clearUpcoming()==1&&service.queueSnapshot().size()==1&&service.current()==good&&service.isPlaying();
                    service.pause();
                    boolean paused=service.enqueue(tailSong)&&service.playNext(prioritySong)&&!service.isPlaying()&&service.current()==good;
                    if(!(added&&uninterrupted&&removed&&cleared&&paused)){finish(false,"QUEUE_EDIT_INTERRUPTED");return;}
                    mode(QueueNavigator.Mode.ONE);service.resume();service.seekTo(Math.max(0,service.duration()-500));editingStage=1;
                }else if(editingStage==1&&service.current()==prioritySong&&service.isPlaying()&&service.currentPosition()>1700){
                    finish(service.mode()==QueueNavigator.Mode.ONE,"QUEUE_EDIT_AND_AUTO_PRIORITY_PASSED");return;
                }
                if(elapsed>20000){finish(false,"QUEUE_EDIT_TIMEOUT");return;}
                ui.postDelayed(this::poll,200);return;
            }
            if ("paused".equals(scenario) && elapsed > 3500 && stopped) {
                finish(service.current() == first && !service.isPlaying(), "PAUSE_HELD"); return;
            }
            if ("pendingPause".equals(scenario) && elapsed > 2000) {
                finish(service.current() == good && stopped && !error, "DELAYED_SKIP_PAUSE_HELD"); return;
            }
            if (("all".equals(scenario) || "orderedEnd".equals(scenario)) && stopped && error && now - stableSince > 1800) {
                finish("all".equals(scenario) ? index == 2 : service.current() == first, "BOUNDED_STOP"); return;
            }
            if (!"all".equals(scenario) && !"orderedEnd".equals(scenario) && !"paused".equals(scenario) && !"pendingPause".equals(scenario)
                    && service.current() == good && service.isPlaying() && service.currentPosition() > 2200) {
                finish(service.lastError().isEmpty(), "VALID_AUDIO_ADVANCED"); return;
            }
            if (elapsed > 65000 || transitions > 8) { finish(false, "TIMEOUT_OR_LOOP"); return; }
            ui.postDelayed(this::poll, 200);
        }

        void finish(boolean passed, String reason) {
            if (finished) return; finished = true;
            Log.i(TAG, "SKIP_PROBE_RESULT scenario=" + scenario + " passed=" + passed + " reason=" + reason
                + " transitions=" + transitions + " positionAdvanced=" + (service.current() == good && service.currentPosition() > 2200));
            service.pause(); mode(originalMode);
            service.restorePausedQueueForProbe(original, Math.max(0, originalIndex), originalPosition);
            if (wasPlaying && !original.isEmpty()) service.resume();
            release();
        }

        void release() {
            if (!original.isEmpty() && originalIndex >= 0)
                PlaybackBookmark.save(app, original, originalIndex, originalPosition);
            app.getSharedPreferences("library", Context.MODE_PRIVATE).edit().putString("history", originalHistory).apply();
            try { app.unbindService(connection); } catch (IllegalArgumentException ignored) { }
            running = false;
            Log.i(TAG, "SKIP_PROBE_RESTORED scenario=" + scenario);
        }
    }
}
