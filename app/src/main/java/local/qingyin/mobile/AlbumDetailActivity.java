package local.qingyin.mobile;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class AlbumDetailActivity extends Activity {
    private long albumId;
    private FrameLayout cover;
    private TextView meta;
    private TextView more;
    private PlaybackService player;
    private LinearLayout songRows;
    private TextView status;
    private int tint;
    private TextView title;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private final ArrayList<Track> songs = new ArrayList<>();
    private boolean alive = true;
    private boolean bound = false;
    private boolean busy = false;
    private boolean hasMore = true;
    private int page = 0;
    private final ServiceConnection connection = new ServiceConnection() { // from class: local.qingyin.mobile.AlbumDetailActivity.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName c, IBinder binder) {
            AlbumDetailActivity.this.player = ((PlaybackService.LocalBinder) binder).service();
            AlbumDetailActivity.this.bound = true;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName c) {
            AlbumDetailActivity.this.player = null;
            AlbumDetailActivity.this.bound = false;
        }
    };

    private int dp(int n) {
        return QingyinUi.dp(this, n);
    }

    private TextView txt(String value, int sp, boolean bold) {
        return QingyinUi.text(this, value, sp, -1, bold);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle state) {
        super.onCreate(state);
        this.albumId = getIntent().getLongExtra("album_id", 0L);
        this.tint = getIntent().getIntExtra("theme", -13356985);
        if (this.albumId < 1) {
            finish();
            return;
        }
        getWindow().setStatusBarColor(this.tint);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ArtistTheme.darker(this.tint, 0.51f));
        LinearLayout body = new LinearLayout(this);
        body.setGravity(1);
        body.setOrientation(1);
        body.setPadding(dp(18), dp(15), dp(18), dp(38));
        body.setBackground(ArtistTheme.gradient(this.tint, ArtistTheme.darker(this.tint, 0.51f), 0));
        body.setMinimumHeight(getResources().getDisplayMetrics().heightPixels);
        scrollView.addView(body);
        setContentView(scrollView);
        MusicOverlayDock.attach(this);
        TextView back = txt("‹   返回歌手", 17, true);
        back.setGravity(19);
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                AlbumDetailActivity.this.m8lambda$onCreate$0$localqingyinmobileAlbumDetailActivity(view);
            }
        });
        body.addView(back, new LinearLayout.LayoutParams(-1, dp(46)));
        this.cover = Artwork.frame(this, 20);
        LinearLayout.LayoutParams coverLp = new LinearLayout.LayoutParams(dp(218), dp(218));
        coverLp.topMargin = dp(13);
        body.addView(this.cover, coverLp);
        this.title = txt("正在加载专辑…", 23, true);
        this.title.setGravity(17);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
        tl.topMargin = dp(20);
        body.addView(this.title, tl);
        this.meta = txt("", 13, false);
        this.meta.setGravity(17);
        this.meta.setTextColor(-1712156);
        body.addView(this.meta);
        TextView play = txt("▶  播放专辑", 16, true);
        play.setGravity(17);
        play.setBackground(ArtistTheme.glass(this, 24));
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-1, dp(52));
        pl.topMargin = dp(23);
        pl.bottomMargin = dp(20);
        body.addView(play, pl);
        play.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                AlbumDetailActivity.this.m9lambda$onCreate$1$localqingyinmobileAlbumDetailActivity(view);
            }
        });
        this.songRows = new LinearLayout(this);
        this.songRows.setOrientation(1);
        body.addView(this.songRows);
        this.status = txt("正在加载歌曲…", 13, false);
        body.addView(this.status);
        this.more = txt("加载更多", 15, true);
        this.more.setGravity(17);
        this.more.setBackground(ArtistTheme.glass(this, 18));
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(-1, dp(48));
        ml.topMargin = dp(13);
        body.addView(this.more, ml);
        this.more.setVisibility(8);
        this.more.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                AlbumDetailActivity.this.m10lambda$onCreate$2$localqingyinmobileAlbumDetailActivity(view);
            }
        });
        this.bound = bindService(new Intent(this, (Class<?>) PlaybackService.class), this.connection, 1);
        fetchDetails();
        fetchSongs(1);
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m8lambda$onCreate$0$localqingyinmobileAlbumDetailActivity(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m9lambda$onCreate$1$localqingyinmobileAlbumDetailActivity(View v) {
        play(0);
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m10lambda$onCreate$2$localqingyinmobileAlbumDetailActivity(View v) {
        fetchSongs(this.page + 1);
    }

    private void fetchDetails() {
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                AlbumDetailActivity.this.m5lambda$fetchDetails$5$localqingyinmobileAlbumDetailActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchDetails$5$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m5lambda$fetchDetails$5$localqingyinmobileAlbumDetailActivity() {
        JSONObject d = null;
        try {
            d = OnlineApi.call(getApplicationContext(), "albumDetails", new JSONObject().put("id", this.albumId)).optJSONObject("album");
        } catch (Exception e) {
        }
        final JSONObject data = d;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                AlbumDetailActivity.this.m4lambda$fetchDetails$4$localqingyinmobileAlbumDetailActivity(data);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchDetails$4$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m4lambda$fetchDetails$4$localqingyinmobileAlbumDetailActivity(JSONObject data) {
        if (this.alive) {
            if (data == null) {
                this.title.setText("暂未取得专辑资料");
                return;
            }
            this.title.setText(data.optString("title", "专辑"));
            this.meta.setText(data.optString("artist", "") + "  ·  " + data.optString("year", ""));
            String image = data.optString("cover", "");
            Artwork.bind(this.cover, image);
            Artwork.loadBitmap(image, new Consumer() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AlbumDetailActivity.this.m3lambda$fetchDetails$3$localqingyinmobileAlbumDetailActivity((Bitmap) obj);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$fetchDetails$3$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m3lambda$fetchDetails$3$localqingyinmobileAlbumDetailActivity(Bitmap bitmap) {
        if (!this.alive || bitmap == null) {
            return;
        }
        int newTint = ArtistTheme.color(bitmap);
        this.tint = newTint;
        getWindow().setStatusBarColor(newTint);
        ((View) this.cover.getParent()).setBackground(ArtistTheme.gradient(newTint, ArtistTheme.darker(newTint, 0.52f), 0));
    }

    private void fetchSongs(final int next) {
        if (this.busy || !this.hasMore) {
            return;
        }
        this.busy = true;
        this.more.setVisibility(8);
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                AlbumDetailActivity.this.m7lambda$fetchSongs$7$localqingyinmobileAlbumDetailActivity(next);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchSongs$7$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m7lambda$fetchSongs$7$localqingyinmobileAlbumDetailActivity(final int next) {
        JSONObject d = null;
        String error = "";
        try {
            d = OnlineApi.call(getApplicationContext(), "albumTracks", new JSONObject().put("id", this.albumId).put("page", next));
        } catch (Exception e) {
            error = e.getMessage() == null ? "网络异常" : e.getMessage();
        }
        final JSONObject data = d;
        final String problem = error;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                AlbumDetailActivity.this.m6lambda$fetchSongs$6$localqingyinmobileAlbumDetailActivity(data, problem, next);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchSongs$6$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m6lambda$fetchSongs$6$localqingyinmobileAlbumDetailActivity(JSONObject data, String problem, int next) {
        if (this.alive) {
            this.busy = false;
            if (data == null) {
                this.status.setText("曲目暂不可用 · " + problem);
                this.more.setText("重试");
                this.more.setVisibility(0);
                return;
            }
            JSONArray arr = data.optJSONArray("songs");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    Track track = OnlineApi.asTrack(arr.optJSONObject(i));
                    if (track != null) {
                        int pos = this.songs.size();
                        this.songs.add(track);
                        addRow(track, pos);
                    }
                }
            }
            this.page = next;
            this.hasMore = data.optBoolean("hasMore", false);
            this.status.setVisibility(this.songs.isEmpty() ? 0 : 8);
            if (this.songs.isEmpty()) {
                this.status.setText("暂无可播放的专辑歌曲");
            }
            this.more.setText("加载更多");
            this.more.setVisibility(this.hasMore ? 0 : 8);
        }
    }

    private void addRow(Track song, final int index) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(16);
        row.setBackground(ArtistTheme.glass(this, 13));
        row.setPadding(dp(12), dp(9), dp(8), dp(9));
        TextView number = txt(String.valueOf(index + 1), 13, true);
        row.addView(number, new LinearLayout.LayoutParams(dp(25), dp(42)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(1);
        TextView songName = txt(song.title, 15, true);
        songName.setSingleLine(true);
        songName.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(songName);
        TextView artist = txt(song.artist, 12, false);
        artist.setTextColor(-1843998);
        labels.addView(artist);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView play = txt("▶", 17, false);
        row.addView(play, new LinearLayout.LayoutParams(dp(29), dp(38)));
        row.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.AlbumDetailActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                AlbumDetailActivity.this.m2lambda$addRow$8$localqingyinmobileAlbumDetailActivity(index, view);
            }
        });
        row.setContentDescription("播放歌曲" + song.title);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(65));
        rp.bottomMargin = dp(6);
        this.songRows.addView(row, rp);
    }

    /* JADX INFO: renamed from: lambda$addRow$8$local-qingyin-mobile-AlbumDetailActivity, reason: not valid java name */
    /* synthetic */ void m2lambda$addRow$8$localqingyinmobileAlbumDetailActivity(int index, View v) {
        play(index);
    }

    private void play(int index) {
        if (this.songs.isEmpty()) {
            return;
        }
        if (!this.bound || this.player == null) {
            Toast.makeText(this, "播放器未准备好", 0).show();
            return;
        }
        Intent service = new Intent(this, (Class<?>) PlaybackService.class);
        startForegroundService(service);
        this.player.playQueue(new ArrayList(this.songs), Math.min(index, this.songs.size() - 1));
        startActivity(new Intent(this, (Class<?>) NowPlayingActivity.class));
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.alive = false;
        if (this.bound) {
            unbindService(this.connection);
        }
        this.worker.shutdownNow();
        this.ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
