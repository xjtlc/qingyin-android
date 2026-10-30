package local.qingyin.mobile;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class ArtistActivity extends Activity {
    private static final int MUTED = -2106659;
    private static final int WHITE = -1285;
    private LinearLayout albumRail;
    private long artistId;
    private String cover;
    private View fade;
    private ArtistActionIcon favoriteIcon;
    private boolean followed;
    private FrameLayout hero;
    private String name;
    private TextView nameView;
    private LinearLayout page;
    private FrameLayout photo;
    private PlaybackService player;
    private LinearLayout preview;
    private TextView rankLabel;
    private LinearLayout sections;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private final ArrayList<Track> tracks = new ArrayList<>();
    private boolean alive = true;
    private boolean bound = false;
    private boolean verified = false;
    private String portraitUrl = "";
    private String bio = "";
    private String birthday = "";
    private int theme = -13356985;
    private final ServiceConnection connection = new ServiceConnection() { // from class: local.qingyin.mobile.ArtistActivity.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName component, IBinder binder) {
            ArtistActivity.this.player = ((PlaybackService.LocalBinder) binder).service();
            ArtistActivity.this.bound = true;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName component) {
            ArtistActivity.this.bound = false;
            ArtistActivity.this.player = null;
        }
    };

    private int dp(int n) {
        return QingyinUi.dp(this, n);
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = QingyinUi.text(this, value, size, color, bold);
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(1);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(16);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private String favoriteKey() {
        return this.name.toLowerCase(Locale.ROOT).trim();
    }

    private boolean favorite() {
        return getSharedPreferences("artist_favorites", 0).getBoolean(favoriteKey(), false);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle state) {
        super.onCreate(state);
        this.name = getIntent().getStringExtra("artist_name");
        if (this.name == null || this.name.trim().isEmpty() || "未知歌手".equals(this.name)) {
            finish();
            return;
        }
        this.name = this.name.trim();
        this.cover = getIntent().getStringExtra("artist_cover");
        if (this.cover == null) {
            this.cover = "";
        }
        this.artistId = getIntent().getLongExtra("artist_id", 0L);
        this.followed = favorite();
        getWindow().setStatusBarColor(-13356985);
        getWindow().setNavigationBarColor(-14409683);
        getWindow().getDecorView().setSystemUiVisibility(0);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setBackgroundColor(this.theme);
        this.page = column();
        this.page.setBackgroundColor(this.theme);
        scroller.addView(this.page, new FrameLayout.LayoutParams(-1, -2));
        setContentView(scroller);
        MusicOverlayDock.attach(this);
        renderHero();
        renderSections();
        this.bound = bindService(new Intent(this, (Class<?>) PlaybackService.class), this.connection, 1);
        fetchProfile();
    }

    private void renderHero() {
        this.hero = new FrameLayout(this);
        this.hero.setBackgroundColor(this.theme);
        this.page.addView(this.hero, lp(-1, 430));
        this.photo = Artwork.frame(this, 0);
        FrameLayout.LayoutParams picture = new FrameLayout.LayoutParams(-1, -1);
        this.hero.addView(this.photo, picture);
        Artwork.bind(this.photo, this.cover);
        this.fade = new View(this);
        this.fade.setBackground(heroFade());
        this.hero.addView(this.fade, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout top = row();
        top.setPadding(dp(18), 0, dp(18), 0);
        FrameLayout back = circle("back", "返回", false);
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistActivity.this.m23lambda$renderHero$0$localqingyinmobileArtistActivity(view);
            }
        });
        top.addView(back, lp(45, 45));
        LinearLayout.LayoutParams spacer = new LinearLayout.LayoutParams(0, 1, 1.0f);
        top.addView(new View(this), spacer);
        FrameLayout.LayoutParams nav = new FrameLayout.LayoutParams(-1, dp(50), 48);
        nav.topMargin = dp(14);
        this.hero.addView(top, nav);
        LinearLayout headline = column();
        headline.setPadding(dp(22), 0, dp(22), dp(6));
        this.nameView = label(this.name, 38, -1285, true);
        this.nameView.setMaxLines(2);
        headline.addView(this.nameView, lp(-1, -2));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -2, 80);
        hp.bottomMargin = dp(109);
        this.hero.addView(headline, hp);
        renderHeroActions();
        updatePhoto(this.cover);
    }

    /* JADX INFO: renamed from: lambda$renderHero$0$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m23lambda$renderHero$0$localqingyinmobileArtistActivity(View v) {
        finish();
    }

    private FrameLayout circle(String type, String description, boolean primary) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(primary ? QingyinUi.rounded(this, -658699, 45.0f) : ArtistTheme.glass(this, 40));
        ArtistActionIcon glyph = new ArtistActionIcon(this, type, primary ? -14599115 : -1285);
        frame.addView(glyph, new FrameLayout.LayoutParams(-1, -1));
        if ("heart".equals(type)) {
            this.favoriteIcon = glyph;
        }
        frame.setContentDescription(description);
        return frame;
    }

    private void updatePhoto(final String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        this.portraitUrl = url;
        Artwork.bind(this.photo, url);
        Artwork.loadBitmap(url, new Consumer() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda14
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ArtistActivity.this.m30lambda$updatePhoto$1$localqingyinmobileArtistActivity(url, (Bitmap) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$updatePhoto$1$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m30lambda$updatePhoto$1$localqingyinmobileArtistActivity(String url, Bitmap bitmap) {
        if (!this.alive || !url.equals(this.portraitUrl) || bitmap == null) {
            return;
        }
        this.theme = ArtistTheme.color(bitmap);
        this.page.setBackgroundColor(this.theme);
        this.hero.setBackgroundColor(this.theme);
        if (this.sections != null) {
            this.sections.setBackground(ArtistTheme.gradient(this.theme, ArtistTheme.darker(this.theme, 0.57f), 0));
        }
        this.fade.setBackground(heroFade());
        getWindow().setStatusBarColor(ArtistTheme.darker(this.theme, 0.77f));
        getWindow().setNavigationBarColor(ArtistTheme.darker(this.theme, 0.57f));
    }

    private GradientDrawable heroFade() {
        GradientDrawable shade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0, 0, 1241513984, this.theme, this.theme});
        shade.setDither(true);
        return shade;
    }

    private void renderHeroActions() {
        LinearLayout actions = row();
        actions.setGravity(17);
        actions.setPadding(dp(24), dp(9), dp(24), dp(17));
        FrameLayout info = circle("info", "歌手个人信息", false);
        info.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistActivity.this.m24lambda$renderHeroActions$2$localqingyinmobileArtistActivity(view);
            }
        });
        actions.addView(info, lp(52, 52));
        View gap = new View(this);
        actions.addView(gap, new LinearLayout.LayoutParams(0, 1, 1.0f));
        FrameLayout play = circle("play", "播放热门歌曲", true);
        play.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistActivity.this.m25lambda$renderHeroActions$3$localqingyinmobileArtistActivity(view);
            }
        });
        actions.addView(play, lp(70, 62));
        View gap2 = new View(this);
        actions.addView(gap2, new LinearLayout.LayoutParams(0, 1, 1.0f));
        final FrameLayout save = circle("heart", "收藏歌手（本机）", false);
        this.favoriteIcon.setFilled(this.followed);
        save.setContentDescription(this.followed ? "取消收藏歌手" : "收藏歌手（本机）");
        save.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistActivity.this.m26lambda$renderHeroActions$4$localqingyinmobileArtistActivity(save, view);
            }
        });
        actions.addView(save, lp(52, 52));
        FrameLayout.LayoutParams buttons = new FrameLayout.LayoutParams(-1, dp(96), 80);
        buttons.bottomMargin = dp(5);
        this.hero.addView(actions, buttons);
    }

    /* JADX INFO: renamed from: lambda$renderHeroActions$2$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m24lambda$renderHeroActions$2$localqingyinmobileArtistActivity(View v) {
        showBiography();
    }

    /* JADX INFO: renamed from: lambda$renderHeroActions$3$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m25lambda$renderHeroActions$3$localqingyinmobileArtistActivity(View v) {
        if (this.tracks.isEmpty()) {
            Toast.makeText(this, "歌曲正在加载", 0).show();
        } else {
            playAt(0);
        }
    }

    /* JADX INFO: renamed from: lambda$renderHeroActions$4$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m26lambda$renderHeroActions$4$localqingyinmobileArtistActivity(FrameLayout save, View v) {
        this.followed = !this.followed;
        getSharedPreferences("artist_favorites", 0).edit().putBoolean(favoriteKey(), this.followed).apply();
        this.favoriteIcon.setFilled(this.followed);
        save.setContentDescription(this.followed ? "取消收藏歌手" : "收藏歌手（本机）");
        Toast.makeText(this, this.followed ? "已收藏到青音本机" : "已取消本机收藏", 0).show();
    }

    private void renderSections() {
        this.sections = column();
        this.sections.setMinimumHeight(Math.max(dp(480), getResources().getDisplayMetrics().heightPixels - dp(390)));
        this.sections.setBackground(ArtistTheme.gradient(this.theme, ArtistTheme.darker(this.theme, 0.57f), 0));
        this.page.addView(this.sections, lp(-1, -2));
        LinearLayout chartHead = sectionTitle("热门歌曲", new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m27lambda$renderSections$5$localqingyinmobileArtistActivity();
            }
        });
        this.rankLabel = (TextView) chartHead.getChildAt(1);
        this.preview = column();
        this.preview.setPadding(dp(18), 0, dp(18), 0);
        this.sections.addView(this.preview, lp(-1, -2));
        sectionTitle("专辑", new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m28lambda$renderSections$6$localqingyinmobileArtistActivity();
            }
        });
        HorizontalScrollView albums = new HorizontalScrollView(this);
        albums.setHorizontalScrollBarEnabled(false);
        this.albumRail = row();
        this.albumRail.setPadding(dp(18), 0, dp(18), 0);
        albums.addView(this.albumRail, new FrameLayout.LayoutParams(-2, -2));
        this.sections.addView(albums, lp(-1, 196));
        setLoadingText(this.preview, "正在获取热门歌曲…");
        setLoadingText(this.albumRail, "正在获取专辑…");
    }

    /* JADX INFO: renamed from: lambda$renderSections$6$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m28lambda$renderSections$6$localqingyinmobileArtistActivity() {
        if (this.artistId > 0) {
            openAlbumList();
        }
    }

    private LinearLayout sectionTitle(String title, final Runnable open) {
        LinearLayout header = row();
        header.setPadding(dp(22), dp(17), dp(20), dp(13));
        TextView t = label(title, 23, -1285, true);
        header.addView(t, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView more = label("查看全部  ›", 13, -729882, false);
        header.addView(more, lp(-2, -2));
        header.setContentDescription("查看" + title + "全部");
        header.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                open.run();
            }
        });
        this.sections.addView(header, lp(-1, 59));
        return header;
    }

    private void setLoadingText(LinearLayout target, String message) {
        target.removeAllViews();
        TextView text = label(message, 13, -2106659, false);
        text.setPadding(dp(9), dp(16), 0, dp(18));
        target.addView(text, lp(-2, -2));
    }

    private void showBiography() {
        Intent screen = new Intent(this, (Class<?>) ArtistBioActivity.class);
        screen.putExtra("artist_name", this.name);
        screen.putExtra("artist_cover", this.portraitUrl.isEmpty() ? this.cover : this.portraitUrl);
        screen.putExtra("artist_bio", this.bio);
        screen.putExtra("artist_birthday", this.birthday);
        screen.putExtra("theme", this.theme);
        startActivity(screen);
    }

    private void fetchProfile() {
        final String lookup = this.name;
        final long id = this.artistId;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m20lambda$fetchProfile$9$localqingyinmobileArtistActivity(lookup, id);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchProfile$9$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m20lambda$fetchProfile$9$localqingyinmobileArtistActivity(String lookup, long id) {
        JSONObject d = null;
        try {
            d = OnlineApi.call(getApplicationContext(), "artistProfile", new JSONObject().put("name", lookup).put("id", id));
        } catch (Exception e) {
        }
        final JSONObject data = d;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m19lambda$fetchProfile$8$localqingyinmobileArtistActivity(data);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchProfile$8$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m19lambda$fetchProfile$8$localqingyinmobileArtistActivity(JSONObject data) {
        if (this.alive) {
            if (data != null && data.optBoolean("verified")) {
                this.artistId = data.optLong("id", 0L);
                this.verified = true;
                this.nameView.setText(data.optString("name", this.name));
                this.bio = data.optString("description", "");
                this.birthday = data.optString("birthday", "");
                String url = data.optString("portrait", "");
                if (!url.isEmpty()) {
                    updatePhoto(url);
                }
                fetchAlbums();
            } else {
                this.artistId = 0L;
                setLoadingText(this.albumRail, "暂无可验证的专辑信息");
            }
            fetchSongs();
        }
    }

    private void fetchSongs() {
        final long id = this.verified ? this.artistId : 0L;
        final String singer = this.name;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m22lambda$fetchSongs$11$localqingyinmobileArtistActivity(singer, id);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchSongs$11$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m22lambda$fetchSongs$11$localqingyinmobileArtistActivity(String singer, long id) {
        JSONArray list = null;
        try {
            JSONObject response = OnlineApi.call(getApplicationContext(), "artistTracks", new JSONObject().put("name", singer).put("id", id).put("page", 1));
            list = response.optJSONArray("songs");
        } catch (Exception e) {
        }
        final JSONArray result = list;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m21lambda$fetchSongs$10$localqingyinmobileArtistActivity(result);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchSongs$10$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m21lambda$fetchSongs$10$localqingyinmobileArtistActivity(JSONArray result) {
        if (this.alive) {
            this.tracks.clear();
            if (result != null) {
                for (int i = 0; i < result.length(); i++) {
                    Track song = OnlineApi.asTrack(result.optJSONObject(i));
                    if (song != null) {
                        this.tracks.add(song);
                    }
                }
            }
            this.preview.removeAllViews();
            if (this.tracks.isEmpty()) {
                setLoadingText(this.preview, "歌曲暂时不可用，请稍后重试");
                return;
            }
            int total = Math.min(3, this.tracks.size());
            for (int i2 = 0; i2 < total; i2++) {
                this.preview.addView(songRow(this.tracks.get(i2), i2));
            }
        }
    }

    private View songRow(Track song, final int index) {
        LinearLayout line = row();
        line.setGravity(16);
        line.setPadding(dp(5), dp(7), dp(8), dp(7));
        line.setBackground(ArtistTheme.glass(this, 14));
        TextView rank = label(String.format(Locale.ROOT, "%02d", Integer.valueOf(index + 1)), 14, -1055512, true);
        rank.setGravity(17);
        line.addView(rank, lp(36, 50));
        FrameLayout cover = Artwork.frame(this, 9);
        Artwork.bind(cover, song.coverUrl);
        line.addView(cover, lp(52, 52));
        LinearLayout words = column();
        words.setPadding(dp(11), 0, dp(2), 0);
        TextView title = label(song.title, 15, -1285, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(title);
        TextView by = label(song.artist, 11, -2106659, false);
        by.setSingleLine(true);
        by.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams bylp = lp(-1, -2);
        bylp.topMargin = dp(5);
        words.addView(by, bylp);
        line.addView(words, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView play = label("▶", 18, -1285, false);
        line.addView(play, lp(28, -2));
        line.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistActivity.this.m29lambda$songRow$12$localqingyinmobileArtistActivity(index, view);
            }
        });
        line.setContentDescription("播放第" + (index + 1) + "首：" + song.title);
        LinearLayout shell = column();
        shell.setPadding(0, 0, 0, dp(7));
        shell.addView(line, lp(-1, 69));
        return shell;
    }

    /* JADX INFO: renamed from: lambda$songRow$12$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m29lambda$songRow$12$localqingyinmobileArtistActivity(int index, View v) {
        playAt(index);
    }

    private void fetchAlbums() {
        final long id = this.artistId;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m18lambda$fetchAlbums$15$localqingyinmobileArtistActivity(id);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchAlbums$15$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m18lambda$fetchAlbums$15$localqingyinmobileArtistActivity(final long id) {
        JSONArray arr = null;
        try {
            arr = OnlineApi.call(getApplicationContext(), "artistAlbums", new JSONObject().put("id", id)).optJSONArray("albums");
        } catch (Exception e) {
        }
        final JSONArray result = arr;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                ArtistActivity.this.m17lambda$fetchAlbums$14$localqingyinmobileArtistActivity(id, result);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetchAlbums$14$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m17lambda$fetchAlbums$14$localqingyinmobileArtistActivity(long id, JSONArray result) {
        if (!this.alive || id != this.artistId) {
            return;
        }
        this.albumRail.removeAllViews();
        if (result == null || result.length() == 0) {
            setLoadingText(this.albumRail, "暂时没有可加载的专辑");
            return;
        }
        for (int i = 0; i < Math.min(result.length(), 12); i++) {
            JSONObject a = result.optJSONObject(i);
            if (a != null) {
                final long albumId = a.optLong("id", 0L);
                if (albumId > 0) {
                    LinearLayout tile = column();
                    LinearLayout.LayoutParams lp = lp(138, -1);
                    lp.rightMargin = dp(13);
                    FrameLayout image = Artwork.frame(this, 14);
                    Artwork.bind(image, a.optString("cover", ""));
                    tile.addView(image, lp(138, 138));
                    TextView title = label(a.optString("title"), 13, -1285, true);
                    title.setSingleLine(true);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    LinearLayout.LayoutParams tl = lp(-1, -2);
                    tl.topMargin = dp(7);
                    tile.addView(title, tl);
                    TextView date = label(a.optString("year", ""), 11, -2106659, false);
                    tile.addView(date);
                    tile.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistActivity$$ExternalSyntheticLambda6
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            ArtistActivity.this.m16lambda$fetchAlbums$13$localqingyinmobileArtistActivity(albumId, view);
                        }
                    });
                    tile.setContentDescription("查看专辑" + a.optString("title"));
                    this.albumRail.addView(tile, lp);
                }
            }
        }
    }

    /* JADX INFO: renamed from: lambda$fetchAlbums$13$local-qingyin-mobile-ArtistActivity, reason: not valid java name */
    /* synthetic */ void m16lambda$fetchAlbums$13$localqingyinmobileArtistActivity(long albumId, View v) {
        openAlbum(albumId);
    }

    private void playAt(int index) {
        if (!this.bound || this.player == null) {
            Toast.makeText(this, "播放器尚未准备好", 0).show();
            return;
        }
        if (index < 0 || index >= this.tracks.size()) {
            return;
        }
        Intent service = new Intent(this, (Class<?>) PlaybackService.class);
        startForegroundService(service);
        this.player.playQueue(new ArrayList(this.tracks), index);
        startActivity(new Intent(this, (Class<?>) NowPlayingActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: openRanking, reason: merged with bridge method [inline-methods] */
    public void m27lambda$renderSections$5$localqingyinmobileArtistActivity() {
        Intent intent = new Intent(this, (Class<?>) ArtistRankingActivity.class);
        intent.putExtra("artist_name", this.name).putExtra("artist_id", this.verified ? this.artistId : 0L);
        intent.putExtra("theme", this.theme);
        startActivity(intent);
    }

    private void openAlbum(long album) {
        startActivity(new Intent(this, (Class<?>) AlbumDetailActivity.class).putExtra("album_id", album).putExtra("theme", this.theme));
    }

    private void openAlbumList() {
        Intent intent = new Intent(this, (Class<?>) ArtistAlbumsActivity.class);
        intent.putExtra("artist_id", this.artistId).putExtra("artist_name", this.name).putExtra("theme", this.theme);
        startActivity(intent);
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
