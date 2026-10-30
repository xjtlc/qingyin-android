package local.qingyin.mobile;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class IndexActivity extends Activity {
    private static final int BLUE = -1815446;
    private static final int NAVY = -15197924;
    private static final int REQUEST_ACCOUNT = 301;
    private static final int SLATE = -8749947;
    private static final int SURFACE = -1;
    private static final int WHITE = -1;
    private TextView account;
    private boolean active;
    private AmbientBackdrop ambient;
    private ViewTreeObserver blurObserver;
    private FrameLayout blurSource;
    private LinearLayout bottomChrome;
    private boolean bound;
    private boolean chromeCollapsed;
    private int chromeScrollDirection;
    private int chromeScrollDistance;
    private LinearLayout compactBottom;
    private FrameLayout compactMiniHost;
    private FrameLayout currentSection;
    private PlaylistRail discoverLists;
    private LinearLayout discoverSongList;
    private PlaylistRail discoverUpdated;
    private DiscoveryFeed discoveryFeed;
    private DiscoveryRepository discoveryRepository;
    private LinearLayout expandedBottom;
    private long generation;
    private PlaylistRail homeKugouLists;
    private FrameLayout libraryCover;
    private LinearLayout libraryLists;
    private PlaylistRail librarySavedLists;
    private LinearLayout librarySavedSection;
    private ProgressBar miniBar;
    private LinearLayout miniContainer;
    private FrameLayout miniCoverArt;
    private LinearLayout miniDetails;
    private LinearLayout miniPlayer;
    private ImageView nextAction;
    private ImageView playAction;
    private PlaybackService player;
    private TextView playingArtist;
    private TextView playingName;
    private long radioFetchedAt;
    private LinearLayout radioLists;
    private int radioSerial;
    private TextView radioStatus;
    private LinearLayout recentPreview;
    private boolean requested;
    private FrameLayout searchButton;
    private LinearLayout[] tabButtons;
    private View[] tabScreens;
    private FrameLayout windowRoot;
    private final ExecutorService network = Executors.newFixedThreadPool(2);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<JSONObject> personalLists = new ArrayList<>();
    private final ArrayList<JSONObject> picks = new ArrayList<>();
    private String radioCategory = "featured";
    private final ArrayList<RadioDirectory.Station> radioStations = new ArrayList<>();
    private String mineRenderSignature = "";
    private final ArrayList<Track> discoverySongs = new ArrayList<>();
    private int selectedTab = 0;
    private String lastMiniCoverUri = "";
    private Boolean accountLoggedIn = null;
    private boolean libraryLoadFailed = false;
    private boolean recommendationsLoading = true;
    private boolean recommendationsFailed = false;
    private boolean songsLoading = true;
    private boolean songsLoadFailed = false;
    private long lastRecommendationsAt = 0;
    private final FrameLayout[][] recommendationArtwork = (FrameLayout[][]) Array.newInstance((Class<?>) FrameLayout.class, 2, 3);
    private final long[] previewAttempt = new long[2];
    private View guessCard;
    private TextView guessCaption, guessIndicator;
    private boolean guessLoading;
    private long guessRequest;
    private final ArrayList<FrostedGlassDrawable> frostedLayers = new ArrayList<>();
    private String appliedAppearance = "";
    private final ViewTreeObserver.OnScrollChangedListener blurScroll = new ViewTreeObserver.OnScrollChangedListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda65
        @Override // android.view.ViewTreeObserver.OnScrollChangedListener
        public final void onScrollChanged() {
            IndexActivity.this.m106lambda$new$0$localqingyinmobileIndexActivity();
        }
    };
    private final Runnable ticker = new Runnable() { // from class: local.qingyin.mobile.IndexActivity.1
        @Override // java.lang.Runnable
        public void run() {
            if (IndexActivity.this.active) {
                IndexActivity.this.updateMini();
                IndexActivity.this.ui.postDelayed(this, IndexActivity.this.chromeCollapsed ? 300L : 1000L);
            }
        }
    };
    private final ServiceConnection connection = new ServiceConnection() { // from class: local.qingyin.mobile.IndexActivity.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName component, IBinder binder) {
            IndexActivity.this.bound = true;
            IndexActivity.this.player = ((PlaybackService.LocalBinder) binder).service();
            IndexActivity.this.updateMini();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName component) {
            IndexActivity.this.bound = false;
            IndexActivity.this.player = null;
            IndexActivity.this.updateMini();
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(float n) {
        return QingyinUi.dp(this, n);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout v() {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(1);
        return x;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout h() {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(0);
        x.setGravity(16);
        return x;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView label(String value, int sp, int color, boolean bold) {
        TextView t = QingyinUi.text(this, value, sp, color, bold);
        t.setIncludeFontPadding(false);
        return t;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout.LayoutParams lp(int w, int height) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), height < 0 ? height : dp(height));
    }

    private Drawable glass(View host, int radius, int tint) {
        FrostedGlassDrawable drawable = new FrostedGlassDrawable(this.blurSource, host, dp(radius), tint);
        this.frostedLayers.add(drawable);
        return drawable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: refreshFrosted, reason: merged with bridge method [inline-methods] */
    public void m106lambda$new$0$localqingyinmobileIndexActivity() {
        for (FrostedGlassDrawable drawable : this.frostedLayers) {
            drawable.refresh();
        }
    }

    private void attachBlurScroll() {
        if (this.blurSource == null || this.blurObserver != null || isDestroyed()) {
            return;
        }
        ViewTreeObserver observer = this.blurSource.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnScrollChangedListener(this.blurScroll);
            this.blurObserver = observer;
        }
    }

    private ImageView transport(int icon, String description) {
        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(-1));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(dp(10.0f), dp(10.0f), dp(10.0f), dp(10.0f));
        image.setBackground(QingyinUi.touch(this, 16777215, 24.0f));
        image.setContentDescription(description);
        return image;
    }

    private void go(String page) {
        if ("guess".equals(page)) {
            playGuessRecommendations();
            return;
        }
        Intent intent = new Intent(this, (Class<?>) OnlineActivity.class).putExtra("page", page);
        if ("account".equals(page) || "mine".equals(page)) {
            startActivityForResult(intent, 301);
        } else {
            startActivity(intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshHome() {
        this.lastRecommendationsAt = 0L;
        RecommendationArtwork.clear(this);
        this.previewAttempt[0] = 0;
        this.previewAttempt[1] = 0;
        renderRecommendationCovers();
        loadHome();
    }

    @Override // android.app.Activity
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 301 || !this.active) {
            return;
        }
        refreshHome();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: goPlaylist, reason: merged with bridge method [inline-methods] */
    public void m81lambda$drawMine$47$localqingyinmobileIndexActivity(JSONObject playlist) {
        startActivity(new Intent(this, (Class<?>) OnlineActivity.class).putExtra("page", "playlist").putExtra("playlist_json", playlist.toString()));
    }

    private void goSearch(String keyword) {
        Intent i = new Intent(this, (Class<?>) OnlineActivity.class).putExtra("page", "search");
        if (keyword != null && !keyword.trim().isEmpty()) {
            i.putExtra("keyword", keyword.trim());
        }
        startActivity(i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void goLocal() {
        startActivity(new Intent(this, (Class<?>) MainActivity.class));
    }

    @Override // android.app.Activity
    public void onCreate(Bundle state) {
        super.onCreate(state);
        HomeWindowInsets.styleStatusBar(getWindow(), AppearancePreferences.dark(this));
        getWindow().setNavigationBarColor(AmbientBackdrop.baseColor(this));
        FrameLayout frameLayout = new FrameLayout(this);
        this.windowRoot = frameLayout;
        frameLayout.setBackgroundColor(AmbientBackdrop.baseColor(this));
        setContentView(frameLayout);
        FrameLayout content = new FrameLayout(this);
        this.blurSource = content;
        frameLayout.addView(content, new FrameLayout.LayoutParams(-1, -1));
        this.ambient = new AmbientBackdrop(this);
        this.ambient.onGlassRefresh(this::m106lambda$new$0$localqingyinmobileIndexActivity);
        content.addView(this.ambient, new FrameLayout.LayoutParams(-1, -1));
        this.tabScreens = new View[]{buildHome(), buildDiscover(), buildRadio(), buildLibrary()};
        int i = 0;
        while (i < this.tabScreens.length) {
            this.tabScreens[i].setVisibility(i == 0 ? 0 : 8);
            content.addView(this.tabScreens[i], new FrameLayout.LayoutParams(-1, -1));
            i++;
        }
        LinearLayout linearLayoutV = v();
        this.bottomChrome = linearLayoutV;
        linearLayoutV.setPadding(dp(13.0f), 0, dp(13.0f), dp(13.0f));
        frameLayout.addView(linearLayoutV, new FrameLayout.LayoutParams(-1, -2, 80));
        LinearLayout miniWrap = v();
        miniWrap.setPadding(dp(5.0f), dp(5.0f), dp(5.0f), dp(4.0f));
        miniWrap.setBackground(glass(miniWrap, 29, -1514967431));
        miniWrap.setElevation(dp(12.0f));
        miniWrap.setClipToOutline(true);
        this.miniPlayer = h();
        this.miniPlayer.setPadding(dp(7.0f), 0, dp(5.0f), 0);
        this.miniPlayer.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m107lambda$onCreate$1$localqingyinmobileIndexActivity(view);
            }
        });
        this.miniCoverArt = Artwork.frame(this, 13);
        this.miniCoverArt.setElevation(dp(2.0f));
        this.miniPlayer.addView(this.miniCoverArt, lp(47, 47));
        LinearLayout now = v();
        this.miniDetails = now;
        now.setGravity(16);
        now.setPadding(dp(12.0f), 0, dp(3.0f), 0);
        this.playingName = label("暂无播放", 14, -1, true);
        this.playingName.setSingleLine(true);
        this.playingName.setEllipsize(TextUtils.TruncateAt.END);
        this.playingArtist = label("青音", 12, -251658241, false);
        this.playingArtist.setSingleLine(true);
        this.playingArtist.setEllipsize(TextUtils.TruncateAt.END);
        now.addView(this.playingName);
        now.addView(this.playingArtist);
        this.miniPlayer.addView(now, new LinearLayout.LayoutParams(0, -1, 1.0f));
        this.playAction = transport(17301540, "播放或暂停");
        this.playAction.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m112lambda$onCreate$2$localqingyinmobileIndexActivity(view);
            }
        });
        this.miniPlayer.addView(this.playAction, lp(44, 46));
        ImageView next = transport(17301538, "下一首");
        this.nextAction = next;
        next.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda26
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m113lambda$onCreate$3$localqingyinmobileIndexActivity(view);
            }
        });
        this.miniPlayer.addView(next, lp(43, 46));
        this.miniCoverArt.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda27
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m114lambda$onCreate$4$localqingyinmobileIndexActivity(view);
            }
        });
        this.playingName.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda28
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m115lambda$onCreate$5$localqingyinmobileIndexActivity(view);
            }
        });
        this.playingArtist.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda29
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m116lambda$onCreate$6$localqingyinmobileIndexActivity(view);
            }
        });
        miniWrap.addView(this.miniPlayer, lp(-1, 53));
        this.miniBar = new ProgressBar(this, null, 16842872);
        this.miniBar.setProgressTintList(ColorStateList.valueOf(-1));
        this.miniBar.setProgressBackgroundTintList(ColorStateList.valueOf(1090519039));
        this.miniBar.setPadding(dp(9.0f), 0, dp(9.0f), 0);
        LinearLayout.LayoutParams progressLp = lp(-1, 2);
        progressLp.topMargin = dp(1.0f);
        miniWrap.addView(this.miniBar, progressLp);
        this.miniContainer = miniWrap;
        this.miniContainer.setVisibility(8);
        LinearLayout.LayoutParams miniLp = lp(-1, 65);
        miniLp.bottomMargin = dp(9.0f);
        linearLayoutV.addView(this.miniContainer, miniLp);
        LinearLayout bottom = h();
        this.expandedBottom = bottom;
        bottom.setGravity(16);
        LinearLayout nav = h();
        nav.setPadding(dp(4.0f), dp(4.0f), dp(4.0f), dp(4.0f));
        nav.setBackground(glass(nav, 35, -1678382094));
        nav.setElevation(dp(11.0f));
        nav.setClipToOutline(false);
        this.tabButtons = new LinearLayout[]{navItem("⌂", "主页", true, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda30
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m117lambda$onCreate$7$localqingyinmobileIndexActivity();
            }
        }), navItem("◈", "新发现", false, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m118lambda$onCreate$8$localqingyinmobileIndexActivity();
            }
        }), navItem("◎", "广播", false, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda32
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m119lambda$onCreate$9$localqingyinmobileIndexActivity();
            }
        }), navItem("▤", "资料库", false, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m108lambda$onCreate$10$localqingyinmobileIndexActivity();
            }
        })};
        View[] viewArr = this.tabButtons;
        int i2 = 0;
        for (int length = viewArr.length; i2 < length; length = length) {
            View tab = viewArr[i2];
            nav.addView(tab, new LinearLayout.LayoutParams(0, dp(57.0f), 1.0f));
            i2++;
            viewArr = viewArr;
        }
        bottom.addView(nav, new LinearLayout.LayoutParams(0, dp(65.0f), 1.0f));
        FrameLayout search = new FrameLayout(this);
        this.searchButton = search;
        search.setBackground(glass(search, 34, -1476990993));
        search.setElevation(dp(11.0f));
        search.addView(new NavIconView(this, "⌕", -15197924), new FrameLayout.LayoutParams(dp(29.0f), dp(29.0f), 17));
        search.setContentDescription("搜索音乐");
        search.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m109lambda$onCreate$11$localqingyinmobileIndexActivity(view);
            }
        });
        LinearLayout.LayoutParams searchLp = lp(65, 65);
        searchLp.leftMargin = dp(9.0f);
        bottom.addView(search, searchLp);
        linearLayoutV.addView(bottom, lp(-1, 68));
        this.compactBottom = h();
        this.compactBottom.setGravity(16);
        this.currentSection = new FrameLayout(this);
        this.currentSection.setBackground(glass(this.currentSection, 28, -1476990993));
        this.currentSection.setElevation(dp(11.0f));
        this.currentSection.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m110lambda$onCreate$12$localqingyinmobileIndexActivity(view);
            }
        });
        this.compactBottom.addView(this.currentSection, lp(56, 56));
        this.compactMiniHost = new FrameLayout(this);
        LinearLayout.LayoutParams compactSlot = new LinearLayout.LayoutParams(0, dp(56.0f), 1.0f);
        compactSlot.leftMargin = dp(9.0f);
        compactSlot.rightMargin = dp(9.0f);
        this.compactBottom.addView(this.compactMiniHost, compactSlot);
        this.compactBottom.setVisibility(8);
        linearLayoutV.addView(this.compactBottom, lp(-1, 56));
        frameLayout.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m111lambda$onCreate$13$localqingyinmobileIndexActivity();
            }
        });
        int startTab = state != null ? state.getInt("current_tab", 0) : getIntent().getIntExtra("start_tab", 0);
        HomeWindowInsets.install(frameLayout, this.tabScreens, this.bottomChrome);
        selectTab(startTab);
        this.requested = bindService(new Intent(this, (Class<?>) PlaybackService.class), this.connection, 1);
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m107lambda$onCreate$1$localqingyinmobileIndexActivity(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m112lambda$onCreate$2$localqingyinmobileIndexActivity(View v) {
        cancelGuessRequest();
        if (this.bound && this.player != null) {
            ensureForeground();
            this.player.toggle();
            updateMini();
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$3$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m113lambda$onCreate$3$localqingyinmobileIndexActivity(View v) {
        cancelGuessRequest();
        if (this.bound && this.player != null) {
            ensureForeground();
            this.player.skip(1);
            updateMini();
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$4$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m114lambda$onCreate$4$localqingyinmobileIndexActivity(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$onCreate$5$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m115lambda$onCreate$5$localqingyinmobileIndexActivity(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$onCreate$6$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m116lambda$onCreate$6$localqingyinmobileIndexActivity(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$onCreate$7$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m117lambda$onCreate$7$localqingyinmobileIndexActivity() {
        selectTab(0);
    }

    /* JADX INFO: renamed from: lambda$onCreate$8$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m118lambda$onCreate$8$localqingyinmobileIndexActivity() {
        selectTab(1);
    }

    /* JADX INFO: renamed from: lambda$onCreate$9$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m119lambda$onCreate$9$localqingyinmobileIndexActivity() {
        selectTab(2);
    }

    /* JADX INFO: renamed from: lambda$onCreate$10$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m108lambda$onCreate$10$localqingyinmobileIndexActivity() {
        selectTab(3);
    }

    /* JADX INFO: renamed from: lambda$onCreate$11$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m109lambda$onCreate$11$localqingyinmobileIndexActivity(View v) {
        goSearch(null);
    }

    /* JADX INFO: renamed from: lambda$onCreate$12$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m110lambda$onCreate$12$localqingyinmobileIndexActivity(View view) {
        resetChromeScroll();
        setChromeCollapsed(false, true);
    }

    /* JADX INFO: renamed from: lambda$onCreate$13$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m111lambda$onCreate$13$localqingyinmobileIndexActivity() {
        attachBlurScroll();
        m106lambda$new$0$localqingyinmobileIndexActivity();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetChromeScroll() {
        this.chromeScrollDirection = 0;
        this.chromeScrollDistance = 0;
    }

    private void pageScrolled(ScrollView page, int y, int previousY) {
        if (!this.active || this.bottomChrome == null || this.tabScreens == null || this.tabScreens[this.selectedTab] != page) {
            return;
        }
        if (!this.bound || this.player == null || this.player.current() == null) {
            if (this.chromeCollapsed) {
                setChromeCollapsed(false, true);
                return;
            }
            return;
        }
        View child = page.getChildAt(0);
        int maxY = child == null ? 0 : Math.max(0, child.getHeight() - page.getHeight());
        if (page instanceof ChromeScrollView) {
            ChromeScrollView chromePage = (ChromeScrollView) page;
            if (chromePage.bottomExitLocked) {
                if (y >= maxY - dp(80.0f)) {
                    return;
                } else {
                    chromePage.bottomExitLocked = false;
                }
            }
        }
        if (maxY <= dp(64.0f) || y <= dp(8.0f)) {
            resetChromeScroll();
            setChromeCollapsed(false, true);
            return;
        }
        if (y >= maxY - dp(12.0f) || !page.canScrollVertically(1)) {
            if (!this.chromeCollapsed && y > dp(40.0f) && y > previousY) {
                setChromeCollapsed(true, true);
            }
            resetChromeScroll();
            return;
        }
        int delta = y - previousY;
        if (delta == 0) {
            return;
        }
        int direction = delta > 0 ? 1 : -1;
        if (direction != this.chromeScrollDirection) {
            this.chromeScrollDirection = direction;
            this.chromeScrollDistance = 0;
        }
        this.chromeScrollDistance += Math.abs(delta);
        if (direction > 0 && y > dp(40.0f) && this.chromeScrollDistance >= dp(35.0f)) {
            setChromeCollapsed(true, true);
            this.chromeScrollDistance = 0;
        }
    }

    private final class ChromeScrollView extends ScrollView {
        private boolean bottomExitLocked;
        private boolean downwardSwipeArmed;
        private float gestureX;
        private float gestureY;

        ChromeScrollView() {
            super(IndexActivity.this);
        }

        @Override // android.view.ViewGroup, android.view.View
        public boolean dispatchTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case 0:
                    this.gestureX = event.getX();
                    this.gestureY = event.getY();
                    this.downwardSwipeArmed = IndexActivity.this.active && IndexActivity.this.chromeCollapsed && IndexActivity.this.tabScreens != null && IndexActivity.this.tabScreens[IndexActivity.this.selectedTab] == this;
                    break;
                case 1:
                    if (this.downwardSwipeArmed) {
                        float downward = event.getY() - this.gestureY;
                        float side = Math.abs(event.getX() - this.gestureX);
                        if (downward >= IndexActivity.this.dp(35.0f) && downward > 1.25f * side) {
                            this.bottomExitLocked = true;
                            this.downwardSwipeArmed = false;
                            IndexActivity.this.resetChromeScroll();
                            IndexActivity.this.setChromeCollapsed(false, true);
                            if ((IndexActivity.this.getApplicationInfo().flags & 2) != 0) {
                                Log.i("QingyinChrome", "DOWN_SWIPE_EXPANDED");
                            }
                        }
                    }
                    this.downwardSwipeArmed = false;
                    break;
                case 3:
                    this.downwardSwipeArmed = false;
                    break;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private void updateCurrentSection() {
        if (this.currentSection == null) {
            return;
        }
        String[] icons = {"⌂", "◈", "◎", "▤"};
        String[] names = {"主页", "新发现", "广播", "资料库"};
        this.currentSection.removeAllViews();
        this.currentSection.addView(new NavIconView(this, icons[this.selectedTab], -1815446), new FrameLayout.LayoutParams(dp(29.0f), dp(29.0f), 17));
        this.currentSection.setContentDescription(names[this.selectedTab] + "，当前页面，点击展开底部导航");
    }

    private void detach(View view) {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setChromeCollapsed(boolean collapsed, boolean animate) {
        if (this.bottomChrome == null || this.compactBottom == null || collapsed == this.chromeCollapsed) {
            return;
        }
        this.chromeCollapsed = collapsed;
        this.bottomChrome.animate().cancel();
        this.bottomChrome.setAlpha(1.0f);
        this.bottomChrome.setTranslationY(0.0f);
        detach(this.miniContainer);
        detach(this.searchButton);
        this.expandedBottom.setVisibility(collapsed ? 8 : 0);
        this.compactBottom.setVisibility(collapsed ? 0 : 8);
        if (!collapsed) {
            LinearLayout.LayoutParams miniLp = lp(-1, 65);
            miniLp.bottomMargin = dp(9.0f);
            this.bottomChrome.addView(this.miniContainer, 0, miniLp);
            LinearLayout.LayoutParams searchLp = lp(65, 65);
            searchLp.leftMargin = dp(9.0f);
            this.expandedBottom.addView(this.searchButton, searchLp);
        } else {
            this.compactMiniHost.addView(this.miniContainer, new FrameLayout.LayoutParams(-1, dp(56.0f), 17));
            this.compactBottom.addView(this.searchButton, lp(56, 56));
        }
        this.miniContainer.setPadding(dp(collapsed ? 4.0f : 5.0f), dp(collapsed ? 3.0f : 5.0f), dp(collapsed ? 4.0f : 5.0f), dp(collapsed ? 3.0f : 4.0f));
        this.miniPlayer.setPadding(dp(collapsed ? 3.0f : 7.0f), 0, dp(collapsed ? 1.0f : 5.0f), 0);
        this.miniPlayer.setLayoutParams(lp(-1, collapsed ? 49 : 53));
        this.miniCoverArt.setLayoutParams(lp(collapsed ? 38 : 47, collapsed ? 38 : 47));
        this.miniDetails.setPadding(dp(collapsed ? 7.0f : 12.0f), 0, dp(3.0f), 0);
        this.playingName.setTextSize(collapsed ? 12.0f : 14.0f);
        this.playingArtist.setTextSize(collapsed ? 10.0f : 12.0f);
        this.nextAction.setVisibility(collapsed ? 8 : 0);
        this.miniBar.setVisibility(collapsed ? 8 : 0);
        if (animate) {
            this.bottomChrome.setAlpha(0.78f);
            this.bottomChrome.setTranslationY(dp(6.0f));
            this.bottomChrome.animate().alpha(1.0f).translationY(0.0f).setDuration(170L).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda46
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    IndexActivity.this.m127lambda$setChromeCollapsed$14$localqingyinmobileIndexActivity(valueAnimator);
                }
            }).withEndAction(this::m106lambda$new$0$localqingyinmobileIndexActivity).start();
        }
        this.bottomChrome.post(this::m106lambda$new$0$localqingyinmobileIndexActivity);
    }

    /* JADX INFO: renamed from: lambda$setChromeCollapsed$14$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m127lambda$setChromeCollapsed$14$localqingyinmobileIndexActivity(ValueAnimator animation) {
        m106lambda$new$0$localqingyinmobileIndexActivity();
    }

    private LinearLayout quickLink(String symbol, String name, final Runnable action) {
        LinearLayout item = h();
        item.setGravity(17);
        TextView glyph = label(symbol, 18, -1815446, false);
        glyph.setGravity(17);
        item.addView(glyph);
        TextView caption = label(name, 13, -15197924, false);
        caption.setPadding(dp(7.0f), 0, 0, 0);
        item.addView(caption);
        item.setContentDescription(name);
        item.setBackground(QingyinUi.touch(this, -1, 12.0f));
        item.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda47
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                action.run();
            }
        });
        return item;
    }

    private LinearLayout section(String title, String more, final Runnable action) {
        LinearLayout head = h();
        head.addView(label(title, 21, -15197924, true), new LinearLayout.LayoutParams(0, -2, 1.0f));
        if (more != null && !more.isEmpty() && action != null) {
            TextView link = label("›", 27, -8749947, false);
            link.setGravity(17);
            link.setContentDescription("查看" + title + "全部");
            link.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda34
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    action.run();
                }
            });
            head.addView(link, lp(39, 40));
        }
        return head;
    }

    private void heading(LinearLayout body, String title, String more, Runnable action) {
        LinearLayout.LayoutParams p = lp(-1, 46);
        p.topMargin = dp(23.0f);
        body.addView(section(title, more, action), p);
    }

    private void gallery(LinearLayout body, LinearLayout track, int height) {
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setClipToPadding(false);
        horizontal.addView(track);
        body.addView(horizontal, lp(-1, height));
    }

    private LinearLayout tile(String name, String sub, String coverUrl, final Runnable action, int width) {
        LinearLayout item = v();
        FrameLayout cover = Artwork.frame(this, 12);
        Artwork.bind(cover, coverUrl);
        DefaultPlaylistArt.decorate(cover, name.contains("默认收藏") && (coverUrl == null || coverUrl.isEmpty()));
        item.addView(cover, lp(width, width));
        TextView title = label(name, 14, -15197924, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = lp(-1, 23);
        tlp.topMargin = dp(7.0f);
        item.addView(title, tlp);
        TextView subtitle = label(sub, 12, -8749947, false);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        item.addView(subtitle, lp(-1, 20));
        item.setContentDescription(name);
        item.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                action.run();
            }
        });
        return item;
    }

    private void addTile(LinearLayout row, LinearLayout tile) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.rightMargin = dp(13.0f);
        row.addView(tile, p);
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class PlaylistRail extends HorizontalScrollView {
        private final LinearLayout cards;
        private int direction;
        private float downX;
        private float downY;
        private boolean showingState;
        private final int touchSlop;

        PlaylistRail() {
            super(IndexActivity.this);
            this.cards = IndexActivity.this.h();
            this.touchSlop = ViewConfiguration.get(IndexActivity.this).getScaledTouchSlop();
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(2);
            setClipToPadding(false);
            setFocusable(false);
            this.cards.setGravity(48);
            addView(this.cards, new FrameLayout.LayoutParams(-2, -2));
        }

        private int viewportWidth() {
            return getWidth() > 0 ? getWidth() : Math.max(IndexActivity.this.dp(220.0f), getResources().getDisplayMetrics().widthPixels - IndexActivity.this.dp(38.0f));
        }

        private int coverSize(int viewport) {
            return Math.max(IndexActivity.this.dp(100.0f), Math.min(IndexActivity.this.dp(220.0f), (int) ((viewport - IndexActivity.this.dp(24.0f)) / 2.18f)));
        }

        void playlists(List<JSONObject> playlists, int start, int end) {
            this.cards.removeAllViews();
            boolean z = false;
            this.showingState = false;
            int size = coverSize(viewportWidth());
            int i = start;
            while (i < Math.min(end, playlists.size())) {
                final JSONObject playlist = playlists.get(i);
                String name = playlist.optString("title", "精选歌单");
                String subtitle = OnlineApi.playlistCountLabel(playlist);
                LinearLayout item = IndexActivity.this.v();
                FrameLayout cover = Artwork.frame(IndexActivity.this, 9);
                Artwork.bind(cover, playlist.optString("cover", ""));
                item.addView(cover, new LinearLayout.LayoutParams(size, size));
                TextView title = IndexActivity.this.label(name, 14, -15197924, z);
                title.setSingleLine(true);
                title.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams titleLp = IndexActivity.this.lp(-1, 23);
                titleLp.topMargin = IndexActivity.this.dp(7.0f);
                item.addView(title, titleLp);
                if (!"点击查看歌曲".equals(subtitle)) {
                    TextView detail = IndexActivity.this.label(subtitle, 12, -8749947, z);
                    detail.setSingleLine(true);
                    detail.setEllipsize(TextUtils.TruncateAt.END);
                    item.addView(detail, IndexActivity.this.lp(-1, 20));
                }
                item.setContentDescription("打开酷狗歌单：" + name + ("点击查看歌曲".equals(subtitle) ? "" : "，" + subtitle));
                item.setBackground(QingyinUi.touch(IndexActivity.this, 16777215, 9.0f));
                item.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$PlaylistRail$$ExternalSyntheticLambda1
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        PlaylistRail.this.m130xe628b7fc(playlist, view);
                    }
                });
                LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(size, -2);
                if (i < Math.min(end, playlists.size()) - 1) {
                    itemLp.rightMargin = IndexActivity.this.dp(12.0f);
                }
                this.cards.addView(item, itemLp);
                i++;
                z = false;
            }
            resizeCards(viewportWidth());
        }

        /* JADX INFO: renamed from: lambda$playlists$0$local-qingyin-mobile-IndexActivity$PlaylistRail, reason: not valid java name */
        /* synthetic */ void m130xe628b7fc(JSONObject playlist, View view) {
            IndexActivity.this.m81lambda$drawMine$47$localqingyinmobileIndexActivity(playlist);
        }

        void state(String title, String note, final Runnable action) {
            this.cards.removeAllViews();
            this.showingState = true;
            LinearLayout notice = IndexActivity.this.v();
            notice.setGravity(17);
            notice.setPadding(IndexActivity.this.dp(20.0f), IndexActivity.this.dp(24.0f), IndexActivity.this.dp(20.0f), IndexActivity.this.dp(24.0f));
            notice.setBackground(QingyinUi.touch(IndexActivity.this, -657928, 12.0f));
            TextView headline = IndexActivity.this.label(title, 15, -15197924, true);
            headline.setGravity(17);
            notice.addView(headline, IndexActivity.this.lp(-1, -2));
            TextView caption = IndexActivity.this.label(note, 12, -8749947, false);
            caption.setGravity(17);
            LinearLayout.LayoutParams noteLp = IndexActivity.this.lp(-1, -2);
            noteLp.topMargin = IndexActivity.this.dp(8.0f);
            notice.addView(caption, noteLp);
            if (action != null) {
                notice.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$PlaylistRail$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        action.run();
                    }
                });
            }
            notice.setContentDescription(title + "，" + note);
            this.cards.addView(notice, new LinearLayout.LayoutParams(viewportWidth(), IndexActivity.this.dp(148.0f)));
            scrollTo(0, 0);
        }

        private void resizeCards(int viewport) {
            int size = coverSize(viewport);
            for (int i = 0; i < this.cards.getChildCount(); i++) {
                LinearLayout item = (LinearLayout) this.cards.getChildAt(i);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) item.getLayoutParams();
                params.width = this.showingState ? viewport : size;
                item.setLayoutParams(params);
                if (!this.showingState) {
                    View cover = item.getChildAt(0);
                    cover.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                }
            }
        }

        @Override // android.widget.HorizontalScrollView, android.view.View
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w <= 0 || w == oldw) {
                return;
            }
            resizeCards(w);
        }

        @Override // android.view.ViewGroup, android.view.View
        public boolean dispatchTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == 0) {
                this.downX = event.getX();
                this.downY = event.getY();
                this.direction = 0;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            } else {
                if (action == 2 && this.direction == 0) {
                    float dx = Math.abs(event.getX() - this.downX);
                    float dy = Math.abs(event.getY() - this.downY);
                    if (Math.max(dx, dy) > this.touchSlop) {
                        this.direction = dx > dy ? 1 : 2;
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(this.direction == 1);
                        }
                    }
                } else if ((action == 1 || action == 3) && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            return super.dispatchTouchEvent(event);
        }

        @Override // android.widget.HorizontalScrollView, android.view.ViewGroup
        public boolean onInterceptTouchEvent(MotionEvent event) {
            if (this.direction == 2) {
                return false;
            }
            return super.onInterceptTouchEvent(event);
        }

        @Override // android.widget.HorizontalScrollView, android.view.View
        public boolean onTouchEvent(MotionEvent event) {
            if (this.direction == 2) {
                return false;
            }
            return super.onTouchEvent(event);
        }
    }

    private LinearLayout navItem(String symbol, String name, boolean selected, final Runnable action) {
        LinearLayout item = v();
        item.setGravity(17);
        NavIconView glyph = new NavIconView(this, symbol, selected ? -1815446 : -11251365);
        item.addView(glyph, lp(28, 28));
        TextView caption = label(name, 11, selected ? -1815446 : -11251365, selected);
        caption.setGravity(17);
        LinearLayout.LayoutParams textLp = lp(-1, 18);
        textLp.topMargin = dp(2.0f);
        item.addView(caption, textLp);
        item.setContentDescription(name);
        item.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda45
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                action.run();
            }
        });
        return item;
    }

    private void selectTab(int position) {
        if (position < 0 || position >= this.tabScreens.length) {
            return;
        }
        if (position != this.selectedTab) cancelGuessRequest();
        this.selectedTab = position;
        resetChromeScroll();
        setChromeCollapsed(false, false);
        updateCurrentSection();
        int i = 0;
        while (i < this.tabScreens.length) {
            this.tabScreens[i].setVisibility(i == position ? 0 : 8);
            LinearLayout b = this.tabButtons[i];
            NavIconView icon = (NavIconView) b.getChildAt(0);
            TextView label = (TextView) b.getChildAt(1);
            icon.tint(i == position ? -1815446 : -11251365);
            label.setTextColor(i != position ? -11251365 : -1815446);
            label.setTypeface(Typeface.DEFAULT, i == position ? 1 : 0);
            b.setSelected(i == position);
            b.setContentDescription(((Object) label.getText()) + (i == position ? "，当前页面" : ""));
            b.setBackground(QingyinUi.touch(this, i == position ? 1407470698 : 16777215, 29.0f));
            i++;
        }
        if (position == 2 && this.radioStations.isEmpty()) {
            loadRadioStations(false);
        }
        if (position == 1) loadDiscovery(false);
        updateAmbient();
        applyAppearance();
        m106lambda$new$0$localqingyinmobileIndexActivity();
    }

    private TextView avatarButton() {
        TextView avatar = label("青", 17, -11251365, true);
        avatar.setGravity(17);
        avatar.setIncludeFontPadding(false);
        avatar.setPadding(0, 0, 0, 0);
        avatar.setBackground(glass(avatar, 24, -1476990993));
        avatar.setElevation(dp(11.0f));
        avatar.setContentDescription("个人资料");
        avatar.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m64lambda$avatarButton$19$localqingyinmobileIndexActivity(view);
            }
        });
        return avatar;
    }

    /* JADX INFO: renamed from: lambda$avatarButton$19$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m64lambda$avatarButton$19$localqingyinmobileIndexActivity(View v) {
        startActivity(new Intent(this, (Class<?>) ProfileActivity.class));
    }

    private ScrollView page(String title, LinearLayout[] out) {
        final ScrollView scroll = new ChromeScrollView();
        scroll.setOnScrollChangeListener(new View.OnScrollChangeListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda53
            @Override // android.view.View.OnScrollChangeListener
            public final void onScrollChange(View view, int i, int i2, int i3, int i4) {
                IndexActivity.this.m120lambda$page$20$localqingyinmobileIndexActivity(scroll, view, i, i2, i3, i4);
            }
        });
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body = v();
        body.setPadding(dp(19.0f), dp(22.0f), dp(19.0f), dp(182.0f));
        LinearLayout bar = h();
        TextView titleView = label(title, 34, -15197924, true);
        bar.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1.0f));
        if ("资料库".equals(title)) {
            final TextView menu = label("⋯", 28, -15197924, true);
            menu.setGravity(17);
            menu.setContentDescription("资料库更多操作");
            menu.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda54
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    IndexActivity.this.m124lambda$page$24$localqingyinmobileIndexActivity(menu, view);
                }
            });
            bar.addView(menu, lp(44, 46));
            this.account = avatarButton();
            LinearLayout.LayoutParams p = lp(45, 45);
            p.leftMargin = dp(6.0f);
            bar.addView(this.account, p);
        } else if ("新发现".equals(title)) {
            TextView more = label("⋯", 28, -15197924, true);
            more.setGravity(17);
            more.setContentDescription("新发现更多操作");
            more.setBackground(QingyinUi.touch(this, 0x18FFFFFF, 23));
            more.setOnClickListener(view -> showDiscoveryMenu());
            bar.addView(more, lp(45, 45));
        } else {
            TextView avatar = avatarButton();
            bar.addView(avatar, lp(45, 45));
        }
        body.addView(bar, lp(-1, 65));
        scroll.addView(body);
        out[0] = body;
        return scroll;
    }

    /* JADX INFO: renamed from: lambda$page$20$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m120lambda$page$20$localqingyinmobileIndexActivity(ScrollView scroll, View view, int x, int y, int previousX, int previousY) {
        pageScrolled(scroll, y, previousY);
    }

    /* JADX INFO: renamed from: lambda$page$24$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m124lambda$page$24$localqingyinmobileIndexActivity(TextView menu, View v) {
        PopupMenu popup = new PopupMenu(this, menu);
        popup.getMenu().add("我的歌单").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda62
            @Override // android.view.MenuItem.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return IndexActivity.this.m121lambda$page$21$localqingyinmobileIndexActivity(menuItem);
            }
        });
        popup.getMenu().add("我的收藏").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda63
            @Override // android.view.MenuItem.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return IndexActivity.this.m122lambda$page$22$localqingyinmobileIndexActivity(menuItem);
            }
        });
        popup.getMenu().add("账号管理").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda64
            @Override // android.view.MenuItem.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return IndexActivity.this.m123lambda$page$23$localqingyinmobileIndexActivity(menuItem);
            }
        });
        popup.show();
    }

    /* JADX INFO: renamed from: lambda$page$21$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ boolean m121lambda$page$21$localqingyinmobileIndexActivity(MenuItem i) {
        go("mine");
        return true;
    }

    /* JADX INFO: renamed from: lambda$page$22$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ boolean m122lambda$page$22$localqingyinmobileIndexActivity(MenuItem i) {
        go("favorites");
        return true;
    }

    /* JADX INFO: renamed from: lambda$page$23$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ boolean m123lambda$page$23$localqingyinmobileIndexActivity(MenuItem i) {
        go("account");
        return true;
    }

    private FrameLayout recommendationCover(boolean z) {
        float f;
        int i = !z ? 1 : 0;
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackground(QingyinUi.rounded(this, 587202559, 15.0f));
        frameLayout.setClipToOutline(true);
        int[] iArr = {76, 82, 87};
        int[] iArr2 = {5, 12, 17};
        int[] iArr3 = {23, 15, 9};
        int i2 = 0;
        while (i2 < 3) {
            FrameLayout frameLayoutFrame = Artwork.frame(this, 9);
            if (i2 == 0) {
                f = -8.0f;
            } else {
                f = i2 == 1 ? 6.0f : 0.0f;
            }
            frameLayoutFrame.setRotation(f);
            frameLayoutFrame.setElevation(dp(i2 + 1));
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dp(iArr[i2]), dp(iArr[i2]));
            layoutParams.leftMargin = dp(iArr2[i2]);
            layoutParams.topMargin = dp(iArr3[i2]);
            frameLayout.addView(frameLayoutFrame, layoutParams);
            this.recommendationArtwork[i][i2] = frameLayoutFrame;
            i2++;
        }
        return frameLayout;
    }

    private void renderRecommendationCovers() {
        int k = 0;
        while (k < 2) {
            String method = k == 0 ? "dailyRecommendations" : "guessYouLike";
            List<String> urls = previewUrls(method);
            int i = 0;
            while (i < 3) {
                FrameLayout art = this.recommendationArtwork[k][i];
                if (art != null) {
                    int urlIndex = 2 - i;
                    if (urls.isEmpty()) {
                        art.setVisibility(i != 2 ? 4 : 0);
                        Artwork.bind(art, "");
                    } else if (urlIndex < urls.size()) {
                        art.setVisibility(0);
                        Artwork.bind(art, urls.get(urlIndex));
                    } else {
                        art.setVisibility(4);
                        Artwork.bind(art, "");
                    }
                }
                i++;
            }
            k++;
        }
        updateAmbient();
    }

    private void loadRecommendationCover(boolean daily, final long ticket) {
        final String method=daily?"dailyRecommendations":"guessYouLike";
        final int index=daily?0:1;
        final String owner=RecommendationArtwork.accountId(this);
        final long epoch=RecommendationArtwork.epoch(this);
        if(!previewAccountVerified||owner.isEmpty()||RecommendationArtwork.fresh(this,method))return;
        long now=SystemClock.elapsedRealtime();
        if(previewAttempt[index]!=0&&now-previewAttempt[index]<600000)return;
        previewAttempt[index]=now;
        network.execute(()->{
            try {
                JSONObject result=OnlineApi.call(getApplicationContext(),method,new JSONObject());
                String expected=daily?"kugou-daily":"kugou-personal-fm";
                if(!expected.equals(result.optString("source"))||!owner.equals(result.optString("accountId")))return;
                ui.post(()->{
                    if(!active||isDestroyed()||ticket!=generation)return;
                    if(RecommendationArtwork.save(this,method,result.optJSONArray("songs"),owner,epoch))renderRecommendationCovers();
                });
            } catch(Exception ignored) {}
        });
    }

    /* JADX INFO: renamed from: lambda$loadRecommendationCover$26$local-qingyin-mobile-IndexActivity, reason: not valid java name */


    /* JADX INFO: renamed from: lambda$loadRecommendationCover$25$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m104x66c65ff0(long ticket) {
        if (this.active && !isDestroyed() && ticket == this.generation) {
            renderRecommendationCovers();
        }
    }

    private Drawable recommendationGlass() {
        GradientDrawable base = new GradientDrawable();
        base.setColor(-1678382094);
        base.setCornerRadius(dp(22.0f));
        base.setStroke(dp(1.0f), -1191182337);
        return base;
    }

    private LinearLayout recommendationCard(boolean daily, String title, String detail, final String page) {
        LinearLayout card = h();
        card.setPadding(dp(8.0f), dp(8.0f), dp(13.0f), dp(8.0f));
        card.setBackground(recommendationGlass());
        card.setClipToOutline(true);
        card.addView(recommendationCover(daily), lp(106, 106));
        LinearLayout words = v();
        words.setPadding(dp(15.0f), dp(13.0f), 0, 0);
        TextView name = label(title, 22, -15197924, true);
        words.addView(name, lp(-1, 34));
        TextView caption = label(detail, 13, -8749947, false);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(caption, lp(-1, 26));
        card.addView(words, new LinearLayout.LayoutParams(0, -1, 1.0f));
        TextView arrow = label(daily ? "›" : "▶", daily ? 28 : 19, -5987156, false);
        arrow.setGravity(17);
        card.addView(arrow, lp(19, -1));
        card.setContentDescription((daily ? "打开" : "播放") + title);
        if (!daily) {
            guessCard = card;
            guessCaption = caption;
            guessIndicator = arrow;
        }
        card.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                IndexActivity.this.m125lambda$recommendationCard$27$localqingyinmobileIndexActivity(page, view);
            }
        });
        return card;
    }

    /* JADX INFO: renamed from: lambda$recommendationCard$27$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m125lambda$recommendationCard$27$localqingyinmobileIndexActivity(String page, View view) {
        go(page);
    }

    private void setGuessLoading(boolean loading) {
        guessLoading = loading;
        if (guessCard != null) {
            guessCard.setEnabled(!loading);
            guessCard.setContentDescription(loading ? "正在获取猜你喜欢" : "播放猜你喜欢");
        }
        if (guessCaption != null) guessCaption.setText(loading ? "正在获取推荐…" : "点按即播 · 发现喜欢的音乐");
        if (guessIndicator != null) guessIndicator.setText(loading ? "…" : "▶");
    }

    private void cancelGuessRequest() {
        if (!guessLoading) return;
        guessRequest++;
        setGuessLoading(false);
    }

    /** Always request current personal FM songs; preview artwork never supplies a queue. */
    private void playGuessRecommendations() {
        if (guessLoading || !active || selectedTab != 0) return;
        if (!bound || player == null) {
            Toast.makeText(this, "播放器正在启动，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        final long ticket = ++guessRequest;
        final long accountEpoch = RecommendationArtwork.epoch(this);
        final PlaybackService requestedPlayer = player;
        final Track previousTrack = player.current();
        final boolean wasRunning = player.isPlaying() || player.isPreparing();
        setGuessLoading(true);
        network.execute(() -> {
            try {
                JSONObject params = new JSONObject().put("refresh", true);
                JSONObject response = OnlineApi.call(getApplicationContext(), "guessYouLike", params);
                String owner = response.optString("accountId", "");
                if (!"kugou-personal-fm".equals(response.optString("source")) || owner.isEmpty())
                    throw new IllegalStateException("酷狗推荐来源或账号信息缺失，请重新登录后重试");
                JSONArray entries = response.optJSONArray("songs");
                ArrayList<Track> tracks = new ArrayList<>();
                HashSet<String> unique = new HashSet<>();
                if (entries != null) for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.optJSONObject(i);
                    Track song = entry == null ? null : OnlineApi.asTrack(entry);
                    if (song != null && unique.add(song.uri)) tracks.add(song);
                }
                if (tracks.isEmpty()) throw new IllegalStateException("酷狗暂时没有返回可播放的推荐歌曲");
                ui.post(() -> {
                    if (!active || isDestroyed() || selectedTab != 0 || ticket != guessRequest) return;
                    setGuessLoading(false);
                    if (accountEpoch != RecommendationArtwork.epoch(this)
                            && !owner.equals(RecommendationArtwork.accountId(this))) {
                        Toast.makeText(this, "账号已更改，请重新点击猜你喜欢", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // A newer playback choice or pause takes precedence over this network response.
                    if (!bound || player != requestedPlayer || player.current() != previousTrack
                            || (wasRunning && !player.isPlaying() && !player.isPreparing())) return;
                    RecommendationArtwork.setAccount(this, owner);
                    RecommendationArtwork.save(this, "guessYouLike", entries, owner, RecommendationArtwork.epoch(this));
                    previewAccountVerified = true;
                    renderRecommendationCovers();
                    ensureForeground();
                    player.playQueue(tracks, 0);
                    updateMini();
                    openPlayer();
                });
            } catch (Exception failure) {
                boolean loginRequired = failure instanceof OnlineApi.ApiException
                        && ("LOGIN_REQUIRED".equals(((OnlineApi.ApiException) failure).code)
                        || "SESSION_EXPIRED".equals(((OnlineApi.ApiException) failure).code));
                ui.post(() -> {
                    if (!active || isDestroyed() || ticket != guessRequest) return;
                    setGuessLoading(false);
                    Toast.makeText(this, loginRequired ? "请先在账号中登录酷狗，再播放猜你喜欢"
                            : "暂时无法获取酷狗推荐，请稍后重试", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private ScrollView buildHome() {
        LinearLayout[] linearLayoutArr = new LinearLayout[1];
        ScrollView scroll = page("主页", linearLayoutArr);
        LinearLayout linearLayout = linearLayoutArr[0];
        heading(linearLayout, "为你推荐", "", null);
        LinearLayout cards = v();
        cards.addView(recommendationCard(true, "每日推荐", "每天为你精选", "daily"), lp(-1, 122));
        LinearLayout.LayoutParams guessLp = lp(-1, 122);
        guessLp.topMargin = dp(12.0f);
        cards.addView(recommendationCard(false, "猜你喜欢", "点按即播 · 发现喜欢的音乐", "guess"), guessLp);
        linearLayout.addView(cards);
        renderRecommendationCovers();
        heading(linearLayout, "最近播放", "›", new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m68lambda$buildHome$28$localqingyinmobileIndexActivity();
            }
        });
        this.recentPreview = h();
        gallery(linearLayout, this.recentPreview, 200);
        heading(linearLayout, "推荐歌单", "›", new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m69lambda$buildHome$29$localqingyinmobileIndexActivity();
            }
        });
        this.homeKugouLists = new PlaylistRail();
        linearLayout.addView(this.homeKugouLists, lp(-1, -2));
        return scroll;
    }

    /* JADX INFO: renamed from: lambda$buildHome$28$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m68lambda$buildHome$28$localqingyinmobileIndexActivity() {
        go("recents");
    }

    /* JADX INFO: renamed from: lambda$buildHome$29$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m69lambda$buildHome$29$localqingyinmobileIndexActivity() {
        go("recommendations");
    }

    private ScrollView buildDiscover() {
        LinearLayout[] out = new LinearLayout[1];
        ScrollView scroll = page("新发现", out);
        LinearLayout body = out[0];
        discoveryRepository = new DiscoveryRepository(this);
        discoveryFeed = new DiscoveryFeed(this, new DiscoveryFeed.Listener() {
            @Override public void onOpen(DiscoveryContent.Section section, DiscoveryContent.Item item) {
                openDiscovery(section, item);
            }
            @Override public void onItemMore(DiscoveryContent.Section section, DiscoveryContent.Item item) {
                showDiscoverySongActions(section, item);
            }
            @Override public void onRetry(DiscoveryContent.Section section) {
                discoveryRepository.retry(section.key, IndexActivity.this::updateDiscovery);
            }
        });
        ArrayList<DiscoveryContent.Section> sections = new ArrayList<>(DiscoveryRepository.placeholders());
        ArrayList<DiscoveryContent.Item> moments = new ArrayList<>();
        String[][] scenes = {{"专注时刻", "轻音乐，陪你进入状态", "专注 轻音乐"}, {"通勤路上", "让每一段路都有节奏", "通勤"},
            {"深夜放松", "慢下来，听见自己", "深夜 放松"}, {"运动节拍", "跟着音乐再向前一步", "运动"},
            {"经典重温", "再听一次熟悉的旋律", "经典老歌"}, {"雨天留声", "给下雨的日子一点声音", "雨天"}};
        for (int i = 0; i < scenes.length; i++) {
            JSONObject query = new JSONObject();
            try { query.put("query", scenes[i][2]); } catch (Exception ignored) {}
            moments.add(new DiscoveryContent.Item("search", "moment-" + i, scenes[i][0], scenes[i][1], "", query, null));
        }
        sections.add(new DiscoveryContent.Section("moments", "此刻想听", "按场景寻找音乐", "topics", moments, null, false, "", 0, false));
        discoveryFeed.setSections(sections);
        body.addView(discoveryFeed, lp(-1, -2));
        return scroll;
    }

    private void loadDiscovery(boolean force) {
        if (discoveryRepository != null) discoveryRepository.load(force, this::updateDiscovery);
    }

    private void updateDiscovery(DiscoveryContent.Section section) {
        if (isDestroyed() || isFinishing() || discoveryFeed == null) return;
        discoveryFeed.update(section);
    }

    private void showDiscoveryMenu() {
        QingyinUi.Sheet sheet = new QingyinUi.Sheet(this, "新发现", "寻找今天想听的音乐", false);
        sheet.addAction("refresh", "刷新内容", "更新酷狗精选歌单和榜单", () -> loadDiscovery(true));
        sheet.addAction("search", "搜索音乐", "歌曲、歌手或专辑", () -> goSearch(null));
        sheet.addAction("heart", "我的收藏", "", () -> go("favorites"));
        sheet.show();
    }

    private void openDiscovery(DiscoveryContent.Section section, DiscoveryContent.Item item) {
        if (item == null) return;
        switch (item.kind) {
            case "song": playDiscovery(section, item, false); break;
            case "playlist":
                startActivity(new Intent(this, OnlineActivity.class).putExtra("page", "discovery")
                    .putExtra("title", item.title).putExtra("playlist_special_id", item.id));
                break;
            case "album":
                long id = item.data.optLong("album_id", item.data.optLong("id", 0));
                if (id > 0) startActivity(new Intent(this, AlbumDetailActivity.class).putExtra("album_id", id));
                break;
            case "chart":
            case "collection":
                Intent intent = new Intent(this, OnlineActivity.class).putExtra("page", "discovery").putExtra("title", item.title);
                if ("chart".equals(item.kind)) intent.putExtra("rank_id", item.data.optString("rankid", item.id));
                else intent.putExtra("songs_json", item.data.optJSONArray("songs") == null ? "[]" : item.data.optJSONArray("songs").toString());
                startActivity(intent);
                break;
            default: goSearch(item.data.optString("query", item.title));
        }
    }

    private ArrayList<Track> discoveryQueue(DiscoveryContent.Section section, Track song) {
        ArrayList<Track> queue = new ArrayList<>();
        if (section != null) for (DiscoveryContent.Item item : section.items) if (item.song != null) queue.add(item.song);
        if (queue.isEmpty() && song != null) queue.add(song);
        return queue;
    }

    private void playDiscovery(DiscoveryContent.Section section, DiscoveryContent.Item item, boolean openPlayer) {
        if (item.song == null) return;
        if (!bound || player == null) {
            Toast.makeText(this, "播放器正在启动，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Track> queue = discoveryQueue(section, item.song);
        int position = 0;
        for (int i = 0; i < queue.size(); i++) if (queue.get(i).uri.equals(item.song.uri)) { position = i; break; }
        ensureForeground();
        player.playQueue(queue, position);
        updateMini();
        if (openPlayer) startActivity(new Intent(this, NowPlayingActivity.class));
    }

    private void showDiscoverySongActions(DiscoveryContent.Section section, DiscoveryContent.Item item) {
        Track song = item.song;
        if (song == null) { openDiscovery(section, item); return; }
        QingyinUi.Sheet sheet = new QingyinUi.Sheet(this, song.title, song.artist, false);
        sheet.setSong(song);
        sheet.addAction("next", "下一首播放", "", () -> queueDiscovery(song, true));
        sheet.addAction("add", "加入播放队列", "", () -> queueDiscovery(song, false));
        sheet.addSeparator();
        sheet.addAction("heart", OnlineLibraryStore.isFavorite(this, song) ? "取消收藏" : "加入收藏", "", () -> OnlineLibraryStore.toggle(this, song));
        sheet.addAction("play", "立即播放", "", () -> playDiscovery(section, item, false));
        sheet.addAction("player", "打开播放页", "", () -> playDiscovery(section, item, true));
        sheet.addAction("queue", "查看播放队列", "", () -> QueueDialog.show(this, player));
        sheet.show();
    }

    private void queueDiscovery(Track song, boolean next) {
        if (!bound || player == null) {
            Toast.makeText(this, "播放器正在启动，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean changed = next ? player.playNext(song) : player.enqueue(song);
        Toast.makeText(this, changed ? (next ? "已设为下一首播放" : "已加入播放队列") : "歌曲已在播放队列", Toast.LENGTH_SHORT).show();
    }

    /* JADX INFO: renamed from: lambda$buildDiscover$30$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m65lambda$buildDiscover$30$localqingyinmobileIndexActivity() {
        go("recommendations");
    }

    /* JADX INFO: renamed from: lambda$buildDiscover$31$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m66lambda$buildDiscover$31$localqingyinmobileIndexActivity() {
        if (!this.picks.isEmpty()) {
            m81lambda$drawMine$47$localqingyinmobileIndexActivity(this.picks.get(0));
        } else {
            go("daily");
        }
    }

    /* JADX INFO: renamed from: lambda$buildDiscover$32$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m67lambda$buildDiscover$32$localqingyinmobileIndexActivity() {
        go("recommendations");
    }

    /* JADX WARN: Multi-variable type inference failed */
    private ScrollView buildRadio() {
        LinearLayout[] linearLayoutArr = new LinearLayout[1];
        ScrollView scrollViewPage = page("直播电台", linearLayoutArr);
        LinearLayout linearLayout = linearLayoutArr[0];
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackground(QingyinUi.rounded(this, -13290152, 22.0f));
        frameLayout.setClipToOutline(true);
        frameLayout.addView(RadioVisuals.headerArt(this), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout linearLayoutV = v();
        linearLayoutV.setPadding(dp(18.0f), dp(19.0f), 0, dp(16.0f));
        View viewLabel = label("●  LIVE RADIO  ·  精选真实广播", 11, -7699, true);
        linearLayoutV.addView(viewLabel);
        View viewLabel2 = label("听见好声音", 23, -1, true);
        LinearLayout.LayoutParams layoutParamsLp = lp(-1, -2);
        layoutParamsLp.topMargin = dp(20.0f);
        linearLayoutV.addView(viewLabel2, layoutParamsLp);
        View viewLabel3 = label("资讯 · 财经 · 城市 · 人文 · 音乐", 12, -1451544, false);
        LinearLayout.LayoutParams layoutParamsLp2 = lp(-1, -2);
        layoutParamsLp2.topMargin = dp(7.0f);
        linearLayoutV.addView(viewLabel3, layoutParamsLp2);
        frameLayout.addView(linearLayoutV, new FrameLayout.LayoutParams(-1, -2, 16));
        LinearLayout.LayoutParams layoutParamsLp3 = lp(-1, 149);
        layoutParamsLp3.topMargin = dp(8.0f);
        layoutParamsLp3.bottomMargin = dp(21.0f);
        linearLayout.addView(frameLayout, layoutParamsLp3);
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        final LinearLayout linearLayoutH = h();
        String[][] strArr = {new String[]{"featured", "精选"}, new String[]{"news", "新闻资讯"}, new String[]{"finance", "财经商业"}, new String[]{"city", "城市交通"}, new String[]{"culture", "人文知识"}, new String[]{"music", "音乐"}};
        int length = strArr.length;
        int i = 0;
        while (true) {
            LinearLayout linearLayout2 = linearLayoutV;
            if (i < length) {
                String[] strArr2 = strArr[i];
                String[][] strArr3 = strArr;
                final String str = strArr2[0];
                int i2 = length;
                View view = viewLabel;
                View view2 = viewLabel2;
                TextView textViewLabel = label(strArr2[1], 14, -15197924, false);
                textViewLabel.setGravity(17);
                textViewLabel.setPadding(dp(13.0f), 0, dp(13.0f), 0);
                textViewLabel.setTag(str);
                textViewLabel.setTypeface(Typeface.DEFAULT, "featured".equals(str) ? 1 : 0);
                textViewLabel.setTextColor("featured".equals(str) ? -1815446 : -8749947);
                textViewLabel.setBackground(QingyinUi.touch(this, "featured".equals(str) ? -202258 : -723721, 17.0f));
                textViewLabel.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda5
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view3) {
                        IndexActivity.this.m75lambda$buildRadio$33$localqingyinmobileIndexActivity(str, linearLayoutH, view3);
                    }
                });
                LinearLayout.LayoutParams layoutParamsLp4 = lp(-2, 38);
                layoutParamsLp4.rightMargin = dp(7.0f);
                linearLayoutH.addView(textViewLabel, layoutParamsLp4);
                i++;
                linearLayoutV = linearLayout2;
                strArr = strArr3;
                length = i2;
                viewLabel = view;
                viewLabel2 = view2;
            } else {
                horizontalScrollView.addView(linearLayoutH);
                linearLayout.addView(horizontalScrollView, new LinearLayout.LayoutParams(-1, dp(45.0f)));
                LinearLayout linearLayoutH2 = h();
                this.radioStatus = label("编辑精选 · 选择频道收听", 13, -8749947, false);
                this.radioStatus.setGravity(16);
                this.radioStatus.setSingleLine(true);
                this.radioStatus.setEllipsize(TextUtils.TruncateAt.END);
                linearLayoutH2.addView(this.radioStatus, new LinearLayout.LayoutParams(0, dp(48.0f), 1.0f));
                TextView textViewLabel2 = label("刷新 ↻", 13, -1815446, true);
                textViewLabel2.setGravity(17);
                textViewLabel2.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda6
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view3) {
                        IndexActivity.this.m76lambda$buildRadio$34$localqingyinmobileIndexActivity(view3);
                    }
                });
                linearLayoutH2.addView(textViewLabel2, lp(70, 44));
                linearLayout.addView(linearLayoutH2, lp(-1, 55));
                this.radioLists = v();
                linearLayout.addView(this.radioLists);
                return scrollViewPage;
            }
        }
    }

    /* JADX INFO: renamed from: lambda$buildRadio$33$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m75lambda$buildRadio$33$localqingyinmobileIndexActivity(String str, LinearLayout linearLayout, View view) {
        if (str.equals(this.radioCategory)) {
            return;
        }
        this.radioCategory = str;
        this.radioStations.clear();
        this.radioLists.removeAllViews();
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            TextView textView = (TextView) linearLayout.getChildAt(i);
            boolean zEquals = str.equals(textView.getTag());
            textView.setBackground(QingyinUi.touch(this, zEquals ? -202258 : -723721, 17.0f));
            textView.setTextColor(zEquals ? -1815446 : -8749947);
            textView.setTypeface(Typeface.DEFAULT, zEquals ? 1 : 0);
        }
        loadRadioStations(true);
    }

    /* JADX INFO: renamed from: lambda$buildRadio$34$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m76lambda$buildRadio$34$localqingyinmobileIndexActivity(View v) {
        loadRadioStations(true);
    }

    private void renderRadioStations() {
        if (this.radioLists == null) {
            return;
        }
        this.radioLists.removeAllViews();
        for (final RadioDirectory.Station station : this.radioStations) {
            LinearLayout row = h();
            row.setPadding(dp(10.0f), dp(9.0f), dp(11.0f), dp(9.0f));
            row.setBackground(QingyinUi.touch(this, -460550, 17.0f));
            FrameLayout cover = RadioVisuals.cover(this, station);
            row.addView(cover, lp(60, 60));
            LinearLayout info = v();
            info.setPadding(dp(13.0f), 0, dp(4.0f), 0);
            TextView title = label(station.name, 15, -15197924, true);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            info.addView(title);
            String spec = "● LIVE  ·  " + station.codec + (station.bitrate > 0 ? "  ·  " + station.bitrate + " kbps" : "");
            TextView sub = label(spec, 12, -8749947, false);
            sub.setPadding(0, dp(4.0f), 0, 0);
            info.addView(sub);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView play = label("▶", 17, -1815446, true);
            play.setGravity(17);
            play.setBackground(QingyinUi.rounded(this, -202514, 17.0f));
            play.setContentDescription("播放直播");
            row.addView(play, lp(35, 35));
            row.setContentDescription("收听直播：" + station.name);
            row.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda61
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    IndexActivity.this.m126lambda$renderRadioStations$35$localqingyinmobileIndexActivity(station, view);
                }
            });
            LinearLayout.LayoutParams rl = lp(-1, 79);
            rl.bottomMargin = dp(8.0f);
            this.radioLists.addView(row, rl);
        }
    }

    /* JADX INFO: renamed from: lambda$renderRadioStations$35$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m126lambda$renderRadioStations$35$localqingyinmobileIndexActivity(RadioDirectory.Station station, View v) {
        if (!this.bound || this.player == null) {
            Toast.makeText(this, "播放器正在启动，请稍后重试", 0).show();
            return;
        }
        ensureForeground();
        ArrayList<Track> live = new ArrayList<>();
        live.add(station.asTrack());
        this.player.playQueue(live, 0);
        updateMini();
    }

    private void loadRadioStations(boolean force) {
        if (this.radioStatus == null || this.radioLists == null) {
            return;
        }
        final String target = this.radioCategory;
        long age = SystemClock.elapsedRealtime() - this.radioFetchedAt;
        if (force || this.radioStations.isEmpty() || age >= 480000) {
            final int ticket = this.radioSerial + 1;
            this.radioSerial = ticket;
            this.radioStatus.setText("正在查找可用直播频道…");
            if (force && this.radioStations.isEmpty()) {
                this.radioLists.removeAllViews();
            }
            this.network.execute(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda50
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m103lambda$loadRadioStations$38$localqingyinmobileIndexActivity(target, ticket);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$loadRadioStations$38$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m103lambda$loadRadioStations$38$localqingyinmobileIndexActivity(String target, final int ticket) {
        try {
            final List<RadioDirectory.Station> found = RadioDirectory.fetch(target);
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m101lambda$loadRadioStations$36$localqingyinmobileIndexActivity(ticket, found);
                }
            });
        } catch (Exception e) {
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m102lambda$loadRadioStations$37$localqingyinmobileIndexActivity(ticket);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$loadRadioStations$36$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m101lambda$loadRadioStations$36$localqingyinmobileIndexActivity(int ticket, List found) {
        if (isDestroyed() || ticket != this.radioSerial) {
            return;
        }
        this.radioStations.clear();
        this.radioStations.addAll(found);
        this.radioFetchedAt = SystemClock.elapsedRealtime();
        this.radioStatus.setText(found.size() + " 个精选直播频道 · 点击收听");
        renderRadioStations();
    }

    /* JADX INFO: renamed from: lambda$loadRadioStations$37$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m102lambda$loadRadioStations$37$localqingyinmobileIndexActivity(int ticket) {
        if (isDestroyed() || ticket != this.radioSerial) {
            return;
        }
        this.radioStatus.setText("暂时无法加载直播 · 点击右侧刷新重试");
        if (this.radioStations.isEmpty()) {
            TextView notice = label("当前分类暂时没有可用频道。\n试试其他分类，或稍后刷新。", 14, -8749947, false);
            notice.setPadding(dp(9.0f), dp(28.0f), dp(9.0f), dp(23.0f));
            this.radioLists.removeAllViews();
            this.radioLists.addView(notice);
        }
    }

    private LinearLayout category(String kind,String name,final Runnable action){
        LinearLayout row=h();row.setPadding(dp(14),0,dp(12),0);
        row.addView(new ActionGlyph(this,kind,0xffEC9CB2),lp(26,26));
        TextView title=label(name,16,NAVY,false);title.setPadding(dp(12),0,dp(8),0);
        row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(new ActionGlyph(this,"chevron",0xff94919F),lp(16,20));
        row.setContentDescription(name);row.setBackground(QingyinUi.touch(this,0x00FFFFFF,0));
        AppearanceStyler.preserveBackground(row);row.setOnClickListener(v->action.run());return row;
    }
    private LinearLayout libraryGroup;
    private void styleLibraryGroup(){
        if(libraryGroup==null)return;
        boolean dark=AppearancePreferences.dark(this);
        GradientDrawable shape=QingyinUi.rounded(this,dark?0x16FFFFFF:0xD9FFFFFF,20);
        shape.setStroke(1,dark?0x0FFFFFFF:0x10000000);libraryGroup.setBackground(shape);
        for(int i=0;i<libraryGroup.getChildCount();i++){
            View child=libraryGroup.getChildAt(i);
            if("library-divider".equals(child.getTag()))child.setBackgroundColor(dark?0x14FFFFFF:0x10000000);
        }
    }

    private ScrollView buildLibrary() {
        LinearLayout[] linearLayoutArr = new LinearLayout[1];
        ScrollView scroll = page("资料库", linearLayoutArr);
        LinearLayout linearLayout = linearLayoutArr[0];
        LinearLayout center = v();
        center.setGravity(17);
        this.libraryCover = Artwork.frame(this, 12);
        center.addView(this.libraryCover, lp(108, 108));
        TextView snow = label("❄", 18, -5711120, false);
        snow.setGravity(17);

        LinearLayout.LayoutParams centerLp = lp(-1, 142);
        centerLp.topMargin = dp(10.0f);
        linearLayout.addView(center, centerLp);
        String[] names = {"播放列表", "艺人", "最近播放", "收藏歌曲", "已下载"};
        String[] icons = {"queue", "artist", "history", "heart", "download"};
        Runnable[] actions = {new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda55
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m70lambda$buildLibrary$40$localqingyinmobileIndexActivity();
            }
        }, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda66
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m71lambda$buildLibrary$41$localqingyinmobileIndexActivity();
            }
        }, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda72
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m72lambda$buildLibrary$42$localqingyinmobileIndexActivity();
            }
        }, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda73
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m73lambda$buildLibrary$43$localqingyinmobileIndexActivity();
            }
        }, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda74
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.goLocal();
            }
        }};
        libraryGroup=v();libraryGroup.setClipToOutline(true);AppearanceStyler.preserveBackground(libraryGroup);
        for(int i=0;i<names.length;i++){
            libraryGroup.addView(category(icons[i],names[i],actions[i]),lp(-1,56));
            if(i<names.length-1){
                View divider=new View(this);divider.setTag("library-divider");
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,1);p.leftMargin=dp(52);p.rightMargin=dp(16);libraryGroup.addView(divider,p);
            }
        }
        styleLibraryGroup();linearLayout.addView(libraryGroup,lp(-1,-2));
        this.librarySavedSection = v();
        heading(this.librarySavedSection, "已收藏歌单", "", null);
        this.librarySavedLists = new PlaylistRail();
        this.librarySavedSection.addView(this.librarySavedLists, lp(-1, -2));
        this.librarySavedSection.setVisibility(8);
        linearLayout.addView(this.librarySavedSection, lp(-1, -2));
        heading(linearLayout, "最近添加", "›", new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m74lambda$buildLibrary$44$localqingyinmobileIndexActivity();
            }
        });
        this.libraryLists = v();
        linearLayout.addView(this.libraryLists);
        return scroll;
    }

    /* JADX INFO: renamed from: lambda$buildLibrary$40$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m70lambda$buildLibrary$40$localqingyinmobileIndexActivity() {
        go("mine");
    }

    /* JADX INFO: renamed from: lambda$buildLibrary$41$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m71lambda$buildLibrary$41$localqingyinmobileIndexActivity() {
        go("artists");
    }

    /* JADX INFO: renamed from: lambda$buildLibrary$42$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m72lambda$buildLibrary$42$localqingyinmobileIndexActivity() {
        go("recents");
    }

    /* JADX INFO: renamed from: lambda$buildLibrary$43$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m73lambda$buildLibrary$43$localqingyinmobileIndexActivity() {
        go("favorites");
    }

    /* JADX INFO: renamed from: lambda$buildLibrary$44$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m74lambda$buildLibrary$44$localqingyinmobileIndexActivity() {
        go("favorites");
    }

    private LinearLayout compactRow(String symbol, String heading, String sub, Runnable action) {
        return compactRow(symbol, heading, sub, "", action);
    }

    private LinearLayout compactRow(String symbol, String heading, String sub, String coverUrl, final Runnable action) {
        LinearLayout card = h();
        card.setPadding(dp(9.0f), dp(6.0f), dp(8.0f), dp(6.0f));
        card.setBackground(QingyinUi.touch(this, -1, 13.0f));
        card.setContentDescription(heading + "，" + sub);
        FrameLayout cover = Artwork.frame(this, 10);
        Artwork.bind(cover, coverUrl);
        card.addView(cover, lp(49, 49));
        LinearLayout copy = v();
        copy.setPadding(dp(11.0f), 0, dp(5.0f), 0);
        TextView title = label(heading, 14, -15197924, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title);
        TextView subtitle = label(sub, 11, -8749947, false);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subLp = lp(-1, -2);
        subLp.topMargin = dp(5.0f);
        copy.addView(subtitle, subLp);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView arrow = label("›", 23, -6575937, false);
        arrow.setGravity(17);
        card.addView(arrow, lp(19, 42));
        card.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                action.run();
            }
        });
        return card;
    }

    private void addRow(LinearLayout target, LinearLayout row) {
        LinearLayout.LayoutParams rowLp = lp(-1, 61);
        rowLp.bottomMargin = dp(6.0f);
        target.addView(row, rowLp);
    }

    private int albumWidth() {
        float screen = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        return Math.max(95, Math.min(190, (int) (((screen - 38.0f) - 13.0f) / 2.0f)));
    }

    private void drawMine() {
        int k;
        LinearLayout item;
        String title;
        if (this.libraryLists == null) {
            return;
        }
        ArrayList<JSONObject> saved = OnlineLibraryStore.savedPlaylists(this);
        final ArrayList<Track> favorites = OnlineLibraryStore.tracks(this, true);
        StringBuilder fingerprint = new StringBuilder();
        for (JSONObject list : this.personalLists) {
            fingerprint.append(list.optString("id")).append(':').append(list.optString("title")).append(':').append(list.optInt("count")).append(':').append(PlaylistCoverResolver.preferred(this, list)).append('|');
        }
        for (JSONObject item2 : saved) {
            fingerprint.append(item2.toString()).append('|');
        }
        for (Track song : favorites) {
            fingerprint.append(song.uri).append(song.coverUrl);
        }
        fingerprint.append(this.accountLoggedIn).append(this.libraryLoadFailed);
        String key = fingerprint.toString();
        if (key.equals(this.mineRenderSignature)) {
            return;
        }
        this.mineRenderSignature = key;
        updateAmbient();
        if (this.librarySavedSection != null) {
            this.librarySavedSection.setVisibility(saved.isEmpty() ? 8 : 0);
            this.librarySavedLists.playlists(saved, 0, saved.size());
        }
        this.libraryLists.removeAllViews();
        if (this.personalLists.isEmpty() && favorites.isEmpty()) {
            if (this.libraryLoadFailed) {
                title = "歌单暂时无法加载";
            } else if (this.accountLoggedIn == null) {
                title = "正在同步歌单…";
            } else {
                title = Boolean.FALSE.equals(this.accountLoggedIn) ? "登录后同步你的歌单" : "这里还没有音乐";
            }
            addRow(this.libraryLists, compactRow("♫", title, "收藏歌曲后也会显示在这里", Boolean.FALSE.equals(this.accountLoggedIn) ? new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m80lambda$drawMine$46$localqingyinmobileIndexActivity();
                }
            } : this::refreshHome));
            return;
        }
        int width = albumWidth();
        int size = Math.min(6, this.personalLists.size() + favorites.size());
        for (int n = 0; n < size; n += 2) {
            LinearLayout row = h();
            int k2 = n;
            while (k2 < Math.min(n + 2, size)) {
                if (k2 < this.personalLists.size()) {
                    final JSONObject playlist = this.personalLists.get(k2);
                    k = k2;
                    item = tile(playlist.optString("title", "我的歌单"), OnlineApi.playlistCountLabel(playlist), PlaylistCoverResolver.preferred(this, playlist), new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda12
                        @Override // java.lang.Runnable
                        public final void run() {
                            IndexActivity.this.m81lambda$drawMine$47$localqingyinmobileIndexActivity(playlist);
                        }
                    }, width);
                } else {
                    k = k2;
                    final int index = k - this.personalLists.size();
                    Track song2 = favorites.get(index);
                    item = tile(song2.title, song2.artist, song2.coverUrl, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda13
                        @Override // java.lang.Runnable
                        public final void run() {
                            IndexActivity.this.m82lambda$drawMine$48$localqingyinmobileIndexActivity(favorites, index);
                        }
                    }, width);
                }
                addTile(row, item);
                k2 = k + 1;
                row = row;
            }
            LinearLayout.LayoutParams params = lp(-1, width + 56);
            params.bottomMargin = dp(12.0f);
            this.libraryLists.addView(row, params);
        }
        if (AppearancePreferences.dark(this)) {
            AppearanceStyler.apply(this.tabScreens[3], true);
        }
    }

    /* JADX INFO: renamed from: lambda$drawMine$46$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m80lambda$drawMine$46$localqingyinmobileIndexActivity() {
        go("account");
    }

    /* JADX INFO: renamed from: lambda$drawMine$48$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m82lambda$drawMine$48$localqingyinmobileIndexActivity(ArrayList favorites, int index) {
        if (this.bound && this.player != null) {
            ensureForeground();
            this.player.playQueue(new ArrayList(favorites), index);
            updateMini();
            return;
        }
        go("favorites");
    }

    private void drawRecent() {
        if (this.recentPreview == null) {
            return;
        }
        this.recentPreview.removeAllViews();
        final ArrayList<Track> history = OnlineLibraryStore.tracks(this, false);
        if (history.isEmpty()) {
            addTile(this.recentPreview, tile("开始聆听", "发现喜欢的音乐", "", new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda67
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m85lambda$drawRecent$49$localqingyinmobileIndexActivity();
                }
            }, 146));
            return;
        }
        for (int i = 0; i < Math.min(8, history.size()); i++) {
            Track song = history.get(i);
            final int index = i;
            addTile(this.recentPreview, tile(song.title, song.artist, song.coverUrl, new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda68
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m86lambda$drawRecent$50$localqingyinmobileIndexActivity(history, index);
                }
            }, 146));
        }
        if (this.libraryCover != null) {
            Artwork.bind(this.libraryCover, history.get(0).coverUrl);
        }
        m106lambda$new$0$localqingyinmobileIndexActivity();
    }

    /* JADX INFO: renamed from: lambda$drawRecent$49$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m85lambda$drawRecent$49$localqingyinmobileIndexActivity() {
        selectTab(1);
    }

    /* JADX INFO: renamed from: lambda$drawRecent$50$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m86lambda$drawRecent$50$localqingyinmobileIndexActivity(ArrayList history, int index) {
        if (this.bound && this.player != null) {
            ensureForeground();
            this.player.playQueue(new ArrayList(history), index);
            updateMini();
            return;
        }
        go("recents");
    }

    private void drawDiscoverSongs() {
        String name;
        String note;
        if (this.discoverSongList == null) {
            return;
        }
        this.discoverSongList.removeAllViews();
        if (this.discoverySongs.isEmpty()) {
            if (this.songsLoading) {
                name = "正在获取新歌…";
            } else {
                name = this.songsLoadFailed ? "新歌加载失败，点击重试" : "发现你喜欢的音乐";
            }
            if (this.songsLoading) {
                note = "请稍候";
            } else {
                note = this.songsLoadFailed ? "网络可能暂时不可用" : "搜索歌曲或探索精选歌单";
            }
            addRow(this.discoverSongList, compactRow("♫", name, note, this.songsLoadFailed ? new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda37
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m77lambda$drawDiscoverSongs$51$localqingyinmobileIndexActivity();
                }
            } : new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda38
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m78lambda$drawDiscoverSongs$52$localqingyinmobileIndexActivity();
                }
            }));
            return;
        }
        for (int i = 0; i < Math.min(5, this.discoverySongs.size()); i++) {
            final Track song = this.discoverySongs.get(i);
            final int index = i;
            LinearLayout row = h();
            FrameLayout art = Artwork.frame(this, 6);
            Artwork.bind(art, song.coverUrl);
            row.addView(art, lp(49, 49));
            LinearLayout details = v();
            details.setPadding(dp(12.0f), 0, dp(3.0f), 0);
            TextView title = label(song.title, 16, -15197924, false);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            details.addView(title);
            TextView artist = label(song.artist, 13, -8749947, false);
            artist.setSingleLine(true);
            artist.setEllipsize(TextUtils.TruncateAt.END);
            details.addView(artist);
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView action = label("⋯", 23, -15197924, true);
            action.setGravity(17);
            action.setContentDescription("播放" + song.title);
            row.addView(action, lp(43, 53));
            final Runnable play = new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda39
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m79lambda$drawDiscoverSongs$53$localqingyinmobileIndexActivity(index, song);
                }
            };
            row.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda40
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    play.run();
                }
            });
            action.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda41
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    play.run();
                }
            });
            this.discoverSongList.addView(row, lp(-1, 58));
            View line = new View(this);
            line.setBackgroundColor(-1250066);
            this.discoverSongList.addView(line, lp(-1, 1));
        }
    }

    /* JADX INFO: renamed from: lambda$drawDiscoverSongs$51$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m77lambda$drawDiscoverSongs$51$localqingyinmobileIndexActivity() {
        loadDiscoverySongs(this.generation);
    }

    /* JADX INFO: renamed from: lambda$drawDiscoverSongs$52$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m78lambda$drawDiscoverSongs$52$localqingyinmobileIndexActivity() {
        goSearch(null);
    }

    /* JADX INFO: renamed from: lambda$drawDiscoverSongs$53$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m79lambda$drawDiscoverSongs$53$localqingyinmobileIndexActivity(int index, Track song) {
        if (this.bound && this.player != null) {
            ensureForeground();
            this.player.playQueue(new ArrayList(this.discoverySongs), index);
            updateMini();
            return;
        }
        goSearch(song.title);
    }

    private void loadDiscoverySongs(final long ticket) {
        if (this.discoverSongList == null) return;
        if (this.picks.isEmpty()) {
            return;
        }
        this.songsLoading = true;
        this.songsLoadFailed = false;
        if (this.discoverySongs.isEmpty()) {
            drawDiscoverSongs();
        }
        final JSONObject first = this.picks.get(0);
        this.network.execute(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m92lambda$loadDiscoverySongs$58$localqingyinmobileIndexActivity(first, ticket);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$loadDiscoverySongs$58$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m92lambda$loadDiscoverySongs$58$localqingyinmobileIndexActivity(JSONObject first, final long ticket) {
        Track track;
        try {
            JSONObject p = new JSONObject();
            p.put("id", first.optString("id"));
            p.put("listId", first.optString("listId"));
            p.put("globalId", first.optString("globalId"));
            p.put("type", first.optInt("type", 0));
            p.put("page", 1);
            p.put("pageSize", 30);
            JSONArray array = OnlineApi.call(getApplicationContext(), "playlistSongs", p).optJSONArray("songs");
            final ArrayList<Track> loaded = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < Math.min(30, array.length()); i++) {
                    JSONObject entry = array.optJSONObject(i);
                    if (entry != null && (track = OnlineApi.asTrack(entry)) != null) {
                        loaded.add(track);
                    }
                }
            }
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda35
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m90lambda$loadDiscoverySongs$56$localqingyinmobileIndexActivity(ticket, loaded);
                }
            });
        } catch (Exception e) {
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda36
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m91lambda$loadDiscoverySongs$57$localqingyinmobileIndexActivity(ticket);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$loadDiscoverySongs$56$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m90lambda$loadDiscoverySongs$56$localqingyinmobileIndexActivity(long ticket, ArrayList loaded) {
        if (!this.active || ticket != this.generation || isDestroyed()) {
            return;
        }
        this.discoverySongs.clear();
        this.discoverySongs.addAll(loaded);
        this.songsLoading = false;
        this.songsLoadFailed = false;
        drawDiscoverSongs();
    }

    /* JADX INFO: renamed from: lambda$loadDiscoverySongs$57$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m91lambda$loadDiscoverySongs$57$localqingyinmobileIndexActivity(long ticket) {
        if (this.active && ticket == this.generation && !isDestroyed()) {
            this.songsLoading = false;
            this.songsLoadFailed = true;
            drawDiscoverSongs();
        }
    }

    private void drawPicks() {
        if (homeKugouLists == null) return;
        if (picks.isEmpty()) {
            homeKugouLists.state(recommendationsLoading ? "正在加载歌单…" : recommendationsFailed ? "歌单加载失败" : "暂无歌单",
                recommendationsLoading ? "请稍候" : "点击重试", recommendationsLoading ? null : this::refreshHome);
        } else {
            homeKugouLists.playlists(picks, 0, Math.min(8, picks.size()));
        }
        updateAmbient();
        if (AppearancePreferences.dark(this)) AppearanceStyler.apply(tabScreens[selectedTab], true);
        m106lambda$new$0$localqingyinmobileIndexActivity();
    }
    /* JADX INFO: renamed from: lambda$drawPicks$59$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m83lambda$drawPicks$59$localqingyinmobileIndexActivity() {
        go("recommendations");
    }

    /* JADX INFO: renamed from: lambda$drawPicks$60$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m84lambda$drawPicks$60$localqingyinmobileIndexActivity() {
        go("recommendations");
    }

    private void hydratePlaylistCovers(final long ticket) {
        final ArrayList<JSONObject> missing = new ArrayList<>();
        for (JSONObject playlist : this.personalLists) {
            if (PlaylistCoverResolver.needsResolution(this, playlist)) {
                missing.add(playlist);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        missing.sort(new Comparator() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return IndexActivity.this.m87xde356981((JSONObject) obj, (JSONObject) obj2);
            }
        });
        this.network.execute(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m89xc6b3da3f(missing, ticket);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$hydratePlaylistCovers$61$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ int m87xde356981(JSONObject a, JSONObject b) {
        return Integer.compare(importantCollection(b.optString("title", "")), importantCollection(a.optString("title", "")));
    }

    /* JADX INFO: renamed from: lambda$hydratePlaylistCovers$63$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m89xc6b3da3f(ArrayList missing, final long ticket) {
        int attempts = 0;
        Iterator it = missing.iterator();
        while (it.hasNext()) {
            final JSONObject playlist = (JSONObject) it.next();
            int attempts2 = attempts + 1;
            if (attempts >= 12) {
                return;
            }
            final String art = PlaylistCoverResolver.resolve(getApplicationContext(), playlist);
            if (!art.isEmpty()) {
                this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda17
                    @Override // java.lang.Runnable
                    public final void run() {
                        IndexActivity.this.m88x5274a1e0(ticket, playlist, art);
                    }
                });
            }
            attempts = attempts2;
        }
    }

    /* JADX INFO: renamed from: lambda$hydratePlaylistCovers$62$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m88x5274a1e0(long ticket, JSONObject playlist, String art) {
        if (!this.active || isDestroyed() || ticket != this.generation || !this.personalLists.contains(playlist)) {
            return;
        }
        try {
            playlist.put("cover", art);
        } catch (Exception e) {
        }
        drawMine();
    }

    private int importantCollection(String title) {
        return (title.contains("默认收藏") || title.contains("我喜欢")) ? 1 : 0;
    }

    private boolean previewAccountVerified;

    private void loadHome() {
        final long ticket = this.generation + 1;
        this.generation = ticket;

        this.libraryLoadFailed = false;
        this.network.execute(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda48
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m97lambda$loadHome$68$localqingyinmobileIndexActivity(ticket);
            }
        });
        if (this.lastRecommendationsAt > 0 && SystemClock.elapsedRealtime() - this.lastRecommendationsAt < 90000) {
            if (this.songsLoading && this.discoverySongs.isEmpty() && !this.picks.isEmpty()) {
                loadDiscoverySongs(ticket);
                return;
            }
            return;
        }
        this.recommendationsLoading = true;
        this.recommendationsFailed = false;
        if (this.picks.isEmpty()) {
            drawPicks();
        }
        this.network.execute(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda49
            @Override // java.lang.Runnable
            public final void run() {
                IndexActivity.this.m100lambda$loadHome$71$localqingyinmobileIndexActivity(ticket);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$loadHome$68$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m97lambda$loadHome$68$localqingyinmobileIndexActivity(final long ticket) {
        try {
            final JSONObject result=OnlineApi.call(getApplicationContext(),"status",new JSONObject());
            ui.post(()->{
                if(!active||isDestroyed()||ticket!=generation)return;
                boolean logged=result.optBoolean("loggedIn",false);
                JSONObject user=result.optJSONObject("user");
                String owner=logged&&user!=null?user.optString("id",""):"";
                boolean changed=RecommendationArtwork.setAccount(this,owner);
                previewAccountVerified=true;
                if(changed){previewAttempt[0]=0;previewAttempt[1]=0;}
                m93lambda$loadHome$64$localqingyinmobileIndexActivity(ticket,logged);
                renderRecommendationCovers();
                if(!owner.isEmpty()){loadRecommendationCover(true,ticket);loadRecommendationCover(false,ticket);}
            });
        } catch(Exception failure) {
            ui.post(()->m94lambda$loadHome$65$localqingyinmobileIndexActivity(ticket));
        }
        try {
            final JSONArray data = OnlineApi.call(getApplicationContext(), "playlists", new JSONObject()).optJSONArray("playlists");
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda59
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m95lambda$loadHome$66$localqingyinmobileIndexActivity(ticket, data);
                }
            });
        } catch (Exception e2) {
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda60
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m96lambda$loadHome$67$localqingyinmobileIndexActivity(ticket);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$loadHome$64$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m93lambda$loadHome$64$localqingyinmobileIndexActivity(long ticket, boolean logged) {
        if (this.active && !isDestroyed() && ticket == this.generation) {
            this.accountLoggedIn = Boolean.valueOf(logged);
            if (this.account != null) {
                this.account.setText("青");
            }
            drawMine();
        }
    }

    /* JADX INFO: renamed from: lambda$loadHome$65$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m94lambda$loadHome$65$localqingyinmobileIndexActivity(long ticket) {
        if (this.active && !isDestroyed() && ticket == this.generation) {
            this.accountLoggedIn = null;
            if (this.account != null) {
                this.account.setText("青");
            }
            drawMine();
        }
    }

    /* JADX INFO: renamed from: lambda$loadHome$66$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m95lambda$loadHome$66$localqingyinmobileIndexActivity(long ticket, JSONArray data) {
        if (!this.active || isDestroyed() || ticket != this.generation) {
            return;
        }
        this.personalLists.clear();
        this.libraryLoadFailed = false;
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject list = data.optJSONObject(i);
                if (list != null) {
                    this.personalLists.add(list);
                }
            }
        }
        drawMine();
        hydratePlaylistCovers(ticket);
    }

    /* JADX INFO: renamed from: lambda$loadHome$67$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m96lambda$loadHome$67$localqingyinmobileIndexActivity(long ticket) {
        if (!this.active || isDestroyed() || ticket != this.generation) {
            return;
        }
        this.libraryLoadFailed = this.personalLists.isEmpty();
        drawMine();
    }

    /* JADX INFO: renamed from: lambda$loadHome$71$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m100lambda$loadHome$71$localqingyinmobileIndexActivity(final long ticket) {
        try {
            JSONObject filter = new JSONObject().put("page", 1).put("pagesize", 30);
            final JSONArray data = OnlineApi.call(getApplicationContext(), "recommendations", filter).optJSONArray("playlists");
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda51
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m98lambda$loadHome$69$localqingyinmobileIndexActivity(ticket, data);
                }
            });
        } catch (Exception e) {
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.IndexActivity$$ExternalSyntheticLambda52
                @Override // java.lang.Runnable
                public final void run() {
                    IndexActivity.this.m99lambda$loadHome$70$localqingyinmobileIndexActivity(ticket);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$loadHome$69$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m98lambda$loadHome$69$localqingyinmobileIndexActivity(long ticket, JSONArray data) {
        if (!this.active || isDestroyed() || ticket != this.generation) {
            return;
        }
        this.picks.clear();
        HashSet<String> unique = new HashSet<>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item != null) {
                    String id = item.optString("id", "");
                    if (!id.isEmpty() && unique.add(id)) {
                        this.picks.add(item);
                    }
                }
            }
        }
        this.recommendationsLoading = false;
        this.recommendationsFailed = false;
        this.lastRecommendationsAt = SystemClock.elapsedRealtime();
        drawPicks();
        loadDiscoverySongs(ticket);
    }

    /* JADX INFO: renamed from: lambda$loadHome$70$local-qingyin-mobile-IndexActivity, reason: not valid java name */
    /* synthetic */ void m99lambda$loadHome$70$localqingyinmobileIndexActivity(long ticket) {
        if (!this.active || isDestroyed() || ticket != this.generation) {
            return;
        }
        this.recommendationsLoading = false;
        this.recommendationsFailed = true;
        if (this.picks.isEmpty()) {
            this.songsLoading = false;
            this.songsLoadFailed = true;
        }
        drawPicks();
    }

    private void applyAppearance() {
        if (this.tabScreens == null || this.ambient == null) {
            return;
        }
        String current = AppearancePreferences.get(this);
        boolean dark = "C".equals(current);
        int surface = AmbientBackdrop.baseColor(this);
        if (this.windowRoot != null && !current.equals(this.appliedAppearance)) {
            this.windowRoot.setBackgroundColor(surface);
        }
        HomeWindowInsets.styleStatusBar(getWindow(), dark);
        getWindow().setNavigationBarColor(surface);
        styleLibraryGroup();
        AppearanceStyler.apply(this.tabScreens[this.selectedTab], dark);
        if (this.selectedTab == 1 && discoveryFeed != null) discoveryFeed.applyTheme();
        this.appliedAppearance = current;
        this.bottomChrome.post(this::m106lambda$new$0$localqingyinmobileIndexActivity);
    }

    private void updateAmbient() {
        if (this.ambient == null) {
            return;
        }
        String source = "";
        Track playing = (!this.bound || this.player == null) ? null : this.player.current();
        if (playing != null && !playing.coverUrl.isEmpty()) {
            source = playing.coverUrl;
        }
        if (source.isEmpty()) {
            if (this.selectedTab != 0) {
                if (this.selectedTab == 1) {
                    if (!this.picks.isEmpty()) {
                        source = this.picks.get(0).optString("cover", "");
                    }
                    if (source.isEmpty()) {
                        List<String> guess = previewUrls("guessYouLike");
                        if (!guess.isEmpty()) {
                            source = guess.get(0);
                        }
                    }
                } else if (this.selectedTab == 3) {
                    for (JSONObject list : this.personalLists) {
                        source = PlaylistCoverResolver.preferred(this, list);
                        if (!source.isEmpty()) {
                            break;
                        }
                    }
                }
            } else {
                List<String> daily = previewUrls("dailyRecommendations");
                if (!daily.isEmpty()) {
                    source = daily.get(0);
                }
            }
        }
        this.ambient.show(this.selectedTab, source);
    }

    private void ensureForeground() {
        Intent intent = new Intent(this, (Class<?>) PlaybackService.class);
        startForegroundService(intent);
    }

    private void openPlayer() {
        if (this.bound && this.player != null && this.player.current() != null) {
            startActivity(new Intent(this, (Class<?>) NowPlayingActivity.class));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMini() {
        boolean failed;
        String str;
        if (this.miniContainer == null) {
            return;
        }
        Track track = (!this.bound || this.player == null) ? null : this.player.current();
        int desired = track == null ? 8 : 0;
        if (this.miniContainer.getVisibility() != desired) {
            this.miniContainer.setVisibility(desired);
            m106lambda$new$0$localqingyinmobileIndexActivity();
        }
        if (track == null) {
            updateAmbient();
            this.lastMiniCoverUri = "";
            if (this.chromeCollapsed) {
                resetChromeScroll();
                setChromeCollapsed(false, true);
                return;
            }
            return;
        }
        if (!track.uri.equals(this.lastMiniCoverUri)) {
            this.lastMiniCoverUri = track.uri;
            Artwork.bind(this.miniCoverArt, track.coverUrl);
            updateAmbient();
        }
        boolean playing = this.player.isPlaying();
        boolean preparing = this.player.isPreparing();
        String error = this.player.lastError();
        if (error == null || error.isEmpty() || playing || preparing) {
            failed = false;
        } else {
            failed = true;
        }
        this.playingName.setText(track.title);
        this.playingArtist.setText(failed ? "播放失败 · 点播放重试" : track.artist);
        ImageView imageView = this.playAction;
        StringBuilder sb = new StringBuilder();
        if (failed) {
            str = "重试播放";
        } else {
            str = (playing || preparing) ? "暂停" : "播放";
        }
        imageView.setContentDescription(sb.append(str).append(track.title).toString());
        this.miniPlayer.setContentDescription(track.title + "，" + (failed ? "播放失败，点击播放按钮重试" : track.artist));
        this.playAction.setImageResource((playing || preparing) ? 17301539 : 17301540);
        int duration = Math.max(1, this.player.duration());
        this.miniBar.setMax(duration);
        this.miniBar.setProgress(Math.min(duration, Math.max(0, this.player.currentPosition())));
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.selectedTab != 0) {
            selectTab(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("current_tab", this.selectedTab);
        super.onSaveInstanceState(state);
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        previewAccountVerified=false;
        this.active = true;
        drawMine();
        drawPicks();
        drawDiscoverSongs();
        drawRecent();
        renderRecommendationCovers();
        loadHome();
        updateAmbient();
        if (selectedTab == 1) loadDiscovery(false);
        applyAppearance();
        attachBlurScroll();
        m106lambda$new$0$localqingyinmobileIndexActivity();
        this.ui.removeCallbacks(this.ticker);
        this.ui.post(this.ticker);
    }

    @Override // android.app.Activity
    protected void onStop() {
        cancelGuessRequest();
        this.active = false;
        this.generation++;
        this.ui.removeCallbacks(this.ticker);
        resetChromeScroll();
        if (this.bottomChrome != null) {
            this.bottomChrome.animate().cancel();
            this.bottomChrome.setAlpha(1.0f);
            this.bottomChrome.setTranslationY(0.0f);
        }
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (this.requested) {
            unbindService(this.connection);
        }
        this.ui.removeCallbacks(this.ticker);
        if (this.blurObserver != null && this.blurObserver.isAlive()) {
            this.blurObserver.removeOnScrollChangedListener(this.blurScroll);
        }
        for (FrostedGlassDrawable drawable : this.frostedLayers) {
            drawable.release();
        }
        this.frostedLayers.clear();
        if (this.ambient != null) {
            this.ambient.release();
        }
        this.network.shutdownNow();
        if (discoveryRepository != null) discoveryRepository.release();
        if (discoveryFeed != null) discoveryFeed.release();
        super.onDestroy();
    }
    private List<String> previewUrls(String method) {
        return previewAccountVerified ? RecommendationArtwork.load(this,method) : java.util.Collections.emptyList();
    }
}
