package local.qingyin.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class ProfileActivity extends Activity {
    private TextView accountId;
    private TextView accountState;
    private volatile boolean alive;
    private TextView appearanceStatus;
    private TextView nickname;
    private int requestId;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private int dp(int n) {
        return QingyinUi.dp(this, n);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = QingyinUi.text(this, value, size, color, bold);
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout col() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(1);
        return c;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(0);
        r.setGravity(16);
        return r;
    }

    private void open(String page) {
        startActivity(new Intent(this, (Class<?>) OnlineActivity.class).putExtra("page", page));
    }

    @Override // android.app.Activity
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(-1);
        getWindow().setNavigationBarColor(-1);
        getWindow().getDecorView().setSystemUiVisibility(8208);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setBackgroundColor(-1);
        LinearLayout linearLayoutCol = col();
        linearLayoutCol.setPadding(dp(22), dp(14), dp(22), dp(110));
        scrollView.addView(linearLayoutCol, new FrameLayout.LayoutParams(-1, -2));
        setContentView(scrollView);
        MusicOverlayDock.attach(this);
        LinearLayout toolbar = row();
        TextView back = text("‹", 35, -12895933, true);
        back.setGravity(17);
        back.setContentDescription("返回主页");
        back.setBackground(QingyinUi.touch(this, -593419, 24.0f));
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ProfileActivity.this.m367lambda$onCreate$0$localqingyinmobileProfileActivity(view);
            }
        });
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView heading = text("个人资料", 23, -15197924, true);
        heading.setGravity(16);
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(0, dp(46), 1.0f);
        headingLp.leftMargin = dp(16);
        toolbar.addView(heading, headingLp);
        linearLayoutCol.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));
        LinearLayout linearLayoutCol2 = col();
        linearLayoutCol2.setGravity(1);
        LinearLayout.LayoutParams identityLp = new LinearLayout.LayoutParams(-1, -2);
        identityLp.topMargin = dp(29);
        linearLayoutCol.addView(linearLayoutCol2, identityLp);
        FrameLayout portrait = new FrameLayout(this);
        portrait.setBackground(QingyinUi.gradient(this, -1319704, -2761482, 40.0f));
        TextView character = text("青", 35, -10002827, true);
        character.setGravity(17);
        portrait.addView(character, new FrameLayout.LayoutParams(-1, -1));
        linearLayoutCol2.addView(portrait, new LinearLayout.LayoutParams(dp(92), dp(92)));
        this.nickname = text("青音用户", 24, -15000801, true);
        this.nickname.setGravity(17);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(-1, -2);
        nameLp.topMargin = dp(16);
        linearLayoutCol2.addView(this.nickname, nameLp);
        this.accountState = text("正在读取账号…", 13, -8815739, false);
        this.accountState.setGravity(17);
        LinearLayout.LayoutParams stateLp = new LinearLayout.LayoutParams(-1, -2);
        stateLp.topMargin = dp(7);
        linearLayoutCol2.addView(this.accountState, stateLp);
        LinearLayout section = col();
        section.setPadding(dp(20), dp(18), dp(20), dp(19));
        section.setBackground(QingyinUi.rounded(this, -593162, 22.0f));
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(-1, -2);
        sectionLp.topMargin = dp(32);
        linearLayoutCol.addView(section, sectionLp);
        View headingSmall = text("账号信息", 15, -8818309, true);
        section.addView(headingSmall);
        TextView headingSmall2 = text("账号信息获取中", 15, -13423049, false);
        this.accountId = headingSmall2;
        LinearLayout.LayoutParams idLp = new LinearLayout.LayoutParams(-1, -2);
        idLp.topMargin = dp(13);
        section.addView(this.accountId, idLp);
        action(linearLayoutCol, "外观与背景", "A / B / C · 随时切换", new Runnable() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                ProfileActivity.this.chooseAppearance();
            }
        });
        action(linearLayoutCol, "我的歌单", "查看已同步歌单", new Runnable() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                ProfileActivity.this.m368lambda$onCreate$1$localqingyinmobileProfileActivity();
            }
        });
        action(linearLayoutCol, "收藏歌曲", "我的喜欢", new Runnable() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                ProfileActivity.this.m369lambda$onCreate$2$localqingyinmobileProfileActivity();
            }
        });
        action(linearLayoutCol, "账号管理", "登录、切换或退出账号", new Runnable() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                ProfileActivity.this.m370lambda$onCreate$3$localqingyinmobileProfileActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m367lambda$onCreate$0$localqingyinmobileProfileActivity(View v) {
        finish();
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m368lambda$onCreate$1$localqingyinmobileProfileActivity() {
        open("mine");
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m369lambda$onCreate$2$localqingyinmobileProfileActivity() {
        open("favorites");
    }

    /* JADX INFO: renamed from: lambda$onCreate$3$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m370lambda$onCreate$3$localqingyinmobileProfileActivity() {
        open("account");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void chooseAppearance() {
        final String[] values = {"A", "B", "C"};
        new AlertDialog.Builder(this).setTitle("外观与背景").setSingleChoiceItems(AppearancePreferences.LABELS, AppearancePreferences.index(this), new DialogInterface.OnClickListener() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ProfileActivity.this.m366lambda$chooseAppearance$4$localqingyinmobileProfileActivity(values, dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$chooseAppearance$4$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m366lambda$chooseAppearance$4$localqingyinmobileProfileActivity(String[] values, DialogInterface dialog, int selected) {
        AppearancePreferences.set(this, values[selected]);
        dialog.dismiss();
        finish();
    }

    private void action(LinearLayout linearLayout, String title, String subtitle, final Runnable callback) {
        LinearLayout linearLayoutRow = row();
        linearLayoutRow.setPadding(dp(16), dp(12), dp(14), dp(12));
        linearLayoutRow.setBackground(QingyinUi.touch(this, -395270, 19.0f));
        LinearLayout words = col();
        words.addView(text(title, 17, -14080467, true));
        TextView sub = text(subtitle, 12, -8225658, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(4);
        words.addView(sub, subLp);
        linearLayoutRow.addView(words, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView arrow = text("›", 26, -7501933, false);
        arrow.setGravity(17);
        linearLayoutRow.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(43)));
        linearLayoutRow.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                callback.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(76));
        lp.topMargin = dp(12);
        linearLayout.addView(linearLayoutRow, lp);
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        this.alive = true;
        final int ticket = this.requestId + 1;
        this.requestId = ticket;
        this.worker.execute(new Runnable() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ProfileActivity.this.m373lambda$onStart$8$localqingyinmobileProfileActivity(ticket);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onStart$8$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m373lambda$onStart$8$localqingyinmobileProfileActivity(final int ticket) {
        try {
            final JSONObject data = OnlineApi.call(getApplicationContext(), "status", new JSONObject());
            runOnUiThread(new Runnable() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ProfileActivity.this.m371lambda$onStart$6$localqingyinmobileProfileActivity(ticket, data);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: local.qingyin.mobile.ProfileActivity$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ProfileActivity.this.m372lambda$onStart$7$localqingyinmobileProfileActivity(ticket);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$onStart$6$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m371lambda$onStart$6$localqingyinmobileProfileActivity(int ticket, JSONObject data) {
        if (!this.alive || ticket != this.requestId || isDestroyed()) {
            return;
        }
        if (!data.optBoolean("loggedIn", false)) {
            this.nickname.setText("青音用户");
            this.accountState.setText("尚未登录");
            this.accountId.setText("登录后查看账号信息");
        } else {
            JSONObject u = data.optJSONObject("user");
            String name = u == null ? "" : u.optString("name", u.optString("nickname", "")).trim();
            String id = u != null ? u.optString("id", "") : "";
            this.nickname.setText(name.isEmpty() ? "已登录用户" : name);
            this.accountState.setText("已连接酷狗账号");
            this.accountId.setText(id.isEmpty() ? "账号已连接" : "酷狗 ID · " + id);
        }
    }

    /* JADX INFO: renamed from: lambda$onStart$7$local-qingyin-mobile-ProfileActivity, reason: not valid java name */
    /* synthetic */ void m372lambda$onStart$7$localqingyinmobileProfileActivity(int ticket) {
        if (!this.alive || ticket != this.requestId || isDestroyed()) {
            return;
        }
        this.accountState.setText("账号状态暂不可用");
        this.accountId.setText("点击账号管理重试");
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.alive = false;
        this.requestId++;
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.worker.shutdownNow();
        super.onDestroy();
    }
}
