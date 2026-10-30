package local.qingyin.mobile;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONObject;

public final class NowPlayingActivity extends Activity {
    private static final int ACCENT = -18743;
    private static final int REQUEST_LYRICS_NOTIFICATIONS = 8421;
    private static final int SOFT = -2173478;
    private static final int WHITE = -1541;
    private boolean active;
    private VinylRecordView albumCover;
    private LinearLayout artArea;
    private TextView artist;
    private boolean bound;
    private LinearLayout commentAction;
    private TextView commentCount;
    private boolean dragging;
    private TextView elapsed;
    private PlayerGlyph heart;
    private HeartBurstView favoriteBurst;
    private PlayerMenuSheet playerMenu;
    private GestureDetector lyricGestures;
    private TextView lyricHint;
    private LinearLayout lyricsColumn;
    private LinearLayout lyricsPanel;
    private ScrollView lyricsScroll;
    private PlayerGlyph modeBtn;
    private PlayerGlyph playBtn;
    private PlaybackService player;
    private TextView qualityLabel;
    private FrameLayout rootScene;
    private SeekBar seek;
    private ValueAnimator tintAnimation;
    private PlayerBackdropDrawable playerBackdrop;
    private TextView title;
    private TextView total;
    private int currentTint = -8891293;
    private boolean lyricsExpanded = false;
    private long manualScrollUntil = 0;
    private final ArrayList<TextView> lyricViews = new ArrayList<>();
    private List<LyricsParser.Line> lyricLines = Collections.emptyList();
    private String loadedUri = "";
    private String lastDisplayedError = "";
    private int currentLine = -2;
    private long displayedElapsedSecond=-1,displayedRemainingSecond=-1;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private final Runnable ticker = new Runnable() { 
        @Override
        public void run() {
            if (NowPlayingActivity.this.active) {
                NowPlayingActivity.this.updateView();
                NowPlayingActivity.this.main.postDelayed(this, 700L);
            }
        }
    };
    private final Runnable progressTicker=new Runnable(){@Override public void run(){
        if(!active)return;
        updatePosition();
        main.postDelayed(this,bound&&player!=null&&player.isPlaying()?100:400);
    }};
    private final ServiceConnection connection = new ServiceConnection() { 
        @Override
        public void onServiceConnected(ComponentName name, IBinder b) {
            NowPlayingActivity.this.player = ((PlaybackService.LocalBinder) b).service();
            NowPlayingActivity.this.bound = true;
            NowPlayingActivity.this.updateView();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            NowPlayingActivity.this.bound = false;
            NowPlayingActivity.this.player = null;
        }
    };

    public int dp(float n) {
        return QingyinUi.dp(this, n);
    }

