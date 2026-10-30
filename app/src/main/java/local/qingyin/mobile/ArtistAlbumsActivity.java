package local.qingyin.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class ArtistAlbumsActivity extends Activity {
    private long artistId;
    private LinearLayout list;
    private TextView more;
    private TextView status;
    private int tint;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean alive = true;
    private boolean busy = false;
    private boolean hasMore = true;
    private int page = 0;

    private int dp(int x) {
        return QingyinUi.dp(this, x);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle state) {
        super.onCreate(state);
        this.artistId = getIntent().getLongExtra("artist_id", 0L);
        String singer = getIntent().getStringExtra("artist_name");
        this.tint = getIntent().getIntExtra("theme", -13356985);
        getWindow().setStatusBarColor(this.tint);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ArtistTheme.darker(this.tint, 0.55f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(1);
        body.setPadding(dp(18), dp(14), dp(18), dp(36));
        body.setBackground(ArtistTheme.gradient(this.tint, ArtistTheme.darker(this.tint, 0.55f), 0));
        body.setMinimumHeight(getResources().getDisplayMetrics().heightPixels);
        scrollView.addView(body);
        setContentView(scrollView);
        MusicOverlayDock.attach(this);
        TextView back = QingyinUi.text(this, "‹   " + (singer == null ? "歌手" : singer) + " · 专辑", 23, -1, true);
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistAlbumsActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistAlbumsActivity.this.m34lambda$onCreate$0$localqingyinmobileArtistAlbumsActivity(view);
            }
        });
        body.addView(back, new LinearLayout.LayoutParams(-1, dp(62)));
        this.list = new LinearLayout(this);
        this.list.setOrientation(1);
        body.addView(this.list);
        this.status = QingyinUi.text(this, "正在获取专辑…", 13, -1843999, false);
        body.addView(this.status);
        this.more = QingyinUi.text(this, "加载更多", 15, -1, true);
        this.more.setGravity(17);
        this.more.setBackground(ArtistTheme.glass(this, 16));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, dp(45));
        mp.topMargin = dp(18);
        body.addView(this.more, mp);
        this.more.setVisibility(8);
        this.more.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistAlbumsActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistAlbumsActivity.this.m35lambda$onCreate$1$localqingyinmobileArtistAlbumsActivity(view);
            }
        });
        if (this.artistId > 0) {
            fetch(1);
        } else {
            this.status.setText("没有可核实的歌手身份");
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$local-qingyin-mobile-ArtistAlbumsActivity, reason: not valid java name */
    /* synthetic */ void m34lambda$onCreate$0$localqingyinmobileArtistAlbumsActivity(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$local-qingyin-mobile-ArtistAlbumsActivity, reason: not valid java name */
    /* synthetic */ void m35lambda$onCreate$1$localqingyinmobileArtistAlbumsActivity(View v) {
        fetch(this.page + 1);
    }

    private void fetch(final int next) {
        if (this.busy || !this.hasMore) {
            return;
        }
        this.busy = true;
        this.more.setVisibility(8);
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.ArtistAlbumsActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ArtistAlbumsActivity.this.m33lambda$fetch$3$localqingyinmobileArtistAlbumsActivity(next);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetch$3$local-qingyin-mobile-ArtistAlbumsActivity, reason: not valid java name */
    /* synthetic */ void m33lambda$fetch$3$localqingyinmobileArtistAlbumsActivity(final int next) {
        JSONObject data = null;
        String message = "";
        try {
            data = OnlineApi.call(getApplicationContext(), "artistAlbums", new JSONObject().put("id", this.artistId).put("page", next));
        } catch (Exception e) {
            message = e.getMessage() == null ? "网络异常" : e.getMessage();
        }
        final JSONObject result = data;
        final String error = message;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.ArtistAlbumsActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ArtistAlbumsActivity.this.m32lambda$fetch$2$localqingyinmobileArtistAlbumsActivity(result, error, next);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetch$2$local-qingyin-mobile-ArtistAlbumsActivity, reason: not valid java name */
    /* synthetic */ void m32lambda$fetch$2$localqingyinmobileArtistAlbumsActivity(JSONObject result, String error, int next) {
        if (this.alive) {
            this.busy = false;
            if (result == null) {
                this.status.setText("专辑加载失败 · " + error);
                this.more.setText("重试");
                this.more.setVisibility(0);
                return;
            }
            JSONArray arr = result.optJSONArray("albums");
            int count = 0;
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject album = arr.optJSONObject(i);
                    if (album != null) {
                        long albumId = album.optLong("id", 0L);
                        if (albumId != 0) {
                            count++;
                            addAlbum(album, albumId);
                        }
                    }
                }
            }
            this.page = next;
            this.hasMore = result.optBoolean("hasMore");
            this.status.setVisibility(this.list.getChildCount() > 0 ? 8 : 0);
            this.status.setText("暂无可展示的专辑");
            this.more.setText("加载更多");
            this.more.setVisibility(this.hasMore ? 0 : 8);
        }
    }

    private void addAlbum(JSONObject album, final long id) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(16);
        row.setPadding(dp(9), dp(7), dp(9), dp(7));
        row.setBackground(ArtistTheme.glass(this, 15));
        FrameLayout art = Artwork.frame(this, 12);
        Artwork.bind(art, album.optString("cover", ""));
        row.addView(art, new LinearLayout.LayoutParams(dp(82), dp(82)));
        LinearLayout label = new LinearLayout(this);
        label.setOrientation(1);
        label.setPadding(dp(15), 0, 0, 0);
        TextView title = QingyinUi.text(this, album.optString("title"), 17, -1, true);
        label.addView(title);
        TextView year = QingyinUi.text(this, album.optString("year", ""), 13, -2172448, false);
        label.addView(year);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView arrow = QingyinUi.text(this, "›", 27, -1, false);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(25), -2));
        row.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistAlbumsActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistAlbumsActivity.this.m31lambda$addAlbum$4$localqingyinmobileArtistAlbumsActivity(id, view);
            }
        });
        row.setContentDescription("查看专辑" + album.optString("title"));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(102));
        rp.bottomMargin = dp(9);
        this.list.addView(row, rp);
    }

    /* JADX INFO: renamed from: lambda$addAlbum$4$local-qingyin-mobile-ArtistAlbumsActivity, reason: not valid java name */
    /* synthetic */ void m31lambda$addAlbum$4$localqingyinmobileArtistAlbumsActivity(long id, View v) {
        startActivity(new Intent(this, (Class<?>) AlbumDetailActivity.class).putExtra("album_id", id).putExtra("theme", this.tint));
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.alive = false;
        this.worker.shutdownNow();
        this.ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
