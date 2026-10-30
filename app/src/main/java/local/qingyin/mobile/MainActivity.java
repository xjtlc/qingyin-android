package local.qingyin.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: classes2.dex */
public final class MainActivity extends Activity {
    private static final int REQUEST_IMPORT_AUDIO = 201;
    private boolean activeUi;
    private TrackAdapter adapter;
    private boolean bindingRequested;
    private boolean bound;
    private TextView count;
    private TextView currentTime;
    private boolean dragging;
    private TextView format;
    private Button heart;
    private ListView list;
    private Button modeButton;
    private TextView nowArtist;
    private TextView nowTitle;
    private PlaybackService player;
    private Button queueButton;
    private EditText search;
    private SeekBar seek;
    private Button toggle;
    private TextView totalTime;
    private final int INK = -15000801;
    private final int MUTED = -8815739;
    private final int BLUE = -1815446;
    private final int BACKGROUND = -526343;
    private final ArrayList<Track> all = new ArrayList<>();
    private final ArrayList<Track> visible = new ArrayList<>();
    private final ExecutorService disk = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String section = "歌曲";
    private String searchText = "";
    private final Runnable delayedFilter = new Runnable() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda22
        @Override // java.lang.Runnable
        public final void run() {
            MainActivity.this.m118lambda$new$0$localqingyinmobileMainActivity();
        }
    };
    private final PlaybackService.OnChange playbackChanged = new PlaybackService.OnChange() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda23
        @Override // local.qingyin.mobile.PlaybackService.OnChange
        public final void changed() {
            MainActivity.this.m120lambda$new$2$localqingyinmobileMainActivity();
        }
    };
    private final Runnable ticker = new Runnable() { // from class: local.qingyin.mobile.MainActivity.1
        @Override // java.lang.Runnable
        public void run() {
            if (MainActivity.this.activeUi) {
                if (MainActivity.this.bound && !MainActivity.this.dragging) {
                    MainActivity.this.updateProgress();
                }
                MainActivity.this.handler.postDelayed(this, 500L);
            }
        }
    };
    private final ServiceConnection connection = new ServiceConnection() { // from class: local.qingyin.mobile.MainActivity.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MainActivity.this.player = ((PlaybackService.LocalBinder) binder).service();
            MainActivity.this.bound = true;
            if (MainActivity.this.activeUi) {
                MainActivity.this.player.setListener(MainActivity.this.playbackChanged);
            }
            if (MainActivity.this.activeUi) {
                MainActivity.this.updatePlayer();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            MainActivity.this.bound = false;
            MainActivity.this.player = null;
            MainActivity.this.updatePlayer();
        }
    };

    /* JADX INFO: renamed from: lambda$new$0$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m118lambda$new$0$localqingyinmobileMainActivity() {
        if (isDestroyed()) {
            return;
        }
        filter();
    }

    /* JADX INFO: renamed from: lambda$new$2$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m120lambda$new$2$localqingyinmobileMainActivity() {
        runOnUiThread(new Runnable() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m119lambda$new$1$localqingyinmobileMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$new$1$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m119lambda$new$1$localqingyinmobileMainActivity() {
        if (!this.activeUi || isDestroyed()) {
            return;
        }
        updatePlayer();
        if (!"最近".equals(this.section)) {
            if (this.adapter != null) {
                this.adapter.notifyDataSetChanged();
                return;
            }
            return;
        }
        filter();
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(-526343);
        getWindow().setNavigationBarColor(-526343);
        getWindow().getDecorView().setSystemUiVisibility(8208);
        migratePreferences();
        createLayout();
        this.bindingRequested = bindService(new Intent(this, (Class<?>) PlaybackService.class), this.connection, 1);
        askPermission();
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        this.activeUi = true;
        this.handler.removeCallbacks(this.ticker);
        this.handler.post(this.ticker);
        if (!this.bound || this.player == null) {
            return;
        }
        this.player.setListener(this.playbackChanged);
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.activeUi = false;
        this.handler.removeCallbacks(this.ticker);
        if (this.bound && this.player != null) {
            this.player.setListener(null);
        }
        super.onStop();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(float n) {
        return Math.round(getResources().getDisplayMetrics().density * n);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(1);
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout horizontal() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(0);
        v.setGravity(16);
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        if (bold) {
            v.setTypeface(Typeface.DEFAULT, 1);
        }
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14.0f);
        b.setTextColor(-15000801);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(10.0f), 0, dp(10.0f), 0);
        b.setBackground(bg(-268050, 14.0f));
        return b;
    }

    private void createLayout() {
        LinearLayout linearLayoutVertical = vertical();
        linearLayoutVertical.setBackgroundColor(-526343);
        linearLayoutVertical.setPadding(dp(15.0f), dp(8.0f), dp(15.0f), dp(7.0f));
        setContentView(linearLayoutVertical);
        LinearLayout header = horizontal();
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_qingyin);
        logo.setPadding(dp(4.0f), dp(4.0f), dp(4.0f), dp(4.0f));
        logo.setBackground(bg(-268050, 12.0f));
        header.addView(logo, lp(37, 37));
        TextView title = text("本地音乐", 23, -15000801, true);
        title.setPadding(dp(10.0f), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button online = button("在线曲库");
        int i = -1815446;
        online.setTextColor(-1815446);
        online.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m109lambda$createLayout$3$localqingyinmobileMainActivity(view);
            }
        });
        header.addView(online, lp(83, 38));
        final Button more = button("⋯");
        more.setContentDescription("本地音乐更多操作");
        more.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m111lambda$createLayout$5$localqingyinmobileMainActivity(more, view);
            }
        });
        header.addView(more, lp(42, 38));
        int i2 = -1;
        linearLayoutVertical.addView(header, lp(-1, 54));
        this.search = new EditText(this);
        this.search.setSingleLine(true);
        this.search.setTextSize(15.0f);
        this.search.setTextColor(-15000801);
        this.search.setHint("⌕  搜索手机歌曲、歌手");
        this.search.setHintTextColor(-8815739);
        this.search.setPadding(dp(16.0f), 0, dp(16.0f), 0);
        this.search.setBackground(bg(-1, 13.0f));
        linearLayoutVertical.addView(this.search, lp(-1, 44));
        this.search.addTextChangedListener(new TextWatcher() { // from class: local.qingyin.mobile.MainActivity.3
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int st, int cnt, int a) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                MainActivity.this.searchText = s.toString().trim().toLowerCase(Locale.ROOT);
                MainActivity.this.handler.removeCallbacks(MainActivity.this.delayedFilter);
                MainActivity.this.handler.postDelayed(MainActivity.this.delayedFilter, 130L);
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable e) {
            }
        });
        final LinearLayout tabs = horizontal();
        tabs.setPadding(0, dp(8.0f), 0, dp(4.0f));
        for (final String name : Arrays.asList("歌曲", "收藏", "最近")) {
            final Button b = button(name);
            if ("歌曲".equals(name)) {
                b.setBackground(bg(i, 14.0f));
                b.setTextColor(i2);
            }
            LinearLayout.LayoutParams p = lp(0, 35);
            p.weight = 1.0f;
            p.setMargins(dp(3.0f), 0, dp(3.0f), 0);
            tabs.addView(b, p);
            b.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda8
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.m112lambda$createLayout$6$localqingyinmobileMainActivity(name, tabs, b, view);
                }
            });
            i = -1815446;
            i2 = -1;
        }
        linearLayoutVertical.addView(tabs);
        this.count = text("正在读取本地音乐…", 12, -8815739, false);
        this.count.setPadding(dp(4.0f), 0, 0, 0);
        linearLayoutVertical.addView(this.count, lp(-1, 21));
        this.list = new ListView(this);
        this.list.setDivider(null);
        this.list.setDividerHeight(dp(4.0f));
        this.list.setBackgroundColor(0);
        this.list.setCacheColorHint(0);
        this.list.setVerticalScrollBarEnabled(false);
        this.adapter = new TrackAdapter();
        this.list.setAdapter((ListAdapter) this.adapter);
        LinearLayout.LayoutParams listParams = lp(-1, 0);
        listParams.weight = 1.0f;
        linearLayoutVertical.addView(this.list, listParams);
        this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda9
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i3, long j) {
                MainActivity.this.m113lambda$createLayout$7$localqingyinmobileMainActivity(adapterView, view, i3, j);
            }
        });
        this.list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda10
            @Override // android.widget.AdapterView.OnItemLongClickListener
            public final boolean onItemLongClick(AdapterView adapterView, View view, int i3, long j) {
                return MainActivity.this.m114lambda$createLayout$8$localqingyinmobileMainActivity(adapterView, view, i3, j);
            }
        });
        LinearLayout linearLayoutVertical2 = vertical();
        linearLayoutVertical2.setPadding(dp(10.0f), dp(7.0f), dp(10.0f), dp(5.0f));
        linearLayoutVertical2.setBackground(bg(-1, 14.0f));
        linearLayoutVertical2.setElevation(dp(6.0f));
        LinearLayout now = horizontal();
        TextView disc = text("♫", 25, -1815446, true);
        disc.setGravity(17);
        disc.setBackground(QingyinUi.gradient(this, -2562305, -1120257, 13.0f));
        now.addView(disc, lp(48, 48));
        disc.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m115lambda$createLayout$9$localqingyinmobileMainActivity(view);
            }
        });
        LinearLayout meta = vertical();
        meta.setPadding(dp(10.0f), 0, 0, 0);
        this.nowTitle = text("还没有播放音乐", 15, -15000801, true);
        this.nowTitle.setSingleLine();
        this.nowTitle.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(this.nowTitle);
        this.nowTitle.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m100lambda$createLayout$10$localqingyinmobileMainActivity(view);
            }
        });
        this.nowArtist = text("选择喜欢的歌曲", 12, -8815739, false);
        this.nowArtist.setSingleLine();
        this.nowArtist.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(this.nowArtist);
        this.nowArtist.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m101lambda$createLayout$11$localqingyinmobileMainActivity(view);
            }
        });
        this.format = text("青音 · 准备就绪", 11, -1815446, true);
        this.format.setSingleLine();
        this.format.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mid = lp(0, -2);
        mid.weight = 1.0f;
        now.addView(meta, mid);
        this.heart = button("♡");
        this.heart.setTextSize(21.0f);
        this.heart.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m102lambda$createLayout$12$localqingyinmobileMainActivity(view);
            }
        });
        Button fullPlayer = button("↗");
        fullPlayer.setContentDescription("打开完整播放页面和同步歌词");
        fullPlayer.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m103lambda$createLayout$13$localqingyinmobileMainActivity(view);
            }
        });
        now.addView(fullPlayer, lp(39, 43));
        now.addView(this.heart, lp(44, 43));
        linearLayoutVertical2.addView(now);
        LinearLayout progress = horizontal();
        this.currentTime = text("00:00", 11, -8815739, false);
        this.totalTime = text("00:00", 11, -8815739, false);
        progress.addView(this.currentTime);
        this.seek = new SeekBar(this);
        this.seek.setProgressTintList(ColorStateList.valueOf(-1815446));
        this.seek.setThumbTintList(ColorStateList.valueOf(-1815446));
        LinearLayout.LayoutParams seekParams = lp(0, 28);
        seekParams.weight = 1.0f;
        progress.addView(this.seek, seekParams);
        progress.addView(this.totalTime);
        linearLayoutVertical2.addView(progress);
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: local.qingyin.mobile.MainActivity.4
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar s) {
                MainActivity.this.dragging = true;
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar s) {
                MainActivity.this.dragging = false;
                if (MainActivity.this.bound) {
                    MainActivity.this.player.seekTo(s.getProgress());
                }
                MainActivity.this.updateProgress();
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                if (fromUser) {
                    MainActivity.this.currentTime.setText(MainActivity.clock(value));
                }
            }
        });
        LinearLayout transport = horizontal();
        transport.setGravity(17);
        this.modeButton = button("↻");
        this.modeButton.setTextSize(19.0f);
        this.modeButton.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m104lambda$createLayout$14$localqingyinmobileMainActivity(view);
            }
        });
        LinearLayout.LayoutParams modeLp = lp(0, 41);
        modeLp.weight = 1.0f;
        transport.addView(this.modeButton, modeLp);
        Button previous = button("⏮");
        previous.setTextSize(21.0f);
        previous.setContentDescription("上一首");
        previous.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m105lambda$createLayout$15$localqingyinmobileMainActivity(view);
            }
        });
        LinearLayout.LayoutParams prevLp = lp(0, 41);
        prevLp.weight = 1.0f;
        transport.addView(previous, prevLp);
        this.toggle = button("▶");
        this.toggle.setTextSize(23.0f);
        this.toggle.setBackground(bg(-1815446, 18.0f));
        this.toggle.setTextColor(-1);
        LinearLayout.LayoutParams playLp = lp(0, 41);
        playLp.weight = 1.4f;
        playLp.setMargins(dp(4.0f), 0, dp(4.0f), 0);
        transport.addView(this.toggle, playLp);
        this.toggle.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m106lambda$createLayout$16$localqingyinmobileMainActivity(view);
            }
        });
        Button next = button("⏭");
        next.setTextSize(21.0f);
        next.setContentDescription("下一首");
        next.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m107lambda$createLayout$17$localqingyinmobileMainActivity(view);
            }
        });
        LinearLayout.LayoutParams nextLp = lp(0, 41);
        nextLp.weight = 1.0f;
        transport.addView(next, nextLp);
        this.queueButton = button("≡");
        this.queueButton.setTextSize(22.0f);
        this.queueButton.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m108lambda$createLayout$18$localqingyinmobileMainActivity(view);
            }
        });
        LinearLayout.LayoutParams queueLp = lp(0, 41);
        queueLp.weight = 1.0f;
        transport.addView(this.queueButton, queueLp);
        linearLayoutVertical2.addView(transport);
        LinearLayout.LayoutParams panel = lp(-1, -2);
        panel.topMargin = dp(8.0f);
        linearLayoutVertical.addView(linearLayoutVertical2, panel);
        updatePlayer();
    }

    /* JADX INFO: renamed from: lambda$createLayout$3$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m109lambda$createLayout$3$localqingyinmobileMainActivity(View v) {
        startActivity(new Intent(this, (Class<?>) OnlineActivity.class));
    }

    /* JADX INFO: renamed from: lambda$createLayout$5$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m111lambda$createLayout$5$localqingyinmobileMainActivity(Button more, View v) {
        PopupMenu popup = new PopupMenu(this, more);
        popup.getMenu().add(0, 1, 0, "导入音乐");
        popup.getMenu().add(0, 2, 1, "重新扫描");
        popup.getMenu().add(0, 3, 2, "睡眠定时");
        popup.getMenu().add(0, 4, 3, "在线曲库");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda18
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return MainActivity.this.m110lambda$createLayout$4$localqingyinmobileMainActivity(menuItem);
            }
        });
        popup.show();
    }

    /* JADX INFO: renamed from: lambda$createLayout$4$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m110lambda$createLayout$4$localqingyinmobileMainActivity(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                pickFiles();
                return true;
            case 2:
                askPermission();
                return true;
            case 3:
                showSleepTimer();
                return true;
            case 4:
                startActivity(new Intent(this, (Class<?>) OnlineActivity.class));
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: renamed from: lambda$createLayout$6$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m112lambda$createLayout$6$localqingyinmobileMainActivity(String name, LinearLayout tabs, Button b, View v) {
        this.section = name;
        for (int i = 0; i < tabs.getChildCount(); i++) {
            Button tab = (Button) tabs.getChildAt(i);
            boolean selected = tab == b;
            tab.setBackground(bg(selected ? -1815446 : -268050, 14.0f));
            tab.setTextColor(selected ? -1 : -15000801);
        }
        filter();
    }

    /* JADX INFO: renamed from: lambda$createLayout$7$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m113lambda$createLayout$7$localqingyinmobileMainActivity(AdapterView parent, View view, int position, long id) {
        playAt(position);
    }

    /* JADX INFO: renamed from: lambda$createLayout$8$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m114lambda$createLayout$8$localqingyinmobileMainActivity(AdapterView parent, View view, int position, long id) {
        if (position < 0 || position >= this.visible.size()) {
            return true;
        }
        showSongActions(this.visible.get(position));
        return true;
    }

    /* JADX INFO: renamed from: lambda$createLayout$9$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m115lambda$createLayout$9$localqingyinmobileMainActivity(View v) {
        openNowPlaying();
    }

    /* JADX INFO: renamed from: lambda$createLayout$10$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m100lambda$createLayout$10$localqingyinmobileMainActivity(View v) {
        openNowPlaying();
    }

    /* JADX INFO: renamed from: lambda$createLayout$11$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m101lambda$createLayout$11$localqingyinmobileMainActivity(View v) {
        openNowPlaying();
    }

    /* JADX INFO: renamed from: lambda$createLayout$12$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m102lambda$createLayout$12$localqingyinmobileMainActivity(View v) {
        favoriteCurrent();
    }

    /* JADX INFO: renamed from: lambda$createLayout$13$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m103lambda$createLayout$13$localqingyinmobileMainActivity(View v) {
        openNowPlaying();
    }

    /* JADX INFO: renamed from: lambda$createLayout$14$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m104lambda$createLayout$14$localqingyinmobileMainActivity(View v) {
        if (this.bound) {
            this.player.cycleMode();
        }
    }

    /* JADX INFO: renamed from: lambda$createLayout$15$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m105lambda$createLayout$15$localqingyinmobileMainActivity(View v) {
        if (this.bound) {
            ensureForeground();
            this.player.skip(-1);
        }
    }

    /* JADX INFO: renamed from: lambda$createLayout$16$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m106lambda$createLayout$16$localqingyinmobileMainActivity(View v) {
        if (this.bound) {
            ensureForeground();
            this.player.toggle();
        }
    }

    /* JADX INFO: renamed from: lambda$createLayout$17$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m107lambda$createLayout$17$localqingyinmobileMainActivity(View v) {
        if (this.bound) {
            ensureForeground();
            this.player.skip(1);
        }
    }

    /* JADX INFO: renamed from: lambda$createLayout$18$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m108lambda$createLayout$18$localqingyinmobileMainActivity(View v) {
        showQueue();
    }

    private void openNowPlaying() {
        if (this.bound && this.player.current() != null) {
            startActivity(new Intent(this, (Class<?>) NowPlayingActivity.class));
        }
    }

    private void ensureForeground() {
        Intent i = new Intent(this, (Class<?>) PlaybackService.class);
        startForegroundService(i);
    }

    private SharedPreferences library() {
        return getSharedPreferences("library", 0);
    }

    private void migratePreferences() {
        SharedPreferences modern = library();
        SharedPreferences old = getPreferences(0);
        SharedPreferences.Editor next = modern.edit();
        if (!modern.contains("favorites") && old.contains("favorites")) {
            next.putStringSet("favorites", new HashSet(old.getStringSet("favorites", Collections.emptySet())));
        }
        if (!modern.contains("history") && old.contains("history")) {
            next.putString("history", old.getString("history", ""));
        }
        next.apply();
    }

    private void showQueue() {
        if (!bound || player == null) return;
        QueueDialog.show(this, player);
    }

    /* JADX INFO: renamed from: lambda$showQueue$19$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m121lambda$showQueue$19$localqingyinmobileMainActivity(List order, DialogInterface dialog, int which) {
        ensureForeground();
        this.player.selectQueue(((Integer) order.get(which)).intValue());
    }

    private void showSleepTimer() {
        String title;
        if (!this.bound) {
            Toast.makeText(this, "播放器正在初始化", 0).show();
            return;
        }
        long remaining = this.player.sleepRemainingMs();
        if (remaining > 0) {
            title = "睡眠定时 · 剩余约 " + ((59999 + remaining) / 60000) + " 分钟";
        } else {
            title = "睡眠定时 · 未开启";
        }
        String[] choices = {"关闭定时", "10 分钟", "20 分钟", "30 分钟", "45 分钟", "60 分钟", "90 分钟", "120 分钟"};
        final int[] minutes = {0, 10, 20, 30, 45, 60, 90, 120};
        new AlertDialog.Builder(this).setTitle(title).setItems(choices, new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda24
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m122lambda$showSleepTimer$20$localqingyinmobileMainActivity(minutes, dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$showSleepTimer$20$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m122lambda$showSleepTimer$20$localqingyinmobileMainActivity(int[] minutes, DialogInterface dialog, int which) {
        this.player.setSleepTimerMinutes(minutes[which]);
        Toast.makeText(this, minutes[which] == 0 ? "已取消睡眠定时" : minutes[which] + " 分钟后自动暂停", 0).show();
    }

    private void pickFiles() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("audio/*");
        intent.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);
        intent.addFlags(65);
        try {
            startActivityForResult(intent, 201);
        } catch (Exception e) {
            Toast.makeText(this, "系统中没有可用的文件选择器", 0).show();
        }
    }

    @Override // android.app.Activity
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code != 201 || result != -1 || data == null) {
            return;
        }
        Set<String> imported = new HashSet<>(library().getStringSet("imported_uris", Collections.emptySet()));
        ArrayList<Uri> selection = new ArrayList<>();
        if (data.getClipData() != null) {
            ClipData batch = data.getClipData();
            for (int i = 0; i < batch.getItemCount(); i++) {
                selection.add(batch.getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            selection.add(data.getData());
        }
        int granted = 0;
        for (Uri uri : selection) {
            if (uri != null && "content".equalsIgnoreCase(uri.getScheme())) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, 1);
                    if (imported.add(uri.toString())) {
                        granted++;
                    }
                } catch (SecurityException e) {
                }
            }
        }
        library().edit().putStringSet("imported_uris", imported).apply();
        Toast.makeText(this, granted > 0 ? "已导入 " + granted + " 个音频文件" : "没有新增音频，检查文件提供者是否允许长期访问", 1).show();
        if (granted > 0) {
            loadSongs();
        }
    }

    private void showSongActions(final Track song){
        boolean saved=favorites().contains(Long.toString(song.id));
        final boolean imported=library().getStringSet("imported_uris",Collections.emptySet()).contains(song.uri);
        QingyinUi.Sheet sheet=new QingyinUi.Sheet(this,song.title,song.artist,false);sheet.setSong(song);
        sheet.addAction("next","下一首播放","",()->queueLocalSong(song,true));
        sheet.addAction("add","加入播放队列","",()->queueLocalSong(song,false));sheet.addSeparator();
        sheet.addAction("heart",saved?"取消收藏":"加入收藏","",()->m123lambda$showSongActions$21$localqingyinmobileMainActivity(song,imported,sheet.dialog,0));
        if(imported)sheet.addAction("remove","从青音列表移除","保留原音频文件",()->m123lambda$showSongActions$21$localqingyinmobileMainActivity(song,true,sheet.dialog,1));
        sheet.show();
    }
    private void queueLocalSong(Track song,boolean next){
        if(!bound||player==null){Toast.makeText(this,"播放器正在初始化",Toast.LENGTH_SHORT).show();return;}
        boolean changed=next?player.playNext(song):player.enqueue(song);
        Toast.makeText(this,changed?(next?"已设为下一首":"已加入播放队列"):"歌曲已在当前队列中",Toast.LENGTH_SHORT).show();
    }

    /* JADX INFO: renamed from: lambda$showSongActions$21$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m123lambda$showSongActions$21$localqingyinmobileMainActivity(Track song, boolean imported, DialogInterface dialog, int selected) {
        if (selected == 0) {
            Set<String> favoriteIds = new HashSet<>(favorites());
            if (!favoriteIds.add(Long.toString(song.id))) {
                favoriteIds.remove(Long.toString(song.id));
            }
            library().edit().putStringSet("favorites", favoriteIds).apply();
            updatePlayer();
            filter();
            return;
        }
        if (imported) {
            Set<String> uris = new HashSet<>(library().getStringSet("imported_uris", Collections.emptySet()));
            uris.remove(song.uri);
            library().edit().putStringSet("imported_uris", uris).apply();
            try {
                getContentResolver().releasePersistableUriPermission(Uri.parse(song.uri), 1);
            } catch (Exception e) {
            }
            Toast.makeText(this, "已从列表移除，原始音频文件未删除", 0).show();
            loadSongs();
        }
    }

    private String permission() {
        return Build.VERSION.SDK_INT >= 33 ? "android.permission.READ_MEDIA_AUDIO" : "android.permission.READ_EXTERNAL_STORAGE";
    }

    private void askPermission() {
        loadSongs();
        if (checkSelfPermission(permission()) != 0) {
            requestPermissions(new String[]{permission()}, 100);
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int req, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(req, permissions, grants);
        if (req != 100 || grants.length <= 0 || grants[0] != 0) {
            if (req == 100) {
                Toast.makeText(this, "未授权系统媒体库；仍可通过“导入音频文件”播放选中的音乐", 1).show();
                loadSongs();
                return;
            }
            return;
        }
        loadSongs();
    }

    private void loadSongs() {
        this.count.setText("正在扫描设备音频…");
        this.disk.execute(new Runnable() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m117lambda$loadSongs$24$localqingyinmobileMainActivity();
            }
        });
    }

    /* JADX WARN: Code duplicated, block: B:35:0x00c9  */
    /* JADX WARN: Code duplicated, block: B:52:0x00da A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:54:0x00c3 A[SYNTHETIC] */
    /* JADX INFO: renamed from: lambda$loadSongs$24$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m117lambda$loadSongs$24$localqingyinmobileMainActivity() {
        PlayerSongActions.reconcileDownloads(getApplicationContext());
        final ArrayList<Track> result = new ArrayList<>();
        String[] fields = {"_id", "title", "artist", "mime_type", "duration"};
        try (Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fields, "is_music != 0", null, "title ASC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow("_id"), titleCol = cursor.getColumnIndexOrThrow("title");
                int artistCol = cursor.getColumnIndexOrThrow("artist"), mimeCol = cursor.getColumnIndexOrThrow("mime_type");
                int durationCol = cursor.getColumnIndexOrThrow("duration");
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol), duration = cursor.isNull(durationCol) ? 0 : cursor.getLong(durationCol);
                    if (duration < 1000) continue;
                    result.add(new Track(id, cursor.getString(titleCol), cursor.getString(artistCol), cursor.getString(mimeCol), duration,
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id), 0));
                }
            }
        } catch (Exception ignored) { }
        for (String link : new HashSet<String>(library().getStringSet("imported_uris", Collections.emptySet()))) {
            try { Track imported = ImportedAudio.inspect(this, Uri.parse(link)); if (imported != null) result.add(imported); }
            catch (Exception ignored) { }
        }
        result.sort((left, right) -> left.title.compareToIgnoreCase(right.title));
        runOnUiThread(() -> m116lambda$loadSongs$23$localqingyinmobileMainActivity(result));
    }

    /* JADX INFO: renamed from: lambda$loadSongs$23$local-qingyin-mobile-MainActivity, reason: not valid java name */
    /* synthetic */ void m116lambda$loadSongs$23$localqingyinmobileMainActivity(ArrayList result) {
        if (isDestroyed()) {
            return;
        }
        this.all.clear();
        this.all.addAll(result);
        filter();
    }

    private Set<String> favorites() {
        return library().getStringSet("favorites", Collections.emptySet());
    }

    private void favoriteCurrent() {
        if (!this.bound || this.player.current() == null) {
            return;
        }
        if (this.player.current().isOnline()) {
            OnlineLibraryStore.toggle(this, this.player.current());
            updatePlayer();
            filter();
            return;
        }
        Set<String> saved = new HashSet<>(favorites());
        String id = Long.toString(this.player.current().id);
        if (!saved.add(id)) {
            saved.remove(id);
        }
        library().edit().putStringSet("favorites", saved).apply();
        updatePlayer();
        filter();
    }

    private void remember(long id) {
        String old = library().getString("history", "");
        List<String> ids = new ArrayList<>(Arrays.asList(old.split(",")));
        ids.remove(Long.toString(id));
        ids.add(0, Long.toString(id));
        if (ids.size() > 60) {
            ids = ids.subList(0, 60);
        }
        library().edit().putString("history", String.join(",", ids)).apply();
    }

    private void filter() {
        Set<String> saved = favorites();
        String str = "";
        final ArrayList<String> history = new ArrayList<>(Arrays.asList(library().getString("history", "").split(",")));
        this.visible.clear();
        for (Track song : this.all) {
            if (!"收藏".equals(this.section) || saved.contains(Long.toString(song.id))) {
                if (!"最近".equals(this.section) || history.contains(Long.toString(song.id))) {
                    if (this.searchText.isEmpty() || (song.title + " " + song.artist).toLowerCase(Locale.ROOT).contains(this.searchText)) {
                        this.visible.add(song);
                    }
                }
            }
        }
        if ("最近".equals(this.section)) {
            this.visible.sort(new Comparator() { // from class: local.qingyin.mobile.MainActivity$$ExternalSyntheticLambda21
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    ArrayList arrayList = history;
                    return Integer.compare(arrayList.indexOf(Long.toString(((Track) obj).id)), arrayList.indexOf(Long.toString(((Track) obj2).id)));
                }
            });
        }
        if (checkSelfPermission(permission()) != 0) {
            str = "（仅手动导入，未授权系统扫描）";
        } else if (this.all.isEmpty()) {
            str = "（可先将音乐文件放入手机）";
        }
        String suffix = str;
        this.count.setText(this.section + " · " + this.visible.size() + " 首" + suffix);
        this.adapter.notifyDataSetChanged();
    }

    private void playAt(int index) {
        if (!this.bound) {
            Toast.makeText(this, "播放器正在初始化", 0).show();
            return;
        }
        if (index < 0 || index >= this.visible.size()) {
            return;
        }
        ArrayList<Track> songs = new ArrayList<>(this.visible);
        ensureForeground();
        this.player.playQueue(songs, index);
        filter();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlayer() {
        if (this.nowTitle == null) {
            return;
        }
        Track song = this.bound ? this.player.current() : null;
        this.nowTitle.setText(song == null ? "还没有播放音乐" : song.title);
        this.nowArtist.setText(song == null ? "选择喜欢的歌曲" : song.artist);
        this.format.setText(song == null ? "青音 · 准备就绪" : song.qualityLabel());
        this.toggle.setText((this.bound && this.player.isPlaying()) ? "Ⅱ" : "▶");
        this.heart.setText((song == null || !favorites().contains(Long.toString(song.id))) ? "♡" : "♥");
        if (this.bound && this.modeButton != null) {
            QueueNavigator.Mode mode = this.player.mode();
            String[] names = {"列表循环", "单曲循环", "顺序播放", "随机播放"};
            String[] symbols = {"↻", "↻₁", "→", "⤨"};
            this.modeButton.setText(symbols[mode.ordinal()]);
            this.modeButton.setContentDescription("播放模式：" + names[mode.ordinal()] + "，点击切换");
        }
        if (this.queueButton != null) {
            this.queueButton.setContentDescription("打开播放队列");
        }
        this.seek.setEnabled((song == null || this.player.isPreparing()) ? false : true);
        updateProgress();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProgress() {
        if (this.seek == null) {
            return;
        }
        int total = this.bound ? Math.max(0, this.player.duration()) : 0;
        int at = this.bound ? Math.max(0, this.player.currentPosition()) : 0;
        this.seek.setMax(Math.max(1, total));
        this.seek.setProgress(Math.min(at, Math.max(1, total)));
        this.currentTime.setText(clock(at));
        this.totalTime.setText(clock(total));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String clock(long ms) {
        long seconds = Math.max(0L, ms) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", Long.valueOf(seconds / 60), Long.valueOf(seconds % 60));
    }

    private final class TrackAdapter extends BaseAdapter {
        private TrackAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return MainActivity.this.visible.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int position) {
            return MainActivity.this.visible.get(position);
        }

        @Override // android.widget.Adapter
        public long getItemId(int position) {
            return ((Track) MainActivity.this.visible.get(position)).id;
        }

        @Override // android.widget.Adapter
        public View getView(int position, View reused, ViewGroup parent) {
            Row row;
            View view;
            if (reused == null) {
                LinearLayout linearLayoutHorizontal = MainActivity.this.horizontal();
                linearLayoutHorizontal.setBackground(MainActivity.this.bg(-1, 15.0f));
                linearLayoutHorizontal.setPadding(MainActivity.this.dp(10.0f), MainActivity.this.dp(8.0f), MainActivity.this.dp(10.0f), MainActivity.this.dp(8.0f));
                TextView note = MainActivity.this.text("♫", 23, -1815446, true);
                note.setGravity(17);
                note.setBackground(MainActivity.this.bg(-268050, 11.0f));
                linearLayoutHorizontal.addView(note, MainActivity.this.lp(43, 43));
                LinearLayout middle = MainActivity.this.vertical();
                middle.setPadding(MainActivity.this.dp(11.0f), 0, MainActivity.this.dp(6.0f), 0);
                TextView title = MainActivity.this.text("", 14, -15000801, true);
                title.setSingleLine();
                title.setEllipsize(TextUtils.TruncateAt.END);
                TextView artist = MainActivity.this.text("", 12, -8815739, false);
                artist.setSingleLine();
                artist.setEllipsize(TextUtils.TruncateAt.END);
                middle.addView(title);
                middle.addView(artist);
                LinearLayout.LayoutParams weight = MainActivity.this.lp(0, -2);
                weight.weight = 1.0f;
                linearLayoutHorizontal.addView(middle, weight);
                TextView time = MainActivity.this.text("", 11, -8815739, false);
                linearLayoutHorizontal.addView(time);
                row = new Row(linearLayoutHorizontal, note, title, artist, time);
                linearLayoutHorizontal.setTag(row);
                view = linearLayoutHorizontal;
            } else {
                row = (Row) reused.getTag();
                view = reused;
            }
            Track t = (Track) MainActivity.this.visible.get(position);
            boolean active = MainActivity.this.bound && MainActivity.this.player.current() != null && MainActivity.this.player.current().id == t.id;
            row.title.setText(t.title);
            row.title.setTextColor(active ? -1815446 : -15000801);
            row.note.setText(active ? "▶" : "♫");
            row.frame.setBackground(MainActivity.this.bg(active ? -1314561 : -1, 15.0f));
            row.artist.setText(t.artist + " · " + t.qualityLabel());
            row.time.setText(MainActivity.clock(t.durationMs));
            return view;
        }
    }

    private static final class Row {
        final TextView artist;
        final LinearLayout frame;
        final TextView note;
        final TextView time;
        final TextView title;

        Row(LinearLayout frame, TextView note, TextView title, TextView artist, TextView time) {
            this.frame = frame;
            this.note = note;
            this.title = title;
            this.artist = artist;
            this.time = time;
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.handler.removeCallbacks(this.ticker);
        this.handler.removeCallbacks(this.delayedFilter);
        if (this.bound && this.player != null) {
            this.player.setListener(null);
        }
        if (this.bindingRequested) {
            unbindService(this.connection);
            this.bindingRequested = false;
        }
        this.bound = false;
        this.disk.shutdownNow();
        super.onDestroy();
    }
}
