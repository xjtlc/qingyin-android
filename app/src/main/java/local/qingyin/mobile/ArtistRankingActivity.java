package local.qingyin.mobile;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class ArtistRankingActivity extends Activity {
    private boolean bound;
    private TextView hotButton;
    private long id;
    private TextView more;
    private String name;
    private TextView newButton;
    private PlaybackService playback;
    private LinearLayout rows;
    private TextView status;
    private int tint;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<Track> songs = new ArrayList<>();
    private boolean active = true;
    private boolean busy = false;
    private boolean hasMore = true;
    private String order = "hot";
    private int requestVersion = 0;
    private int page = 0;
    private final ServiceConnection connection = new ServiceConnection() { // from class: local.qingyin.mobile.ArtistRankingActivity.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName c, IBinder b) {
            ArtistRankingActivity.this.playback = ((PlaybackService.LocalBinder) b).service();
            ArtistRankingActivity.this.bound = true;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName c) {
            ArtistRankingActivity.this.bound = false;
            ArtistRankingActivity.this.playback = null;
        }
    };

    private int dp(int x) {
        return QingyinUi.dp(this, x);
    }

    private TextView text(String x, int sz, boolean bold) {
        return QingyinUi.text(this, x, sz, -329478, bold);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle state) {
        super.onCreate(state);
        this.id = getIntent().getLongExtra("artist_id", 0L);
        this.name = getIntent().getStringExtra("artist_name");
        if (this.name == null) {
            this.name = "";
        }
        this.tint = getIntent().getIntExtra("theme", -13356985);
        getWindow().setStatusBarColor(this.tint);
        getWindow().setNavigationBarColor(ArtistTheme.darker(this.tint, 0.7f));
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ArtistTheme.darker(this.tint, 0.62f));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(18), dp(18), dp(18), dp(32));
        linearLayout.setBackground(ArtistTheme.gradient(this.tint, ArtistTheme.darker(this.tint, 0.62f), 0));
        linearLayout.setMinimumHeight(getResources().getDisplayMetrics().heightPixels);
        scrollView.addView(linearLayout);
        setContentView(scrollView);
        MusicOverlayDock.attach(this);
        TextView back = text("‹   " + this.name + " · 热门歌曲", 23, true);
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistRankingActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistRankingActivity.this.m42lambda$onCreate$0$localqingyinmobileArtistRankingActivity(view);
            }
        });
        back.setContentDescription("返回歌手主页");
        linearLayout.addView(back, new LinearLayout.LayoutParams(-1, dp(54)));
        LinearLayout sorting = new LinearLayout(this);
        sorting.setGravity(16);
        sorting.setPadding(0, dp(12), 0, dp(6));
        this.hotButton = sortOption("按热度", true);
        this.newButton = sortOption("按发布时间", false);
        sorting.addView(this.hotButton, new LinearLayout.LayoutParams(0, dp(43), 1.0f));
        LinearLayout.LayoutParams recentLp = new LinearLayout.LayoutParams(0, dp(43), 1.0f);
        recentLp.leftMargin = dp(10);
        sorting.addView(this.newButton, recentLp);
        this.hotButton.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistRankingActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistRankingActivity.this.m43lambda$onCreate$1$localqingyinmobileArtistRankingActivity(view);
            }
        });
        this.newButton.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistRankingActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistRankingActivity.this.m44lambda$onCreate$2$localqingyinmobileArtistRankingActivity(view);
            }
        });
        linearLayout.addView(sorting, new LinearLayout.LayoutParams(-1, -2));
        this.rows = new LinearLayout(this);
        this.rows.setOrientation(1);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(-1, -2);
        listLp.topMargin = dp(18);
        linearLayout.addView(this.rows, listLp);
        this.status = text("正在获取真实歌曲…", 13, false);
        this.status.setPadding(dp(8), dp(15), 0, dp(15));
        linearLayout.addView(this.status);
        this.more = text("加载更多", 15, true);
        this.more.setGravity(17);
        this.more.setBackground(ArtistTheme.glass(this, 16));
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(-1, dp(48));
        ml.topMargin = dp(10);
        linearLayout.addView(this.more, ml);
        this.more.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistRankingActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistRankingActivity.this.m45lambda$onCreate$3$localqingyinmobileArtistRankingActivity(view);
            }
        });
        this.more.setVisibility(8);
        this.bound = bindService(new Intent(this, (Class<?>) PlaybackService.class), this.connection, 1);
        fetch(1);
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$local-qingyin-mobile-ArtistRankingActivity, reason: not valid java name */
    /* synthetic */ void m42lambda$onCreate$0$localqingyinmobileArtistRankingActivity(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$local-qingyin-mobile-ArtistRankingActivity, reason: not valid java name */
    /* synthetic */ void m43lambda$onCreate$1$localqingyinmobileArtistRankingActivity(View v) {
        selectOrder("hot");
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$local-qingyin-mobile-ArtistRankingActivity, reason: not valid java name */
    /* synthetic */ void m44lambda$onCreate$2$localqingyinmobileArtistRankingActivity(View v) {
        selectOrder("latest");
    }

    /* JADX INFO: renamed from: lambda$onCreate$3$local-qingyin-mobile-ArtistRankingActivity, reason: not valid java name */
    /* synthetic */ void m45lambda$onCreate$3$localqingyinmobileArtistRankingActivity(View v) {
        fetch(this.page + 1);
    }

    private TextView sortOption(String title, boolean selected) {
        TextView item = text(title, 14, selected);
        item.setGravity(17);
        item.setBackground(selected ? QingyinUi.rounded(this, 1677721599, 17.0f) : ArtistTheme.glass(this, 17));
        item.setContentDescription("切换" + title);
        return item;
    }

    private void selectOrder(String str) {
        if (str.equals(this.order)) {
            return;
        }
        this.order = str;
        this.requestVersion++;
        this.busy = false;
        this.hasMore = true;
        this.page = 0;
        this.songs.clear();
        this.rows.removeAllViews();
        this.hotButton.setBackground("hot".equals(str) ? QingyinUi.rounded(this, 1677721599, 17.0f) : ArtistTheme.glass(this, 17));
        this.newButton.setBackground("latest".equals(str) ? QingyinUi.rounded(this, 1677721599, 17.0f) : ArtistTheme.glass(this, 17));
        this.hotButton.setTypeface(null, "hot".equals(str) ? 1 : 0);
        this.newButton.setTypeface(null, "latest".equals(str) ? 1 : 0);
        fetch(1);
    }

    private void fetch(final int next) {
        if (this.busy || !this.hasMore) {
            return;
        }
        this.busy = true;
        this.more.setVisibility(8);
        this.status.setText("正在加载歌曲…");
        this.status.setVisibility(0);
        final int ticket = this.requestVersion;
        final String selectedSort = this.order;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.ArtistRankingActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                ArtistRankingActivity.this.m41lambda$fetch$5$localqingyinmobileArtistRankingActivity(next, selectedSort, ticket);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetch$5$local-qingyin-mobile-ArtistRankingActivity, reason: not valid java name */
    /* synthetic */ void m41lambda$fetch$5$localqingyinmobileArtistRankingActivity(final int next, String selectedSort, final int ticket) {
        JSONObject response = null;
        String problem = "";
        try {
            response = OnlineApi.call(getApplicationContext(), "artistTracks", new JSONObject().put("id", this.id).put("name", this.name).put("page", next).put("sort", selectedSort));
        } catch (Exception e) {
            problem = e.getMessage() == null ? "网络异常" : e.getMessage();
        }
        final JSONObject data = response;
        final String error = problem;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.ArtistRankingActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ArtistRankingActivity.this.m40lambda$fetch$4$localqingyinmobileArtistRankingActivity(ticket, data, error, next);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetch$4$local-qingyin-mobile-ArtistRankingActivity, reason: not valid java name */
    /* synthetic */ void m40lambda$fetch$4$localqingyinmobileArtistRankingActivity(int ticket, JSONObject data, String error, int next) {
        if (!this.active || ticket != this.requestVersion) {
            return;
        }
        this.busy = false;
        if (data == null) {
            this.status.setText("加载失败 · " + error);
            this.more.setText("重试");
            this.more.setVisibility(0);
            return;
        }
        JSONArray entries = data.optJSONArray("songs");
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                Track song = OnlineApi.asTrack(entries.optJSONObject(i));
                if (song != null) {
                    int position = this.songs.size();
                    this.songs.add(song);
                    addRow(song, position);
                }
            }
        }
        this.page = next;
        this.hasMore = data.optBoolean("hasMore", false);
        this.status.setVisibility(this.songs.isEmpty() ? 0 : 8);
        if (this.songs.isEmpty()) {
            this.status.setText("暂无可显示的歌曲");
        }
        this.more.setText("加载更多");
        this.more.setVisibility(this.hasMore ? 0 : 8);
    }

    private void addRow(Track song, final int at) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(16);
        row.setPadding(dp(9), dp(6), dp(9), dp(6));
        TextView rank = text(String.format(Locale.ROOT, "%02d", Integer.valueOf(at + 1)), 13, true);
        rank.setGravity(17);
        row.addView(rank, new LinearLayout.LayoutParams(dp(38), dp(55)));
        FrameLayout cover = Artwork.frame(this, 10);
        Artwork.bind(cover, song.coverUrl);
        row.addView(cover, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(1);
        info.setPadding(dp(12), 0, 0, 0);
        TextView title = text(song.title, 16, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title);
        TextView artist = text(song.artist, 12, false);
        artist.setTextColor(-1976096);
        info.addView(artist);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView play = text("▶", 18, false);
        row.addView(play, new LinearLayout.LayoutParams(dp(28), dp(35)));
        row.setBackground(ArtistTheme.glass(this, 12));
        row.setContentDescription("播放" + song.title);
        row.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistRankingActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistRankingActivity.this.m39lambda$addRow$6$localqingyinmobileArtistRankingActivity(at, view);
            }
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(69));
        rp.bottomMargin = dp(6);
        this.rows.addView(row, rp);
    }

    /* JADX INFO: renamed from: lambda$addRow$6$local-qingyin-mobile-ArtistRankingActivity, reason: not valid java name */
    /* synthetic */ void m39lambda$addRow$6$localqingyinmobileArtistRankingActivity(int at, View v) {
        if (!this.bound || this.playback == null) {
            Toast.makeText(this, "播放器未就绪", 0).show();
            return;
        }
        Intent svc = new Intent(this, (Class<?>) PlaybackService.class);
        startForegroundService(svc);
        this.playback.playQueue(new ArrayList(this.songs), at);
        startActivity(new Intent(this, (Class<?>) NowPlayingActivity.class));
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.active = false;
        if (this.bound) {
            unbindService(this.connection);
        }
        this.worker.shutdownNow();
        this.ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
