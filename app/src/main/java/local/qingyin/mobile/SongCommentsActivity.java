package local.qingyin.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class SongCommentsActivity extends Activity {
    private static final int MUTED = -6580320;
    private static final int PINK = -2991480;
    private static final int TEXT = -14146511;
    private long audioId;
    private LinearLayout filters;
    private LinearLayout list;
    private TextView more;
    private ScrollView scroll;
    private long specialId;
    private TextView status;
    private TextView totalLabel;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private final ArrayList<JSONObject> comments = new ArrayList<>();
    private String uri = "";
    private String hash = "";
    private String title = "";
    private String artist = "";
    private String cover = "";
    private String filter = "all";
    private int page = 0;
    private int generation = 0;
    private boolean alive = true;
    private boolean loading = false;
    private boolean hasMore = true;

    private int dp(int n) {
        return QingyinUi.dp(this, n);
    }

    private LinearLayout column() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(1);
        return r;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(0);
        r.setGravity(16);
        return r;
    }

    private TextView text(String value, int size, int ink, boolean heavy) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(ink);
        if (heavy) {
            t.setTypeface(Typeface.DEFAULT, 1);
        }
        return t;
    }

    private GradientDrawable rounded(int color, int radius) {
        return QingyinUi.rounded(this, color, radius);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle state) {
        super.onCreate(state);
        this.uri = getIntent().getStringExtra("track_uri");
        this.title = getIntent().getStringExtra("track_title");
        this.artist = getIntent().getStringExtra("track_artist");
        this.cover = getIntent().getStringExtra("track_cover");
        if (this.uri == null) {
            this.uri = "";
        }
        if (this.title == null) {
            this.title = "";
        }
        if (this.artist == null) {
            this.artist = "";
        }
        if (this.cover == null) {
            this.cover = "";
        }
        try {
            Uri u = Uri.parse(this.uri);
            if ("qingyin-online".equals(u.getScheme()) && "track".equals(u.getAuthority())) {
                this.hash = u.getLastPathSegment();
                if (this.hash != null && this.hash.matches("(?i)[0-9a-f]{32}")) {
                    this.audioId = Long.parseLong(u.getQueryParameter("audioId"));
                    if (this.audioId <= 0) {
                        throw new IllegalArgumentException();
                    }
                    getWindow().setStatusBarColor(-138259);
                    getWindow().setNavigationBarColor(-1);
                    getWindow().setSoftInputMode(16);
                    int flags = 8192 | 16;
                    getWindow().getDecorView().setSystemUiVisibility(flags);
                    LinearLayout linearLayoutColumn = column();
                    linearLayoutColumn.setBackgroundColor(-1);
                    setContentView(linearLayoutColumn);
                    LinearLayout linearLayoutColumn2 = column();
                    linearLayoutColumn2.setPadding(dp(14), dp(9), dp(14), dp(11));
                    linearLayoutColumn2.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-138258, -1}));
                    LinearLayout linearLayoutRow = row();
                    TextView back = text("‹", 35, -14146511, false);
                    back.setGravity(17);
                    back.setContentDescription("返回播放页");
                    linearLayoutRow.addView(back, new LinearLayout.LayoutParams(dp(43), dp(54)));
                    back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda20
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            SongCommentsActivity.this.m452lambda$onCreate$0$localqingyinmobileSongCommentsActivity(view);
                        }
                    });
                    FrameLayout thumb = Artwork.frame(this, 10);
                    Artwork.bind(thumb, this.cover);
                    linearLayoutRow.addView(thumb, new LinearLayout.LayoutParams(dp(45), dp(45)));
                    LinearLayout song = column();
                    song.setPadding(dp(12), 0, dp(3), 0);
                    TextView name = text(this.title, 17, -14146511, true);
                    name.setMaxLines(1);
                    name.setEllipsize(TextUtils.TruncateAt.END);
                    song.addView(name);
                    TextView singer = text(this.artist + " · 酷狗评论", 12, -7963000, false);
                    singer.setSingleLine(true);
                    singer.setEllipsize(TextUtils.TruncateAt.END);
                    song.addView(singer);
                    linearLayoutRow.addView(song, new LinearLayout.LayoutParams(0, -2, 1.0f));
                    TextView reload = text("↻", 28, -8358015, false);
                    reload.setGravity(17);
                    reload.setContentDescription("刷新评论");
                    linearLayoutRow.addView(reload, new LinearLayout.LayoutParams(dp(45), dp(48)));
                    reload.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda21
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            SongCommentsActivity.this.m453lambda$onCreate$1$localqingyinmobileSongCommentsActivity(view);
                        }
                    });
                    linearLayoutColumn2.addView(linearLayoutRow);
                    linearLayoutColumn.addView(linearLayoutColumn2, new LinearLayout.LayoutParams(-1, -2));
                    this.filters = row();
                    this.filters.setPadding(dp(17), dp(4), dp(17), dp(8));
                    linearLayoutColumn.addView(this.filters, new LinearLayout.LayoutParams(-1, dp(53)));
                    buildFilters();
                    LinearLayout description = row();
                    description.setPadding(dp(21), dp(2), dp(19), dp(6));
                    this.totalLabel = text("真实酷狗评论", 12, -6580320, false);
                    description.addView(this.totalLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));
                    View hint = text("向下浏览评论", 12, -6580320, false);
                    description.addView(hint);
                    linearLayoutColumn.addView(description);
                    this.scroll = new ScrollView(this);
                    this.scroll.setFillViewport(true);
                    this.scroll.setVerticalScrollBarEnabled(false);
                    this.list = column();
                    this.list.setPadding(dp(19), 0, dp(19), dp(14));
                    this.scroll.addView(this.list, new FrameLayout.LayoutParams(-1, -2));
                    linearLayoutColumn.addView(this.scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
                    LinearLayout footer = row();
                    footer.setPadding(dp(15), dp(9), dp(15), dp(12));
                    footer.setBackgroundColor(-66820);
                    TextView composer = text("这首歌让你想到什么？", 15, -6251610, false);
                    composer.setGravity(16);
                    composer.setPadding(dp(17), 0, dp(12), 0);
                    composer.setBackground(rounded(-791053, 16));
                    footer.addView(composer, new LinearLayout.LayoutParams(0, dp(49), 1.0f));
                    composer.setContentDescription("发表评论");
                    composer.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda22
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            SongCommentsActivity.this.m454lambda$onCreate$2$localqingyinmobileSongCommentsActivity(view);
                        }
                    });
                    TextView send = text("评论", 14, -2991480, true);
                    send.setGravity(17);
                    send.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda1
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            SongCommentsActivity.this.m455lambda$onCreate$3$localqingyinmobileSongCommentsActivity(view);
                        }
                    });
                    footer.addView(send, new LinearLayout.LayoutParams(dp(67), dp(49)));
                    linearLayoutColumn.addView(footer);
                    refresh();
                    return;
                }
                throw new IllegalArgumentException();
            }
            throw new IllegalArgumentException();
        } catch (Exception e) {
            Toast.makeText(this, "当前歌曲暂不支持酷狗评论", 1).show();
            finish();
        }
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m452lambda$onCreate$0$localqingyinmobileSongCommentsActivity(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m453lambda$onCreate$1$localqingyinmobileSongCommentsActivity(View v) {
        refresh();
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m454lambda$onCreate$2$localqingyinmobileSongCommentsActivity(View v) {
        compose(null);
    }

    /* JADX INFO: renamed from: lambda$onCreate$3$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m455lambda$onCreate$3$localqingyinmobileSongCommentsActivity(View v) {
        compose(null);
    }

    private void buildFilters() {
        this.filters.removeAllViews();
        String[][] options = {new String[]{"all", "全部"}, new String[]{"image", "有图"}, new String[]{"reply", "有回复"}};
        for (final String[] option : options) {
            boolean selected = option[0].equals(this.filter);
            TextView tab = text(option[1], 14, selected ? -2991480 : -7171941, selected);
            tab.setGravity(17);
            tab.setBackground(rounded(selected ? -3850 : -460550, 21));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(39), 1.0f);
            p.rightMargin = dp(9);
            this.filters.addView(tab, p);
            tab.setContentDescription(option[1] + "评论，筛选当前已加载内容");
            tab.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda17
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SongCommentsActivity.this.m440lambda$buildFilters$4$localqingyinmobileSongCommentsActivity(option, view);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$buildFilters$4$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m440lambda$buildFilters$4$localqingyinmobileSongCommentsActivity(String[] option, View v) {
        if (this.filter.equals(option[0])) {
            return;
        }
        this.filter = option[0];
        buildFilters();
        render();
    }

    private void refresh() {
        this.generation++;
        this.page = 0;
        this.hasMore = true;
        this.loading = false;
        this.specialId = 0L;
        this.comments.clear();
        render();
        fetch(1);
        final int token = this.generation;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                SongCommentsActivity.this.m457lambda$refresh$6$localqingyinmobileSongCommentsActivity(token);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$refresh$6$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m457lambda$refresh$6$localqingyinmobileSongCommentsActivity(final int token) {
        int count = -1;
        try {
            JSONObject params = new JSONObject().put("hash", this.hash);
            JSONObject raw = OnlineApi.call(getApplicationContext(), "songCommentCount", params).optJSONObject("raw");
            count = count(raw, 0);
        } catch (Exception e) {
        }
        final int value = count;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                SongCommentsActivity.this.m456lambda$refresh$5$localqingyinmobileSongCommentsActivity(token, value);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$refresh$5$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m456lambda$refresh$5$localqingyinmobileSongCommentsActivity(int token, int value) {
        if (!this.alive || token != this.generation) {
            return;
        }
        this.totalLabel.setText(value >= 0 ? "共 " + compact(value) + " 条评论 · 来自酷狗" : "评论来自酷狗音乐");
    }

    private void fetch(final int next) {
        if (!this.alive || this.loading || !this.hasMore || next > 30) {
            return;
        }
        this.loading = true;
        renderFooter();
        final int token = this.generation;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                SongCommentsActivity.this.m446lambda$fetch$8$localqingyinmobileSongCommentsActivity(next, token);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetch$8$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m446lambda$fetch$8$localqingyinmobileSongCommentsActivity(final int next, final int token) {
        JSONObject raw = null;
        String error = "";
        try {
            JSONObject req = new JSONObject().put("albumAudioId", this.audioId).put("page", next);
            raw = OnlineApi.call(getApplicationContext(), "songComments", req).optJSONObject("raw");
        } catch (Exception e) {
            error = e.getMessage() == null ? "网络暂不可用" : e.getMessage();
        }
        final JSONObject result = raw;
        final String failure = error;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                SongCommentsActivity.this.m445lambda$fetch$7$localqingyinmobileSongCommentsActivity(token, failure, result, next);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$fetch$7$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m445lambda$fetch$7$localqingyinmobileSongCommentsActivity(int token, String failure, JSONObject result, int next) {
        if (this.alive && token == this.generation) {
            this.loading = false;
            if (failure.isEmpty()) {
                JSONArray data = rows(result, 0);
                if (data == null) {
                    this.status.setText("酷狗未返回可识别的评论列表");
                    this.more.setText("重试");
                    this.more.setVisibility(0);
                    return;
                }
                long incoming = id(result);
                if (incoming > 0) {
                    this.specialId = incoming;
                }
                int seen = 0;
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item != null) {
                        long fromItem = id(item);
                        if (this.specialId <= 0 && fromItem > 0) {
                            this.specialId = fromItem;
                        }
                        this.comments.add(item);
                        seen++;
                    }
                }
                this.page = next;
                this.hasMore = data.length() >= 20 && this.page < 30;
                render();
                if (next == 1) {
                    this.scroll.smoothScrollTo(0, 0);
                    return;
                }
                return;
            }
            this.status.setText("加载失败：" + failure);
            this.more.setText("重试加载");
            this.more.setVisibility(0);
        }
    }

    private void render() {
        if (this.list == null) {
            return;
        }
        this.list.removeAllViews();
        int visible = 0;
        for (JSONObject c : this.comments) {
            if (!"image".equals(this.filter) || pictures(c).length() != 0) {
                if (!"reply".equals(this.filter) || replyCount(c) > 0) {
                    addComment(c);
                    visible++;
                }
            }
        }
        if (visible == 0) {
            TextView empty = text(this.comments.isEmpty() ? "正在读取酷狗评论…" : "已加载的评论中没有这一类型，继续加载可查看更多", 14, -6580320, false);
            empty.setGravity(17);
            empty.setPadding(dp(9), dp(42), dp(9), dp(42));
            this.list.addView(empty);
        }
        this.status = text("", 13, -6580320, false);
        this.status.setGravity(17);
        this.status.setPadding(dp(4), dp(12), dp(4), dp(13));
        this.list.addView(this.status);
        this.more = text("", 14, -2991480, true);
        this.more.setGravity(17);
        this.more.setBackground(rounded(-134923, 15));
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(-1, dp(45));
        ml.bottomMargin = dp(12);
        this.list.addView(this.more, ml);
        this.more.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SongCommentsActivity.this.m458lambda$render$9$localqingyinmobileSongCommentsActivity(view);
            }
        });
        renderFooter();
    }

    /* JADX INFO: renamed from: lambda$render$9$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m458lambda$render$9$localqingyinmobileSongCommentsActivity(View v) {
        fetch(this.page + 1);
    }

    private void renderFooter() {
        String str;
        if (this.status == null || this.more == null) {
            return;
        }
        TextView textView = this.status;
        if (this.loading) {
            str = "正在从酷狗加载…";
        } else if (this.comments.isEmpty()) {
            str = "暂时没有评论";
        } else {
            str = this.hasMore ? "已加载 " + this.comments.size() + " 条 · 筛选仅作用于已加载评论" : "已加载到末尾 · 共 " + this.comments.size() + " 条";
        }
        textView.setText(str);
        this.more.setText(this.loading ? "加载中…" : "加载更多评论");
        this.more.setVisibility((this.loading || !this.hasMore) ? 8 : 0);
    }

    private void addComment(JSONObject obj) {
        String avatarUrl;
        int i;
        LinearLayout linearLayoutColumn = column();
        linearLayoutColumn.setPadding(dp(2), dp(15), dp(2), dp(15));
        LinearLayout top = row();
        FrameLayout avatar = Artwork.frame(this, 22);
        String str = "pic";
        String avatarUrl2 = value(obj, "user_pic", "avatar", "headpic", "headimg", "pic", "user_avatar");
        JSONObject user = firstObject(obj, "user", "userinfo", "user_info", "uinfo");
        String avatarUrl3 = (!avatarUrl2.isEmpty() || user == null) ? avatarUrl2 : value(user, "pic", "avatar", "headimg");
        Artwork.bind(avatar, avatarUrl3);
        top.addView(avatar, new LinearLayout.LayoutParams(dp(43), dp(43)));
        LinearLayout meta = column();
        meta.setPadding(dp(12), 0, 0, 0);
        String nickname = value(obj, "nickname", "user_name", "username", "nick_name", "usernick");
        if (nickname.isEmpty() && user != null) {
            nickname = value(user, "nickname", "name", "username");
        }
        if (nickname.isEmpty()) {
            nickname = "酷狗用户";
        }
        final String nickname2 = nickname;
        TextView name = text(nickname2, 14, -7373176, true);
        meta.addView(name);
        String date = value(obj, "time", "addtime", "add_time", "create_time", "created_at", "pub_time");
        TextView when = text(date(date), 11, -5462098, false);
        meta.addView(when);
        top.addView(meta, new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayoutColumn.addView(top);
        final String body = value(obj, "content", "text", "comment", "content_text");
        if (!body.isEmpty()) {
            TextView message = text(body, 16, -14146511, false);
            message.setLineSpacing(dp(5), 1.0f);
            message.setPadding(dp(55), dp(9), dp(8), dp(9));
            linearLayoutColumn.addView(message, new LinearLayout.LayoutParams(-1, -2));
        }
        JSONArray images = pictures(obj);
        if (images.length() <= 0) {
            avatarUrl = avatarUrl3;
        } else {
            LinearLayout thumbnails = row();
            thumbnails.setPadding(dp(54), dp(6), 0, dp(6));
            int max = Math.min(images.length(), 3);
            int n = 0;
            while (n < max) {
                Object raw = images.opt(n);
                String url = raw instanceof JSONObject ? value((JSONObject) raw, "url", str, "image", "src", "original", "imgurl") : String.valueOf(raw);
                FrameLayout image = Artwork.frame(this, 10);
                Artwork.bind(image, url);
                String str2 = str;
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(106), 1.0f);
                ip.rightMargin = dp(7);
                thumbnails.addView(image, ip);
                final String finalUrl = url;
                image.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        SongCommentsActivity.this.m436lambda$addComment$10$localqingyinmobileSongCommentsActivity(finalUrl, view);
                    }
                });
                n++;
                max = max;
                avatarUrl3 = avatarUrl3;
                str = str2;
                user = user;
            }
            avatarUrl = avatarUrl3;
            linearLayoutColumn.addView(thumbnails);
        }
        final long tid = replyId(obj);
        final long resource = this.specialId > 0 ? this.specialId : id(obj);
        final int replies = replyCount(obj);
        LinearLayout actions = row();
        actions.setPadding(dp(55), dp(6), 0, 0);
        String likes = value(obj, "like", "likecount", "like_count", "likenum", "praise_count");
        TextView like = text("♡ " + (likes.isEmpty() ? "—" : likes), 13, -6580320, false);
        like.setContentDescription("酷狗点赞数，暂不支持点赞");
        actions.addView(like, new LinearLayout.LayoutParams(0, dp(36), 1.0f));
        TextView reply = text("回复", 13, -8357499, false);
        reply.setGravity(17);
        actions.addView(reply, new LinearLayout.LayoutParams(dp(52), dp(36)));
        reply.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SongCommentsActivity.this.m437lambda$addComment$11$localqingyinmobileSongCommentsActivity(resource, tid, nickname2, view);
            }
        });
        TextView copy = text("复制", 13, -5924189, false);
        copy.setGravity(17);
        actions.addView(copy, new LinearLayout.LayoutParams(dp(46), dp(36)));
        copy.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SongCommentsActivity.this.m438lambda$addComment$12$localqingyinmobileSongCommentsActivity(body, view);
            }
        });
        linearLayoutColumn.addView(actions);
        if (replies > 0) {
            final LinearLayout expansion = column();
            expansion.setPadding(dp(54), 0, dp(3), 0);
            final TextView expand = text("展开 " + compact(replies) + " 条回复  ⌄", 13, -2991480, false);
            expand.setPadding(0, dp(2), 0, dp(9));
            expansion.addView(expand);
            linearLayoutColumn.addView(expansion);
            i = -1;
            expand.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda16
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SongCommentsActivity.this.m439lambda$addComment$13$localqingyinmobileSongCommentsActivity(expansion, expand, replies, resource, tid, view);
                }
            });
        } else {
            i = -1;
        }
        this.list.addView(linearLayoutColumn);
        View separator = new View(this);
        separator.setBackgroundColor(-921615);
        this.list.addView(separator, new LinearLayout.LayoutParams(i, dp(1)));
    }

    /* JADX INFO: renamed from: lambda$addComment$10$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m436lambda$addComment$10$localqingyinmobileSongCommentsActivity(String finalUrl, View v) {
        preview(finalUrl);
    }

    /* JADX INFO: renamed from: lambda$addComment$11$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m437lambda$addComment$11$localqingyinmobileSongCommentsActivity(long resource, long tid, String displayName, View v) {
        compose(new ReplyTarget(resource, tid, displayName));
    }

    /* JADX INFO: renamed from: lambda$addComment$12$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m438lambda$addComment$12$localqingyinmobileSongCommentsActivity(String body, View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService("clipboard");
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("酷狗评论", body));
        }
        Toast.makeText(this, "评论已复制", 0).show();
    }

    /* JADX INFO: renamed from: lambda$addComment$13$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m439lambda$addComment$13$localqingyinmobileSongCommentsActivity(LinearLayout expansion, TextView expand, int replies, long parent, long tid, View v) {
        if (expansion.getChildCount() > 1) {
            while (expansion.getChildCount() > 1) {
                expansion.removeViewAt(1);
            }
            expand.setText("展开 " + compact(replies) + " 条回复  ⌄");
        } else {
            if (parent <= 0 || tid <= 0) {
                Toast.makeText(this, "酷狗未提供这条评论的回复标识", 0).show();
                return;
            }
            expand.setText("收起回复  ⌃");
            LinearLayout inner = column();
            expansion.addView(inner);
            loadReplies(parent, tid, 1, inner, expand);
        }
    }

    private void loadReplies(final long special, final long tid, final int next, final LinearLayout target, final TextView toggle) {
        final TextView busy = text("正在获取酷狗回复…", 12, -6580320, false);
        busy.setPadding(dp(5), dp(10), 0, dp(10));
        target.addView(busy);
        final int token = this.generation;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                SongCommentsActivity.this.m451lambda$loadReplies$18$localqingyinmobileSongCommentsActivity(special, tid, next, token, target, busy, toggle);
            }
        });
    }

    /* JADX WARN: Code duplicated, block: B:18:0x0048  */
    /* JADX WARN: Code duplicated, block: B:19:0x004b  */
    /* JADX INFO: renamed from: lambda$loadReplies$18$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m451lambda$loadReplies$18$localqingyinmobileSongCommentsActivity(final long special, final long tid, final int next, final int token, final LinearLayout target, final TextView busy, final TextView toggle) {
        JSONArray items = null;
        String failure = "";
        try {
            JSONObject query = new JSONObject().put("specialId", special).put("tid", String.valueOf(tid)).put("page", next);
            JSONObject raw = OnlineApi.call(getApplicationContext(), "songReplies", query).optJSONObject("raw");
            items = rows(raw, 0);
        } catch (Exception problem) { failure = problem.getMessage() == null ? "请求失败" : problem.getMessage(); }
        final JSONArray data = items; final String error = failure;
        this.ui.post(() -> m450lambda$loadReplies$17$localqingyinmobileSongCommentsActivity(token, target, busy, error, data, special, tid, next, toggle));
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r9v0 */
    /* JADX WARN: Type inference failed for: r9v2, types: [boolean] */
    /* JADX WARN: Type inference failed for: r9v5 */
    /* JADX INFO: renamed from: lambda$loadReplies$17$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m450lambda$loadReplies$17$localqingyinmobileSongCommentsActivity(int token, final LinearLayout target, TextView busy, String err, JSONArray data, final long special, final long tid, final int next, final TextView toggle) {
        if (this.alive && token == this.generation && target.getParent() != null) {
            target.removeView(busy);
            int r9 = 0;
            if (!err.isEmpty() || data == null) {
                final TextView failed = text("回复加载失败 · 点击重试", 12, -2991480, false);
                target.addView(failed);
                failed.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda2
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        SongCommentsActivity.this.m447lambda$loadReplies$14$localqingyinmobileSongCommentsActivity(target, failed, special, tid, next, toggle, view);
                    }
                });
                return;
            }
            int i = 0;
            while (i < data.length()) {
                JSONObject entry = data.optJSONObject(i);
                if (entry != null) {
                    String[] strArr = new String[4];
                    strArr[r9] = "nickname";
                    strArr[1] = "user_name";
                    strArr[2] = "username";
                    strArr[3] = "nick_name";
                    String user = value(entry, strArr);
                    if (user.isEmpty()) {
                        user = "酷狗用户";
                    }
                    final String user2 = user;
                    String[] strArr2 = new String[3];
                    strArr2[r9] = "content";
                    strArr2[1] = "text";
                    strArr2[2] = "comment";
                    String content = value(entry, strArr2);
                    TextView reply = text(user2 + "：" + content, 13, -9936276, false);
                    reply.setLineSpacing(dp(3), 1.0f);
                    reply.setPadding(dp(8), dp(8), dp(8), dp(9));
                    reply.setBackground(rounded(-526857, 8));
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                    p.bottomMargin = dp(5);
                    target.addView(reply, p);
                    reply.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda3
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            SongCommentsActivity.this.m448lambda$loadReplies$15$localqingyinmobileSongCommentsActivity(special, tid, user2, view);
                        }
                    });
                }
                i++;
                r9 = 0;
            }
            if (data.length() >= 20 && next < 30) {
                final TextView moreReplies = text("查看更多回复 ›", 12, -2991480, true);
                moreReplies.setPadding(dp(8), dp(8), 0, dp(10));
                target.addView(moreReplies);
                moreReplies.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda4
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        SongCommentsActivity.this.m449lambda$loadReplies$16$localqingyinmobileSongCommentsActivity(target, moreReplies, special, tid, next, toggle, view);
                    }
                });
                return;
            }
            if (data.length() == 0 && next == 1) {
                TextView no = text("酷狗暂无可显示的回复", 12, -6580320, false);
                target.addView(no);
            }
        }
    }

    /* JADX INFO: renamed from: lambda$loadReplies$14$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m447lambda$loadReplies$14$localqingyinmobileSongCommentsActivity(LinearLayout target, TextView failed, long special, long tid, int next, TextView toggle, View v) {
        target.removeView(failed);
        loadReplies(special, tid, next, target, toggle);
    }

    /* JADX INFO: renamed from: lambda$loadReplies$15$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m448lambda$loadReplies$15$localqingyinmobileSongCommentsActivity(long special, long tid, String who, View v) {
        compose(new ReplyTarget(special, tid, who));
    }

    /* JADX INFO: renamed from: lambda$loadReplies$16$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m449lambda$loadReplies$16$localqingyinmobileSongCommentsActivity(LinearLayout target, TextView moreReplies, long special, long tid, int next, TextView toggle, View v) {
        target.removeView(moreReplies);
        loadReplies(special, tid, next + 1, target, toggle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ReplyTarget {
        final long special;
        final long tid;
        final String username;

        ReplyTarget(long s, long t, String name) {
            this.special = s;
            this.tid = t;
            this.username = name;
        }
    }

    private void compose(final ReplyTarget reply) {
        if (reply != null && (reply.special <= 0 || reply.tid <= 0)) {
            Toast.makeText(this, "酷狗尚未提供这条评论的可回复标识", 1).show();
            return;
        }
        final EditText edit = new EditText(this);
        edit.setTextSize(16.0f);
        edit.setTextColor(-14146511);
        edit.setHint("文明交流，分享听歌感受");
        edit.setMinLines(3);
        edit.setMaxLines(6);
        edit.setGravity(48);
        edit.setPadding(dp(16), dp(18), dp(16), dp(14));
        edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(300)});
        LinearLayout frame = column();
        frame.setPadding(dp(16), 0, dp(16), 0);
        frame.addView(edit);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle(reply == null ? "发表评论" : "回复 " + reply.username).setView(frame).setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("发送", (DialogInterface.OnClickListener) null).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda18
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                SongCommentsActivity.this.m444lambda$compose$22$localqingyinmobileSongCommentsActivity(dialog, edit, reply, dialogInterface);
            }
        });
        dialog.show();
    }

    /* JADX INFO: renamed from: lambda$compose$22$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m444lambda$compose$22$localqingyinmobileSongCommentsActivity(final AlertDialog dialog, final EditText edit, final ReplyTarget reply, DialogInterface v) {
        final Button send = dialog.getButton(-1);
        send.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SongCommentsActivity.this.m443lambda$compose$21$localqingyinmobileSongCommentsActivity(edit, send, reply, dialog, view);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$compose$21$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m443lambda$compose$21$localqingyinmobileSongCommentsActivity(EditText edit, final Button send, final ReplyTarget reply, final AlertDialog dialog, View clicked) {
        final String content = edit.getText().toString().trim();
        if (content.isEmpty()) {
            edit.setError("请输入评论内容");
            return;
        }
        send.setEnabled(false);
        send.setText("提交中…");
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                SongCommentsActivity.this.m442lambda$compose$20$localqingyinmobileSongCommentsActivity(content, reply, dialog, send);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$compose$20$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m442lambda$compose$20$localqingyinmobileSongCommentsActivity(String content, ReplyTarget reply, final AlertDialog dialog, final Button send) {
        String method;
        String err = "";
        try {
            JSONObject params = new JSONObject().put("content", content).put("title", this.title);
            if (reply == null) {
                method = "postSongComment";
                params.put("albumAudioId", this.audioId);
            } else {
                method = "postSongReply";
                params.put("specialId", reply.special).put("tid", String.valueOf(reply.tid));
            }
            JSONObject result = OnlineApi.call(getApplicationContext(), method, params);
            if (!result.optBoolean("submitted", false)) {
                err = "服务端尚未确认发布";
            }
        } catch (Exception e) {
            err = e.getMessage() == null ? "发布失败" : e.getMessage();
        }
        final String message = err;
        this.ui.post(new Runnable() { // from class: local.qingyin.mobile.SongCommentsActivity$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                SongCommentsActivity.this.m441lambda$compose$19$localqingyinmobileSongCommentsActivity(dialog, send, message);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$compose$19$local-qingyin-mobile-SongCommentsActivity, reason: not valid java name */
    /* synthetic */ void m441lambda$compose$19$localqingyinmobileSongCommentsActivity(AlertDialog dialog, Button send, String message) {
        if (!this.alive || !dialog.isShowing()) {
            return;
        }
        send.setEnabled(true);
        send.setText("发送");
        if (!message.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("发送未完成").setMessage(message).setPositiveButton("确定", (DialogInterface.OnClickListener) null).show();
            return;
        }
        dialog.dismiss();
        Toast.makeText(this, "酷狗已确认发布，正在刷新", 0).show();
        refresh();
    }

    private void preview(String url) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        FrameLayout frame = Artwork.frame(this, 14);
        Artwork.bind(frame, url);
        frame.setBackgroundColor(-1579544);
        dialog.setView(frame, dp(8), dp(8), dp(8), dp(8));
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(28), dp(360));
        }
    }

    private static JSONObject firstObject(JSONObject obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String key : keys) {
            JSONObject child = obj.optJSONObject(key);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private static String value(JSONObject obj, String... keys) {
        if (obj == null) {
            return "";
        }
        for (String key : keys) {
            Object v = obj.opt(key);
            if ((v instanceof String) && !((String) v).trim().isEmpty()) {
                return ((String) v).trim();
            }
            if (v instanceof Number) {
                return String.valueOf(v);
            }
        }
        return "";
    }

    private static JSONArray rows(JSONObject obj, int depth) {
        if (obj == null || depth > 4) {
            return null;
        }
        String[] strArr = {"list", "comments", "comment_list", "info", "rows", "items", "records"};
        for (int i = 0; i < 7; i++) {
            String key = strArr[i];
            JSONArray arr = obj.optJSONArray(key);
            if (arr != null) {
                return arr;
            }
        }
        String[] strArr2 = {"data", "body", "result", "comment"};
        for (int i2 = 0; i2 < 4; i2++) {
            String key2 = strArr2[i2];
            JSONObject inner = obj.optJSONObject(key2);
            JSONArray arr2 = rows(inner, depth + 1);
            if (arr2 != null) {
                return arr2;
            }
        }
        return null;
    }

    private static JSONArray pictures(JSONObject obj) {
        if (obj == null) {
            return new JSONArray();
        }
        String[] strArr = {"images", "pics", "pic_list", "img_list", "image_list", "image"};
        for (int i = 0; i < 6; i++) {
            String key = strArr[i];
            JSONArray arr = obj.optJSONArray(key);
            if (arr != null) {
                return arr;
            }
            String single = value(obj, key);
            if (single.startsWith("https://")) {
                return new JSONArray().put(single);
            }
        }
        return new JSONArray();
    }

    private static long number(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static long id(JSONObject obj) {
        if (obj == null) {
            return 0L;
        }
        long v = number(value(obj, "childrenid", "special_child_id", "special_id"));
        if (v > 0) {
            return v;
        }
        JSONObject inner = obj.optJSONObject("data");
        if (inner == null) {
            return 0L;
        }
        return id(inner);
    }

    private static long replyId(JSONObject obj) {
        return number(value(obj, "tid", "comment_id", "commentid", "id"));
    }

    private static int replyCount(JSONObject obj) {
        return (int) Math.max(0L, Math.min(1000000L, number(value(obj, "reply_count", "reply_num", "replynum", "replycnt", "replyCount", "sub_count"))));
    }

    private static int count(JSONObject obj, int depth) {
        if (obj == null || depth > 3) {
            return -1;
        }
        String[] strArr = {"total", "count", "commentcount", "comment_count", "comments_num", "num"};
        for (int i = 0; i < 6; i++) {
            String key = strArr[i];
            long n = number(value(obj, key));
            if (n > 0) {
                return (int) Math.min(2147483647L, n);
            }
        }
        JSONObject inner = obj.optJSONObject("data");
        if (inner == null) {
            return -1;
        }
        return count(inner, depth + 1);
    }

    private static String compact(long n) {
        return n >= 10000 ? String.format(Locale.CHINA, "%.1f万", Float.valueOf(n / 10000.0f)) : String.valueOf(n);
    }

    private static String date(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        try {
            long value = Long.parseLong(raw);
            if (value > 1000000000 && value < 100000000000L) {
                value *= 1000;
            }
            if (value >= 100000000000L) {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(value));
            }
        } catch (Exception e) {
        }
        return raw.length() > 24 ? raw.substring(0, 24) : raw;
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.alive = false;
        this.generation++;
        this.worker.shutdownNow();
        super.onDestroy();
    }
}
