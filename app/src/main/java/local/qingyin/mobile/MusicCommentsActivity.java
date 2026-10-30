package local.qingyin.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class MusicCommentsActivity extends Activity {
    private static final int BLUE = -10124632;
    private static final int INK = -14144973;
    private static final int LINE = -921358;
    private static final int MUTE = -6907740;
    private BaseAdapter adapter;
    private long audioId;
    private TextView countLabel;
    private LinearLayout empty;
    private TextView emptyAction;
    private TextView emptyText;
    private TextView heading;
    private ProgressBar indicator;
    private ListView list;
    private TextView loadMore;
    private TextView sortButton;
    private LinearLayout tabs;
    private float touchStartY;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService jobs = Executors.newFixedThreadPool(3);
    private final ArrayList<KugouCommentModel.Comment> all = new ArrayList<>();
    private final ArrayList<KugouCommentModel.Comment> shown = new ArrayList<>();
    private final HashMap<String, ReplyState> replies = new HashMap<>();
    private final HashSet<String> expandedText = new HashSet<>();
    private final HashSet<String> seenIds = new HashSet<>();
    private String trackUri = "";
    private String hash = "";
    private String songName = "";
    private String artist = "";
    private String cover = "";
    private String filter = "all";
    private String specialId = "";
    private String draft = "";
    private boolean alive = true;
    private boolean busy = false;
    private boolean hasMore = true;
    private boolean failed = false;
    private int page = 0;
    private int revision = 0;
    private int total = -1;
    private String order = "provider";

    /* JADX INFO: Access modifiers changed from: private */
    public int d(float v) {
        return QingyinUi.dp(this, v);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout stack() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(1);
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setGravity(16);
        return v;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView text(String s, int size, int ink, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(ink);
        t.setIncludeFontPadding(false);
        if (bold) {
            t.setTypeface(Typeface.create("sans-serif-medium", 0));
        }
        return t;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public GradientDrawable bg(int color, int radius) {
        return QingyinUi.rounded(this, color, radius);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pad(View v, int l, int t, int r, int b) {
        v.setPadding(d(l), d(t), d(r), d(b));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout.LayoutParams l(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : d(w), h < 0 ? h : d(h));
    }

    @Override // android.app.Activity
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        this.trackUri = getIntent().getStringExtra("track_uri");
        this.songName = getIntent().getStringExtra("track_title");
        this.artist = getIntent().getStringExtra("track_artist");
        this.cover = getIntent().getStringExtra("track_cover");
        if (this.trackUri == null) {
            this.trackUri = "";
        }
        if (this.songName == null) {
            this.songName = "";
        }
        if (this.artist == null) {
            this.artist = "";
        }
        if (this.cover == null) {
            this.cover = "";
        }
        try {
            Uri uri = Uri.parse(this.trackUri);
            if ("qingyin-online".equals(uri.getScheme()) && "track".equals(uri.getAuthority())) {
                this.hash = uri.getLastPathSegment();
                if (this.hash != null && this.hash.matches("(?i)[a-f0-9]{32}")) {
                    this.audioId = Long.parseLong(uri.getQueryParameter("audioId"));
                    if (this.audioId <= 0) {
                        throw new IllegalArgumentException();
                    }
                    getWindow().setStatusBarColor(-331275);
                    getWindow().setNavigationBarColor(-771);
                    getWindow().setSoftInputMode(16);
                    int flags = 8192 | 16;
                    getWindow().getDecorView().setSystemUiVisibility(flags);
                    LinearLayout base = stack();
                    base.setBackgroundColor(-1);
                    setContentView(base);
                    buildHeader(base);
                    buildTabs(base);
                    FrameLayout body = new FrameLayout(this);
                    base.addView(body, new LinearLayout.LayoutParams(-1, 0, 1.0f));
                    this.list = new ListView(this);
                    this.list.setBackgroundColor(0);
                    this.list.setCacheColorHint(0);
                    this.list.setDividerHeight(0);
                    this.list.setDivider(null);
                    this.list.setVerticalScrollBarEnabled(false);
                    this.list.setClipToPadding(false);
                    pad(this.list, 17, 4, 17, 18);
                    LinearLayout footer = stack();
                    footer.setGravity(17);
                    pad(footer, 0, 10, 0, 20);
                    this.loadMore = text("加载更多评论", 13, -10124632, true);
                    this.loadMore.setGravity(17);
                    this.loadMore.setMinHeight(d(42.0f));
                    footer.addView(this.loadMore, l(-1, 42));
                    this.list.addFooterView(footer, null, false);
                    this.adapter = new BaseAdapter() { // from class: local.qingyin.mobile.MusicCommentsActivity.1
                        @Override // android.widget.Adapter
                        public int getCount() {
                            return MusicCommentsActivity.this.shown.size();
                        }

                        @Override // android.widget.Adapter
                        public Object getItem(int n) {
                            return MusicCommentsActivity.this.shown.get(n);
                        }

                        @Override // android.widget.Adapter
                        public long getItemId(int n) {
                            return ((KugouCommentModel.Comment) MusicCommentsActivity.this.shown.get(n)).key().hashCode();
                        }

                        @Override // android.widget.BaseAdapter, android.widget.Adapter
                        public boolean hasStableIds() {
                            return true;
                        }

                        @Override // android.widget.Adapter
                        public View getView(int n, View old, ViewGroup parent) {
                            return MusicCommentsActivity.this.commentView(old, (KugouCommentModel.Comment) MusicCommentsActivity.this.shown.get(n));
                        }
                    };
                    this.list.setAdapter((ListAdapter) this.adapter);
                    body.addView(this.list, new FrameLayout.LayoutParams(-1, -1));
                    this.empty = stack();
                    this.empty.setGravity(17);
                    pad(this.empty, 34, 10, 34, 20);
                    TextView visual = text("♫", 40, -3096629, false);
                    visual.setGravity(17);
                    this.empty.addView(visual, l(-1, 65));
                    this.emptyText = text("评论加载中…", 14, -6907740, false);
                    this.emptyText.setGravity(17);
                    this.emptyText.setLineSpacing(d(4.0f), 1.0f);
                    this.empty.addView(this.emptyText, l(-1, -2));
                    this.indicator = new ProgressBar(this);
                    this.indicator.setIndeterminateTintList(ColorStateList.valueOf(-10124632));
                    LinearLayout.LayoutParams spin = l(27, 27);
                    spin.gravity = 17;
                    spin.topMargin = d(15.0f);
                    this.empty.addView(this.indicator, spin);
                    this.emptyAction = text("点击重试", 14, -10124632, true);
                    this.emptyAction.setGravity(17);
                    this.emptyAction.setMinHeight(d(48.0f));
                    this.emptyAction.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda1
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            MusicCommentsActivity.this.m214lambda$onCreate$0$localqingyinmobileMusicCommentsActivity(view);
                        }
                    });
                    this.empty.addView(this.emptyAction, l(-1, 52));
                    body.addView(this.empty, new FrameLayout.LayoutParams(-1, -1));
                    this.loadMore.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda2
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            MusicCommentsActivity.this.m215lambda$onCreate$1$localqingyinmobileMusicCommentsActivity(view);
                        }
                    });
                    this.list.setOnScrollListener(new AbsListView.OnScrollListener() { // from class: local.qingyin.mobile.MusicCommentsActivity.2
                        boolean interacted;

                        @Override // android.widget.AbsListView.OnScrollListener
                        public void onScrollStateChanged(AbsListView v, int state) {
                            if (state == 1 || state == 2) {
                                this.interacted = true;
                            }
                        }

                        @Override // android.widget.AbsListView.OnScrollListener
                        public void onScroll(AbsListView v, int first, int count, int totalCount) {
                            if (this.interacted && count > 0 && MusicCommentsActivity.this.shown.size() > 0 && first + count >= totalCount - 2 && MusicCommentsActivity.this.hasMore && !MusicCommentsActivity.this.busy && !MusicCommentsActivity.this.failed && MusicCommentsActivity.this.page > 0) {
                                MusicCommentsActivity.this.fetch(MusicCommentsActivity.this.page + 1);
                            }
                        }
                    });
                    this.draft = getPreferences(0).getString("draft_" + this.hash, "");
                    this.list.setOnTouchListener(new View.OnTouchListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda3
                        @Override // android.view.View.OnTouchListener
                        public final boolean onTouch(View view, MotionEvent motionEvent) {
                            return MusicCommentsActivity.this.m216lambda$onCreate$2$localqingyinmobileMusicCommentsActivity(view, motionEvent);
                        }
                    });
                    buildComposer(base);
                    refresh();
                    return;
                }
                throw new IllegalArgumentException();
            }
            throw new IllegalArgumentException();
        } catch (Exception e) {
            Toast.makeText(this, "该歌曲暂时没有可用的评论标识", 1).show();
            finish();
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m214lambda$onCreate$0$localqingyinmobileMusicCommentsActivity(View v) {
        refresh();
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m215lambda$onCreate$1$localqingyinmobileMusicCommentsActivity(View v) {
        if (this.busy) {
            return;
        }
        this.failed = false;
        fetch(this.page + 1);
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ boolean m216lambda$onCreate$2$localqingyinmobileMusicCommentsActivity(View v, MotionEvent event) {
        if (event.getActionMasked() == 0) {
            this.touchStartY = event.getY();
        }
        if (event.getActionMasked() != 1 || this.list.getFirstVisiblePosition() != 0 || this.list.getChildCount() <= 0 || this.list.getChildAt(0).getTop() < 0 || event.getY() - this.touchStartY <= d(120.0f) || this.busy) {
            return false;
        }
        refresh();
        return true;
    }

    private void buildHeader(LinearLayout linearLayout) {
        LinearLayout linearLayoutStack = stack();
        linearLayoutStack.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-332301, -1}));
        pad(linearLayoutStack, 12, 4, 14, 6);
        LinearLayout linearLayoutRow = row();
        TextView back = text("‹", 31, -14144973, false);
        back.setGravity(17);
        back.setContentDescription("返回播放页面");
        linearLayoutRow.addView(back, l(40, 46));
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicCommentsActivity.this.m204lambda$buildHeader$3$localqingyinmobileMusicCommentsActivity(view);
            }
        });
        FrameLayout art = Artwork.frame(this, 10);
        Artwork.bind(art, this.cover);
        linearLayoutRow.addView(art, l(42, 42));
        LinearLayout names = stack();
        pad(names, 11, 0, 3, 0);
        TextView track = text(this.songName, 16, -14144973, true);
        track.setSingleLine(true);
        track.setEllipsize(TextUtils.TruncateAt.END);
        names.addView(track, l(-1, 22));
        TextView by = text(this.artist, 12, -7501419, false);
        by.setSingleLine(true);
        by.setEllipsize(TextUtils.TruncateAt.END);
        names.addView(by, l(-1, 19));
        linearLayoutRow.addView(names, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView reload = text("↻", 25, -7171429, false);
        reload.setGravity(17);
        reload.setContentDescription("刷新评论");
        linearLayoutRow.addView(reload, l(44, 45));
        reload.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicCommentsActivity.this.m205lambda$buildHeader$4$localqingyinmobileMusicCommentsActivity(view);
            }
        });
        linearLayoutStack.addView(linearLayoutRow);
        linearLayout.addView(linearLayoutStack, l(-1, 59));
        LinearLayout desc = row();
        pad(desc, 20, 7, 19, 4);
        this.heading = text("评论", 20, -14144973, true);
        desc.addView(this.heading, l(-2, 30));
        this.countLabel = text("", 12, -6907740, false);
        pad(this.countLabel, 6, 4, 0, 0);
        desc.addView(this.countLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.sortButton = text("默认顺序  ⌄", 12, -10124632, true);
        this.sortButton.setGravity(21);
        this.sortButton.setContentDescription("选择已加载评论排序方式");
        this.sortButton.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicCommentsActivity.this.m206lambda$buildHeader$5$localqingyinmobileMusicCommentsActivity(view);
            }
        });
        desc.addView(this.sortButton, l(129, 36));
        linearLayout.addView(desc, l(-1, 42));
    }

    /* JADX INFO: renamed from: lambda$buildHeader$3$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m204lambda$buildHeader$3$localqingyinmobileMusicCommentsActivity(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$buildHeader$4$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m205lambda$buildHeader$4$localqingyinmobileMusicCommentsActivity(View v) {
        refresh();
    }

    /* JADX INFO: renamed from: lambda$buildHeader$5$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m206lambda$buildHeader$5$localqingyinmobileMusicCommentsActivity(View v) {
        showSortMenu();
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(this, this.sortButton);
        popup.getMenu().add(0, 1, 0, "默认顺序（酷狗返回）");
        popup.getMenu().add(0, 2, 1, "按赞数（已加载）");
        popup.getMenu().add(0, 3, 2, "最近发表（已加载）");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda21
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return MusicCommentsActivity.this.m220lambda$showSortMenu$6$localqingyinmobileMusicCommentsActivity(menuItem);
            }
        });
        popup.show();
    }

    /* JADX INFO: renamed from: lambda$showSortMenu$6$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ boolean m220lambda$showSortMenu$6$localqingyinmobileMusicCommentsActivity(MenuItem item) {
        String str;
        String str2;
        if (item.getItemId() == 2) {
            str = "likes";
        } else {
            str = item.getItemId() == 3 ? "recent" : "provider";
        }
        this.order = str;
        TextView textView = this.sortButton;
        if ("likes".equals(this.order)) {
            str2 = "赞数排序  ⌄";
        } else {
            str2 = "recent".equals(this.order) ? "时间排序  ⌄" : "默认顺序  ⌄";
        }
        textView.setText(str2);
        applyFilter();
        this.list.setSelection(0);
        Toast.makeText(this, "仅对已加载评论排序", 0).show();
        return true;
    }

    private void buildTabs(LinearLayout base) {
        this.tabs = row();
        pad(this.tabs, 17, 0, 17, 8);
        base.addView(this.tabs, l(-1, 49));
        updateTabs();
        View border = new View(this);
        border.setBackgroundColor(-921358);
        base.addView(border, l(-1, 1));
    }

    private void updateTabs() {
        if (this.tabs == null) {
            return;
        }
        this.tabs.removeAllViews();
        String[][] values = {new String[]{"all", "全部"}, new String[]{"image", "有图"}, new String[]{"reply", "有回复"}};
        for (final String[] option : values) {
            boolean chosen = this.filter.equals(option[0]);
            TextView tab = text(option[1], 13, chosen ? -10124632 : -7170654, chosen);
            tab.setGravity(17);
            tab.setBackground(bg(chosen ? -1379846 : -526343, 21));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, d(35.0f), 1.0f);
            params.rightMargin = d(8.0f);
            this.tabs.addView(tab, params);
            tab.setContentDescription(option[1] + "，筛选已加载评论");
            tab.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda4
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MusicCommentsActivity.this.m222lambda$updateTabs$7$localqingyinmobileMusicCommentsActivity(option, view);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$updateTabs$7$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m222lambda$updateTabs$7$localqingyinmobileMusicCommentsActivity(String[] option, View v) {
        if (this.filter.equals(option[0])) {
            return;
        }
        this.filter = option[0];
        updateTabs();
        applyFilter();
        this.list.setSelection(0);
    }

    private void buildComposer(LinearLayout linearLayout) {
        View line = new View(this);
        line.setBackgroundColor(-921358);
        linearLayout.addView(line, l(-1, 1));
        LinearLayout bar = row();
        pad(bar, 15, 8, 15, 9);
        bar.setBackgroundColor(-1);
        TextView compose = text("这首歌让你想到什么？", 14, -5592911, false);
        compose.setGravity(16);
        pad(compose, 15, 0, 5, 0);
        compose.setBackground(bg(-657929, 20));
        bar.addView(compose, new LinearLayout.LayoutParams(0, d(43.0f), 1.0f));
        compose.setContentDescription("输入评论");
        compose.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicCommentsActivity.this.m202xec17337e(view);
            }
        });
        TextView send = text("发表", 14, -10124632, true);
        send.setGravity(17);
        bar.addView(send, l(57, 43));
        send.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicCommentsActivity.this.m203xaf039cdd(view);
            }
        });
        linearLayout.addView(bar, l(-1, 61));
    }

    /* JADX INFO: renamed from: lambda$buildComposer$8$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m202xec17337e(View v) {
        compose(null);
    }

    /* JADX INFO: renamed from: lambda$buildComposer$9$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m203xaf039cdd(View v) {
        compose(null);
    }

    private void refresh() {
        this.revision++;
        this.busy = false;
        this.failed = false;
        this.hasMore = true;
        this.page = 0;
        this.total = -1;
        this.specialId = "";
        if (this.countLabel != null) {
            this.countLabel.setText("");
        }
        this.all.clear();
        this.shown.clear();
        this.seenIds.clear();
        this.replies.clear();
        this.expandedText.clear();
        this.adapter.notifyDataSetChanged();
        updateState();
        fetch(1);
        final int token = this.revision;
        this.jobs.execute(new Runnable() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda27
            @Override // java.lang.Runnable
            public final void run() {
                MusicCommentsActivity.this.m219lambda$refresh$11$localqingyinmobileMusicCommentsActivity(token);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$refresh$11$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m219lambda$refresh$11$localqingyinmobileMusicCommentsActivity(final int token) {
        int result = -1;
        try {
            JSONObject r = OnlineApi.call(getApplicationContext(), "songCommentCount", new JSONObject().put("hash", this.hash));
            result = KugouCommentModel.total(r.optJSONObject("raw"), 0);
        } catch (Exception e) {
        }
        final int value = result;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                MusicCommentsActivity.this.m218lambda$refresh$10$localqingyinmobileMusicCommentsActivity(token, value);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$refresh$10$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m218lambda$refresh$10$localqingyinmobileMusicCommentsActivity(int token, int value) {
        if (!this.alive || token != this.revision || value < 0) {
            return;
        }
        this.total = value;
        this.countLabel.setText(KugouCommentModel.compact(value) + " 条");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetch(final int next) {
        if (!this.alive || this.busy || !this.hasMore || next > 200) {
            return;
        }
        this.busy = true;
        this.failed = false;
        updateState();
        final int token = this.revision;
        this.jobs.execute(new Runnable() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MusicCommentsActivity.this.m211lambda$fetch$13$localqingyinmobileMusicCommentsActivity(next, token);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetch$13$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m211lambda$fetch$13$localqingyinmobileMusicCommentsActivity(final int next, final int token) {
        KugouCommentModel.Result result = null;
        String error = "";
        try {
            JSONObject reply = OnlineApi.call(getApplicationContext(), "songComments", new JSONObject().put("albumAudioId", this.audioId).put("page", next));
            JSONObject body = reply.optJSONObject("raw");
            if (body == null) {
                throw new Exception("未收到评论数据");
            }
            if (body.optInt("status", 1) == 0) {
                throw new Exception("酷狗评论服务暂不可用");
            }
            result = KugouCommentModel.parse(body);
            if (!result.recognized) {
                throw new Exception("评论格式暂不受支持");
            }
            final KugouCommentModel.Result incoming = result;
            final String failure = error;
            this.ui.post(new Runnable() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MusicCommentsActivity.this.m210lambda$fetch$12$localqingyinmobileMusicCommentsActivity(token, failure, incoming, next);
                }
            });
        } catch (Exception e) {
            error = e.getMessage() == null ? "网络连接异常" : e.getMessage();
        }
    }

    /* JADX INFO: renamed from: lambda$fetch$12$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m210lambda$fetch$12$localqingyinmobileMusicCommentsActivity(int token, String failure, KugouCommentModel.Result incoming, int next) {
        if (!this.alive || token != this.revision) {
            return;
        }
        boolean z = false;
        this.busy = false;
        if (!failure.isEmpty()) {
            this.failed = true;
            updateState(failure);
            return;
        }
        if (incoming == null) {
            this.failed = true;
            updateState("读取评论失败");
            return;
        }
        this.page = next;
        if (!incoming.specialId.isEmpty()) {
            this.specialId = incoming.specialId;
        }
        int before = this.all.size();
        for (KugouCommentModel.Comment c : incoming.items) {
            if (this.specialId.isEmpty() && !c.specialId.isEmpty()) {
                this.specialId = c.specialId;
            }
            if (this.seenIds.add(c.key())) {
                this.all.add(c);
            }
        }
        if (this.total < 0 && incoming.total >= 0) {
            this.total = incoming.total;
            this.countLabel.setText(KugouCommentModel.compact(this.total) + " 条");
        }
        if (incoming.rawCount >= 20 && this.all.size() > before && next < 200) {
            z = true;
        }
        this.hasMore = z;
        applyFilter();
        updateState();
    }

    private void applyFilter() {
        this.shown.clear();
        for (KugouCommentModel.Comment c : this.all) {
            if (!"image".equals(this.filter) || !c.images.isEmpty()) {
                if (!"reply".equals(this.filter) || c.replyCount > 0) {
                    this.shown.add(c);
                }
            }
        }
        if ("likes".equals(this.order)) {
            this.shown.sort(new Comparator() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda6
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return Long.compare(((KugouCommentModel.Comment) obj2).likes, ((KugouCommentModel.Comment) obj).likes);
                }
            });
        } else if ("recent".equals(this.order)) {
            this.shown.sort(new Comparator() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda7
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return Long.compare(KugouCommentModel.timeMillis(((KugouCommentModel.Comment) obj2).when), KugouCommentModel.timeMillis(((KugouCommentModel.Comment) obj).when));
                }
            });
        }
        this.adapter.notifyDataSetChanged();
        updateState();
    }

    private void updateState() {
        updateState("");
    }

    private void updateState(String reason) {
        String str;
        String message;
        String str2;
        if (this.empty == null) {
            return;
        }
        boolean noRows = this.shown.isEmpty();
        int i = 0;
        this.empty.setVisibility(noRows ? 0 : 8);
        this.list.setVisibility(noRows ? 8 : 0);
        if (noRows) {
            if (this.busy) {
                message = "正在获取真实评论…";
            } else if (this.failed) {
                message = "加载失败\n" + (reason.isEmpty() ? "请检查网络后重试" : reason);
            } else {
                message = this.all.isEmpty() ? "暂时还没有评论" : "已加载的评论中暂无此类内容\n仅筛选当前已加载评论，可继续加载更多";
            }
            this.emptyText.setText(message);
            this.emptyAction.setVisibility(this.busy ? 8 : 0);
            TextView textView = this.emptyAction;
            if (this.failed) {
                str2 = "重新加载";
            } else if (this.all.isEmpty()) {
                str2 = (!this.hasMore || this.page <= 0) ? "刷新评论" : "继续获取评论";
            } else {
                str2 = this.hasMore ? "继续加载此分类" : "查看全部";
            }
            textView.setText(str2);
            this.emptyAction.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda12
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MusicCommentsActivity.this.m221lambda$updateState$16$localqingyinmobileMusicCommentsActivity(view);
                }
            });
            this.indicator.setVisibility(this.busy ? 0 : 8);
        }
        if (this.loadMore != null) {
            TextView textView2 = this.loadMore;
            if (this.busy) {
                str = "正在加载评论…";
            } else if (this.failed) {
                str = "加载失败 · 点击重试";
            } else {
                str = this.hasMore ? "加载更多评论" : "已显示全部已获取评论";
            }
            textView2.setText(str);
            TextView textView3 = this.loadMore;
            if (noRows || (!this.busy && !this.failed && !this.hasMore)) {
                i = 8;
            }
            textView3.setVisibility(i);
        }
    }

    /* JADX INFO: renamed from: lambda$updateState$16$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m221lambda$updateState$16$localqingyinmobileMusicCommentsActivity(View v) {
        if (this.failed) {
            if (this.page == 0) {
                refresh();
                return;
            } else {
                this.failed = false;
                fetch(this.page + 1);
                return;
            }
        }
        if (!this.hasMore || this.page <= 0) {
            if (this.all.isEmpty()) {
                refresh();
                return;
            }
            this.filter = "all";
            updateTabs();
            applyFilter();
            return;
        }
        fetch(this.page + 1);
    }

    private FrameLayout avatar(KugouCommentModel.Comment c) {
        FrameLayout icon = Artwork.frame(this, 24);
        icon.setBackground(bg(-923408, 24));
        TextView initial = (TextView) icon.getChildAt(0);
        String first = c.name.isEmpty() ? "♫" : c.name.substring(0, c.name.offsetByCodePoints(0, 1));
        initial.setText(first);
        initial.setTextSize(17.0f);
        initial.setTextColor(-5406566);
        Artwork.bind(icon, c.avatar);
        return icon;
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class CommentRow extends LinearLayout {
        final TextView body;
        final TextView copy;
        final LinearLayout detail;
        final TextView expand;
        final LinearLayout gallery;
        String galleryKey;
        final TextView initial;
        final TextView likes;
        final TextView nickname;
        final FrameLayout portrait;
        final TextView reply;
        final TextView thread;
        final TextView when;

        CommentRow() {
            super(MusicCommentsActivity.this);
            this.galleryKey = "";
            setOrientation(1);
            MusicCommentsActivity.this.pad(this, 2, 13, 2, 9);
            LinearLayout linearLayoutRow = MusicCommentsActivity.this.row();
            linearLayoutRow.setGravity(48);
            this.portrait = Artwork.frame(MusicCommentsActivity.this, 24);
            this.initial = (TextView) this.portrait.getChildAt(0);
            linearLayoutRow.addView(this.portrait, MusicCommentsActivity.this.l(39, 39));
            LinearLayout linearLayoutStack = MusicCommentsActivity.this.stack();
            MusicCommentsActivity.this.pad(linearLayoutStack, 11, 0, 0, 0);
            LinearLayout top = MusicCommentsActivity.this.row();
            LinearLayout author = MusicCommentsActivity.this.stack();
            this.nickname = MusicCommentsActivity.this.text("", 14, -9276290, true);
            this.nickname.setSingleLine(true);
            this.nickname.setEllipsize(TextUtils.TruncateAt.END);
            author.addView(this.nickname, MusicCommentsActivity.this.l(-1, 22));
            this.when = MusicCommentsActivity.this.text("", 11, -5855570, false);
            author.addView(this.when, MusicCommentsActivity.this.l(-1, 17));
            top.addView(author, new LinearLayout.LayoutParams(0, -2, 1.0f));
            this.likes = MusicCommentsActivity.this.text("", 14, -6381141, false);
            this.likes.setGravity(53);
            MusicCommentsActivity.this.pad(this.likes, 6, 2, 0, 0);
            top.addView(this.likes, MusicCommentsActivity.this.l(-2, 38));
            linearLayoutStack.addView(top, MusicCommentsActivity.this.l(-1, -2));
            this.body = MusicCommentsActivity.this.text("", 15, -14144973, false);
            this.body.setLineSpacing(MusicCommentsActivity.this.d(3.0f), 1.04f);
            LinearLayout.LayoutParams bp = MusicCommentsActivity.this.l(-1, -2);
            bp.topMargin = MusicCommentsActivity.this.d(7.0f);
            linearLayoutStack.addView(this.body, bp);
            this.expand = MusicCommentsActivity.this.text("", 12, -10124632, true);
            MusicCommentsActivity.this.pad(this.expand, 0, 7, 0, 2);
            linearLayoutStack.addView(this.expand, MusicCommentsActivity.this.l(-1, 31));
            this.gallery = MusicCommentsActivity.this.stack();
            LinearLayout.LayoutParams gp = MusicCommentsActivity.this.l(-1, -2);
            gp.topMargin = MusicCommentsActivity.this.d(8.0f);
            linearLayoutStack.addView(this.gallery, gp);
            LinearLayout actions = MusicCommentsActivity.this.row();
            MusicCommentsActivity.this.pad(actions, 0, 7, 0, 0);
            this.reply = MusicCommentsActivity.this.text("回复", 13, -8289392, false);
            this.reply.setGravity(17);
            this.reply.setBackground(MusicCommentsActivity.this.bg(-723465, 14));
            actions.addView(this.reply, MusicCommentsActivity.this.l(57, 34));
            this.copy = MusicCommentsActivity.this.text("复制", 12, -6775898, false);
            this.copy.setGravity(17);
            LinearLayout.LayoutParams cp = MusicCommentsActivity.this.l(55, 34);
            cp.leftMargin = MusicCommentsActivity.this.d(7.0f);
            actions.addView(this.copy, cp);
            linearLayoutStack.addView(actions, MusicCommentsActivity.this.l(-1, -2));
            this.thread = MusicCommentsActivity.this.text("", 13, -10124632, false);
            this.thread.setGravity(16);
            linearLayoutStack.addView(this.thread, MusicCommentsActivity.this.l(-1, 37));
            this.detail = MusicCommentsActivity.this.stack();
            linearLayoutStack.addView(this.detail, MusicCommentsActivity.this.l(-1, -2));
            linearLayoutRow.addView(linearLayoutStack, new LinearLayout.LayoutParams(0, -2, 1.0f));
            addView(linearLayoutRow, MusicCommentsActivity.this.l(-1, -2));
            View divider = new View(MusicCommentsActivity.this);
            divider.setBackgroundColor(-921358);
            LinearLayout.LayoutParams sep = MusicCommentsActivity.this.l(-1, 1);
            sep.topMargin = MusicCommentsActivity.this.d(13.0f);
            addView(divider, sep);
        }

        void bind(final KugouCommentModel.Comment c) {
            int side;
            String first = c.name.isEmpty() ? "♫" : c.name.substring(0, c.name.offsetByCodePoints(0, 1));
            this.initial.setText(first);
            this.initial.setTextSize(17.0f);
            this.initial.setTextColor(-5340517);
            Artwork.bind(this.portrait, c.avatar);
            this.nickname.setText(c.name);
            this.when.setText(KugouCommentModel.date(c.when));
            this.likes.setText(c.likes > 0 ? "♡  " + KugouCommentModel.compact(c.likes) : "♡");
            this.likes.setContentDescription("当前点赞数" + c.likes + "，酷狗接口不支持在青音中点赞");
            this.body.setText(c.text);
            this.body.setOnLongClickListener(new View.OnLongClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$CommentRow$$ExternalSyntheticLambda0
                @Override // android.view.View.OnLongClickListener
                public final boolean onLongClick(View view) {
                    return CommentRow.this.m223x6446da62(c, view);
                }
            });
            boolean longText = c.text.length() > 170;
            final boolean opened = MusicCommentsActivity.this.expandedText.contains(c.key());
            this.body.setMaxLines((!longText || opened) ? Integer.MAX_VALUE : 5);
            this.body.setEllipsize((!longText || opened) ? null : TextUtils.TruncateAt.END);
            this.expand.setVisibility(longText ? 0 : 8);
            this.expand.setText(opened ? "收起全文  ⌃" : "展开全文  ⌄");
            this.expand.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$CommentRow$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    CommentRow.this.m224x8d9b2fa3(opened, c, view);
                }
            });
            if (!this.galleryKey.equals(c.key()) || this.gallery.getChildCount() == 0) {
                this.galleryKey = c.key();
                this.gallery.removeAllViews();
                int i = 9;
                int total = Math.min(9, c.images.size());
                if (total == 1) {
                    side = 174;
                } else {
                    side = total == 2 ? 124 : 94;
                }
                int start = 0;
                while (start < total) {
                    LinearLayout strip = MusicCommentsActivity.this.row();
                    int i2 = start;
                    while (i2 < Math.min(total, start + 3)) {
                        final String url = c.images.get(i2);
                        FrameLayout photo = Artwork.frame(MusicCommentsActivity.this, i);
                        Artwork.bind(photo, url);
                        LinearLayout.LayoutParams p = MusicCommentsActivity.this.l(side, side);
                        p.rightMargin = MusicCommentsActivity.this.d(6.0f);
                        p.bottomMargin = MusicCommentsActivity.this.d(6.0f);
                        strip.addView(photo, p);
                        photo.setContentDescription("打开第" + (i2 + 1) + "张评论图片");
                        photo.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$CommentRow$$ExternalSyntheticLambda2
                            @Override // android.view.View.OnClickListener
                            public final void onClick(View view) {
                                CommentRow.this.m225xb6ef84e4(url, view);
                            }
                        });
                        i2++;
                        i = 9;
                    }
                    this.gallery.addView(strip, MusicCommentsActivity.this.l(-1, -2));
                    start += 3;
                    i = 9;
                }
            }
            this.gallery.setVisibility(c.images.isEmpty() ? 8 : 0);
            this.reply.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$CommentRow$$ExternalSyntheticLambda3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    CommentRow.this.m226xe043da25(c, view);
                }
            });
            this.copy.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$CommentRow$$ExternalSyntheticLambda4
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    CommentRow.this.m227x9982f66(c, view);
                }
            });
            if (c.replyCount > 0) {
                ReplyState rs = (ReplyState) MusicCommentsActivity.this.replies.get(c.key());
                this.thread.setVisibility(0);
                this.thread.setText((rs == null || !rs.open) ? "查看 " + KugouCommentModel.compact(c.replyCount) + " 条回复  ›" : "收起 " + KugouCommentModel.compact(c.replyCount) + " 条回复  ⌃");
                this.thread.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$CommentRow$$ExternalSyntheticLambda5
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        CommentRow.this.m228x32ec84a7(c, view);
                    }
                });
                this.detail.removeAllViews();
                if (rs != null && rs.open) {
                    MusicCommentsActivity.this.addReplies(this.detail, c, rs);
                }
                this.detail.setVisibility((rs == null || !rs.open) ? 8 : 0);
                return;
            }
            this.thread.setVisibility(8);
            this.detail.removeAllViews();
            this.detail.setVisibility(8);
        }

        /* JADX INFO: renamed from: lambda$bind$0$local-qingyin-mobile-MusicCommentsActivity$CommentRow, reason: not valid java name */
        /* synthetic */ boolean m223x6446da62(KugouCommentModel.Comment c, View v) {
            MusicCommentsActivity.this.copy(c.text);
            return true;
        }

        /* JADX INFO: renamed from: lambda$bind$1$local-qingyin-mobile-MusicCommentsActivity$CommentRow, reason: not valid java name */
        /* synthetic */ void m224x8d9b2fa3(boolean opened, KugouCommentModel.Comment c, View v) {
            HashSet hashSet = MusicCommentsActivity.this.expandedText;
            String strKey = c.key();
            if (opened) {
                hashSet.remove(strKey);
            } else {
                hashSet.add(strKey);
            }
            MusicCommentsActivity.this.adapter.notifyDataSetChanged();
        }

        /* JADX INFO: renamed from: lambda$bind$2$local-qingyin-mobile-MusicCommentsActivity$CommentRow, reason: not valid java name */
        /* synthetic */ void m225xb6ef84e4(String url, View v) {
            MusicCommentsActivity.this.preview(url);
        }

        /* JADX INFO: renamed from: lambda$bind$3$local-qingyin-mobile-MusicCommentsActivity$CommentRow, reason: not valid java name */
        /* synthetic */ void m226xe043da25(KugouCommentModel.Comment c, View v) {
            MusicCommentsActivity.this.compose(new ReplyTarget(c, c.name));
        }

        /* JADX INFO: renamed from: lambda$bind$4$local-qingyin-mobile-MusicCommentsActivity$CommentRow, reason: not valid java name */
        /* synthetic */ void m227x9982f66(KugouCommentModel.Comment c, View v) {
            MusicCommentsActivity.this.copy(c.text);
        }

        /* JADX INFO: renamed from: lambda$bind$5$local-qingyin-mobile-MusicCommentsActivity$CommentRow, reason: not valid java name */
        /* synthetic */ void m228x32ec84a7(KugouCommentModel.Comment c, View v) {
            MusicCommentsActivity.this.toggleReplies(c);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public View commentView(View recycled, KugouCommentModel.Comment c) {
        CommentRow card = recycled instanceof CommentRow ? (CommentRow) recycled : new CommentRow();
        card.bind(c);
        return card;
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ReplyState {
        String error;
        boolean loading;
        boolean more;
        boolean open;
        int page;
        final ArrayList<KugouCommentModel.Comment> rows;

        private ReplyState() {
            this.more = true;
            this.error = "";
            this.rows = new ArrayList<>();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ReplyTarget {
        final KugouCommentModel.Comment comment;
        final String pid;
        final String quoted;
        final String toName;

        ReplyTarget(KugouCommentModel.Comment c, String name) {
            this(c, name, "", "");
        }

        ReplyTarget(KugouCommentModel.Comment c, String name, String replyId, String replyText) {
            this.comment = c;
            this.toName = name;
            this.pid = replyId;
            this.quoted = replyText;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleReplies(KugouCommentModel.Comment c) {
        ReplyState s = this.replies.get(c.key());
        if (s == null) {
            s = new ReplyState();
            this.replies.put(c.key(), s);
        }
        s.open = !s.open;
        this.adapter.notifyDataSetChanged();
        if (!s.open || s.page != 0 || s.loading) {
            return;
        }
        fetchReplies(c, s, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addReplies(LinearLayout linearLayout, final KugouCommentModel.Comment parent, final ReplyState state) {
        String str;
        LinearLayout linearLayoutStack = stack();
        pad(linearLayoutStack, 11, 11, 10, 10);
        linearLayoutStack.setBackground(bg(-592135, 12));
        for (final KugouCommentModel.Comment c : state.rows) {
            LinearLayout linearLayoutStack2 = stack();
            pad(linearLayoutStack2, 0, 4, 0, 10);
            LinearLayout meta = row();
            TextView nick = text(c.name, 12, -10124632, true);
            nick.setSingleLine(true);
            nick.setEllipsize(TextUtils.TruncateAt.END);
            meta.addView(nick, new LinearLayout.LayoutParams(0, d(20.0f), 1.0f));
            View date = text(KugouCommentModel.date(c.when), 10, -6907740, false);
            meta.addView(date, l(-2, 20));
            linearLayoutStack2.addView(meta, l(-1, 20));
            TextView line = text(c.text, 13, -10394511, false);
            line.setLineSpacing(d(3.0f), 1.04f);
            linearLayoutStack2.addView(line, l(-1, -2));
            linearLayoutStack2.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda11
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MusicCommentsActivity.this.m200lambda$addReplies$17$localqingyinmobileMusicCommentsActivity(parent, c, view);
                }
            });
            linearLayoutStack.addView(linearLayoutStack2);
        }
        if (state.loading) {
            str = "正在获取回复…";
        } else if (!state.error.isEmpty()) {
            str = "加载失败 · 点击重试";
        } else if (state.more) {
            str = "查看更多回复 ›";
        } else {
            str = state.rows.isEmpty() ? "暂无可显示的回复" : "没有更多回复";
        }
        TextView stateLine = text(str, 12, -10124632, false);
        pad(stateLine, 0, 6, 0, 6);
        stateLine.setGravity(16);
        linearLayoutStack.addView(stateLine, l(-1, 35));
        stateLine.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicCommentsActivity.this.m201lambda$addReplies$18$localqingyinmobileMusicCommentsActivity(state, parent, view);
            }
        });
        linearLayout.addView(linearLayoutStack, l(-1, -2));
    }

    /* JADX INFO: renamed from: lambda$addReplies$17$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m200lambda$addReplies$17$localqingyinmobileMusicCommentsActivity(KugouCommentModel.Comment parent, KugouCommentModel.Comment c, View v) {
        compose(new ReplyTarget(parent, c.name, c.tid, c.text));
    }

    /* JADX INFO: renamed from: lambda$addReplies$18$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m201lambda$addReplies$18$localqingyinmobileMusicCommentsActivity(ReplyState state, KugouCommentModel.Comment parent, View v) {
        if (state.loading) {
            return;
        }
        if (state.more || !state.error.isEmpty()) {
            fetchReplies(parent, state, state.page + 1);
        }
    }

    private void fetchReplies(final KugouCommentModel.Comment c, final ReplyState s, final int next) {
        final String sid = c.specialId.isEmpty() ? this.specialId : c.specialId;
        if (sid.isEmpty() || c.tid.isEmpty()) {
            s.error = "缺少楼中楼标识";
            s.more = false;
            this.adapter.notifyDataSetChanged();
        } else {
            if (s.loading || next > 200) {
                return;
            }
            s.loading = true;
            s.error = "";
            this.adapter.notifyDataSetChanged();
            final int token = this.revision;
            this.jobs.execute(new Runnable() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    MusicCommentsActivity.this.m213x69a67d4c(sid, c, next, token, s);
                }
            });
        }
    }

    /* JADX WARN: Code duplicated, block: B:26:0x0057  */
    /* JADX WARN: Code duplicated, block: B:27:0x005a  */
    /* JADX INFO: renamed from: lambda$fetchReplies$20$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m213x69a67d4c(String sid, KugouCommentModel.Comment c, final int next, final int token, final ReplyState state) {
        KugouCommentModel.Result incoming = null;
        String error = "";
        try {
            JSONObject response = OnlineApi.call(getApplicationContext(), "songReplies", new JSONObject().put("specialId", sid).put("tid", c.tid).put("page", next));
            incoming = KugouCommentModel.parse(response.optJSONObject("raw"));
            if (!incoming.recognized) throw new Exception("服务端未返回回复列表");
        } catch (Exception failure) { error = failure.getMessage() == null ? "网络异常" : failure.getMessage(); }
        final KugouCommentModel.Result result = incoming;
        final String message = error;
        ui.post(() -> m212xa9556f22(token, state, message, result, next));
    }

    /* JADX INFO: renamed from: lambda$fetchReplies$19$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m212xa9556f22(int token, ReplyState s, String failure, KugouCommentModel.Result result, int next) {
        if (!this.alive || token != this.revision) {
            return;
        }
        boolean z = false;
        s.loading = false;
        s.error = failure;
        if (failure.isEmpty() && result != null) {
            s.page = next;
            HashSet<String> known = new HashSet<>();
            for (KugouCommentModel.Comment previous : s.rows) {
                known.add(previous.key());
            }
            for (KugouCommentModel.Comment reply : result.items) {
                if (reply.tid.isEmpty() || known.add(reply.key())) {
                    s.rows.add(reply);
                }
            }
            if (result.rawCount >= 20 && next < 200) {
                z = true;
            }
            s.more = z;
        }
        this.adapter.notifyDataSetChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void copy(String string) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService("clipboard");
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("歌曲评论", string));
        }
        Toast.makeText(this, "已复制评论", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void preview(String url) {
        final Dialog dialog = new Dialog(this, 16973834);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(-15724268);
        final ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        final ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(-1710614));
        FrameLayout.LayoutParams center = new FrameLayout.LayoutParams(d(30.0f), d(30.0f), 17);
        frame.addView(loading, center);
        TextView close = text("×", 30, -1, false);
        close.setGravity(17);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(d(56.0f), d(56.0f), 53);
        cl.topMargin = d(30.0f);
        frame.addView(close, cl);
        close.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                dialog.dismiss();
            }
        });
        frame.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(frame);
        dialog.show();
        Artwork.loadBitmap(url, new Consumer() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda26
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MusicCommentsActivity.this.m217lambda$preview$23$localqingyinmobileMusicCommentsActivity(dialog, loading, image, (Bitmap) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$preview$23$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m217lambda$preview$23$localqingyinmobileMusicCommentsActivity(Dialog dialog, ProgressBar loading, ImageView image, Bitmap bitmap) {
        if (!this.alive || !dialog.isShowing()) {
            return;
        }
        loading.setVisibility(8);
        if (bitmap == null) {
            Toast.makeText(this, "图片暂时无法加载", 0).show();
        } else {
            image.setImageBitmap(bitmap);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void compose(final ReplyTarget target) {
        String str;
        String previous;
        if (target != null && (target.comment.tid.isEmpty() || (target.comment.specialId.isEmpty() && this.specialId.isEmpty()))) {
            Toast.makeText(this, "酷狗未提供这条评论的回复标识", 0).show();
            return;
        }
        if (target == null) {
            str = "draft_" + this.hash;
        } else {
            str = "reply_" + this.hash + "_" + target.comment.tid + (target.pid.isEmpty() ? "" : "_" + target.pid);
        }
        final String key = str;
        final SharedPreferences drafts = getPreferences(0);
        final Dialog dialog = new Dialog(this);
        LinearLayout linearLayoutStack = stack();
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(-1);
        float radius = d(22.0f);
        sheetBg.setCornerRadii(new float[]{radius, radius, radius, radius, 0.0f, 0.0f, 0.0f, 0.0f});
        linearLayoutStack.setBackground(sheetBg);
        pad(linearLayoutStack, 19, 11, 19, 17);
        LinearLayout bar = row();
        TextView cancel = text("取消", 14, -6907740, false);
        cancel.setGravity(16);
        bar.addView(cancel, l(54, 47));
        cancel.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda22
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                dialog.dismiss();
            }
        });
        TextView title = text(target == null ? "发表评论" : "回复 " + target.toName, 16, -14144973, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setGravity(17);
        bar.addView(title, new LinearLayout.LayoutParams(0, d(47.0f), 1.0f));
        final TextView publish = text("发布", 14, -1, true);
        publish.setGravity(17);
        publish.setBackground(bg(-10124632, 15));
        bar.addView(publish, l(66, 38));
        linearLayoutStack.addView(bar, l(-1, 49));
        final EditText input = new EditText(this);
        input.setTextSize(15.0f);
        input.setTextColor(-14144973);
        input.setHintTextColor(-5789261);
        input.setHint(target == null ? "这一刻，想和大家分享什么？" : "回复 " + target.toName + "…");
        input.setGravity(51);
        input.setMinLines(4);
        input.setMaxLines(8);
        input.setSingleLine(false);
        input.setBackground(bg(-526343, 14));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(300)});
        pad(input, 13, 14, 13, 12);
        LinearLayout.LayoutParams ip = l(-1, -2);
        ip.topMargin = d(12.0f);
        linearLayoutStack.addView(input, ip);
        String previous2 = drafts.getString(key, "");
        if (target == null && !this.draft.isEmpty()) {
            String previous3 = this.draft;
            previous = previous3;
        } else {
            previous = previous2;
        }
        input.setText(previous);
        input.setSelection(input.length());
        LinearLayout bottom = row();
        View notice = text("发布后同步到酷狗 · 请遵守社区规范", 11, -6907740, false);
        String previous4 = previous;
        bottom.addView(notice, new LinearLayout.LayoutParams(0, -2, 1.0f));
        final TextView count = text(input.length() + "/300", 11, -6907740, false);
        bottom.addView(count, l(-2, 33));
        linearLayoutStack.addView(bottom, l(-1, 39));
        final TextView failure = text("", 12, -4044961, false);
        failure.setVisibility(8);
        failure.setLineSpacing(d(3.0f), 1.0f);
        linearLayoutStack.addView(failure, l(-1, -2));
        input.addTextChangedListener(new TextWatcher() { // from class: local.qingyin.mobile.MusicCommentsActivity.3
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int c, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int n) {
                count.setText(s.length() + "/300");
                publish.setAlpha(s.toString().trim().isEmpty() ? 0.45f : 1.0f);
                drafts.edit().putString(key, s.toString()).apply();
                if (target == null) {
                    MusicCommentsActivity.this.draft = s.toString();
                }
                failure.setVisibility(8);
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        publish.setAlpha(previous4.trim().isEmpty() ? 0.45f : 1.0f);
        publish.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda23
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicCommentsActivity.this.m209lambda$compose$27$localqingyinmobileMusicCommentsActivity(input, publish, failure, target, dialog, drafts, key, view);
            }
        });
        dialog.setContentView(linearLayoutStack);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(17170445);
            window.setGravity(80);
            window.setLayout(-1, -2);
            window.setSoftInputMode(21);
        }
        input.requestFocus();
    }

    /* JADX INFO: renamed from: lambda$compose$27$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m209lambda$compose$27$localqingyinmobileMusicCommentsActivity(EditText input, final TextView publish, final TextView failure, final ReplyTarget target, final Dialog dialog, final SharedPreferences drafts, final String key, View v) {
        final String content = input.getText().toString().trim();
        if (content.isEmpty()) {
            input.setError("请输入评论内容");
            return;
        }
        if (publish.isEnabled()) {
            publish.setEnabled(false);
            publish.setAlpha(0.55f);
            publish.setText("发送中");
            failure.setVisibility(8);
            this.jobs.execute(new Runnable() { // from class: local.qingyin.mobile.MusicCommentsActivity$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    MusicCommentsActivity.this.m208lambda$compose$26$localqingyinmobileMusicCommentsActivity(content, target, dialog, publish, failure, drafts, key);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$compose$26$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m208lambda$compose$26$localqingyinmobileMusicCommentsActivity(String content, final ReplyTarget target, final Dialog dialog, final TextView publish, final TextView failure, final SharedPreferences drafts, final String key) {
        String error;
        try {
            JSONObject params = new JSONObject().put("content", content).put("title", this.songName);
            String method;
            if (target == null) {
                method = "postSongComment"; params.put("albumAudioId", this.audioId);
            } else {
                method = "postSongReply";
                params.put("specialId", target.comment.specialId.isEmpty() ? this.specialId : target.comment.specialId).put("tid", target.comment.tid);
                if (!target.pid.isEmpty() && !target.pid.equals(target.comment.tid))
                    params.put("pid", target.pid).put("replyUser", target.toName).put("replyContent", target.quoted);
            }
            JSONObject response = OnlineApi.call(getApplicationContext(), method, params);
            error = response.optBoolean("submitted", false) ? "" : "酷狗尚未确认发布结果，请检查后再重试";
        } catch (Exception problem) { error = problem.getMessage() == null ? "暂时无法连接酷狗评论服务" : problem.getMessage(); }
        final String result = error;
        this.ui.post(() -> m207lambda$compose$25$localqingyinmobileMusicCommentsActivity(dialog, publish, result, failure, drafts, key, target));
    }

    /* JADX INFO: renamed from: lambda$compose$25$local-qingyin-mobile-MusicCommentsActivity, reason: not valid java name */
    /* synthetic */ void m207lambda$compose$25$localqingyinmobileMusicCommentsActivity(Dialog dialog, TextView publish, String result, TextView failure, SharedPreferences drafts, String key, ReplyTarget target) {
        if (!this.alive || !dialog.isShowing()) {
            return;
        }
        publish.setEnabled(true);
        publish.setAlpha(1.0f);
        publish.setText("发布");
        if (!result.isEmpty()) {
            failure.setText(result + "\n内容已保留；如果超时，请先检查是否已经发布，避免重复提交。");
            failure.setVisibility(0);
            return;
        }
        drafts.edit().remove(key).apply();
        if (target == null) {
            this.draft = "";
        }
        dialog.dismiss();
        Toast.makeText(this, "酷狗已确认接收，显示可能稍有延迟", 1).show();
        refresh();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.alive = false;
        this.revision++;
        this.jobs.shutdownNow();
        super.onDestroy();
    }
}
