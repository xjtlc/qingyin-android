package local.qingyin.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class OnlineActivity extends Activity {
    private static final String KUGOU_OFFICIAL_LOGIN = "https://m.kugou.com/loginReg.php?act=login";
    private static final LinkedHashMap<String, CachedList> LIST_CACHE = new LinkedHashMap<String, CachedList>(12, 0.75f, true) { // from class: local.qingyin.mobile.OnlineActivity.2
        @Override // java.util.LinkedHashMap
        protected boolean removeEldestEntry(Map.Entry<String, CachedList> entry) {
            return size() > 8;
        }
    };
    private Button account;
    private boolean active;
    private JSONObject activePlaylist;
    private BrowserAdapter adapter;
    private boolean ambientSearchPage;
    private boolean bindingRequested;
    private View blurSource;
    private boolean bound;
    private LinearLayout browseToolbar;
    private TextView emptyState;
    private boolean headerAttached;
    private ListView listing;
    private FrameLayout miniArt;
    private LinearLayout miniPanel;
    private ProgressBar miniProgress;
    private TextView miniSubtitle;
    private TextView miniTitle;
    private Button miniToggle;
    private boolean morePlaylist;
    private boolean moreSearch;
    private Button nextButton;
    private PlaybackService player;
    private FrameLayout playlistArt;
    private LinearLayout playlistHeader;
    private boolean playlistLoading;
    private TextView playlistMessage;
    private TextView playlistMeta;
    private Button playlistPlay;
    private boolean playlistRequestInFlight;
    private ActionIcon playlistSave;
    private ActionIcon playlistShuffle;
    private TextView playlistTitle;
    private View playlistToolbar;
    private AlertDialog qrDialog;
    private String qrKey;
    private Runnable qrPoll;
    private EditText query;
    private String recommendationMethod;
    private String recommendationTitle;
    private String discoveryTitle;
    private String discoveryRankId = "";
    private String discoveryPlaylistId = "";
    private JSONArray discoverySnapshot;
    private long discoverySerial = -1;
    private boolean discoveryLoading;
    private boolean discoveryReloadOnStart;
    private DiscoveryRepository discoveryRepository;
    private Future<?> discoveryRequest;
    private FrameLayout resultsFrame;
    private AmbientBackdrop searchAmbient;
    private LinearLayout searchHistoryContainer;
    private EditText searchInput;
    private LinearLayout searchPanel;
    private TextView sectionTitle;
    private boolean showingPlaylists;
    private TextView status;
    private final ExecutorService network = Executors.newFixedThreadPool(2);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<Track> songs = new ArrayList<>();
    private final ArrayList<JSONObject> lists = new ArrayList<>();
    private boolean localArtistView = false;
    private boolean searchEnteredDirectly = false;
    private boolean mineListSelected = false;
    private String miniArtUri = "";
    private String highlightedUri = "";
    private final ArrayList<Track> sourceOrder = new ArrayList<>();
    private int sortMode = 0;
    private int savedView = 0;
    private long viewSerial = 0;
    private String playlistArtUrl = "";
    private String shownPlaybackError = "";
    private final ArrayList<FrostedGlassDrawable> frostedLayers = new ArrayList<>();
    private final ViewTreeObserver.OnGlobalLayoutListener blurLayout = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda48
        @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
        public final void onGlobalLayout() {
            OnlineActivity.this.m292lambda$new$0$localqingyinmobileOnlineActivity();
        }
    };
    private final Runnable playbackTicker = new Runnable() { // from class: local.qingyin.mobile.OnlineActivity.1
        @Override // java.lang.Runnable
        public void run() {
            if (OnlineActivity.this.active) {
                OnlineActivity.this.updateMini();
                OnlineActivity.this.ui.postDelayed(this, 1000L);
            }
        }
    };
    private int searchPage = 1;
    private int playlistPage = 1;
    private final PlaybackService.OnChange playbackChange = new PlaybackService.OnChange() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda49
        @Override // local.qingyin.mobile.PlaybackService.OnChange
        public final void changed() {
            OnlineActivity.this.m294lambda$new$2$localqingyinmobileOnlineActivity();
        }
    };
    private final ServiceConnection service = new ServiceConnection() { // from class: local.qingyin.mobile.OnlineActivity.3
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder local2) {
            OnlineActivity.this.player = ((PlaybackService.LocalBinder) local2).service();
            OnlineActivity.this.bound = true;
            if (OnlineActivity.this.active) {
                OnlineActivity.this.player.setListener(OnlineActivity.this.playbackChange);
            }
            OnlineActivity.this.updateMini();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            OnlineActivity.this.bound = false;
            OnlineActivity.this.player = null;
        }
    };
    private final Set<String> seenGuessHashes = new HashSet();
    private boolean recommendationBusy = false;

    private static final class CachedList {
        final boolean hasMore;
        final int page;
        final long savedAt = SystemClock.elapsedRealtime();
        final ArrayList<Track> tracks;

        CachedList(ArrayList<Track> tracks, int page, boolean more) {
            this.tracks = new ArrayList<>(tracks);
            this.page = page;
            this.hasMore = more;
        }
    }

    private static String listKey(JSONObject list) {
        String id = list.optString("globalId");
        if (id.isEmpty()) {
            id = list.optString("listId");
        }
        if (id.isEmpty()) {
            id = list.optString("id");
        }
        return list.optInt("type", 0) + ":" + id;
    }

    /* JADX INFO: renamed from: lambda$new$2$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m294lambda$new$2$localqingyinmobileOnlineActivity() {
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda22
            @Override // java.lang.Runnable
            public final void run() {
                OnlineActivity.this.m293lambda$new$1$localqingyinmobileOnlineActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$new$1$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m293lambda$new$1$localqingyinmobileOnlineActivity() {
        if (!this.active || this.player == null || isDestroyed()) {
            return;
        }
        String error = this.player.lastError();
        if (error != null && !error.isEmpty() && !this.player.isPreparing() && !this.player.isPlaying() && !error.equals(this.shownPlaybackError)) {
            this.shownPlaybackError = error;
            say("播放失败：" + error);
        } else if (error == null || error.isEmpty()) {
            this.shownPlaybackError = "";
        }
        updateMini();
    }

    @Override // android.app.Activity
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(-526343);
        getWindow().setNavigationBarColor(-526343);
        getWindow().getDecorView().setSystemUiVisibility(8208);
        this.ambientSearchPage = "search".equals(getIntent().getStringExtra("page"));
        LinearLayout linearLayoutVertical = vertical();
        this.blurSource = linearLayoutVertical;
        linearLayoutVertical.setPadding(dp(15.0f), 0, dp(15.0f), 0);
        linearLayoutVertical.setBackgroundColor(this.ambientSearchPage ? 0 : -526343);
        FrameLayout frameLayout = new FrameLayout(this);
        this.searchAmbient = new AmbientBackdrop(this);
        this.searchAmbient.setVisibility(this.ambientSearchPage ? 0 : 8);
        frameLayout.addView(this.searchAmbient, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.addView(linearLayoutVertical, new FrameLayout.LayoutParams(-1, -1));
        setContentView(frameLayout);
        if (this.ambientSearchPage) {
            List<String> covers = RecommendationArtwork.load(this, "guessYouLike");
            this.searchAmbient.show(4, covers.isEmpty() ? "" : covers.get(0));
            getWindow().setStatusBarColor(AmbientBackdrop.baseColor(this));
            getWindow().setNavigationBarColor(AmbientBackdrop.baseColor(this));
        }
        LinearLayout linearLayoutHorizontal = horizontal();
        this.browseToolbar = linearLayoutHorizontal;
        linearLayoutHorizontal.setGravity(16);
        ActionIcon back = new ActionIcon(0);
        back.setContentDescription("返回");
        if (this.ambientSearchPage && AppearancePreferences.dark(this)) {
            back.setInk(-592902);
        }
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m300lambda$onCreate$3$localqingyinmobileOnlineActivity(view);
            }
        });
        linearLayoutHorizontal.addView(back, new LinearLayout.LayoutParams(dp(43.0f), dp(48.0f)));
        LinearLayout heading = vertical();
        heading.setGravity(16);
        heading.setPadding(dp(4.0f), 0, 0, 0);
        this.sectionTitle = t("精选歌单", 29, -15000801, true);
        this.sectionTitle.setIncludeFontPadding(false);
        this.sectionTitle.setSingleLine(true);
        this.sectionTitle.setEllipsize(TextUtils.TruncateAt.END);
        heading.addView(this.sectionTitle, new LinearLayout.LayoutParams(-1, -2));
        this.status = t("正在加载…", 13, -8815739, false);
        this.status.setIncludeFontPadding(false);
        this.status.setSingleLine(true);
        this.status.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(7.0f);
        heading.addView(this.status, statusLp);
        linearLayoutHorizontal.addView(heading, new LinearLayout.LayoutParams(0, -1, 1.0f));
        ActionIcon searchIcon = new ActionIcon(1);
        searchIcon.setInk(-1815446);
        searchIcon.setContentDescription("搜索歌曲或歌手");
        searchIcon.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m301lambda$onCreate$4$localqingyinmobileOnlineActivity(view);
            }
        });
        linearLayoutHorizontal.addView(searchIcon, new LinearLayout.LayoutParams(dp(43.0f), dp(49.0f)));
        final ActionIcon menuIcon = new ActionIcon(2);
        if (this.ambientSearchPage && AppearancePreferences.dark(this)) {
            menuIcon.setInk(-592902);
        }
        menuIcon.setContentDescription("更多操作");
        menuIcon.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m302lambda$onCreate$5$localqingyinmobileOnlineActivity(menuIcon, view);
            }
        });
        linearLayoutHorizontal.addView(menuIcon, new LinearLayout.LayoutParams(dp(43.0f), dp(49.0f)));
        ViewGroup.LayoutParams browseToolbarLp = new LinearLayout.LayoutParams(-1, dp(96.0f));
        linearLayoutVertical.addView(linearLayoutHorizontal, browseToolbarLp);
        this.query = new EditText(this);
        this.query.setSingleLine(true);
        this.account = button("登录账号", false);
        this.resultsFrame = new FrameLayout(this);
        this.listing = new ListView(this);
        this.listing.setDivider(null);
        this.listing.setDividerHeight(0);
        this.listing.setVerticalScrollBarEnabled(false);
        this.listing.setBackgroundColor(this.ambientSearchPage ? 0 : -1);
        this.listing.setClipToOutline(false);
        this.listing.setClipToPadding(false);
        this.listing.setPadding(0, 0, 0, dp(18.0f));
        this.playlistHeader = createPlaylistHeader();
        this.nextButton = button("加载更多歌曲  ↓", false);
        this.nextButton.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m304lambda$onCreate$7$localqingyinmobileOnlineActivity(view);
            }
        });
        LinearLayout footer = vertical();
        footer.setPadding(dp(8.0f), dp(8.0f), dp(8.0f), dp(20.0f));
        footer.addView(this.nextButton, new LinearLayout.LayoutParams(-1, dp(44.0f)));
        this.listing.addFooterView(footer, null, false);
        this.nextButton.setVisibility(8);
        footer.setVisibility(8);
        this.adapter = new BrowserAdapter();
        this.listing.setAdapter((ListAdapter) this.adapter);
        this.resultsFrame.addView(this.listing, new FrameLayout.LayoutParams(-1, -1));
        this.emptyState = t("正在加载音乐…", 14, -8815739, false);
        this.emptyState.setGravity(17);
        this.emptyState.setPadding(dp(26.0f), 0, dp(26.0f), dp(48.0f));
        this.resultsFrame.addView(this.emptyState, new FrameLayout.LayoutParams(-1, -1));
        this.listing.setEmptyView(this.emptyState);
        linearLayoutVertical.addView(this.resultsFrame, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.listing.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda10
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                OnlineActivity.this.m305lambda$onCreate$8$localqingyinmobileOnlineActivity(adapterView, view, i, j);
            }
        });
        this.listing.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda12
            @Override // android.widget.AdapterView.OnItemLongClickListener
            public final boolean onItemLongClick(AdapterView adapterView, View view, int i, long j) {
                return OnlineActivity.this.m306lambda$onCreate$9$localqingyinmobileOnlineActivity(adapterView, view, i, j);
            }
        });
        this.listing.setOnScrollListener(new AbsListView.OnScrollListener() { // from class: local.qingyin.mobile.OnlineActivity.4
            @Override // android.widget.AbsListView.OnScrollListener
            public void onScrollStateChanged(AbsListView view, int state) {
                OnlineActivity.this.m292lambda$new$0$localqingyinmobileOnlineActivity();
            }

            @Override // android.widget.AbsListView.OnScrollListener
            public void onScroll(AbsListView view, int first, int count, int total) {
                OnlineActivity.this.m292lambda$new$0$localqingyinmobileOnlineActivity();
            }
        });
        this.playlistToolbar = createPlaylistToolbar(linearLayoutVertical);
        FrameLayout.LayoutParams toolbarLp = new FrameLayout.LayoutParams(-1, dp(52.0f), 48);
        toolbarLp.leftMargin = dp(17.0f);
        toolbarLp.rightMargin = dp(17.0f);
        toolbarLp.topMargin = dp(8.0f);
        frameLayout.addView(this.playlistToolbar, toolbarLp);
        this.playlistToolbar.setVisibility(8);
        this.miniPanel = vertical();
        this.miniPanel.setPadding(dp(11.0f), dp(6.0f), dp(9.0f), dp(5.0f));
        this.miniPanel.setBackground(glass(linearLayoutVertical, this.miniPanel, 30, -805306369));
        this.miniPanel.setElevation(dp(6.0f));
        LinearLayout miniRow = horizontal();
        this.miniArt = Artwork.frame(this, 12);
        miniRow.addView(this.miniArt, new LinearLayout.LayoutParams(dp(42.0f), dp(42.0f)));
        LinearLayout miniMeta = vertical();
        miniMeta.setPadding(dp(10.0f), 0, dp(5.0f), 0);
        this.miniTitle = t("还没有播放", 14, -15000801, true);
        this.miniSubtitle = t("青音 · 在线播放", 11, -8815739, false);
        this.miniTitle.setSingleLine(true);
        this.miniSubtitle.setSingleLine(true);
        this.miniArt.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m295lambda$onCreate$10$localqingyinmobileOnlineActivity(view);
            }
        });
        this.miniTitle.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m296lambda$onCreate$11$localqingyinmobileOnlineActivity(view);
            }
        });
        this.miniSubtitle.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m297lambda$onCreate$12$localqingyinmobileOnlineActivity(view);
            }
        });
        this.miniTitle.setEllipsize(TextUtils.TruncateAt.END);
        this.miniSubtitle.setEllipsize(TextUtils.TruncateAt.END);
        miniMeta.addView(this.miniTitle);
        miniMeta.addView(this.miniSubtitle);
        miniRow.addView(miniMeta, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.miniToggle = button("▶", true);
        this.miniToggle.setTextSize(18.0f);
        this.miniToggle.setTextColor(-15000801);
        this.miniToggle.setBackground(QingyinUi.touch(this, 0, 23.0f));
        this.miniToggle.setContentDescription("播放或暂停");
        this.miniToggle.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m298lambda$onCreate$13$localqingyinmobileOnlineActivity(view);
            }
        });
        miniRow.addView(this.miniToggle, new LinearLayout.LayoutParams(dp(42.0f), dp(42.0f)));
        Button following = button("⏭", false);
        following.setTextSize(21.0f);
        following.setTextColor(-15000801);
        following.setBackground(QingyinUi.touch(this, 0, 23.0f));
        following.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m299lambda$onCreate$14$localqingyinmobileOnlineActivity(view);
            }
        });
        following.setContentDescription("下一首");
        miniRow.addView(following, new LinearLayout.LayoutParams(dp(39.0f), dp(42.0f)));
        this.miniPanel.addView(miniRow);
        this.miniProgress = new ProgressBar(this, null, 16842872);
        this.miniProgress.setProgressTintList(ColorStateList.valueOf(-1815446));
        this.miniProgress.setProgressBackgroundTintList(ColorStateList.valueOf(-268050));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(3.0f));
        barLp.topMargin = dp(8.0f);
        this.miniPanel.addView(this.miniProgress, barLp);
        FrameLayout.LayoutParams miniLp = new FrameLayout.LayoutParams(-1, dp(66.0f), 80);
        miniLp.leftMargin = dp(19.0f);
        miniLp.rightMargin = dp(19.0f);
        miniLp.bottomMargin = dp(13.0f);
        frameLayout.addView(this.miniPanel, miniLp);
        this.miniPanel.setVisibility(8);
        this.searchPanel = createSearchPanel();
        frameLayout.addView(this.searchPanel, new FrameLayout.LayoutParams(-1, -1));
        this.searchPanel.setVisibility(8);
        linearLayoutVertical.getViewTreeObserver().addOnGlobalLayoutListener(this.blurLayout);
        MusicOverlayDock.attach(this);
        this.bindingRequested = bindService(new Intent(this, (Class<?>) PlaybackService.class), this.service, 1);
        this.sortMode = getSharedPreferences("online_ui", 0).getInt("sort", 0);
        routeFromIndex();
        refreshStatus();
    }

    /* JADX INFO: renamed from: lambda$onCreate$3$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m300lambda$onCreate$3$localqingyinmobileOnlineActivity(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$onCreate$4$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m301lambda$onCreate$4$localqingyinmobileOnlineActivity(View v) {
        showSearchDialog();
    }

    /* JADX INFO: renamed from: lambda$onCreate$5$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m302lambda$onCreate$5$localqingyinmobileOnlineActivity(ActionIcon menuIcon, View v) {
        showPageMenu(menuIcon);
    }

    /* JADX INFO: renamed from: lambda$onCreate$7$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m304lambda$onCreate$7$localqingyinmobileOnlineActivity(View v) {
        if (this.activePlaylist != null) {
            loadPlaylist(this.activePlaylist, this.playlistPage + 1, true);
        } else {
            search(true);
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$8$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m305lambda$onCreate$8$localqingyinmobileOnlineActivity(AdapterView parent, View v, int pos, long id) {
        int row = pos - this.listing.getHeaderViewsCount();
        if (row < 0) {
            return;
        }
        if (this.showingPlaylists && row < this.lists.size()) {
            JSONObject item = this.lists.get(row);
            if (item.has("__artist")) {
                showArtistSongs(item.optString("__artist"));
                return;
            } else {
                openList(item);
                return;
            }
        }
        if (this.showingPlaylists || row >= this.songs.size()) {
            return;
        }
        play(row);
    }

    /* JADX INFO: renamed from: lambda$onCreate$9$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ boolean m306lambda$onCreate$9$localqingyinmobileOnlineActivity(AdapterView parent, View v, int pos, long id) {
        int row = pos - this.listing.getHeaderViewsCount();
        if (this.showingPlaylists || row < 0 || row >= this.songs.size()) {
            return false;
        }
        showSongActions(row);
        return true;
    }

    /* JADX INFO: renamed from: lambda$onCreate$10$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m295lambda$onCreate$10$localqingyinmobileOnlineActivity(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$onCreate$11$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m296lambda$onCreate$11$localqingyinmobileOnlineActivity(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$onCreate$12$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m297lambda$onCreate$12$localqingyinmobileOnlineActivity(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$onCreate$13$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m298lambda$onCreate$13$localqingyinmobileOnlineActivity(View v) {
        if (this.bound) {
            ensureForeground();
            this.player.toggle();
            updateMini();
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$14$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m299lambda$onCreate$14$localqingyinmobileOnlineActivity(View v) {
        if (this.bound) {
            ensureForeground();
            this.player.skip(1);
        }
    }

    private void routeFromIndex() {
        Intent source = getIntent();
        String page = source.getStringExtra("page");
        if ("discovery".equals(page)) {
            openDiscoverySection(source);
            return;
        }
        if ("daily".equals(page)) {
            showAccountRecommendations("dailyRecommendations", "每日推荐");
            return;
        }
        if ("guess".equals(page)) {
            // Old saved intents must not restore the removed recommendation list page.
            startActivity(new Intent(this, IndexActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        if ("mine".equals(page)) {
            showLists(true);
            return;
        }
        if ("artists".equals(page)) {
            showArtistIndex();
            return;
        }
        if ("favorites".equals(page)) {
            showSaved(true);
            return;
        }
        if ("recents".equals(page)) {
            showSaved(false);
            return;
        }
        if ("playlist".equals(page)) {
            try {
                JSONObject list = new JSONObject(source.getStringExtra("playlist_json"));
                if (list.optString("id", "").isEmpty() && list.optString("globalId", "").isEmpty() && list.optString("listId", "").isEmpty()) {
                }
                openList(list);
                return;
            } catch (Exception e) {
            }
        }
        if ("search".equals(page)) {
            String keyword = source.getStringExtra("keyword");
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.query.setText(keyword.trim());
                search(false);
                return;
            }
            this.searchEnteredDirectly = true;
            this.sectionTitle.setText("搜索音乐");
            setBrowserStatus("输入歌曲或歌手，开始搜索");
            this.showingPlaylists = false;
            this.songs.clear();
            this.lists.clear();
            this.sourceOrder.clear();
            this.adapter.notifyDataSetChanged();
            this.emptyState.setText("寻找你想听的旋律");
            updateMore();
            showSearchDialog();
            return;
        }
        if ("account".equals(page)) {
            this.viewSerial++;
            this.activePlaylist = null;
            this.savedView = 0;
            this.songs.clear();
            this.lists.clear();
            this.sourceOrder.clear();
            this.showingPlaylists = true;
            this.adapter.notifyDataSetChanged();
            this.sectionTitle.setText("账号管理");
            setBrowserStatus("管理登录与云端音乐");
            this.emptyState.setText("登录后可同步个人歌单\n右上角更多可重新打开账号管理");
            updateMore();
            accountAction();
            return;
        }
        showLists(false);
    }

    private boolean isDiscoveryView() {
        return this.discoveryTitle != null && this.discoverySerial == this.viewSerial;
    }

    private void openDiscoverySection(Intent source) {
        String title = source.getStringExtra("title");
        this.discoveryTitle = title == null || title.trim().isEmpty() ? "发现音乐" : title.trim();
        String rankId = source.getStringExtra("rank_id");
        this.discoveryRankId = rankId != null && rankId.matches("[0-9]{1,20}") ? rankId : "";
        String playlistId = source.getStringExtra("playlist_special_id");
        this.discoveryPlaylistId = playlistId != null && playlistId.matches("[1-9][0-9]{0,14}") ? playlistId : "";
        this.discoverySnapshot = null;
        String snapshot = source.getStringExtra("songs_json");
        if (snapshot != null && !snapshot.trim().isEmpty()) {
            try { this.discoverySnapshot = new JSONArray(snapshot); }
            catch (Exception ignored) { /* The empty state below offers another attempt. */ }
        }
        this.savedView = 0;
        this.activePlaylist = null;
        this.localArtistView = false;
        this.recommendationMethod = null;
        this.recommendationTitle = null;
        this.showingPlaylists = false;
        this.moreSearch = false;
        this.morePlaylist = false;
        this.sortMode = 0;
        this.songs.clear();
        this.sourceOrder.clear();
        this.lists.clear();
        this.discoverySerial = ++this.viewSerial;
        this.sectionTitle.setText(this.discoveryTitle);
        this.adapter.notifyDataSetChanged();
        updateMore();
        loadDiscoverySection();
    }

    private void loadDiscoverySection() {
        if (!isDiscoveryView() || this.discoveryLoading || isFinishing() || isDestroyed()) return;
        this.discoveryReloadOnStart = false;
        final long ticket = ++this.viewSerial;
        this.discoverySerial = ticket;
        this.emptyState.setOnClickListener(null);
        this.emptyState.setClickable(false);
        this.sectionTitle.setText(this.discoveryTitle);
        if (this.discoverySnapshot != null && this.discoverySnapshot.length() > 0) {
            showSongs(this.discoverySnapshot, this.discoveryTitle, false);
            finishDiscoverySection();
            return;
        }
        if (this.discoveryRankId.isEmpty() && this.discoveryPlaylistId.isEmpty()) {
            showDiscoveryFailure(false);
            return;
        }
        this.discoveryLoading = true;
        String loadingText = this.discoveryPlaylistId.isEmpty() ? "正在获取排行榜歌曲…" : "正在获取歌单歌曲…";
        this.emptyState.setText(loadingText);
        setBrowserStatus(this.songs.isEmpty() ? loadingText : "正在更新歌曲列表…");
        final String rankId = this.discoveryRankId;
        final String playlistId = this.discoveryPlaylistId;
        final DiscoveryRepository repository = new DiscoveryRepository(getApplicationContext());
        this.discoveryRepository = repository;
        this.discoveryRequest = this.network.submit(() -> {
            try {
                final JSONArray entries = playlistId.isEmpty()
                    ? repository.fetchChartSongs(rankId) : repository.fetchPlaylistSongs(playlistId);
                this.ui.post(() -> {
                    if (!canCompleteDiscovery(ticket)) return;
                    this.discoveryLoading = false;
                    this.discoveryRepository = null;
                    this.discoveryRequest = null;
                    if (entries == null || entries.length() == 0) {
                        showDiscoveryFailure(false);
                        return;
                    }
                    showSongs(entries, this.discoveryTitle, false);
                    finishDiscoverySection();
                });
            } catch (Exception failure) {
                this.ui.post(() -> {
                    if (!canCompleteDiscovery(ticket)) return;
                    this.discoveryLoading = false;
                    this.discoveryRepository = null;
                    this.discoveryRequest = null;
                    showDiscoveryFailure(true);
                });
            } finally {
                repository.release();
            }
        });
    }

    private boolean canCompleteDiscovery(long ticket) {
        return this.active && !isFinishing() && !isDestroyed()
            && ticket == this.viewSerial && ticket == this.discoverySerial;
    }

    private void finishDiscoverySection() {
        this.moreSearch = false;
        this.morePlaylist = false;
        if (this.songs.isEmpty()) {
            showDiscoveryFailure(false);
            return;
        }
        this.emptyState.setOnClickListener(null);
        this.emptyState.setClickable(false);
        setBrowserStatus("酷狗音乐 · " + this.songs.size() + " 首歌曲");
        updateMore();
    }

    private void showDiscoveryFailure(boolean failed) {
        this.moreSearch = false;
        this.morePlaylist = false;
        this.sectionTitle.setText(this.discoveryTitle);
        if (this.songs.isEmpty()) {
            this.emptyState.setText(failed ? "歌曲暂时无法加载\n点击重试，或返回新发现刷新内容" : "暂时没有可播放歌曲\n点击重试，或返回新发现刷新内容");
            this.emptyState.setContentDescription("暂无歌曲，点击重新加载");
            this.emptyState.setOnClickListener(v -> {
                if (isDiscoveryView()) loadDiscoverySection();
            });
            setBrowserStatus(failed ? "加载失败 · 点击下方重试" : "暂无歌曲 · 点击下方重试");
        } else {
            setBrowserStatus("更新失败 · 已保留 " + this.songs.size() + " 首歌曲，可在更多中刷新");
        }
        updateMore();
    }

    private void cancelDiscoveryRequest(boolean reloadOnStart) {
        if (this.discoveryLoading && isDiscoveryView()) {
            this.discoverySerial = ++this.viewSerial;
            this.discoveryReloadOnStart = reloadOnStart;
        }
        this.discoveryLoading = false;
        Future<?> request = this.discoveryRequest;
        this.discoveryRequest = null;
        if (request != null) request.cancel(true);
        DiscoveryRepository repository = this.discoveryRepository;
        this.discoveryRepository = null;
        if (repository != null) repository.release();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(float n) {
        return QingyinUi.dp(this, n);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView t(String value, int size, int color, boolean bold) {
        return QingyinUi.text(this, value, size, color, bold);
    }

    private Button button(String title, boolean accent) {
        return QingyinUi.button(this, title, accent);
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

    private Drawable glass(View source, View host, int radius, int tint) {
        FrostedGlassDrawable drawable = new FrostedGlassDrawable(source, host, dp(radius), tint);
        this.frostedLayers.add(drawable);
        return drawable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: refreshFrosted, reason: merged with bridge method [inline-methods] */
    public void m292lambda$new$0$localqingyinmobileOnlineActivity() {
        for (FrostedGlassDrawable drawable : this.frostedLayers) {
            drawable.refresh();
        }
    }

    private View createPlaylistToolbar(View source) {
        LinearLayout linearLayoutHorizontal = horizontal();
        ActionIcon back = new ActionIcon(0);
        back.setContentDescription("返回");
        back.setBackground(glass(source, back, 25, -822083585));
        back.setElevation(dp(4.0f));
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m281x2ed5577f(view);
            }
        });
        linearLayoutHorizontal.addView(back, new LinearLayout.LayoutParams(dp(49.0f), dp(49.0f)));
        View space = new View(this);
        linearLayoutHorizontal.addView(space, new LinearLayout.LayoutParams(0, 1, 1.0f));
        LinearLayout options = horizontal();
        options.setBackground(glass(source, options, 25, -822083585));
        options.setElevation(dp(4.0f));
        ActionIcon search = new ActionIcon(1);
        search.setContentDescription("搜索歌曲或歌手");
        search.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda26
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m282x427d2b00(view);
            }
        });
        options.addView(search, new LinearLayout.LayoutParams(dp(49.0f), dp(49.0f)));
        final ActionIcon more = new ActionIcon(2);
        more.setContentDescription("歌单更多操作");
        more.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda27
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m283x5624fe81(more, view);
            }
        });
        options.addView(more, new LinearLayout.LayoutParams(dp(49.0f), dp(49.0f)));
        linearLayoutHorizontal.addView(options, new LinearLayout.LayoutParams(dp(98.0f), dp(49.0f)));
        return linearLayoutHorizontal;
    }

    /* JADX INFO: renamed from: lambda$createPlaylistToolbar$15$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m281x2ed5577f(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$createPlaylistToolbar$16$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m282x427d2b00(View v) {
        showSearchDialog();
    }

    /* JADX INFO: renamed from: lambda$createPlaylistToolbar$17$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m283x5624fe81(ActionIcon more, View v) {
        showPageMenu(more);
    }

    private LinearLayout createPlaylistHeader() {
        LinearLayout linearLayoutVertical = vertical();
        linearLayoutVertical.setGravity(1);
        linearLayoutVertical.setPadding(dp(10.0f), dp(82.0f), dp(10.0f), 0);
        this.playlistArt = Artwork.frame(this, 10);
        this.playlistArt.setElevation(dp(7.0f));
        int cover = Math.max(dp(160.0f), Math.min(dp(270.0f), getResources().getDisplayMetrics().widthPixels - dp(92.0f)));
        this.playlistArt.setContentDescription("歌单封面");
        linearLayoutVertical.addView(this.playlistArt, new LinearLayout.LayoutParams(cover, cover));
        this.playlistTitle = t("歌单", 22, -15000801, true);
        this.playlistTitle.setGravity(17);
        this.playlistTitle.setIncludeFontPadding(false);
        this.playlistTitle.setPadding(dp(6.0f), 0, dp(6.0f), 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(24.0f);
        linearLayoutVertical.addView(this.playlistTitle, titleLp);
        this.playlistMeta = t("在线歌单", 12, -8815739, false);
        this.playlistMeta.setGravity(17);
        this.playlistMeta.setIncludeFontPadding(false);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
        metaLp.topMargin = dp(9.0f);
        linearLayoutVertical.addView(this.playlistMeta, metaLp);
        LinearLayout controls = horizontal();
        controls.setGravity(17);
        controls.setPadding(dp(8.0f), 0, dp(8.0f), 0);
        this.playlistShuffle = new ActionIcon(3);
        this.playlistShuffle.setContentDescription("随机播放歌单");
        this.playlistShuffle.setBackground(QingyinUi.touch(this, -1645852, 28.0f));
        this.playlistShuffle.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m278x3b5c80ba(view);
            }
        });
        controls.addView(this.playlistShuffle, new LinearLayout.LayoutParams(dp(52.0f), dp(52.0f)));
        this.playlistPlay = button("▶  播放", false);
        this.playlistPlay.setTextSize(16.0f);
        this.playlistPlay.setTypeface(Typeface.DEFAULT, 1);
        this.playlistPlay.setTextColor(-1);
        this.playlistPlay.setBackground(QingyinUi.touch(this, -15000801, 28.0f));
        this.playlistPlay.setContentDescription("播放歌单");
        this.playlistPlay.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m279x4f04543b(view);
            }
        });
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(0, dp(52.0f), 1.0f);
        playLp.leftMargin = dp(13.0f);
        playLp.rightMargin = dp(13.0f);
        controls.addView(this.playlistPlay, playLp);
        this.playlistSave = new ActionIcon(4);
        this.playlistSave.setContentDescription("收藏歌单到本机");
        this.playlistSave.setBackground(QingyinUi.touch(this, -1645852, 28.0f));
        this.playlistSave.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m280xff708151(view);
            }
        });
        controls.addView(this.playlistSave, new LinearLayout.LayoutParams(dp(52.0f), dp(52.0f)));
        LinearLayout.LayoutParams controlsLp = new LinearLayout.LayoutParams(-1, dp(52.0f));
        controlsLp.topMargin = dp(20.0f);
        controlsLp.bottomMargin = dp(20.0f);
        linearLayoutVertical.addView(controls, controlsLp);
        this.playlistMessage = t("正在加载歌曲…", 12, -8815739, false);
        this.playlistMessage.setGravity(17);
        this.playlistMessage.setPadding(dp(8.0f), 0, dp(8.0f), dp(18.0f));
        linearLayoutVertical.addView(this.playlistMessage, new LinearLayout.LayoutParams(-1, -2));
        View line = new View(this);
        line.setBackgroundColor(-2106659);
        linearLayoutVertical.addView(line, new LinearLayout.LayoutParams(-1, dp(1.0f)));
        return linearLayoutVertical;
    }

    /* JADX INFO: renamed from: lambda$createPlaylistHeader$18$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m278x3b5c80ba(View v) {
        playPlaylist(true);
    }

    /* JADX INFO: renamed from: lambda$createPlaylistHeader$19$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m279x4f04543b(View v) {
        playPlaylist(false);
    }

    /* JADX INFO: renamed from: lambda$createPlaylistHeader$20$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m280xff708151(View v) {
        if (this.activePlaylist == null) {
            return;
        }
        boolean saved = OnlineLibraryStore.togglePlaylist(this, this.activePlaylist);
        updatePlaylistActions();
        say(saved ? "歌单已收藏到本机" : "已取消本机歌单收藏");
    }

    private void syncPlaylistHeader() {
        if (this.listing == null || this.playlistHeader == null) {
            return;
        }
        boolean detail = (this.activePlaylist == null || this.showingPlaylists) ? false : true;
        GradientDrawable gradientDrawableRounded = null;
        if (detail && !this.headerAttached) {
            this.listing.addHeaderView(this.playlistHeader, null, false);
            this.headerAttached = true;
        } else if (!detail && this.headerAttached) {
            this.listing.removeHeaderView(this.playlistHeader);
            this.headerAttached = false;
        }
        boolean playlistIndex = this.showingPlaylists && !detail;
        this.browseToolbar.setVisibility(detail ? 8 : 0);
        this.playlistToolbar.setVisibility(detail ? 0 : 8);
        LinearLayout.LayoutParams browseLp = (LinearLayout.LayoutParams) this.browseToolbar.getLayoutParams();
        if (browseLp.height != dp(playlistIndex ? 96.0f : 65.0f)) {
            browseLp.height = dp(playlistIndex ? 96.0f : 65.0f);
            this.browseToolbar.setLayoutParams(browseLp);
        }
        this.sectionTitle.setTextSize(playlistIndex ? 29.0f : 21.0f);
        this.status.setTextSize(playlistIndex ? 13.0f : 11.0f);
        boolean softSearch = this.ambientSearchPage && !detail;
        ListView listView = this.listing;
        int i = -1;
        if (!softSearch && !detail) {
            gradientDrawableRounded = QingyinUi.rounded(this, -1, 0.0f);
        }
        listView.setBackground(gradientDrawableRounded);
        this.listing.setClipToOutline(false);
        if (detail) {
            i = -790287;
        } else if (!playlistIndex) {
            i = -526343;
        }
        int pageColor = i;
        this.resultsFrame.setBackgroundColor(softSearch ? 0 : pageColor);
        this.blurSource.setBackgroundColor(softSearch ? 0 : pageColor);
        if (this.searchAmbient != null) {
            this.searchAmbient.setVisibility(softSearch ? 0 : 8);
        }
        getWindow().setStatusBarColor(softSearch ? AmbientBackdrop.baseColor(this) : pageColor);
        getWindow().setNavigationBarColor(softSearch ? AmbientBackdrop.baseColor(this) : pageColor);
        if (softSearch) {
            boolean dark = AppearancePreferences.dark(this);
            getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : 8208);
            AppearanceStyler.apply(this.blurSource, dark);
        }
        if (!detail) {
            this.playlistLoading = false;
        }
        if (detail) {
            String title = this.activePlaylist.optString("title", "歌单");
            this.playlistTitle.setText(title);
            String cover = PlaylistSnapshot.cover(this, this.activePlaylist);
            if (cover.isEmpty()) {
                cover = this.activePlaylist.optString("cover", "").trim();
            }
            if (cover.isEmpty()) {
                for (Track song : this.sourceOrder) {
                    if (!song.coverUrl.isEmpty()) {
                        cover = song.coverUrl;
                        break;
                    }
                }
            }
            if (!cover.equals(this.playlistArtUrl)) {
                this.playlistArtUrl = cover;
                Artwork.bind(this.playlistArt, cover);
            }
            DefaultPlaylistArt.decorate(this.playlistArt, title.contains("默认收藏") && cover.isEmpty());
            this.playlistArt.setContentDescription(title + " · 歌单封面");
            updatePlaylistMeta();
            updatePlaylistActions();
        }
        this.adapter.notifyDataSetChanged();
        m292lambda$new$0$localqingyinmobileOnlineActivity();
    }

    private void updatePlaylistMeta() {
        StringBuilder sbAppend;
        String str;
        String string;
        if (this.activePlaylist == null || this.playlistMeta == null) {
            return;
        }
        String count = OnlineApi.playlistCountLabel(this.activePlaylist);
        if ("点击查看歌曲".equals(count)) {
            if (this.sourceOrder.isEmpty()) {
                string = "酷狗音乐 · 在线歌单";
            } else {
                if (this.playlistLoading || this.morePlaylist) {
                    sbAppend = new StringBuilder().append("已加载 ").append(this.sourceOrder.size());
                    str = " 首";
                } else {
                    sbAppend = new StringBuilder().append(this.sourceOrder.size());
                    str = " 首歌曲";
                }
                string = sbAppend.append(str).toString();
            }
            count = string;
        }
        this.playlistMeta.setText(count);
        boolean visible = this.playlistLoading || this.songs.isEmpty() || this.status.getText().toString().contains("失败");
        this.playlistMessage.setVisibility(visible ? 0 : 8);
        this.playlistMessage.setText((this.playlistLoading || !this.songs.isEmpty() || this.status.getText().toString().contains("失败")) ? this.status.getText() : "暂无可播放歌曲 · 可在更多中刷新");
    }

    private void updatePlaylistActions() {
        if (this.activePlaylist == null || this.playlistSave == null) {
            return;
        }
        boolean saved = OnlineLibraryStore.isPlaylistSaved(this, this.activePlaylist);
        this.playlistSave.setIcon(saved ? 5 : 4);
        this.playlistSave.setContentDescription(saved ? "已收藏到本机，点击取消收藏" : "收藏歌单到本机");
        boolean playable = !this.songs.isEmpty();
        this.playlistPlay.setEnabled(playable);
        this.playlistPlay.setAlpha(playable ? 1.0f : 0.4f);
        this.playlistShuffle.setEnabled(playable);
        this.playlistShuffle.setAlpha(playable ? 1.0f : 0.4f);
        this.playlistShuffle.setInk((this.bound && this.player != null && this.player.mode() == QueueNavigator.Mode.SHUFFLE) ? -1815446 : -15000801);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBrowserStatus(String message) {
        this.status.setText(message);
        // Recommendation pages keep a single title; loading and errors live in the list.
        this.status.setVisibility(this.recommendationMethod == null ? View.VISIBLE : View.GONE);
        updatePlaylistMeta();
    }

    private void playPlaylist(boolean shuffle) {
        if (this.songs.isEmpty()) {
            say(this.playlistLoading ? "歌单正在加载，请稍后再试" : "歌单没有可播放歌曲");
            return;
        }
        if (!this.bound || this.player == null) {
            say("播放器正在启动，请稍后再试");
            return;
        }
        QueueNavigator.Mode desired = shuffle ? QueueNavigator.Mode.SHUFFLE : QueueNavigator.Mode.LOOP;
        for (int i = 0; i < QueueNavigator.Mode.values().length && this.player.mode() != desired; i++) {
            this.player.cycleMode();
        }
        play(shuffle ? new Random().nextInt(this.songs.size()) : 0);
        updatePlaylistActions();
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ActionIcon extends View {
        static final int BACK = 0;
        static final int CHECK = 5;
        static final int MORE = 2;
        static final int PLUS = 4;
        static final int SEARCH = 1;
        static final int SHUFFLE = 3;
        private int icon;
        private int ink;
        private final Paint paint;

        ActionIcon(int type) {
            super(OnlineActivity.this);
            this.paint = new Paint(1);
            this.ink = -15000801;
            this.icon = type;
            setClickable(true);
            setFocusable(true);
        }

        void setIcon(int type) {
            if (this.icon != type) {
                this.icon = type;
                invalidate();
            }
        }

        void setInk(int color) {
            if (this.ink != color) {
                this.ink = color;
                invalidate();
            }
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            float scale = Math.min(getWidth(), getHeight()) / 48.0f;
            canvas.translate(getWidth() / 2.0f, getHeight() / 2.0f);
            canvas.scale(scale, scale);
            this.paint.setColor(this.ink);
            this.paint.setStrokeWidth(2.2f);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
            Path path = new Path();
            if (this.icon == 0) {
                path.moveTo(4.0f, -10.0f);
                path.lineTo(-6.0f, 0.0f);
                path.lineTo(4.0f, 10.0f);
                canvas.drawPath(path, this.paint);
            } else if (this.icon == 1) {
                canvas.drawCircle(-2.0f, -2.0f, 7.5f, this.paint);
                canvas.drawLine(4.0f, 4.0f, 10.0f, 10.0f, this.paint);
            } else if (this.icon == 2) {
                this.paint.setStyle(Paint.Style.FILL);
                for (int i = -1; i <= 1; i++) {
                    canvas.drawCircle(i * 8, 0.0f, 1.9f, this.paint);
                }
            } else if (this.icon == 4) {
                canvas.drawLine(-9.0f, 0.0f, 9.0f, 0.0f, this.paint);
                canvas.drawLine(0.0f, -9.0f, 0.0f, 9.0f, this.paint);
            } else if (this.icon == 5) {
                path.moveTo(-9.0f, 0.0f);
                path.lineTo(-2.0f, 7.0f);
                path.lineTo(10.0f, -9.0f);
                canvas.drawPath(path, this.paint);
            } else if (this.icon == 3) {
                path.moveTo(-11.0f, -7.0f);
                path.lineTo(-7.0f, -7.0f);
                path.cubicTo(-1.0f, -7.0f, 1.0f, 7.0f, 7.0f, 7.0f);
                path.lineTo(11.0f, 7.0f);
                path.moveTo(7.0f, 3.0f);
                path.lineTo(11.0f, 7.0f);
                path.lineTo(7.0f, 11.0f);
                path.moveTo(-11.0f, 7.0f);
                path.lineTo(-7.0f, 7.0f);
                path.cubicTo(-1.0f, 7.0f, 1.0f, -7.0f, 7.0f, -7.0f);
                path.lineTo(11.0f, -7.0f);
                path.moveTo(7.0f, -11.0f);
                path.lineTo(11.0f, -7.0f);
                path.lineTo(7.0f, -3.0f);
                canvas.drawPath(path, this.paint);
            }
            canvas.restore();
        }
    }

    private void ensureForeground() {
        Intent i = new Intent(this, (Class<?>) PlaybackService.class);
        startForegroundService(i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMini() {
        String str;
        String string;
        if (this.miniPanel == null || !this.bound || this.player == null) {
            return;
        }
        Track track = this.player.current();
        this.miniPanel.setVisibility(8);
        int bottom = dp(90.0f);
        if (this.listing.getPaddingBottom() != bottom) {
            this.listing.setPadding(0, 0, 0, bottom);
        }
        updatePlaylistActions();
        m292lambda$new$0$localqingyinmobileOnlineActivity();
        if (track == null) {
            this.miniArtUri = "";
            if (!this.highlightedUri.isEmpty()) {
                this.highlightedUri = "";
                this.adapter.notifyDataSetChanged();
                return;
            }
            return;
        }
        if (!track.uri.equals(this.highlightedUri)) {
            this.highlightedUri = track.uri;
            this.adapter.notifyDataSetChanged();
        }
        if (!track.uri.equals(this.miniArtUri)) {
            this.miniArtUri = track.uri;
            Artwork.bind(this.miniArt, track.coverUrl);
        }
        this.miniTitle.setText(track.title);
        String error = this.player.lastError();
        TextView textView = this.miniSubtitle;
        if (error == null || error.isEmpty()) {
            StringBuilder sbAppend = new StringBuilder().append(track.artist).append(" · ");
            if (this.player.isPreparing()) {
                str = "正在缓冲";
            } else {
                str = this.player.isPlaying() ? "正在播放" : "已暂停";
            }
            string = sbAppend.append(str).toString();
        } else {
            string = this.player.isPreparing() ? "正在跳过不可播放歌曲" : "播放失败 · 轻点查看播放页";
        }
        textView.setText(string);
        this.miniToggle.setText((this.player.isPlaying() || this.player.isPreparing()) ? "Ⅱ" : "▶");
        int duration = Math.max(1, this.player.duration());
        this.miniProgress.setMax(duration);
        this.miniProgress.setProgress(Math.max(0, Math.min(duration, this.player.currentPosition())));
    }

    private void updateMore() {
        if (this.nextButton != null) {
            boolean z = true;
            boolean visible = !this.showingPlaylists && (this.activePlaylist == null ? this.moreSearch : this.morePlaylist);
            this.nextButton.setVisibility(visible ? 0 : 8);
            ((View) this.nextButton.getParent()).setVisibility(visible ? 0 : 8);
            Button button = this.nextButton;
            if (this.activePlaylist != null && this.playlistLoading) {
                z = false;
            }
            button.setEnabled(z);
            this.nextButton.setText((this.activePlaylist == null || !this.playlistLoading) ? "加载更多歌曲  ↓" : "正在加载更多…");
        }
        syncPlaylistHeader();
    }

    private LinearLayout createSearchPanel() {
        LinearLayout linearLayoutVertical = vertical();
        linearLayoutVertical.setPadding(dp(18.0f), dp(17.0f), dp(18.0f), dp(16.0f));
        linearLayoutVertical.setBackgroundColor(-526343);
        LinearLayout bar = horizontal();
        TextView back = t("‹", 32, -15000801, false);
        back.setGravity(17);
        back.setContentDescription("关闭搜索");
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda34
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m284lambda$createSearchPanel$21$localqingyinmobileOnlineActivity(view);
            }
        });
        bar.addView(back, new LinearLayout.LayoutParams(dp(37.0f), dp(54.0f)));
        LinearLayout inputBox = horizontal();
        inputBox.setPadding(dp(13.0f), 0, dp(11.0f), 0);
        inputBox.setBackground(QingyinUi.rounded(this, -1, 19.0f));
        TextView magnifier = t("⌕", 25, -8815739, false);
        magnifier.setGravity(17);
        inputBox.addView(magnifier, new LinearLayout.LayoutParams(dp(28.0f), dp(52.0f)));
        this.searchInput = new EditText(this);
        this.searchInput.setSingleLine(true);
        this.searchInput.setTextSize(16.0f);
        this.searchInput.setTextColor(-15000801);
        this.searchInput.setHintTextColor(-6644571);
        this.searchInput.setHint("歌曲、歌手");
        this.searchInput.setBackgroundColor(0);
        this.searchInput.setPadding(dp(7.0f), 0, 0, 0);
        this.searchInput.setImeOptions(3);
        inputBox.addView(this.searchInput, new LinearLayout.LayoutParams(0, dp(52.0f), 1.0f));
        TextView submit = t("搜索", 14, -1815446, true);
        submit.setGravity(17);
        submit.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda35
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m285lambda$createSearchPanel$22$localqingyinmobileOnlineActivity(view);
            }
        });
        inputBox.addView(submit, new LinearLayout.LayoutParams(dp(46.0f), dp(52.0f)));
        bar.addView(inputBox, new LinearLayout.LayoutParams(0, dp(54.0f), 1.0f));
        linearLayoutVertical.addView(bar, new LinearLayout.LayoutParams(-1, dp(56.0f)));
        this.searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda36
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return OnlineActivity.this.m286lambda$createSearchPanel$23$localqingyinmobileOnlineActivity(textView, i, keyEvent);
            }
        });
        TextView recent = t("最近搜索", 19, -15000801, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, dp(43.0f));
        titleParams.topMargin = dp(28.0f);
        linearLayoutVertical.addView(recent, titleParams);
        this.searchHistoryContainer = vertical();
        linearLayoutVertical.addView(this.searchHistoryContainer, new LinearLayout.LayoutParams(-1, -2));
        TextView helper = t("搜索歌曲或歌手 · 结果会显示在当前页面", 12, -8815739, false);
        helper.setPadding(dp(6.0f), dp(22.0f), 0, 0);
        linearLayoutVertical.addView(helper, new LinearLayout.LayoutParams(-1, -2));
        return linearLayoutVertical;
    }

    /* JADX INFO: renamed from: lambda$createSearchPanel$21$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m284lambda$createSearchPanel$21$localqingyinmobileOnlineActivity(View v) {
        closeSearch();
    }

    /* JADX INFO: renamed from: lambda$createSearchPanel$22$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m285lambda$createSearchPanel$22$localqingyinmobileOnlineActivity(View v) {
        performSearch();
    }

    /* JADX INFO: renamed from: lambda$createSearchPanel$23$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ boolean m286lambda$createSearchPanel$23$localqingyinmobileOnlineActivity(TextView v, int action, KeyEvent event) {
        if (action == 3) {
            performSearch();
            return true;
        }
        return false;
    }

    private void refreshSearchHistory() {
        if (this.searchHistoryContainer == null) {
            return;
        }
        this.searchHistoryContainer.removeAllViews();
        ArrayList<String> history = searches();
        if (history.isEmpty()) {
            TextView empty = t("还没有搜索记录", 14, -8815739, false);
            empty.setPadding(dp(12.0f), dp(18.0f), 0, dp(18.0f));
            this.searchHistoryContainer.addView(empty);
            return;
        }
        for (int i = 0; i < Math.min(8, history.size()); i++) {
            final String keyword = history.get(i);
            LinearLayout row = horizontal();
            row.setPadding(dp(13.0f), 0, dp(12.0f), 0);
            row.setBackground(QingyinUi.touch(this, -1, 12.0f));
            TextView icon = t("↗", 18, -8815739, false);
            icon.setGravity(17);
            row.addView(icon, new LinearLayout.LayoutParams(dp(34.0f), dp(52.0f)));
            TextView title = t(keyword, 15, -15000801, false);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setPadding(dp(8.0f), 0, dp(3.0f), 0);
            row.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));
            row.setContentDescription("搜索 " + keyword);
            row.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda41
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    OnlineActivity.this.m309xae877eff(keyword, view);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(54.0f));
            lp.bottomMargin = dp(5.0f);
            this.searchHistoryContainer.addView(row, lp);
        }
        TextView clear = t("清空搜索历史", 12, -8815739, false);
        clear.setGravity(21);
        clear.setPadding(0, dp(12.0f), dp(10.0f), dp(6.0f));
        clear.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda42
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                OnlineActivity.this.m310xc22f5280(view);
            }
        });
        this.searchHistoryContainer.addView(clear, new LinearLayout.LayoutParams(-1, dp(43.0f)));
    }

    /* JADX INFO: renamed from: lambda$refreshSearchHistory$24$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m309xae877eff(String keyword, View v) {
        this.searchInput.setText(keyword);
        performSearch();
    }

    /* JADX INFO: renamed from: lambda$refreshSearchHistory$25$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m310xc22f5280(View v) {
        saveSearches(new ArrayList<>());
        refreshSearchHistory();
    }

    private void showSearchDialog() {
        if (this.searchPanel == null) {
            return;
        }
        this.searchInput.setText(this.query.getText());
        this.searchInput.setSelection(this.searchInput.length());
        refreshSearchHistory();
        this.searchPanel.setVisibility(0);
        this.searchInput.requestFocus();
        this.searchInput.postDelayed(new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda43
            @Override // java.lang.Runnable
            public final void run() {
                OnlineActivity.this.m325lambda$showSearchDialog$26$localqingyinmobileOnlineActivity();
            }
        }, 140L);
    }

    /* JADX INFO: renamed from: lambda$showSearchDialog$26$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m325lambda$showSearchDialog$26$localqingyinmobileOnlineActivity() {
        InputMethodManager input;
        if (this.searchPanel.getVisibility() != 0 || isDestroyed() || (input = (InputMethodManager) getSystemService("input_method")) == null) {
            return;
        }
        input.showSoftInput(this.searchInput, 1);
    }

    private void performSearch() {
        String term = this.searchInput.getText().toString().trim();
        if (term.isEmpty()) {
            say("请输入歌曲名或歌手");
            return;
        }
        this.query.setText(term);
        hideSearchKeyboard();
        this.searchPanel.setVisibility(8);
        this.searchEnteredDirectly = false;
        search(false);
    }

    private void hideSearchKeyboard() {
        InputMethodManager input = (InputMethodManager) getSystemService("input_method");
        if (input != null) {
            input.hideSoftInputFromWindow(this.searchInput.getWindowToken(), 0);
        }
        this.searchInput.clearFocus();
    }

    private void closeSearch() {
        hideSearchKeyboard();
        this.searchPanel.setVisibility(8);
        if (!this.searchEnteredDirectly || !this.songs.isEmpty() || !this.lists.isEmpty()) {
            return;
        }
        finish();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.searchPanel != null && this.searchPanel.getVisibility() == 0) {
            closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    private void showPageMenu(View anchor) {
        QingyinUi.Sheet sheet = new QingyinUi.Sheet(this, "列表操作", this.sectionTitle.getText().toString(), false);
        if (!this.showingPlaylists) {
            sheet.addAction("▶", "播放当前列表", "从第一首开始播放", new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda57
                @Override // java.lang.Runnable
                public final void run() {
                    OnlineActivity.this.m323lambda$showPageMenu$27$localqingyinmobileOnlineActivity();
                }
            });
            sheet.addAction("↕", "歌曲排序", "按歌曲名称排序", new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda58
                @Override // java.lang.Runnable
                public final void run() {
                    OnlineActivity.this.showSort();
                }
            });
        }
        sheet.addAction("◇", "在线音质", "选择优先播放的规格", new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda59
            @Override // java.lang.Runnable
            public final void run() {
                OnlineActivity.this.pickQuality();
            }
        });
        sheet.addAction("↻", "刷新列表", "重新获取当前内容", new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda60
            @Override // java.lang.Runnable
            public final void run() {
                OnlineActivity.this.m324lambda$showPageMenu$28$localqingyinmobileOnlineActivity();
            }
        });
        sheet.addAction("◉", "账号管理", "酷狗登录、核对账户与同步歌单", new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda61
            @Override // java.lang.Runnable
            public final void run() {
                OnlineActivity.this.accountAction();
            }
        });
        sheet.addAction("≡", "播放队列", "管理下一首与待播歌曲", () -> QueueDialog.show(this, player));
        if ("dailyRecommendations".equals(recommendationMethod))
            sheet.addAction("ⓘ", "每日推荐来源", "酷狗概念版账号推荐", () -> new AlertDialog.Builder(this)
                .setTitle("每日推荐来源")
                .setMessage("来自青音内登录的酷狗概念版账号推荐接口。青音使用独立会话，结果可能与标准版酷狗或其他设备不同。\n\n默认保留接口原始顺序。更多菜单中的“刷新列表”会实时请求推荐，失败时不会用热门歌曲替代。")
                .setPositiveButton("知道了", null).show());
        sheet.show();
    }

    /* JADX INFO: renamed from: lambda$showPageMenu$27$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m323lambda$showPageMenu$27$localqingyinmobileOnlineActivity() {
        play(0);
    }

    /* JADX INFO: renamed from: lambda$showPageMenu$28$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m324lambda$showPageMenu$28$localqingyinmobileOnlineActivity() {
        if (isDiscoveryView()) {
            loadDiscoverySection();
            return;
        }
        if (this.recommendationMethod != null) {
            showAccountRecommendations(this.recommendationMethod, this.recommendationTitle, "guessYouLike".equals(this.recommendationMethod));
            return;
        }
        if (!this.localArtistView) {
            if (this.savedView == 0) {
                if (this.activePlaylist != null) {
                    JSONObject current = this.activePlaylist;
                    synchronized (LIST_CACHE) {
                        LIST_CACHE.remove(listKey(current));
                    }
                    PlaylistSnapshot.invalidate(this, current);
                    openList(current);
                    return;
                }
                if (this.showingPlaylists || this.query.getText().toString().trim().isEmpty()) {
                    showLists(this.mineListSelected);
                    return;
                } else {
                    search(false);
                    return;
                }
            }
            showSaved(this.savedView == 1);
            return;
        }
        showArtistIndex();
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class BrowserAdapter extends BaseAdapter {
        private BrowserAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return (OnlineActivity.this.showingPlaylists ? OnlineActivity.this.lists : OnlineActivity.this.songs).size();
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public boolean isEmpty() {
            return getCount() == 0 && OnlineActivity.this.activePlaylist == null;
        }

        @Override // android.widget.Adapter
        public Object getItem(int p) {
            return (OnlineActivity.this.showingPlaylists ? OnlineActivity.this.lists : OnlineActivity.this.songs).get(p);
        }

        @Override // android.widget.Adapter
        public long getItemId(int p) {
            return p;
        }

        @Override // android.widget.Adapter
        public View getView(final int i, View view, ViewGroup viewGroup) {
            Holder holder;
            View view2;
            int i2;
            OnlineActivity onlineActivity;
            float f;
            int iDp;
            float f2;
            int i3;
            boolean z;
            int i4;
            if (view == null) {
                LinearLayout linearLayoutVertical = OnlineActivity.this.vertical();
                LinearLayout linearLayoutHorizontal = OnlineActivity.this.horizontal();
                linearLayoutHorizontal.setPadding(OnlineActivity.this.dp(5.0f), OnlineActivity.this.dp(8.0f), OnlineActivity.this.dp(1.0f), OnlineActivity.this.dp(8.0f));
                linearLayoutHorizontal.setBackground(QingyinUi.touch(OnlineActivity.this, -1, 0.0f));
                linearLayoutVertical.addView(linearLayoutHorizontal, new LinearLayout.LayoutParams(-1, OnlineActivity.this.dp(82.0f)));
                View view3 = new View(OnlineActivity.this);
                view3.setBackgroundColor(-1579030);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, OnlineActivity.this.dp(1.0f));
                layoutParams.leftMargin = OnlineActivity.this.dp(80.0f);
                linearLayoutVertical.addView(view3, layoutParams);
                FrameLayout frameLayoutFrame = Artwork.frame(OnlineActivity.this, 10);
                linearLayoutHorizontal.addView(frameLayoutFrame, new LinearLayout.LayoutParams(OnlineActivity.this.dp(64.0f), OnlineActivity.this.dp(64.0f)));
                LinearLayout linearLayoutVertical2 = OnlineActivity.this.vertical();
                linearLayoutVertical2.setGravity(16);
                linearLayoutVertical2.setPadding(OnlineActivity.this.dp(12.0f), 0, OnlineActivity.this.dp(2.0f), 0);
                TextView textViewT = OnlineActivity.this.t("", 16, -15000801, false);
                textViewT.setIncludeFontPadding(false);
                textViewT.setSingleLine(true);
                textViewT.setEllipsize(TextUtils.TruncateAt.END);
                TextView textViewT2 = OnlineActivity.this.t("", 12, -8815739, false);
                textViewT2.setIncludeFontPadding(false);
                textViewT2.setSingleLine(true);
                textViewT2.setEllipsize(TextUtils.TruncateAt.END);
                linearLayoutVertical2.addView(textViewT);
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
                layoutParams2.topMargin = OnlineActivity.this.dp(5.0f);
                linearLayoutVertical2.addView(textViewT2, layoutParams2);
                linearLayoutHorizontal.addView(linearLayoutVertical2, new LinearLayout.LayoutParams(0, -1, 1.0f));
                TextView textViewT3 = OnlineActivity.this.t("›", 22, -5723985, false);
                textViewT3.setGravity(17);
                linearLayoutHorizontal.addView(textViewT3, new LinearLayout.LayoutParams(OnlineActivity.this.dp(22.0f), OnlineActivity.this.dp(48.0f)));
                holder = new Holder(frameLayoutFrame, textViewT, textViewT2, textViewT3, linearLayoutHorizontal, view3);
                linearLayoutVertical.setTag(holder);
                view2 = linearLayoutVertical;
            } else {
                holder = (Holder) view.getTag();
                view2 = view;
            }
            boolean z2 = (OnlineActivity.this.activePlaylist == null || OnlineActivity.this.showingPlaylists) ? false : true;
            boolean z3 = OnlineActivity.this.showingPlaylists;
            int i5 = 64;
            if (z3) {
                i2 = 82;
            } else {
                i2 = z2 ? 64 : 70;
            }
            if (!z3) {
                i5 = z2 ? 44 : 52;
            }
            LinearLayout linearLayout = holder.row;
            if (z3) {
                onlineActivity = OnlineActivity.this;
                f = 5.0f;
            } else {
                onlineActivity = OnlineActivity.this;
                f = z2 ? 1.0f : 11.0f;
            }
            int iDp2 = onlineActivity.dp(f);
            int iDp3 = OnlineActivity.this.dp(z3 ? 8.0f : 6.0f);
            if (z3) {
                iDp = OnlineActivity.this.dp(1.0f);
            } else {
                iDp = z2 ? 0 : OnlineActivity.this.dp(10.0f);
            }
            linearLayout.setPadding(iDp2, iDp3, iDp, OnlineActivity.this.dp(z3 ? 8.0f : 6.0f));
            holder.row.setLayoutParams(new LinearLayout.LayoutParams(-1, OnlineActivity.this.dp(i2)));
            holder.thumb.setLayoutParams(new LinearLayout.LayoutParams(OnlineActivity.this.dp(i5), OnlineActivity.this.dp(i5)));
            holder.thumb.setBackground(QingyinUi.gradient(OnlineActivity.this, -1451545, -2696715, z2 ? 6.0f : 10.0f));
            LinearLayout.LayoutParams layoutParams3 = (LinearLayout.LayoutParams) holder.divider.getLayoutParams();
            OnlineActivity onlineActivity2 = OnlineActivity.this;
            if (z3) {
                f2 = 80.0f;
            } else {
                f2 = z2 ? 57.0f : 72.0f;
            }
            layoutParams3.leftMargin = onlineActivity2.dp(f2);
            holder.divider.setLayoutParams(layoutParams3);
            View view4 = holder.divider;
            if (OnlineActivity.this.ambientSearchPage && AppearancePreferences.dark(OnlineActivity.this) && OnlineActivity.this.activePlaylist == null) {
                i3 = 1146902380;
            } else {
                i3 = z2 ? -2106659 : -1579030;
            }
            view4.setBackgroundColor(i3);
            holder.title.setTextSize(z3 ? 16.0f : 15.0f);
            if (OnlineActivity.this.showingPlaylists) {
                JSONObject jSONObject = (JSONObject) OnlineActivity.this.lists.get(i);
                String strPreferred = PlaylistCoverResolver.preferred(OnlineActivity.this, jSONObject);
                Artwork.bind(holder.thumb, strPreferred);
                DefaultPlaylistArt.decorate(holder.thumb, jSONObject.optString("title", "").contains("默认收藏") && strPreferred.isEmpty());
                holder.title.setText(jSONObject.optString("title", "未命名歌单"));
                String str = "";
                if (jSONObject.has("__artist")) {
                    int iOptInt = jSONObject.optInt("songCount", -1);
                    str = iOptInt >= 0 ? iOptInt + " 首歌曲" : "";
                } else {
                    String strPlaylistCountLabel = OnlineApi.playlistCountLabel(jSONObject);
                    if (!"点击查看歌曲".equals(strPlaylistCountLabel)) {
                        str = strPlaylistCountLabel;
                    }
                }
                holder.detail.setText(str);
                holder.detail.setVisibility(str.isEmpty() ? 8 : 0);
                holder.arrow.setText("›");
                holder.arrow.setTextSize(20.0f);
                holder.arrow.setTextColor(-4934469);
                holder.arrow.setContentDescription("打开歌单");
                holder.arrow.setOnClickListener(null);
                holder.title.setTextColor(-15000801);
                holder.title.setTypeface(Typeface.DEFAULT, 0);
                holder.row.setBackground(QingyinUi.touch(OnlineActivity.this, -1, 0.0f));
                z = false;
            } else {
                holder.detail.setVisibility(0);
                Track track = (Track) OnlineActivity.this.songs.get(i);
                Artwork.bind(holder.thumb, track.coverUrl);
                DefaultPlaylistArt.decorate(holder.thumb, false);
                boolean z4 = OnlineActivity.this.bound && OnlineActivity.this.player != null && OnlineActivity.this.player.current() != null && TextUtils.equals(OnlineActivity.this.player.current().uri, track.uri);
                holder.title.setText(track.title);
                holder.detail.setText(track.artist + " · " + (track.durationMs > 0 ? String.format(Locale.ROOT, "%02d:%02d", Long.valueOf((track.durationMs / 1000) / 60), Long.valueOf((track.durationMs / 1000) % 60)) : "在线播放"));
                holder.arrow.setText("⋯");
                holder.arrow.setTextSize(22.0f);
                holder.arrow.setTextColor(-8815739);
                holder.arrow.setContentDescription("歌曲操作");
                holder.arrow.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$BrowserAdapter$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view5) {
                        BrowserAdapter.this.m336xd327f91a(i, view5);
                    }
                });
                holder.title.setTextColor(z4 ? -1815446 : -15000801);
                holder.title.setTypeface(Typeface.DEFAULT, z4 ? 1 : 0);
                z = false;
                View childAt = ((ViewGroup) view2).getChildAt(0);
                OnlineActivity onlineActivity3 = OnlineActivity.this;
                if (z4) {
                    i4 = -1186327;
                } else {
                    i4 = z2 ? 0 : -1;
                }
                childAt.setBackground(QingyinUi.touch(onlineActivity3, i4, 8.0f));
            }
            if (OnlineActivity.this.ambientSearchPage) {
                AppearanceStyler.apply(view2, (OnlineActivity.this.activePlaylist == null && AppearancePreferences.dark(OnlineActivity.this)) ? true : z);
            }
            return view2;
        }

        /* JADX INFO: renamed from: lambda$getView$0$local-qingyin-mobile-OnlineActivity$BrowserAdapter, reason: not valid java name */
        /* synthetic */ void m336xd327f91a(int selectedPosition, View v) {
            OnlineActivity.this.showSongActions(selectedPosition);
        }
    }

    private static final class Holder {
        final TextView arrow;
        final TextView detail;
        final View divider;
        final LinearLayout row;
        final FrameLayout thumb;
        final TextView title;

        Holder(FrameLayout art, TextView name, TextView artist, TextView control, LinearLayout content, View line) {
            this.thumb = art;
            this.title = name;
            this.detail = artist;
            this.arrow = control;
            this.row = content;
            this.divider = line;
        }
    }

    private void openPlayer() {
        if (this.bound && this.player != null && this.player.current() != null) {
            startActivity(new Intent(this, (Class<?>) NowPlayingActivity.class));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    private void showSongActions(int position){
        if(position<0||position>=songs.size())return;
        Track song=songs.get(position);final ArrayList<Track> selectedQueue=new ArrayList<>(songs);
        boolean liked=OnlineLibraryStore.isFavorite(this,song);
        QingyinUi.Sheet sheet=new QingyinUi.Sheet(this,song.title,song.artist,false);sheet.setSong(song);
        sheet.addAction("next","下一首播放","",()->queueChosen(song,true));
        sheet.addAction("add","加入播放队列","",()->queueChosen(song,false));
        sheet.addSeparator();
        sheet.addAction("heart",liked?"取消收藏":"加入收藏","",()->{OnlineLibraryStore.toggle(this,song);adapter.notifyDataSetChanged();});
        sheet.addAction("play","立即播放","",()->playSelected(selectedQueue,song));
        sheet.addAction("player","打开播放页","",()->{playSelected(selectedQueue,song);openPlayer();});
        sheet.addAction("queue","查看播放队列","",()->QueueDialog.show(this,player));sheet.show();
    }
    private void queueChosen(Track song,boolean next){
        if(!bound||player==null){say("播放器正在启动，请稍后再试");return;}
        boolean changed=next?player.playNext(song):player.enqueue(song);
        say(changed?(next?"已设为下一首播放":"已加入播放队列"):(next?"这首歌曲正在播放":"歌曲已在播放队列"));
    }

    /* JADX INFO: renamed from: lambda$showSongActions$29$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m331lambda$showSongActions$29$localqingyinmobileOnlineActivity(Track song, int pos, DialogInterface d, int index) {
        if (index != 0) {
            if (index != 1) {
                play(pos);
                openPlayer();
                return;
            } else {
                play(pos);
                return;
            }
        }
        OnlineLibraryStore.toggle(this, song);
        this.adapter.notifyDataSetChanged();
    }

    private void updateListTools() {
    }

    private void applySort() {
        this.songs.clear();
        this.songs.addAll(this.sourceOrder);
        switch (this.sortMode) {
            case 1:
                this.songs.sort(Comparator.comparing(new Function() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda33
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((Track) obj).title;
                    }
                }, Collator.getInstance(Locale.CHINA)));
                break;
            case 2:
                this.songs.sort(Comparator.comparing(new Function() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda44
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((Track) obj).title;
                    }
                }, Collator.getInstance(Locale.CHINA)).reversed());
                break;
            case 3:
                this.songs.sort(Comparator.comparing(new Function() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda55
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((Track) obj).artist;
                    }
                }, Collator.getInstance(Locale.CHINA)));
                break;
            case 4:
                this.songs.sort(Comparator.comparing(new Function() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda66
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((Track) obj).artist;
                    }
                }, Collator.getInstance(Locale.CHINA)).reversed());
                break;
            case 5:
                Collections.reverse(this.songs);
                break;
        }
        this.adapter.notifyDataSetChanged();
        updateListTools();
    }

    /* JADX INFO: Access modifiers changed from: private */
    private void showSort(){
        final String[] names={"原始顺序（搜索相关性）","歌曲名 · 升序","歌曲名 · 降序","歌手 · 升序","歌手 · 降序","原始顺序 · 倒序"};
        new AlertDialog.Builder(this).setTitle("歌曲排序")
            .setSingleChoiceItems(names,sortMode,(d,position)->{
                sortMode=position;
                if(recommendationMethod==null)
                    getSharedPreferences("online_ui",MODE_PRIVATE).edit().putInt("sort",position).apply();
                applySort();d.dismiss();
            }).setNegativeButton("取消",null).show();
    }

    /* JADX INFO: renamed from: lambda$showSort$34$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m332lambda$showSort$34$localqingyinmobileOnlineActivity(DialogInterface d, int position) {
        this.sortMode = position;
        getSharedPreferences("online_ui", 0).edit().putInt("sort", position).apply();
        applySort();
        d.dismiss();
    }

    private ArrayList<String> searches() {
        String raw = getSharedPreferences("online_ui", 0).getString("search_history", "[]");
        ArrayList<String> data = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length() && data.size() < 12; i++) {
                String value = a.optString(i, "").trim();
                if (!value.isEmpty()) {
                    data.add(value);
                }
            }
        } catch (Exception e) {
        }
        return data;
    }

    private void saveSearches(ArrayList<String> values) {
        JSONArray data = new JSONArray();
        for (int i = 0; i < values.size() && i < 12; i++) {
            data.put(values.get(i));
        }
        getSharedPreferences("online_ui", 0).edit().putString("search_history", data.toString()).apply();
    }

    private void rememberSearch(String keyword) {
        String value = keyword.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            return;
        }
        if (value.length() > 100) {
            value = value.substring(0, 100);
        }
        ArrayList<String> list = searches();
        final String normalized = value;
        list.removeIf(new Predicate() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda64
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((String) obj).equalsIgnoreCase(normalized);
            }
        });
        list.add(0, value);
        saveSearches(list);
    }

    private void showSearchHistory() {
        final ArrayList<String> records = searches();
        if (records.isEmpty()) {
            say("暂无搜索历史");
        } else {
            new AlertDialog.Builder(this).setTitle("最近搜索 · 点击再次搜索").setItems((CharSequence[]) records.toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda71
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    OnlineActivity.this.m326lambda$showSearchHistory$36$localqingyinmobileOnlineActivity(records, dialogInterface, i);
                }
            }).setNeutralButton("删除单条", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda72
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    OnlineActivity.this.m328lambda$showSearchHistory$38$localqingyinmobileOnlineActivity(records, dialogInterface, i);
                }
            }).setPositiveButton("清空全部", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    OnlineActivity.this.m330lambda$showSearchHistory$40$localqingyinmobileOnlineActivity(dialogInterface, i);
                }
            }).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).show();
        }
    }

    /* JADX INFO: renamed from: lambda$showSearchHistory$36$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m326lambda$showSearchHistory$36$localqingyinmobileOnlineActivity(ArrayList records, DialogInterface d, int index) {
        this.query.setText((CharSequence) records.get(index));
        search(false);
    }

    /* JADX INFO: renamed from: lambda$showSearchHistory$38$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m328lambda$showSearchHistory$38$localqingyinmobileOnlineActivity(final ArrayList records, DialogInterface d, int w) {
        new AlertDialog.Builder(this).setTitle("删除搜索记忆").setItems((CharSequence[]) records.toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda23
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                OnlineActivity.this.m327lambda$showSearchHistory$37$localqingyinmobileOnlineActivity(records, dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$showSearchHistory$37$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m327lambda$showSearchHistory$37$localqingyinmobileOnlineActivity(ArrayList records, DialogInterface dialog, int i) {
        records.remove(i);
        saveSearches(records);
    }

    /* JADX INFO: renamed from: lambda$showSearchHistory$40$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m330lambda$showSearchHistory$40$localqingyinmobileOnlineActivity(DialogInterface d, int w) {
        new AlertDialog.Builder(this).setTitle("清空搜索历史？").setMessage("仅删除搜索记录，不影响收藏或播放历史。").setPositiveButton("清空", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda31
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                OnlineActivity.this.m329lambda$showSearchHistory$39$localqingyinmobileOnlineActivity(dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$showSearchHistory$39$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m329lambda$showSearchHistory$39$localqingyinmobileOnlineActivity(DialogInterface confirm, int which) {
        saveSearches(new ArrayList<>());
    }

    private ArrayList<Track> localListeningTracks() {
        ArrayList<Track> all = OnlineLibraryStore.tracks(this, true);
        Set<String> seen = new HashSet<>();
        Iterator<Track> it = all.iterator();
        while (it.hasNext()) {
            seen.add(it.next().uri);
        }
        for (Track t : OnlineLibraryStore.tracks(this, false)) {
            if (seen.add(t.uri)) {
                all.add(t);
            }
        }
        return all;
    }

    private void showArtistIndex() {
        clearRecommendationPage();
        this.viewSerial++;
        this.localArtistView = true;
        this.savedView = 0;
        this.activePlaylist = null;
        this.morePlaylist = false;
        this.moreSearch = false;
        this.lists.clear();
        this.songs.clear();
        this.sourceOrder.clear();
        Set<String> seen = new HashSet<>();
        for (Track song : localListeningTracks()) {
            if (seen.add(song.artist)) {
                int count = 0;
                for (Track candidate : localListeningTracks()) {
                    if (song.artist.equals(candidate.artist)) {
                        count++;
                    }
                }
                JSONObject row = new JSONObject();
                try {
                    row.put("title", song.artist);
                    row.put("cover", song.coverUrl);
                    row.put("songCount", count);
                    row.put("__artist", song.artist);
                } catch (Exception e) {
                }
                this.lists.add(row);
            }
        }
        this.lists.sort(new Comparator() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda39
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return String.CASE_INSENSITIVE_ORDER.compare(((JSONObject) obj).optString("title"), ((JSONObject) obj2).optString("title"));
            }
        });
        this.showingPlaylists = true;
        this.sectionTitle.setText("我的艺人");
        setBrowserStatus("来自最近播放与收藏 · " + this.lists.size() + " 位");
        this.emptyState.setText("还没有聆听记录\n播放或收藏歌曲后，会在这里按艺人整理");
        this.adapter.notifyDataSetChanged();
        updateMore();
    }

    private void showArtistSongs(String name) {
        clearRecommendationPage();
        this.viewSerial++;
        this.savedView = 0;
        this.activePlaylist = null;
        this.morePlaylist = false;
        this.moreSearch = false;
        this.localArtistView = true;
        this.sourceOrder.clear();
        this.songs.clear();
        this.lists.clear();
        for (Track t : localListeningTracks()) {
            if (name.equals(t.artist)) {
                this.sourceOrder.add(t);
            }
        }
        this.showingPlaylists = false;
        applySort();
        this.sectionTitle.setText(name);
        setBrowserStatus("来自最近播放与收藏 · " + this.songs.size() + " 首");
        this.emptyState.setText("这个艺人还没有保存的歌曲");
        updateMore();
    }

    private void showSaved(boolean favorites) {
        clearRecommendationPage();
        this.localArtistView = false;
        this.viewSerial++;
        this.savedView = favorites ? 1 : 2;
        this.activePlaylist = null;
        this.moreSearch = false;
        this.morePlaylist = false;
        this.sourceOrder.clear();
        this.sourceOrder.addAll(OnlineLibraryStore.tracks(this, favorites));
        this.showingPlaylists = false;
        this.lists.clear();
        applySort();
        this.sectionTitle.setText(favorites ? "我喜欢的音乐" : "最近播放");
        setBrowserStatus("仅保存在本机 · " + this.songs.size() + " 首");
        this.emptyState.setText(favorites ? "尚未收藏歌曲\n播放歌曲时点爱心即可收藏" : "还没有播放记录\n去发现页听一首吧");
        updateMore();
    }

    private void clearRecommendationPage(){
        if(recommendationMethod!=null)
            sortMode=getSharedPreferences("online_ui",MODE_PRIVATE).getInt("sort",0);
        recommendationMethod=null;recommendationTitle=null;
        recommendationBusy=false;
        if(status!=null)status.setVisibility(View.VISIBLE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAccountRecommendations(String method, String title) {
        showAccountRecommendations(method, title, false);
    }

    private void showAccountRecommendations(String method,String title,boolean next){
        if(next&&(recommendationBusy||!"guessYouLike".equals(method)))return;
        recommendationMethod=method;recommendationTitle=title;
        recommendationBusy=true;
        final long requestedArtworkEpoch=RecommendationArtwork.epoch(this);
        final long ticket=++viewSerial;
        savedView=0;activePlaylist=null;localArtistView=false;
        moreSearch=false;morePlaylist=false;
        if(!next){
            sortMode=0; // A song-list sorting preference must not reorder provider recommendations.
            if("guessYouLike".equals(method))seenGuessHashes.clear();
            songs.clear();lists.clear();sourceOrder.clear();showingPlaylists=false;
            adapter.notifyDataSetChanged();updateMore();
            emptyState.setText("正在加载"+title+"…");
        }
        sectionTitle.setText(title);
        setBrowserStatus(next?"正在获取下一批推荐…":"正在获取酷狗"+title+"…");
        // Snapshot on the UI thread; network callbacks must not touch mutable UI state.
        final Set<String> previous=new HashSet<>(seenGuessHashes);
        network.execute(()->{
            try{
                JSONObject params=new JSONObject();
                if(next)params.put("refresh",true);
                JSONObject response=OnlineApi.call(getApplicationContext(),method,params);
                if("dailyRecommendations".equals(method)&&(!"kugou-daily".equals(response.optString("source"))||response.optString("accountId").isEmpty()))
                    throw new IllegalStateException("每日推荐来源或账号信息缺失，请重新登录后重试");
                JSONArray entries=response.optJSONArray("songs");
                if(entries==null||entries.length()==0)
                    throw new IllegalStateException("酷狗未返回推荐歌曲");
                JSONArray fresh=new JSONArray();
                Set<String> batch=new HashSet<>();
                for(int i=0;i<entries.length();i++){
                    JSONObject song=entries.optJSONObject(i);
                    if(song==null)continue;
                    String hash=song.optString("hash","").toLowerCase(Locale.ROOT);
                    if(!hash.matches("[0-9a-f]{32}")||!batch.add(hash))continue;
                    if(next&&previous.contains(hash))continue;
                    fresh.put(song);
                }
                if(fresh.length()==0)
                    throw new IllegalStateException(next?
                        "酷狗暂时没有提供未听过的新一批歌曲":"酷狗未返回可用推荐歌曲");
                ui.post(()->{
                    if(isDestroyed()||ticket!=viewSerial)return;
                    if(requestedArtworkEpoch!=RecommendationArtwork.epoch(this) &&
                        !response.optString("accountId", "").equals(RecommendationArtwork.accountId(this))) {
                        recommendationBusy=false;
                        String message="账号已更改，请在更多菜单刷新列表";
                        setBrowserStatus(message);
                        if(sourceOrder.isEmpty())emptyState.setText(message);else say(message);
                        return;
                    }
                    recommendationBusy=false;
                    showSongs(fresh,title,false);
                    String responseAccount=response.optString("accountId", "");
                    if (!responseAccount.isEmpty()) {
                        RecommendationArtwork.setAccount(this,responseAccount);
                        RecommendationArtwork.save(this,method,fresh,responseAccount,RecommendationArtwork.epoch(this));
                    }
                    if("guessYouLike".equals(method)){
                        for(int i=0;i<fresh.length();i++){
                            JSONObject item=fresh.optJSONObject(i);
                            if(item!=null)seenGuessHashes.add(item.optString("hash","").toLowerCase(Locale.ROOT));
                        }
                    }
                    setBrowserStatus("");
                });
            }catch(Exception failure){
                boolean loginRequired=failure instanceof OnlineApi.ApiException &&
                    ("LOGIN_REQUIRED".equals(((OnlineApi.ApiException)failure).code)||
                     "SESSION_EXPIRED".equals(((OnlineApi.ApiException)failure).code));
                String message=failure.getMessage()==null?"网络暂时不可用":failure.getMessage();
                ui.post(()->{
                    if(isDestroyed()||ticket!=viewSerial)return;
                    recommendationBusy=false;
                    if(next){
                        setBrowserStatus("换一批失败 · 当前歌曲已保留");
                        say(message);
                        return;
                    }
                    setBrowserStatus(loginRequired?"请先登录酷狗音乐":"获取推荐失败");
                    emptyState.setText(loginRequired?
                        "登录酷狗账号，获取属于你的"+title:
                        "酷狗"+title+"暂时不可用\n"+message);
                    if(loginRequired)new AlertDialog.Builder(this)
                        .setTitle("登录后使用"+title)
                        .setMessage("该内容由酷狗音乐账号推荐接口提供，登录后才能获取。")
                        .setPositiveButton("去登录",(dialog,which)->beginQrLogin())
                        .setNegativeButton("稍后再说",null).show();
                });
            }
        });
    }

    /* JADX INFO: renamed from: lambda$showAccountRecommendations$45$local-qingyin-mobile-OnlineActivity, reason: not valid java name */


    /* JADX INFO: renamed from: lambda$showAccountRecommendations$42$local-qingyin-mobile-OnlineActivity, reason: not valid java name */


    /* JADX INFO: renamed from: lambda$showAccountRecommendations$44$local-qingyin-mobile-OnlineActivity, reason: not valid java name */


    /* JADX INFO: renamed from: lambda$showAccountRecommendations$43$local-qingyin-mobile-OnlineActivity, reason: not valid java name */


    /* JADX INFO: Access modifiers changed from: private */
    public void say(String message) {
        Toast.makeText(this, message, 0).show();
    }

    private void request(String method, JSONObject params, Consumer<JSONObject> onSuccess) {
        request(method, params, onSuccess, false);
    }

    private void request(String method,JSONObject params,Consumer<JSONObject> onSuccess,boolean quietFailure){
        final long requestTicket=viewSerial;
        final boolean contentRequest="playlistSongs".equals(method)||"search".equals(method)||
            "playlists".equals(method)||"recommendations".equals(method);
        network.execute(()->{
            try{
                JSONObject result=OnlineApi.call(getApplicationContext(),method,params);
                ui.post(()->{
                    if(isDestroyed()||isFinishing()||(!active&&!contentRequest))return;
                    if(contentRequest&&requestTicket!=viewSerial)return;
                    onSuccess.accept(result);
                });
            }catch(Exception failure){
                final String message=failure.getMessage()==null?"服务暂时不可用":failure.getMessage();
                ui.post(()->{
                    if(isDestroyed()||isFinishing()||(!active&&!contentRequest))return;
                    if(contentRequest&&requestTicket!=viewSerial)return;
                    // An account-status probe cannot turn a loaded playlist into a loading error.
                    if(quietFailure)return;
                    if("status".equals(method)){
                        if(active)say("账号状态获取失败："+message);
                        return;
                    }
                    if("playlistSongs".equals(method)){
                        playlistLoading=false;playlistRequestInFlight=false;
                    }
                    setBrowserStatus("请求失败："+message);
                    if(listing!=null&&adapter!=null&&adapter.getCount()==0&&emptyState!=null)
                        emptyState.setText("加载失败\n请检查网络后点击右上角菜单刷新");
                    if(active&&!"status".equals(method))say("操作未完成："+message);
                    if("playlistSongs".equals(method))updateMore();
                });
            }
        });
    }

    /* JADX INFO: renamed from: lambda$request$48$local-qingyin-mobile-OnlineActivity, reason: not valid java name */


    /* JADX INFO: renamed from: lambda$request$46$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m312lambda$request$46$localqingyinmobileOnlineActivity(boolean contentRequest, long requestTicket, Consumer onSuccess, JSONObject result) {
        if (isDestroyed() || isFinishing()) {
            return;
        }
        if (!this.active && !contentRequest) {
            return;
        }
        if (!contentRequest || requestTicket == this.viewSerial) {
            onSuccess.accept(result);
        }
    }

    /* JADX INFO: renamed from: lambda$request$47$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m313lambda$request$47$localqingyinmobileOnlineActivity(boolean contentRequest, long requestTicket, boolean quietFailure, String method, String message) {
        if (isDestroyed() || isFinishing()) {
            return;
        }
        if (!this.active && !contentRequest) {
            return;
        }
        if ((!contentRequest || requestTicket == this.viewSerial) && !quietFailure) {
            if ("status".equals(method)) {
                if (this.active) {
                    say("账号状态获取失败：" + message);
                    return;
                }
                return;
            }
            if ("playlistSongs".equals(method)) {
                this.playlistLoading = false;
                this.playlistRequestInFlight = false;
            }
            setBrowserStatus("请求失败：" + message);
            if (this.listing != null && this.adapter != null && this.adapter.getCount() == 0 && this.emptyState != null) {
                this.emptyState.setText("加载失败\n请检查网络后点击右上角菜单刷新");
            }
            if (this.active && !"status".equals(method)) {
                say("操作未完成：" + message);
            }
            if ("playlistSongs".equals(method)) {
                updateMore();
            }
        }
    }

    private void refreshStatus() {
        request("status", new JSONObject(), new Consumer() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda18
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                OnlineActivity.this.m311lambda$refreshStatus$49$localqingyinmobileOnlineActivity((JSONObject) obj);
            }
        }, true);
    }

    /* JADX INFO: renamed from: lambda$refreshStatus$49$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m311lambda$refreshStatus$49$localqingyinmobileOnlineActivity(JSONObject data) {
        boolean logged = data.optBoolean("loggedIn", false);
        JSONObject cacheUser=data.optJSONObject("user");
        RecommendationArtwork.setAccount(this,logged&&cacheUser!=null?cacheUser.optString("id",""):"");
        this.account.setText(logged ? "已登录" : "登录账号");
        if (this.status.getText().toString().contains("连接") || this.status.getText().toString().contains("启动")) {
            setBrowserStatus(logged ? "音乐引擎已连接 · 账号已登录" : "音乐引擎已连接 · 可以浏览公开歌单");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void accountAction() {
        request("status", new JSONObject(), new Consumer() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda47
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                OnlineActivity.this.m274lambda$accountAction$55$localqingyinmobileOnlineActivity((JSONObject) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$accountAction$55$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m274lambda$accountAction$55$localqingyinmobileOnlineActivity(JSONObject data) {
        JSONObject cacheUser=data.optJSONObject("user");
        RecommendationArtwork.setAccount(this,data.optBoolean("loggedIn",false)&&cacheUser!=null?cacheUser.optString("id",""):"");
        if (data.optBoolean("loggedIn", false)) {
            JSONObject user = data.optJSONObject("user");
            String name = user != null ? user.optString("nickname", user.optString("name", "已登录酷狗账号")) : "已登录酷狗账号";
            if (name.trim().isEmpty()) {
                name = "已登录酷狗账号";
            }
            String id = user == null ? "" : user.optString("id", "");
            String identity = name + (id.trim().isEmpty() ? "" : "\\n酷狗ID：" + id);
            new AlertDialog.Builder(this).setTitle("酷狗账号").setMessage(identity + "\\n\\n已在青音同步该酷狗账号。请在官方酷狗核对账号及会员状态，歌曲是否可播仍由酷狗实际授权决定。").setPositiveButton("退出登录", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda65
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    OnlineActivity.this.m271lambda$accountAction$52$localqingyinmobileOnlineActivity(dialogInterface, i);
                }
            }).setNeutralButton("QQ登录说明", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda67
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    OnlineActivity.this.m272lambda$accountAction$53$localqingyinmobileOnlineActivity(dialogInterface, i);
                }
            }).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).show();
            return;
        }
        String[] options = {"使用QQ登录酷狗（官方引导）", "酷狗概念版扫码同步"};
        new AlertDialog.Builder(this).setTitle("登录酷狗音乐").setItems(options, new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda68
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                OnlineActivity.this.m273lambda$accountAction$54$localqingyinmobileOnlineActivity(dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$accountAction$52$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m271lambda$accountAction$52$localqingyinmobileOnlineActivity(DialogInterface d, int w) {
        new AlertDialog.Builder(this).setTitle("确认退出账号？").setMessage("退出后需要重新扫码才能访问个人歌单。").setPositiveButton("退出", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda70
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                OnlineActivity.this.m270lambda$accountAction$51$localqingyinmobileOnlineActivity(dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$accountAction$51$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m270lambda$accountAction$51$localqingyinmobileOnlineActivity(DialogInterface dialog, int which) {
        request("logout", new JSONObject(), new Consumer() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda52
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                OnlineActivity.this.m269lambda$accountAction$50$localqingyinmobileOnlineActivity((JSONObject) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$accountAction$50$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m269lambda$accountAction$50$localqingyinmobileOnlineActivity(JSONObject result) {
        RecommendationArtwork.setAccount(this, "");
        this.account.setText("登录账号");
        showLists(false);
    }

    /* JADX INFO: renamed from: lambda$accountAction$53$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m272lambda$accountAction$53$localqingyinmobileOnlineActivity(DialogInterface d, int w) {
        showKugouQqGuide();
    }

    /* JADX INFO: renamed from: lambda$accountAction$54$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m273lambda$accountAction$54$localqingyinmobileOnlineActivity(DialogInterface dialog, int choice) {
        if (choice != 0) {
            beginQrLogin();
        } else {
            showKugouQqGuide();
        }
    }

    private void showKugouQqGuide() {
        new AlertDialog.Builder(this).setTitle("通过QQ登录酷狗").setMessage("1. 打开酷狗官方登录页，选择「QQ登录」，由QQ或酷狗官方完成授权。\\n\\n2. 在酷狗概念版 App 登录或核对同一个酷狗账号，确认歌单、会员是否属于该账号。\\n\\n3. 回青音选择「扫码同步」，用另一台设备上的酷狗概念版扫码确认。\\n\\n注意：官方网页登录不会自动把登录态传给青音；QQ未绑定原酷狗账号时可能进入新账号。青音不会读取QQ密码、截取Cookie或借用酷狗的QQ应用ID。").setPositiveButton("打开酷狗官方登录页", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda62
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                OnlineActivity.this.m320lambda$showKugouQqGuide$56$localqingyinmobileOnlineActivity(dialogInterface, i);
            }
        }).setNeutralButton("扫码同步酷狗账号", new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda63
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                OnlineActivity.this.m321lambda$showKugouQqGuide$57$localqingyinmobileOnlineActivity(dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$showKugouQqGuide$56$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m320lambda$showKugouQqGuide$56$localqingyinmobileOnlineActivity(DialogInterface dialog, int which) {
        openKugouOfficialLogin();
    }

    /* JADX INFO: renamed from: lambda$showKugouQqGuide$57$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m321lambda$showKugouQqGuide$57$localqingyinmobileOnlineActivity(DialogInterface dialog, int which) {
        beginQrLogin();
    }

    private void openKugouOfficialLogin() {
        try {
            Intent official = new Intent("android.intent.action.VIEW", Uri.parse("https://m.kugou.com/loginReg.php?act=login"));
            startActivity(official);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this).setTitle("无法打开浏览器").setMessage("请使用浏览器访问酷狗官方登录页：\\nhttps://m.kugou.com/loginReg.php?act=login").setPositiveButton("知道了", (DialogInterface.OnClickListener) null).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopQr() {
        if (this.qrPoll != null) {
            this.ui.removeCallbacks(this.qrPoll);
            this.qrPoll = null;
        }
        this.qrKey = null;
    }

    private void beginQrLogin(){
        stopQr();
        setBrowserStatus("正在生成概念版账号登录二维码…");
        request("qrStart",new JSONObject(),data->{
            qrKey=data.optString("key","");
            String image=data.optString("image","");
            if(qrKey.isEmpty()||image.isEmpty()){
                say("二维码获取失败，请稍后重试");return;
            }
            try{
                String encoded=image;
                int marker=encoded.indexOf(',');
                if(encoded.startsWith("data:")&&marker>=0)encoded=encoded.substring(marker+1);
                byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
                Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                if(bitmap==null)throw new IllegalArgumentException("QR bitmap");
                LinearLayout panel=vertical();
                panel.setPadding(dp(14),dp(12),dp(14),dp(5));
                ImageView qrImage=new ImageView(this);
                qrImage.setImageBitmap(bitmap);
                qrImage.setAdjustViewBounds(true);
                qrImage.setContentDescription("酷狗音乐概念版登录二维码");
                panel.addView(qrImage,new LinearLayout.LayoutParams(-1,dp(270)));
                TextView hint=t("请使用另一台设备的酷狗音乐概念版 App 扫码确认",
                    12,QingyinUi.MUTED,false);
                hint.setGravity(Gravity.CENTER);
                panel.addView(hint,new LinearLayout.LayoutParams(-1,dp(57)));
                qrDialog=new AlertDialog.Builder(this).setTitle("扫码登录")
                    .setView(panel).setNegativeButton("取消",(d,w)->stopQr()).create();
                qrDialog.setOnDismissListener(d->{stopQr();qrDialog=null;});
                qrDialog.show();
                final String pollingKey=qrKey;
                qrPoll=new Runnable(){
                    @Override public void run(){
                        if(!active||qrDialog==null||!qrDialog.isShowing()||
                           !pollingKey.equals(qrKey))return;
                        network.execute(()->{
                            try{
                                JSONObject params=new JSONObject().put("key",pollingKey);
                                JSONObject state=OnlineApi.call(getApplicationContext(),"qrCheck",params);
                                ui.post(()->{
                                    if(isDestroyed()||qrDialog==null||!qrDialog.isShowing()||
                                       !pollingKey.equals(qrKey))return;
                                    String result=state.optString("status","waiting");
                                    if("success".equals(result)){
                                        AlertDialog complete=qrDialog;
                                        qrDialog=null;stopQr();
                                        complete.dismiss();
                                        account.setText("已登录");
                                        say("登录成功");
                                        if(recommendationMethod!=null)
                                            showAccountRecommendations(recommendationMethod,recommendationTitle);
                                        else showLists(true);
                                    }else if("expired".equals(result)){
                                        AlertDialog expired=qrDialog;qrDialog=null;stopQr();
                                        expired.dismiss();say("二维码已过期，请重新登录");
                                    }else{
                                        if("scanned".equals(result))
                                            setBrowserStatus("已扫码，请在手机中确认登录…");
                                        if(qrPoll!=null)ui.postDelayed(qrPoll,2300);
                                    }
                                });
                            }catch(Exception failure){
                                ui.post(()->{
                                    if(qrPoll!=null&&active&&qrDialog!=null&&qrDialog.isShowing())
                                        ui.postDelayed(qrPoll,3500);
                                });
                            }
                        });
                    }
                };
                ui.postDelayed(qrPoll,1200);
            }catch(Exception problem){
                stopQr();
                say("二维码显示失败，请重新尝试");
            }
        });
    }

    public void pickQuality() {
        int selected = 0;
        String current = OnlineApi.quality(this);
        for (int i = 0; i < OnlineApi.QUALITIES.length; i++) {
            if (OnlineApi.QUALITIES[i].equals(current)) {
                selected = i;
            }
        }
        new AlertDialog.Builder(this).setTitle("在线音质").setSingleChoiceItems(OnlineApi.QUALITY_NAMES, selected, new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda51
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                OnlineActivity.this.m308lambda$pickQuality$61$localqingyinmobileOnlineActivity(dialogInterface, i2);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$pickQuality$61$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m308lambda$pickQuality$61$localqingyinmobileOnlineActivity(DialogInterface dialog, int index) {
        OnlineApi.setQuality(this, OnlineApi.QUALITIES[index]);
        dialog.dismiss();
        say("已保存音质偏好，实际格式取决于歌曲和账号权益");
        refreshStatus();
    }

    private void hydratePrivatePlaylistCovers(final long ticket) {
        final ArrayList<JSONObject> pending = new ArrayList<>();
        for (JSONObject item : this.lists) {
            if (PlaylistCoverResolver.needsResolution(this, item)) {
                pending.add(item);
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        pending.sort(new Comparator() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return OnlineActivity.this.m287x1402942c((JSONObject) obj, (JSONObject) obj2);
            }
        });
        this.network.execute(new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                OnlineActivity.this.m289x3b523b2e(pending, ticket);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$hydratePrivatePlaylistCovers$62$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ int m287x1402942c(JSONObject a, JSONObject b) {
        return Integer.compare(collectionPriority(b.optString("title", "")), collectionPriority(a.optString("title", "")));
    }

    /* JADX INFO: renamed from: lambda$hydratePrivatePlaylistCovers$64$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m289x3b523b2e(ArrayList pending, final long ticket) {
        int attempts = 0;
        Iterator it = pending.iterator();
        while (it.hasNext()) {
            final JSONObject item = (JSONObject) it.next();
            int attempts2 = attempts + 1;
            if (attempts >= 12) {
                return;
            }
            final String cover = PlaylistCoverResolver.resolve(getApplicationContext(), item);
            if (!cover.isEmpty()) {
                this.ui.post(new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda32
                    @Override // java.lang.Runnable
                    public final void run() {
                        OnlineActivity.this.m288x27aa67ad(ticket, item, cover);
                    }
                });
            }
            attempts = attempts2;
        }
    }

    /* JADX INFO: renamed from: lambda$hydratePrivatePlaylistCovers$63$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m288x27aa67ad(long ticket, JSONObject item, String cover) {
        if (!this.active || isDestroyed() || ticket != this.viewSerial || !this.showingPlaylists || !this.lists.contains(item)) {
            return;
        }
        try {
            item.put("cover", cover);
        } catch (Exception e) {
        }
        this.adapter.notifyDataSetChanged();
    }

    private int collectionPriority(String title) {
        return (title.contains("默认收藏") || title.contains("我喜欢")) ? 1 : 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showLists(final boolean privateLists) {
        clearRecommendationPage();
        this.savedView = 0;
        this.activePlaylist = null;
        this.moreSearch = false;
        this.morePlaylist = false;
        this.localArtistView = false;
        final long ticket = this.viewSerial + 1;
        this.viewSerial = ticket;
        this.lists.clear();
        this.songs.clear();
        this.sourceOrder.clear();
        this.showingPlaylists = true;
        this.adapter.notifyDataSetChanged();
        updateMore();
        this.emptyState.setText("正在加载歌单…");
        request(privateLists ? "playlists" : "recommendations", new JSONObject(), new Consumer() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda17
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                OnlineActivity.this.m322lambda$showLists$65$localqingyinmobileOnlineActivity(ticket, privateLists, (JSONObject) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$showLists$65$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m322lambda$showLists$65$localqingyinmobileOnlineActivity(long ticket, boolean privateLists, JSONObject data) {
        String str;
        if (ticket != this.viewSerial) {
            return;
        }
        JSONArray entries = data.optJSONArray("playlists");
        if (entries == null) {
            say("没有返回歌单");
            return;
        }
        this.lists.clear();
        this.sourceOrder.clear();
        this.songs.clear();
        this.showingPlaylists = true;
        this.mineListSelected = privateLists;
        this.sectionTitle.setText(privateLists ? "我的歌单" : "精选歌单");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item != null) {
                this.lists.add(item);
            }
        }
        setBrowserStatus(this.lists.size() + " 个歌单");
        TextView textView = this.emptyState;
        if (privateLists) {
            str = "暂无云端歌单\n可登录账号查看个人歌单";
        } else {
            str = "暂无精选歌单\n请稍后刷新重试";
        }
        textView.setText(str);
        this.adapter.notifyDataSetChanged();
        updateMore();
        updateListTools();
        if (privateLists) {
            hydratePrivatePlaylistCovers(ticket);
        }
    }

    private void openList(final JSONObject list) {
        CachedList cached;
        clearRecommendationPage();
        this.viewSerial++;
        this.savedView = 0;
        this.localArtistView = false;
        this.activePlaylist = list;
        this.playlistPage = 1;
        this.morePlaylist = false;
        this.moreSearch = false;
        this.playlistLoading = false;
        this.playlistRequestInFlight = false;
        this.sourceOrder.clear();
        this.songs.clear();
        this.lists.clear();
        this.showingPlaylists = false;
        this.sectionTitle.setText(list.optString("title", "歌单"));
        this.emptyState.setText("正在获取歌单曲目…");
        this.listing.setSelection(0);
        synchronized (LIST_CACHE) {
            cached = LIST_CACHE.get(listKey(list));
        }
        PlaylistSnapshot.Result saved = PlaylistSnapshot.read(this, list);
        if (saved != null && (cached == null || cached.hasMore || cached.tracks.size() < saved.songs.size())) {
            CachedList cached2 = new CachedList(saved.songs, saved.page, saved.hasMore);
            synchronized (LIST_CACHE) {
                LIST_CACHE.put(listKey(list), cached2);
            }
            cached = cached2;
        }
        if (cached != null && !cached.tracks.isEmpty()) {
            this.sourceOrder.addAll(cached.tracks);
            applySort();
            this.playlistPage = cached.page;
            this.morePlaylist = cached.hasMore;
            setBrowserStatus("已加载 " + this.sourceOrder.size() + " 首");
            this.emptyState.setText("暂无可播放歌曲");
            updateMore();
            int providerCount = list.optInt("count", 0);
            int recordedCount = saved == null ? this.sourceOrder.size() : saved.count;
            if (!this.morePlaylist) {
                if (providerCount > 0 && providerCount != recordedCount) {
                    loadPlaylist(list, 1, false);
                    return;
                }
                return;
            }
            if (this.playlistPage < 40 && this.sourceOrder.size() < 4000) {
                this.ui.postDelayed(new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda69
                    @Override // java.lang.Runnable
                    public final void run() {
                        OnlineActivity.this.m307lambda$openList$66$localqingyinmobileOnlineActivity(list);
                    }
                }, 80L);
                return;
            }
            return;
        }
        setBrowserStatus("正在加载歌曲…");
        updateMore();
        loadPlaylist(list, 1, false);
    }

    /* JADX INFO: renamed from: lambda$openList$66$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m307lambda$openList$66$localqingyinmobileOnlineActivity(JSONObject list) {
        if (this.activePlaylist == list && this.morePlaylist) {
            loadPlaylist(list, this.playlistPage + 1, true);
        }
    }

    private void loadPlaylist(final JSONObject list, final int page, final boolean append) {
        if ((!append || this.morePlaylist) && this.activePlaylist == list && !this.playlistRequestInFlight) {
            final long ticket = this.viewSerial;
            this.playlistLoading = true;
            this.playlistRequestInFlight = true;
            this.nextButton.setEnabled(false);
            JSONObject params = new JSONObject();
            try {
                params.put("id", list.optString("id"));
                params.put("listId", list.optString("listId"));
                params.put("globalId", list.optString("globalId"));
                params.put("type", list.optInt("type", 0));
                try {
                    params.put("page", page);
                    params.put("pageSize", 100);
                } catch (Exception e) {
                }
            } catch (Exception e2) {
            }
            if (append) {
                setBrowserStatus("已加载 " + this.sourceOrder.size() + " 首 · 正在获取更多");
            } else {
                this.sectionTitle.setText(list.optString("title", "歌单"));
                if (this.sourceOrder.isEmpty()) {
                    this.songs.clear();
                    this.showingPlaylists = false;
                    this.lists.clear();
                    this.adapter.notifyDataSetChanged();
                    this.emptyState.setText("正在获取歌单曲目…");
                }
                setBrowserStatus("正在加载歌曲…");
            }
            request("playlistSongs", params, new Consumer() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda53
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    OnlineActivity.this.m291lambda$loadPlaylist$68$localqingyinmobileOnlineActivity(list, ticket, append, page, (JSONObject) obj);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$loadPlaylist$68$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m291lambda$loadPlaylist$68$localqingyinmobileOnlineActivity(final JSONObject list, final long ticket, boolean append, final int page, JSONObject data) {
        String str;
        if (this.activePlaylist != list || ticket != this.viewSerial) {
            return;
        }
        this.playlistLoading = false;
        this.playlistRequestInFlight = false;
        JSONArray entries = data.optJSONArray("songs");
        if (!append) {
            showSongs(entries, list.optString("title", "歌单"), false);
        } else if (entries != null && entries.length() > 0) {
            int first = this.listing.getFirstVisiblePosition();
            View anchor = this.listing.getChildAt(0);
            int top = anchor == null ? 0 : anchor.getTop();
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.optJSONObject(i);
                Track t = entry == null ? null : OnlineApi.asTrack(entry);
                if (t != null) {
                    this.sourceOrder.add(t);
                    if (this.sortMode == 0) {
                        this.songs.add(t);
                    }
                }
            }
            int i2 = this.sortMode;
            if (i2 == 0) {
                this.adapter.notifyDataSetChanged();
            } else {
                applySort();
            }
            if (anchor != null) {
                this.listing.setSelectionFromTop(first, top);
            }
        }
        this.playlistPage = page;
        this.morePlaylist = data.optBoolean("hasMore") && entries != null && entries.length() > 0;
        boolean proceed = this.morePlaylist && page < 40 && this.sourceOrder.size() < 4000;
        this.playlistLoading = proceed;
        if (proceed) {
            str = "正在完整加载歌单 · 已获取 " + this.sourceOrder.size() + " 首";
        } else {
            str = "歌单已加载 " + this.sourceOrder.size() + " 首" + (this.morePlaylist ? " · 可继续加载" : "");
        }
        setBrowserStatus(str);
        synchronized (LIST_CACHE) {
            LIST_CACHE.put(listKey(list), new CachedList(this.sourceOrder, page, this.morePlaylist));
        }
        if (!proceed && !this.morePlaylist) {
            PlaylistSnapshot.write(getApplicationContext(), list, this.sourceOrder, page, false);
        }
        updateMore();
        if (proceed) {
            this.ui.postDelayed(new Runnable() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    OnlineActivity.this.m290lambda$loadPlaylist$67$localqingyinmobileOnlineActivity(list, ticket, page);
                }
            }, 50L);
        }
    }

    /* JADX INFO: renamed from: lambda$loadPlaylist$67$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m290lambda$loadPlaylist$67$localqingyinmobileOnlineActivity(JSONObject list, long ticket, int page) {
        if (this.activePlaylist == list && ticket == this.viewSerial) {
            loadPlaylist(list, page + 1, true);
        }
    }

    private void search(final boolean more) {
        final String text = this.query.getText().toString().trim();
        if (text.isEmpty()) {
            say("输入歌曲名或歌手名");
            return;
        }
        if (more && !this.moreSearch) {
            say("没有更多结果");
            return;
        }
        if (!more) {
            clearRecommendationPage();
            this.activePlaylist = null;
            this.savedView = 0;
            this.viewSerial++;
        }
        final long ticket = this.viewSerial;
        if (!more) {
            this.songs.clear();
            this.sourceOrder.clear();
            this.showingPlaylists = false;
            this.adapter.notifyDataSetChanged();
            this.emptyState.setText("正在搜索…");
            this.sectionTitle.setText("搜索：" + text);
            setBrowserStatus("正在搜索歌曲…");
            updateMore();
        }
        final int page = more ? 1 + this.searchPage : 1;
        JSONObject params = new JSONObject();
        try {
            params.put("keyword", text);
            params.put("page", page);
        } catch (Exception e) {
        }
        request("search", params, new Consumer() { // from class: local.qingyin.mobile.OnlineActivity$$ExternalSyntheticLambda28
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                OnlineActivity.this.m315lambda$search$69$localqingyinmobileOnlineActivity(ticket, text, more, page, (JSONObject) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$search$69$local-qingyin-mobile-OnlineActivity, reason: not valid java name */
    /* synthetic */ void m315lambda$search$69$localqingyinmobileOnlineActivity(long ticket, String text, boolean more, int page, JSONObject data) {
        if (ticket != this.viewSerial) {
            return;
        }
        JSONArray entries = data.optJSONArray("songs");
        showSongs(entries, "搜索：" + text, more);
        if (!more) {
            rememberSearch(text);
        }
        this.searchPage = page;
        this.moreSearch = entries != null && entries.length() >= 30;
        updateMore();
    }

    private void showSongs(JSONArray entries, String header, boolean append) {
        Track song;
        if (!append) {
            this.sourceOrder.clear();
            this.lists.clear();
        }
        this.emptyState.setText("暂无可播放歌曲\n可以换个关键词或歌单试试");
        this.showingPlaylists = false;
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                JSONObject item = entries.optJSONObject(i);
                if (item != null && (song = OnlineApi.asTrack(item)) != null) {
                    this.sourceOrder.add(song);
                }
            }
        }
        applySort();
        this.sectionTitle.setText(header);
        setBrowserStatus(this.songs.size() + " 首歌曲 · 音质：" + OnlineApi.quality(this));
        updateMore();
    }

    private void play(int index) {
        if (index < 0 || index >= this.songs.size()) {
            return;
        }
        if (!this.bound || this.player == null) {
            say("播放器正在启动，请稍后再试");
            return;
        }
        ensureForeground();
        this.player.playQueue(new ArrayList(this.songs), index);
        updateMini();
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        this.active = true;
        if (this.discoveryReloadOnStart && isDiscoveryView()) loadDiscoverySection();
        if (this.ambientSearchPage && this.searchAmbient != null) {
            List<String> covers = RecommendationArtwork.load(this, "guessYouLike");
            this.searchAmbient.show(4, covers.isEmpty() ? "" : covers.get(0));
            syncPlaylistHeader();
        }
        if (this.bound && this.player != null) {
            this.player.setListener(this.playbackChange);
        }
        if (this.savedView != 0) {
            showSaved(this.savedView == 1);
        }
        this.ui.removeCallbacks(this.playbackTicker);
        this.ui.post(this.playbackTicker);
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.active = false;
        cancelDiscoveryRequest(true);
        this.ui.removeCallbacks(this.playbackTicker);
        if (this.bound && this.player != null) {
            this.player.setListener(null);
        }
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        cancelDiscoveryRequest(false);
        if (this.searchAmbient != null) {
            this.searchAmbient.release();
        }
        stopQr();
        if (this.qrDialog != null) {
            this.qrDialog.dismiss();
            this.qrDialog = null;
        }
        if (this.bindingRequested) {
            unbindService(this.service);
        }
        this.network.shutdownNow();
        this.ui.removeCallbacks(this.playbackTicker);
        this.ui.removeCallbacksAndMessages(null);
        if (this.blurSource != null && this.blurSource.getViewTreeObserver().isAlive()) {
            this.blurSource.getViewTreeObserver().removeOnGlobalLayoutListener(this.blurLayout);
        }
        for (FrostedGlassDrawable drawable : this.frostedLayers) {
            drawable.release();
        }
        this.frostedLayers.clear();
        super.onDestroy();
    }
    private void playSelected(java.util.List<Track> selectedQueue,Track song){
        if(!bound||player==null){say("播放器正在启动，请稍后再试");return;}
        int selected=selectedQueue.indexOf(song);
        if(selected<0)return;
        ensureForeground();player.playQueue(selectedQueue,selected);updateMini();
    }
}