    public LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }

    private LinearLayout v() {
        return vertical();
    }

    private GradientDrawable shape(int color, int radius) {
        return QingyinUi.rounded(this, color, radius);
    }

    public TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT, 1);
        }
        return t;
    }

    public LinearLayout horizontal() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(0);
        r.setGravity(16);
        return r;
    }

    public LinearLayout vertical() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(1);
        return r;
    }

    private PlayerGlyph action(String symbol, String description, int size) {
        PlayerGlyph icon = new PlayerGlyph(this, symbol, -1541);
        icon.setBackground(QingyinUi.touch(this, 352321535, 16.0f));
        icon.setVisualSize(size);
        icon.setMinimumWidth(dp(44));
        icon.setMinimumHeight(dp(44));
        icon.setContentDescription(description);
        icon.setClickable(true);
        return icon;
    }

    private void openArtist() {
        Track track = !bound || player == null ? null : player.current();
        openArtist(track);
    }

    private void openArtist(Track track) {
        if (track == null) return;
        String whole = track.artist == null ? "" : track.artist.trim();
        if (track.isRadio() || whole.isEmpty() || "未知歌手".equals(whole)) {
            new AlertDialog.Builder(this).setTitle("歌手信息").setMessage("当前音源暂未提供歌手资料。").setPositiveButton("关闭",null).show();return;
        }
        String[] artists = whole.split("\\s+(?:/|、|&|,|feat\\.?|ft\\.?)\\s+|\\s*、\\s*");
        if (artists.length > 1) {
            String[] choices = Arrays.stream(artists).map(String::trim)
                .filter(name -> !name.isEmpty()).distinct().toArray(String[]::new);
            if (choices.length > 1) {
                new AlertDialog.Builder(this).setTitle("查看哪位歌手？")
                    .setItems(choices, (dialog, selected) -> startArtist(choices[selected],
                        selected == 0 ? track.artistId : 0, track.coverUrl)).show();
                return;
            }
        }
        startArtist(whole, track.artistId, track.coverUrl);
    }

    private void startArtist(String name, long id, String cover) {
        Intent intent = new Intent(this, (Class<?>) ArtistActivity.class);
        intent.putExtra("artist_name", name);
        intent.putExtra("artist_id", id);
        intent.putExtra("artist_cover", cover);
        startActivity(intent);
    }

    private void foreground() {
        Intent i = new Intent(this, (Class<?>) PlaybackService.class);
        startForegroundService(i);
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        HomeWindowInsets.styleStatusBar(getWindow(),true);
        getWindow().setNavigationBarColor(-13030087);
        FrameLayout frameLayout = new FrameLayout(this);
        this.rootScene = frameLayout;
        paintPalette(this.currentTint);
        LinearLayout linearLayoutVertical = vertical();
        linearLayoutVertical.setPadding(dp(24.0f), dp(10.0f), dp(24.0f), dp(20.0f));
        frameLayout.addView(linearLayoutVertical, new FrameLayout.LayoutParams(-1, -1));
        HomeWindowInsets.install(frameLayout,new View[]{linearLayoutVertical},null);
        setContentView(frameLayout);
        LinearLayout top = horizontal();
        PlayerGlyph close = action("down", "收起播放页", 29);
        close.setBackgroundColor(0);
        close.setIconScale(0.76f);
        close.setAlpha(0.8f);
        close.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onClosePlayer(view);
            }
        });
        top.addView(close, new LinearLayout.LayoutParams(dp(46.0f), dp(47.0f)));
        TextView topTitle = label("正在播放", 14, -855638017, false);
        topTitle.setGravity(17);
        top.addView(topTitle, new LinearLayout.LayoutParams(0, dp(47.0f), 1.0f));
        final PlayerGlyph settings = action("more", "播放设置", 24);
        settings.setBackgroundColor(0);
        settings.setIconScale(0.78f);
        settings.setAlpha(0.8f);
        settings.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onOpenPlayerSettings(settings, view);
            }
        });
        top.addView(settings, new LinearLayout.LayoutParams(dp(46.0f), dp(47.0f)));
        linearLayoutVertical.addView(top);
        FrameLayout visualStage = new FrameLayout(this);
        LinearLayout.LayoutParams visualLp = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        visualLp.topMargin = dp(5.0f);
        visualLp.bottomMargin = dp(11.0f);
        linearLayoutVertical.addView(visualStage, visualLp);
        this.artArea = vertical();
        this.artArea.setGravity(17);
        this.artArea.setContentDescription("黑胶唱片与专辑封面，点击切换为同步歌词");
        this.artArea.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onToggleLyrics(view);
            }
        });
        this.albumCover = new VinylRecordView(this);
        this.albumCover.setCoverStyle(playerPreferences().getInt("style",0)==1);
        this.artArea.addView(this.albumCover, new LinearLayout.LayoutParams(-1, -1));
        visualStage.addView(this.artArea, new FrameLayout.LayoutParams(-1, -1));
        this.lyricsPanel = vertical();
        this.lyricsPanel.setBackgroundColor(0);
        this.lyricsPanel.setVisibility(8);
        this.lyricGestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() { 
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (NowPlayingActivity.this.lyricsExpanded) {
                    NowPlayingActivity.this.toggleLyrics();
                    return true;
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!NowPlayingActivity.this.lyricsExpanded || !NowPlayingActivity.this.bound || NowPlayingActivity.this.player == null) {
                    return true;
                }
                int y = (((int) e.getY()) + NowPlayingActivity.this.lyricsScroll.getScrollY()) - NowPlayingActivity.this.lyricsColumn.getTop();
                for (int i = 0; i < NowPlayingActivity.this.lyricViews.size() && i < NowPlayingActivity.this.lyricLines.size(); i++) {
                    TextView row = (TextView) NowPlayingActivity.this.lyricViews.get(i);
                    if (y >= row.getTop() && y < row.getBottom()) {
                        long time = ((LyricsParser.Line) NowPlayingActivity.this.lyricLines.get(i)).millis;
                        NowPlayingActivity.this.player.seekTo((int) Math.min(2147483647L, Math.max(0L, time)));
                        NowPlayingActivity.this.manualScrollUntil = 0L;
                        return true;
                    }
                }
                return true;
            }
        });
        this.lyricsScroll = new ScrollView(this) { 
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                NowPlayingActivity.this.lyricGestures.onTouchEvent(event);
                return super.dispatchTouchEvent(event);
            }
        };
        this.lyricsScroll.setFillViewport(true);
        this.lyricsScroll.setVerticalScrollBarEnabled(false);
        this.lyricsScroll.setVerticalFadingEdgeEnabled(true);
        this.lyricsScroll.setFadingEdgeLength(dp(45.0f));
        this.lyricsScroll.setOnTouchListener(new View.OnTouchListener() { 
            @Override
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return NowPlayingActivity.this.onLyricsTouch(view, motionEvent);
            }
        });
        this.lyricsColumn = vertical();
        this.lyricsColumn.setGravity(17);
        this.lyricsColumn.setPadding(dp(11.0f), dp(42.0f), dp(11.0f), dp(28.0f));
        this.lyricsScroll.addView(this.lyricsColumn);
        this.lyricsPanel.addView(this.lyricsScroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        visualStage.addView(this.lyricsPanel, new FrameLayout.LayoutParams(-1, -1));
        this.lyricHint = label("正在准备歌词…", 17, -2173478, false);
        this.lyricHint.setGravity(17);
        this.lyricsColumn.addView(this.lyricHint, lp(-1, 105));
        LinearLayout linearLayoutHorizontal = horizontal();
        LinearLayout song = v();
        this.title = label("还没有播放歌曲", 22, -1541, false);
        this.title.setTypeface(Typeface.create("sans-serif-medium", 0));
        this.title.setIncludeFontPadding(false);
        this.title.setSingleLine(true);
        this.title.setEllipsize(TextUtils.TruncateAt.END);
        song.addView(this.title);
        this.artist = label("选一首歌，让此刻有声", 15, -2173478, false);
        this.artist.setIncludeFontPadding(false);
        this.artist.setSingleLine(true);
        this.artist.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams a = lp(-1, 23);
        a.topMargin = dp(5.0f);
        song.addView(this.artist, a);
        this.artist.setClickable(true);
        this.artist.setContentDescription("查看歌手资料");
        this.artist.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onOpenArtist(view);
            }
        });
        linearLayoutHorizontal.addView(song, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.heart = action("heart", "收藏当前歌曲", 26);
        this.heart.setBackgroundColor(0);
        this.heart.setIconScale(0.76f);
        this.heart.setAlpha(0.83f);
        this.heart.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onToggleFavorite(view);
            }
        });
        LinearLayout.LayoutParams heartLp = lp(44, 50);
        heartLp.leftMargin = dp(4.0f);
        linearLayoutHorizontal.addView(this.heart, heartLp);
        this.commentAction = vertical();
        this.commentAction.setGravity(17);
        this.commentAction.setBackground(QingyinUi.touch(this, 16777215, 14.0f));
        PlayerGlyph commentIcon = action("comment", "查看酷狗评论", 24);
        commentIcon.setBackgroundColor(0);
        commentIcon.setIconScale(0.76f);
        commentIcon.setAlpha(0.82f);
        commentIcon.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onOpenComments(view);
            }
        });
        commentIcon.setOnClickListener(null);
        commentIcon.setClickable(false);
        commentIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        this.commentAction.addView(commentIcon, lp(42, 30));
        this.commentCount = label("评论", 10, -2173478, false);
        this.commentCount.setGravity(17);
        this.commentCount.setSingleLine(true);
        this.commentAction.addView(this.commentCount, lp(48, 17));
        this.commentAction.setContentDescription("查看当前歌曲的酷狗评论");
        this.commentAction.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onOpenCommentPanel(view);
            }
        });
        LinearLayout.LayoutParams commentLp = lp(50, 50);
        commentLp.leftMargin = dp(2.0f);
        linearLayoutHorizontal.addView(this.commentAction, commentLp);
        linearLayoutVertical.addView(linearLayoutHorizontal, lp(-1, 65));
        this.seek = new SeekBar(this);
        this.seek.setProgressTintList(ColorStateList.valueOf(-571151634));
        this.seek.setProgressBackgroundTintList(ColorStateList.valueOf(1358556656));
        GradientDrawable seekThumb = new GradientDrawable();
        seekThumb.setShape(1);
        seekThumb.setColor(-396556);
        seekThumb.setSize(dp(10.0f), dp(10.0f));
        this.seek.setThumb(seekThumb);
        this.seek.setSplitTrack(false);
        ViewGroup.LayoutParams seekLp = lp(-1, 34);
        linearLayoutVertical.addView(this.seek, seekLp);
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { 
            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                NowPlayingActivity.this.dragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                NowPlayingActivity.this.dragging = false;
                if (!NowPlayingActivity.this.bound || NowPlayingActivity.this.player == null) {
                    return;
                }
                NowPlayingActivity.this.player.seekTo(bar.getProgress());
            }

            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (fromUser) {
                    NowPlayingActivity.this.elapsed.setText(NowPlayingActivity.clock(value));
                }
            }
        });
        LinearLayout times = horizontal();
        this.elapsed = label("00:00", 11, -2173478, false);
        times.addView(this.elapsed, new LinearLayout.LayoutParams(0, dp(25.0f), 1.0f));
        this.qualityLabel = label("音质待确认", 11, -1074928152, false);
        this.qualityLabel.setGravity(17);
        this.qualityLabel.setSingleLine(true);
        this.qualityLabel.setEllipsize(TextUtils.TruncateAt.END);
        times.addView(this.qualityLabel, new LinearLayout.LayoutParams(0, dp(25.0f), 1.6f));
        this.total = label("−00:00", 11, -2173478, false);
        this.total.setGravity(5);
        times.addView(this.total, new LinearLayout.LayoutParams(0, dp(25.0f), 1.0f));
        linearLayoutVertical.addView(times, lp(-1, 24));
        LinearLayout controls = horizontal();
        controls.setGravity(17);
        this.modeBtn = action("repeat", "切换播放模式", 25);
        this.modeBtn.setBackgroundColor(0);
        this.modeBtn.setIconScale(0.72f);
        this.modeBtn.setAlpha(0.66f);
        this.modeBtn.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onCycleMode(view);
            }
        });
        controls.addView(this.modeBtn, new LinearLayout.LayoutParams(0, dp(68.0f), 1.0f));
        PlayerGlyph previous = action("previous", "上一首", 27);
        previous.setBackgroundColor(0);
        previous.setIconScale(0.78f);
        previous.setAlpha(0.77f);
        previous.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onPreviousSong(view);
            }
        });
        controls.addView(previous, new LinearLayout.LayoutParams(0, dp(68.0f), 1.0f));
        this.playBtn = action("play", "播放或暂停", 39);
        this.playBtn.setBackgroundColor(0);
        this.playBtn.setIconScale(0.8f);
        this.playBtn.setAlpha(0.89f);
        this.playBtn.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onTogglePlayback(view);
            }
        });
        controls.addView(this.playBtn, new LinearLayout.LayoutParams(0, dp(72.0f), 1.14f));
        PlayerGlyph next = action("next", "下一首", 27);
        next.setBackgroundColor(0);
        next.setIconScale(0.78f);
        next.setAlpha(0.77f);
        next.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onNextSong(view);
            }
        });
        controls.addView(next, new LinearLayout.LayoutParams(0, dp(68.0f), 1.0f));
        PlayerGlyph queue = action("queue", "歌曲列表", 25);
        queue.setBackgroundColor(0);
        queue.setIconScale(0.72f);
        queue.setAlpha(0.66f);
        queue.setOnClickListener(new View.OnClickListener() { 
            @Override
            public final void onClick(View view) {
                NowPlayingActivity.this.onOpenQueue(view);
            }
        });
        controls.addView(queue, new LinearLayout.LayoutParams(0, dp(68.0f), 1.0f));
        LinearLayout.LayoutParams controlLp = lp(-1, 76);
        controlLp.topMargin = dp(12.0f);
        linearLayoutVertical.addView(controls, controlLp);
        if (saved != null && saved.getBoolean("lyrics_visible", false)) {
            toggleLyrics();
        }
        this.bound = bindService(new Intent(this, (Class<?>) PlaybackService.class), this.connection, 1);
    }

    void onClosePlayer(View v) {
        finish();
    }

    void onOpenPlayerSettings(PlayerGlyph settings, View v) {
        showMore(settings);
    }

    void onToggleLyrics(View v) {
        toggleLyrics();
    }

    boolean onLyricsTouch(View view, MotionEvent event) {
        if (event.getActionMasked() == 0) {
            this.manualScrollUntil = SystemClock.uptimeMillis() + 7000;
            return false;
        }
        return false;
    }

    void onOpenArtist(View v) {
        openArtist();
    }

    void onToggleFavorite(View v) {
        toggleFavorite();
    }

    void onOpenComments(View v) {
        showComments();
    }

    void onOpenCommentPanel(View v) {
        showComments();
    }

    void onCycleMode(View v) {
        if (this.bound && this.player != null) {
            this.player.cycleMode();
            refreshMode();
            Toast.makeText(this, modeName(), 0).show();
        }
    }

    void onPreviousSong(View v) {
        if (!this.bound || this.player == null) {
            return;
        }
        foreground();
        this.player.skip(-1);
    }

    void onTogglePlayback(View v) {
        if (this.bound && this.player != null) {
            foreground();
            this.player.toggle();
            updateView();
        }
    }

    void onNextSong(View v) {
        if (!this.bound || this.player == null) {
            return;
        }
        foreground();
        this.player.skip(1);
    }

    void onOpenQueue(View v) {
        showQueue();
    }

    private String outputDeviceName() {
        AudioManager audio = (AudioManager) getSystemService("audio");
        if (audio != null) {
            for (AudioDeviceInfo device : audio.getDevices(2)) {
                int kind = device.getType();
                if (kind == 8 || kind == 7 || kind == 4 || kind == 3 || kind == 22) {
                    CharSequence name = device.getProductName();
                    return "可用设备 · " + ((name == null || name.length() == 0) ? "耳机" : name.toString());
                }
            }
            return "本机媒体输出";
        }
        return "本机媒体输出";
    }

    private void showOutputInformation() {
        new AlertDialog.Builder(this).setTitle("音频输出").setMessage(outputDeviceName() + "\n实际播放路由由 Android 系统决定，可在系统媒体输出面板切换。").setPositiveButton("蓝牙设备设置", new DialogInterface.OnClickListener() { 
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                NowPlayingActivity.this.onOpenSoundSettings(dialogInterface, i);
            }
        }).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).show();
    }

    void onOpenSoundSettings(DialogInterface d, int w) {
        startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"));
    }

    public void toggleLyrics() {
        this.lyricsExpanded = !this.lyricsExpanded;
        this.artArea.setVisibility(this.lyricsExpanded ? 8 : 0);
        this.lyricsPanel.setVisibility(this.lyricsExpanded ? 0 : 8);
        this.albumCover.setSpinning(!this.lyricsExpanded && this.active && this.bound && this.player != null && this.player.isPlaying());
        View incoming = this.lyricsExpanded ? this.lyricsPanel : this.artArea;
        incoming.setAlpha(0.0f);
        incoming.animate().alpha(1.0f).setDuration(240L).start();
        this.manualScrollUntil = 0L;
        if (this.lyricsExpanded && this.currentLine >= 0 && this.currentLine < this.lyricViews.size()) {
            this.lyricsScroll.post(new Runnable() { 
                @Override
                public final void run() {
                    NowPlayingActivity.this.scrollToCurrentLyric();
                }
            });
        }
    }

    void scrollToCurrentLyric() {
        TextView line = this.lyricViews.get(this.currentLine);
        this.lyricsScroll.smoothScrollTo(0, Math.max(0, line.getTop() - (this.lyricsScroll.getHeight() / 3)));
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("lyrics_visible", this.lyricsExpanded);
        super.onSaveInstanceState(state);
    }

    private String modeName() {
        if (!this.bound || this.player == null) {
            return "列表循环";
        }
        switch (this.player.mode()) {
            case ONE:
                return "单曲循环";
            case ORDER:
                return "顺序播放";
            case SHUFFLE:
                return "随机播放";
            default:
                return "列表循环";
        }
    }

    private void refreshMode() {
        String glyph;
        if (this.modeBtn == null || !this.bound || this.player == null) {
            return;
        }
        switch (this.player.mode()) {
            case ONE:
                glyph = "repeat-one";
                break;
            case ORDER:
                glyph = "order";
                break;
            case SHUFFLE:
                glyph = "shuffle";
                break;
            default:
                glyph = "repeat";
                break;
        }
        this.modeBtn.setState(glyph);
        this.modeBtn.setContentDescription("播放模式：" + modeName() + "，点击切换");
    }

    private void showMore(View anchor) {
        Track song=!bound||player==null?null:player.current();
        if(song==null)return;
        if(playerMenu!=null)playerMenu.dismiss();
        playerMenu=new PlayerMenuSheet(this,song,isFavorite(song),action->{
            switch(action){
                case "favorite":showFavoriteActions(song);break;
                case "download":PlayerSongActions.showDownload(this,song);break;
                case "share":PlayerSongActions.showShare(this,song);break;
                case "listen":PlayerSongActions.showListenTogether(this,song);break;
                case "artist":openArtist(song);break;
                case "album":openAlbum(song);break;
                case "quality":showQuality();break;
                case "effects":showEffects();break;
                case "style":showPlayerStyle();break;
                case "heart_effect":showHeartEffects();break;
                case "mode":showPlaybackModes();break;
                case "timer":showTimer();break;
            }
        });
        playerMenu.addItem("artist","artist","歌手",song.artist)
            .addItem("album","album","专辑",song.albumTitle)
            .addItem("quality","quality","音质",song.isOnline()&&player.isPreparing()?"音质待确认":song.currentQualityLabel())
            .addItem("effects","effects","音效",PlaybackService.AUDIO_EFFECTS[player.effectPreset()])
            .addItem("style","style","播放器样式",playerPreferences().getInt("style",0)==1?"经典封面":"黑胶唱片")
            .addItem("heart_effect","heart_effect","红心动效",heartEffectName())
            .addItem("mode","history","播放模式",modeName())
            .addItem("timer","history","定时关闭",player.sleepRemainingMs()>0?"已开启":"未开启");
        playerMenu.show();
    }

    private SharedPreferences playerPreferences(){return getSharedPreferences("player_visuals",MODE_PRIVATE);}
    private boolean isFavorite(Track song){return song.isOnline()?OnlineLibraryStore.isFavorite(this,song):getSharedPreferences("library",0).getStringSet("favorites",Collections.emptySet()).contains(Long.toString(song.id));}
    private void showFavoriteActions(Track song){
        if(song.isRadio()){new AlertDialog.Builder(this).setTitle("电台直播").setMessage("直播音源暂不支持收藏为歌曲。").setPositiveButton("关闭",null).show();return;}
        ActionSheet sheet=new ActionSheet(this,"收藏歌曲",song.title,false);sheet.setSong(song);
        sheet.addAction("heart",isFavorite(song)?"取消收藏":"加入我喜欢的歌曲","保存在本机收藏中",()->toggleFavorite(song));
        if(song.isOnline())sheet.addAction("queue","查看收藏歌曲","打开我喜欢的歌曲",()->startActivity(new Intent(this,OnlineActivity.class).putExtra("page","favorites")));
        sheet.show();
    }
    private void openAlbum(Track song){
        long id=PlayerSongActions.albumId(song);
        if(id>0){startActivity(new Intent(this,AlbumDetailActivity.class).putExtra("album_id",id).putExtra("theme",currentTint));return;}
        new AlertDialog.Builder(this).setTitle("专辑信息").setMessage("这首歌曲暂未提供可关联的专辑信息。")
            .setPositiveButton("搜索歌曲",(d,w)->startActivity(new Intent(this,OnlineActivity.class).putExtra("page","search").putExtra("keyword",song.title+" "+song.artist)))
            .setNegativeButton("关闭",null).show();
    }
    private void showPlayerStyle(){
        new AlertDialog.Builder(this).setTitle("播放器样式")
            .setSingleChoiceItems(new String[]{"黑胶唱片","经典封面"},playerPreferences().getInt("style",0),(dialog,index)->{
                playerPreferences().edit().putInt("style",index).apply();albumCover.setCoverStyle(index==1);
                if(lyricsExpanded)toggleLyrics();dialog.dismiss();
            }).setNegativeButton("取消",null).show();
    }
    private String heartEffectName(){return new String[]{"关闭","轻弹","爱心绽放"}[Math.max(0,Math.min(2,playerPreferences().getInt("heart_effect",1)))];}
    private void showHeartEffects(){
        new AlertDialog.Builder(this).setTitle("红心动效")
            .setSingleChoiceItems(new String[]{"关闭","轻弹","爱心绽放"},Math.max(0,Math.min(2,playerPreferences().getInt("heart_effect",1))),(dialog,index)->{
                playerPreferences().edit().putInt("heart_effect",index).apply();dialog.dismiss();animateFavorite();
            }).setNegativeButton("取消",null).show();
    }
    private void showPlaybackModes(){
        if(!bound||player==null)return;
        new AlertDialog.Builder(this).setTitle("播放模式")
            .setSingleChoiceItems(new String[]{"列表循环","单曲循环","顺序播放","随机播放"},player.mode().ordinal(),(dialog,index)->{
                if(!bound||player==null){dialog.dismiss();return;}
                for(int i=0;i<4&&player.mode().ordinal()!=index;i++)player.cycleMode();refreshMode();dialog.dismiss();
            }).setNegativeButton("取消",null).show();
    }
    private void animateFavorite(){
        int effect=playerPreferences().getInt("heart_effect",1);if(effect==0||!active)return;
        heart.animate().cancel();heart.setScaleX(1);heart.setScaleY(1);
        heart.animate().scaleX(1.25f).scaleY(1.25f).setDuration(130).withEndAction(()->heart.animate().scaleX(1).scaleY(1).setDuration(180).start()).start();
        if(effect==2){
            if(favoriteBurst!=null&&favoriteBurst.getParent()==rootScene)rootScene.removeView(favoriteBurst);
            int[] location=new int[2],scene=new int[2];heart.getLocationOnScreen(location);rootScene.getLocationOnScreen(scene);
            favoriteBurst=new HeartBurstView(this,location[0]-scene[0]+heart.getWidth()/2f,location[1]-scene[1]+heart.getHeight()/2f);
            rootScene.addView(favoriteBurst,new FrameLayout.LayoutParams(-1,-1));favoriteBurst.start();
        }
    }

    void cycleModeFromSettings() {
        if (this.bound && this.player != null) {
            this.player.cycleMode();
            refreshMode();
            Toast.makeText(this, "已切换为" + modeName(), 0).show();
        }
    }

    private void toggleLockLyrics() {
        if (!this.bound || this.player == null) {
            Toast.makeText(this, "播放器正在启动，请稍后重试", 0).show();
        } else if (this.player.lockscreenLyricsEnabled()) {
            this.player.setLockscreenLyricsEnabled(false);
            Toast.makeText(this, "已关闭锁屏同步歌词", 0).show();
        } else {
            new AlertDialog.Builder(this).setTitle("在锁屏和通知栏显示歌词").setMessage("开启后，青音会额外显示一条无声的同步歌词通知，播放时随歌词时间更新；关闭后立即移除。系统原生媒体卡片仍由你的手机系统控制，不能强制变成 iOS 液态玻璃。歌词可能在锁屏上被其他人看到。").setPositiveButton("开启", new DialogInterface.OnClickListener() { 
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    NowPlayingActivity.this.onEnableLockscreenLyrics(dialogInterface, i);
                }
            }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
        }
    }

    void onEnableLockscreenLyrics(DialogInterface dialog, int which) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 8421);
        } else {
            enableLockLyrics();
        }
    }

    private void enableLockLyrics() {
        if (this.bound && this.player != null) {
            this.player.setLockscreenLyricsEnabled(true);
            Toast.makeText(this, this.player.lockscreenLyricsEnabled() ? "已开启 · 有同步歌词时会显示静音通知" : "未开启，请检查通知权限", 1).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != 8421) {
            return;
        }
        if (grants.length > 0 && grants[0] == 0) {
            enableLockLyrics();
        } else {
            Toast.makeText(this, "通知权限未允许，锁屏歌词保持关闭", 1).show();
        }
    }

    public static String clock(long ms) {
        long s = Math.max(0L, ms) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", Long.valueOf(s / 60), Long.valueOf(s % 60));
    }

    public void updateView() {
        String str;
        String str2;
        String str3;
        int idx;
        if (!this.bound || this.player == null) {
            return;
        }
        String failure = this.player.lastError();
        if (failure == null) {
            failure = "";
        }
        if (!failure.isEmpty() && !this.player.isPreparing() && !this.player.isPlaying() && !failure.equals(this.lastDisplayedError)) {
            this.lastDisplayedError = failure;
            Toast.makeText(this, "播放失败：" + failure, 1).show();
        } else if (failure.isEmpty()) {
            this.lastDisplayedError = "";
        }
        Track song = this.player.current();
        if (song == null) {
            return;
        }
        if(!TextUtils.equals(this.title.getText(),song.title))this.title.setText(song.title);
        if(!TextUtils.equals(this.artist.getText(),song.artist))this.artist.setText(song.artist);
        boolean live = song.isRadio();
        TextView textView = this.qualityLabel;
        str = song.isOnline() && this.player.isPreparing() ? "音质待确认" : song.currentQualityLabel();
        if(!TextUtils.equals(textView.getText(),str)){textView.setText(str);textView.setContentDescription("当前音质：" + str);}
        int i = 8;
        this.seek.setVisibility(live ? 8 : 0);
        this.elapsed.setVisibility(live ? 4 : 0);
        this.total.setVisibility(live ? 4 : 0);
        this.heart.setVisibility(live ? 8 : 0);
        LinearLayout linearLayout = this.commentAction;
        if (song.isOnline() && !live) {
            i = 0;
        }
        linearLayout.setVisibility(i);
        refreshMode();
        PlayerGlyph playerGlyph = this.playBtn;
        if (this.player.isPreparing()) {
            str2 = "loading";
        } else {
            str2 = this.player.isPlaying() ? "pause" : "play";
        }
        playerGlyph.setState(str2);
        this.albumCover.setSpinning(this.active && !this.lyricsExpanded && this.player.isPlaying());
        PlayerGlyph playerGlyph2 = this.playBtn;
        if (this.player.isPreparing()) {
            str3 = "正在缓冲音频";
        } else {
            str3 = this.player.isPlaying() ? "暂停播放" : "继续播放";
        }
        if(!TextUtils.equals(playerGlyph2.getContentDescription(),str3))playerGlyph2.setContentDescription(str3);
        boolean liked = song.isOnline() ? OnlineLibraryStore.isFavorite(this, song) : getSharedPreferences("library", 0).getStringSet("favorites", Collections.emptySet()).contains(Long.toString(song.id));
        this.heart.setState(liked ? "heart-fill" : "heart");
        this.seek.setEnabled((live || this.player.isPreparing()) ? false : true);
        if (!this.loadedUri.equals(song.uri)) {
            this.loadedUri = song.uri;
            this.currentLine = -2;
            this.albumCover.setCover(song.coverUrl);
            updateAlbumColors(song.coverUrl, song.uri);
            this.commentCount.setText("评论");
            if (song.isOnline() && !live) {
                loadCommentCount(song);
            }
            requestLyrics(song);
        }
        updatePosition();
    }

    private void updatePosition(){
        if(!bound||player==null||player.current()==null||player.current().isRadio())return;
        int idx;
        int duration=Math.max(1,player.duration()),position=Math.max(0,player.currentPosition());
        if(seek.getMax()!=duration)seek.setMax(duration);
        if(!dragging){
            int progress=Math.min(duration,position);if(seek.getProgress()!=progress)seek.setProgress(progress);
            long second=position/1000;if(second!=displayedElapsedSecond){displayedElapsedSecond=second;elapsed.setText(clock(position));}
        }
        int remaining=Math.max(0,duration-position);long second=remaining/1000;
        if(second!=displayedRemainingSecond){displayedRemainingSecond=second;total.setText("−"+clock(remaining));}
        if (!this.lyricLines.isEmpty() && (idx = LyricsParser.indexAt(this.lyricLines, position)) != this.currentLine) {
            if (this.currentLine >= 0 && this.currentLine < this.lyricViews.size()) {
                restyle(this.currentLine, false);
            }
            this.currentLine = idx;
            if (idx >= 0 && idx < this.lyricViews.size()) {
                restyle(idx, true);
                TextView focus = this.lyricViews.get(idx);
                if (this.lyricsExpanded && SystemClock.uptimeMillis() > this.manualScrollUntil) {
                    this.lyricsScroll.smoothScrollTo(0, Math.max(0, focus.getTop() - (this.lyricsScroll.getHeight() / 3)));
                }
            }
        }
    }

    private void paintPalette(int base) {
        if(playerBackdrop==null){playerBackdrop=new PlayerBackdropDrawable();rootScene.setBackground(playerBackdrop);}
        playerBackdrop.setAccent(base);
        getWindow().setNavigationBarColor(playerBackdrop.bottomColor());
    }

    private void updateAlbumColors(String url, final String expected) {
        Artwork.loadBitmap(url, new Consumer() { 
            @Override
            public final void accept(Object obj) {
                NowPlayingActivity.this.applyAlbumPalette(expected, (Bitmap) obj);
            }
        });
    }

    void applyAlbumPalette(String expected, Bitmap bitmap) {
        if (isDestroyed() || !expected.equals(this.loadedUri)) {
            return;
        }
        int tone = bitmap==null||bitmap.isRecycled()?0xff535762:CoverPalette.extract(bitmap);
        if (this.tintAnimation != null) {
            this.tintAnimation.cancel();
        }
        this.tintAnimation = ValueAnimator.ofArgb(this.currentTint, tone);
        this.tintAnimation.setDuration(620L);
        this.tintAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { 
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                NowPlayingActivity.this.animatePaletteFrame(valueAnimator);
            }
        });
        this.tintAnimation.start();
    }

    void animatePaletteFrame(ValueAnimator a) {
        this.currentTint = ((Integer) a.getAnimatedValue()).intValue();
        paintPalette(this.currentTint);
    }

    private void loadCommentCount(final Track song) {
        final String expected = song.uri;
        this.work.execute(new Runnable() { 
            @Override
            public final void run() {
                NowPlayingActivity.this.fetchCommentCount(song, expected);
            }
        });
    }

    void fetchCommentCount(Track song, final String expected) {
        String count = "";
        try {
            JSONObject params = new JSONObject().put("hash", Uri.parse(song.uri).getLastPathSegment());
            JSONObject raw = OnlineApi.call(getApplicationContext(), "songCommentCount", params).optJSONObject("raw");
            JSONObject inner = raw == null ? null : raw.optJSONObject("data");
            int n = commentTotal(inner != null ? inner : raw);
            if (n >= 0) {
                count = n >= 10000 ? String.format(Locale.ROOT, "%.1f万", Float.valueOf(n / 10000.0f)) : String.valueOf(n);
            }
        } catch (Exception e) {
        }
        final String result = count;
        this.main.post(new Runnable() { 
            @Override
            public final void run() {
                NowPlayingActivity.this.applyCommentCount(expected, result);
            }
        });
    }

    void applyCommentCount(String expected, String result) {
        if (!isDestroyed() && expected.equals(this.loadedUri)) {
            this.commentCount.setText(result.isEmpty() ? "评论" : result);
        }
    }

    private static int commentTotal(JSONObject raw) {
        int value;
        if (raw == null) {
            return -1;
        }
        String[] strArr = {"count", "total", "comment_count", "commentcount", "comments_num", "num", "totalCount"};
        for (int i = 0; i < 7; i++) {
            String key = strArr[i];
            if (!raw.isNull(key) && raw.has(key) && (value = raw.optInt(key, -1)) >= 0) {
                return value;
            }
        }
        JSONObject nested = raw.optJSONObject("data");
        if (nested == null) {
            return -1;
        }
        return commentTotalShallow(nested);
    }

    private static int commentTotalShallow(JSONObject raw) {
        String[] strArr = {"count", "total", "comment_count", "commentcount", "num"};
        for (int i = 0; i < 5; i++) {
            String key = strArr[i];
            int n = raw.optInt(key, -1);
            if (n >= 0) {
                return n;
            }
        }
        return -1;
    }

    private void showComments() {
        Track song = (!this.bound || this.player == null) ? null : this.player.current();
        if (song == null || !song.isOnline() || song.isRadio()) {
            Toast.makeText(this, "当前歌曲没有酷狗在线评论", 0).show();
            return;
        }
        Intent i = new Intent(this, (Class<?>) MusicCommentsActivity.class);
        i.putExtra("track_uri", song.uri);
        i.putExtra("track_title", song.title);
        i.putExtra("track_artist", song.artist);
        i.putExtra("track_cover", song.coverUrl);
        startActivity(i);
    }

    private void restyle(int i, boolean z) {
        TextView textView = this.lyricViews.get(i);
        textView.setTextColor(z ? -1541 : -4278593);
        textView.setTextSize(z ? 21.0f : 18.0f);
        textView.setTypeface(Typeface.DEFAULT, z ? 1 : 0);
        textView.animate().alpha(z ? 1.0f : 0.56f).setDuration(220L).start();
    }

    private void setLyrics(List<LyricsParser.Line> lines, String message) {
        this.lyricLines = lines;
        this.lyricViews.clear();
        this.lyricsColumn.removeAllViews();
        this.currentLine = -2;
        if (lines.isEmpty()) {
            this.lyricHint = label(message, 17, -2173478, false);
            this.lyricHint.setGravity(17);
            this.lyricsColumn.addView(this.lyricHint, new LinearLayout.LayoutParams(-1, dp(95.0f)));
            return;
        }
        for (LyricsParser.Line line : lines) {
            TextView row = label(line.text, 18, -2173478, false);
            row.setGravity(17);
            row.setPadding(dp(7.0f), dp(15.0f), dp(7.0f), dp(15.0f));
            row.setAlpha(0.56f);
            row.setShadowLayer(dp(2.0f), 0.0f, dp(1.0f), 1426063360);
            long j = line.millis;
            row.setContentDescription("歌词：" + line.text + "。单击返回封面，双击跳转至 " + clock(line.millis));
            this.lyricsColumn.addView(row, new LinearLayout.LayoutParams(-1, -2));
            this.lyricViews.add(row);
        }
        this.lyricsScroll.scrollTo(0, 0);
    }

    private void requestLyrics(final Track song) {
        setLyrics(Collections.emptyList(), song.isOnline() ? "正在获取同步歌词…" : "本地歌词暂未接入");
        if (song.isOnline()) {
            final String expected = song.uri;
            this.work.execute(new Runnable() { 
                @Override
                public final void run() {
                    NowPlayingActivity.this.fetchLyrics(song, expected);
                }
            });
        }
    }

    void fetchLyrics(Track song, final String expected) {
        String raw = "";
        try {
            Uri uri = Uri.parse(song.uri);
            JSONObject query = new JSONObject();
            query.put("hash", uri.getLastPathSegment());
            query.put("title", song.title);
            query.put("duration", Math.max(0L, song.durationMs / 1000));
            JSONObject result = OnlineApi.call(getApplicationContext(), "lyrics", query);
            raw = result.optString("text", "");
        } catch (Exception e) {
        }
        final List<LyricsParser.Line> lines = LyricsParser.parse(raw);
        this.main.post(new Runnable() { 
            @Override
            public final void run() {
                NowPlayingActivity.this.applyLyrics(expected, lines);
            }
        });
    }

    void applyLyrics(String expected, List<LyricsParser.Line> lines) {
        if (!isDestroyed() && expected.equals(this.loadedUri)) {
            setLyrics(lines, lines.isEmpty() ? "这首歌暂时没有可显示的同步歌词" : "");
        }
    }

    private void toggleFavorite() {
        if (!this.bound || this.player == null || this.player.current() == null) {
            return;
        }
        Track song = this.player.current();
        toggleFavorite(song);
    }

    private void toggleFavorite(Track song) {
        if(song==null||song.isRadio())return;
        boolean added=!isFavorite(song);
        if (song.isOnline()) {
            OnlineLibraryStore.toggle(this, song);
        } else {
            SharedPreferences prefs = getSharedPreferences("library", 0);
            Set<String> saved = new HashSet<>(prefs.getStringSet("favorites", Collections.emptySet()));
            String id = Long.toString(song.id);
            if (!saved.add(id)) {
                saved.remove(id);
            }
            prefs.edit().putStringSet("favorites", saved).apply();
        }
        updateView();
        if(added)animateFavorite();
    }

    private void showQueue() {
        if (bound && player != null) QueueDialog.show(this, player);
    }

    public void showEffects() {
        if (!this.bound || this.player == null) {
            return;
        }
        new AlertDialog.Builder(this).setTitle("播放音效 · 原音不做处理").setSingleChoiceItems(PlaybackService.AUDIO_EFFECTS, this.player.effectPreset(), new DialogInterface.OnClickListener() { 
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                NowPlayingActivity.this.onEffectChoice(dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    void onEffectChoice(DialogInterface dialog, int index) {
        boolean supported = this.player.setEffectPreset(index);
        dialog.dismiss();
        Toast.makeText(this, supported ? "已切换：" + PlaybackService.AUDIO_EFFECTS[index] : "当前设备不支持此音效，已恢复原音", 1).show();
    }

    private void showVolume() {
        final AudioManager audio = (AudioManager) getSystemService("audio");
        if (audio == null) {
            return;
        }
        SeekBar bar = new SeekBar(this);
        bar.setMax(Math.max(1, audio.getStreamMaxVolume(3)));
        bar.setProgress(audio.getStreamVolume(3));
        bar.setPadding(dp(25.0f), dp(18.0f), dp(25.0f), dp(15.0f));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { 
            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }

            @Override
            public void onProgressChanged(SeekBar s, int value, boolean user) {
                if (user) {
                    audio.setStreamVolume(3, value, 0);
                }
            }
        });
        new AlertDialog.Builder(this).setTitle("媒体音量").setView(bar).setPositiveButton("完成", new DialogInterface.OnClickListener() { 
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                NowPlayingActivity.onVolumeDialogButton(dialogInterface, i);
            }
        }).show();
    }

    static void onVolumeDialogButton(DialogInterface d, int w) {
    }

    public void showQuality() {
        String actual;
        int selected = 0;
        String old = OnlineApi.quality(this);
        for (int i = 0; i < OnlineApi.QUALITIES.length; i++) {
            if (OnlineApi.QUALITIES[i].equals(old)) {
                selected = i;
            }
        }
        LinearLayout header = vertical();
        header.setPadding(dp(22.0f), dp(20.0f), dp(22.0f), dp(11.0f));
        TextView heading = label("在线音质", 21, -14408662, true);
        header.addView(heading, lp(-1, 35));
        LinearLayout current = vertical();
        current.setPadding(dp(15.0f), dp(11.0f), dp(12.0f), dp(12.0f));
        current.setBackground(shape(-988174, 16));
        TextView nowLabel = label("正在播放", 12, -8949377, false);
        current.addView(nowLabel, lp(-1, 21));
        Track playing = (!this.bound || this.player == null) ? null : this.player.current();
        if (playing == null) {
            actual = "暂无正在播放的歌曲";
        } else {
            actual = (playing.isOnline() && this.player.isPreparing()) ? "正在获取音质…" : playing.actualAudioFormat();
        }
        TextView nowValue = label(actual, 16, -13751753, true);
        nowValue.setSingleLine(true);
        nowValue.setEllipsize(TextUtils.TruncateAt.END);
        current.addView(nowValue, lp(-1, 27));
        LinearLayout.LayoutParams currentLp = lp(-1, -2);
        currentLp.topMargin = dp(9.0f);
        header.addView(current, currentLp);
        new AlertDialog.Builder(this).setCustomTitle(header).setSingleChoiceItems(OnlineApi.QUALITY_NAMES, selected, new DialogInterface.OnClickListener() { 
            @Override
            public final void onClick(DialogInterface dialogInterface, int i2) {
                NowPlayingActivity.this.onQualityChoice(dialogInterface, i2);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    void onQualityChoice(DialogInterface dialog, int index) {
        OnlineApi.setQuality(this, OnlineApi.QUALITIES[index]);
        dialog.dismiss();
        Toast.makeText(this, "已保存，下次解析在线音源时生效；音质受账号权益限制", 1).show();
    }

    private void showTimer() {
        if (!this.bound || this.player == null) {
            return;
        }
        String[] labels = {"关闭定时", "10 分钟", "20 分钟", "30 分钟", "45 分钟", "60 分钟", "90 分钟"};
        final int[] values = {0, 10, 20, 30, 45, 60, 90};
        new AlertDialog.Builder(this).setTitle("睡眠定时").setItems(labels, new DialogInterface.OnClickListener() { 
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                NowPlayingActivity.this.onTimerChoice(values, dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    void onTimerChoice(int[] values, DialogInterface d, int index) {
        this.player.setSleepTimerMinutes(values[index]);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.active = true;
        this.main.removeCallbacks(this.ticker);
        this.main.post(this.ticker);
        this.main.removeCallbacks(this.progressTicker);
        this.main.post(this.progressTicker);
    }

    @Override
    protected void onStop() {
        this.active = false;
        PlayerSongActions.closePending(this);
        if(playerMenu!=null){playerMenu.dismiss();playerMenu=null;}
        heart.animate().cancel();heart.setScaleX(1);heart.setScaleY(1);
        if(favoriteBurst!=null&&favoriteBurst.getParent()==rootScene)rootScene.removeView(favoriteBurst);
        this.albumCover.setSpinning(false);
        this.main.removeCallbacks(this.ticker);
        this.main.removeCallbacks(this.progressTicker);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.main.removeCallbacks(this.ticker);
        this.main.removeCallbacks(this.progressTicker);
        if (this.bound) {
            unbindService(this.connection);
        }
        if (this.tintAnimation != null) {
            this.tintAnimation.cancel();
        }
        this.work.shutdownNow();
        super.onDestroy();
    }
}
