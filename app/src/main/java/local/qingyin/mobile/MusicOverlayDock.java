package local.qingyin.mobile;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/* JADX INFO: loaded from: classes2.dex */
final class MusicOverlayDock implements Application.ActivityLifecycleCallbacks {
    private final Activity activity;
    private TextView artist;
    private final View backdrop;
    private boolean blurRegistered;
    private boolean bound;
    private boolean closed;
    private FrameLayout cover;
    private LinearLayout dock;
    private FrameLayout miniBox;
    private ImageView play;
    private PlaybackService player;
    private boolean requested;
    private boolean resumed;
    private TextView title;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final FrostedGlassDrawable[] surfaces = new FrostedGlassDrawable[3];
    private final ViewTreeObserver.OnScrollChangedListener scrollListener = new ViewTreeObserver.OnScrollChangedListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda0
        @Override // android.view.ViewTreeObserver.OnScrollChangedListener
        public final void onScrollChanged() {
            MusicOverlayDock.this.refresh();
        }
    };
    private String lastCover = "";
    private final Runnable ticker = new Runnable() { // from class: local.qingyin.mobile.MusicOverlayDock.1
        @Override // java.lang.Runnable
        public void run() {
            if (MusicOverlayDock.this.closed || !MusicOverlayDock.this.resumed) {
                return;
            }
            MusicOverlayDock.this.update();
            MusicOverlayDock.this.ui.postDelayed(this, 1000L);
        }
    };
    private final ServiceConnection service = new ServiceConnection() { // from class: local.qingyin.mobile.MusicOverlayDock.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicOverlayDock.this.player = ((PlaybackService.LocalBinder) binder).service();
            MusicOverlayDock.this.bound = true;
            MusicOverlayDock.this.update();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            MusicOverlayDock.this.bound = false;
            MusicOverlayDock.this.player = null;
            MusicOverlayDock.this.update();
        }
    };

    private int dp(int value) {
        return QingyinUi.dp(this.activity, value);
    }

    static void attach(Activity owner) {
        new MusicOverlayDock(owner);
    }

    private MusicOverlayDock(final Activity owner) {
        this.activity = owner;
        FrameLayout window = (FrameLayout) owner.findViewById(16908290);
        if (window == null || window.getChildCount() < 1) {
            this.backdrop = null;
            return;
        }
        this.backdrop = window.getChildAt(0);
        if (this.backdrop instanceof ScrollView) {
            ScrollView scroller = (ScrollView) this.backdrop;
            scroller.setPadding(scroller.getPaddingLeft(), scroller.getPaddingTop(), scroller.getPaddingRight(), scroller.getPaddingBottom() + dp(78));
            scroller.setClipToPadding(false);
        }
        this.dock = new LinearLayout(owner);
        this.dock.setGravity(16);
        this.dock.setPadding(dp(13), 0, dp(13), dp(10));
        FrameLayout.LayoutParams outer = new FrameLayout.LayoutParams(-1, dp(79), 80);
        window.addView(this.dock, outer);
        FrameLayout library = circle("▤", "打开资料库", 0, -1359614990, 0);
        library.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                Activity activity = owner;
                activity.startActivity(new Intent(activity, (Class<?>) IndexActivity.class).putExtra("start_tab", 3).addFlags(603979776));
            }
        });
        this.dock.addView(library, new LinearLayout.LayoutParams(dp(58), dp(58)));
        this.miniBox = new FrameLayout(owner);
        this.miniBox.setBackground(glass(this.miniBox, 1, dp(30), -1447858567));
        this.miniBox.setElevation(dp(11));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, dp(58), 1.0f);
        middle.leftMargin = dp(9);
        middle.rightMargin = dp(9);
        this.dock.addView(this.miniBox, middle);
        LinearLayout mini = new LinearLayout(owner);
        mini.setGravity(16);
        mini.setPadding(dp(6), dp(5), dp(4), dp(5));
        this.miniBox.addView(mini, new FrameLayout.LayoutParams(-1, -1));
        this.cover = Artwork.frame(owner, 12);
        mini.addView(this.cover, new LinearLayout.LayoutParams(dp(45), dp(45)));
        LinearLayout meta = new LinearLayout(owner);
        meta.setOrientation(1);
        meta.setGravity(16);
        meta.setPadding(dp(9), 0, dp(1), 0);
        this.title = QingyinUi.text(owner, "", 13, -1, true);
        this.title.setSingleLine(true);
        this.title.setEllipsize(TextUtils.TruncateAt.END);
        this.artist = QingyinUi.text(owner, "", 11, -268435457, false);
        this.artist.setSingleLine(true);
        this.artist.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(this.title);
        meta.addView(this.artist);
        mini.addView(meta, new LinearLayout.LayoutParams(0, -1, 1.0f));
        this.play = new ImageView(owner);
        this.play.setImageResource(17301540);
        this.play.setImageTintList(ColorStateList.valueOf(-1));
        this.play.setPadding(dp(9), dp(9), dp(9), dp(9));
        this.play.setContentDescription("播放或暂停");
        this.play.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicOverlayDock.this.m183lambda$new$1$localqingyinmobileMusicOverlayDock(view);
            }
        });
        mini.addView(this.play, new LinearLayout.LayoutParams(dp(41), dp(43)));
        this.cover.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicOverlayDock.this.m184lambda$new$2$localqingyinmobileMusicOverlayDock(view);
            }
        });
        this.title.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicOverlayDock.this.m185lambda$new$3$localqingyinmobileMusicOverlayDock(view);
            }
        });
        this.artist.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicOverlayDock.this.m186lambda$new$4$localqingyinmobileMusicOverlayDock(view);
            }
        });
        this.miniBox.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MusicOverlayDock.this.m187lambda$new$5$localqingyinmobileMusicOverlayDock(view);
            }
        });
        this.miniBox.setVisibility(4);
        FrameLayout search = circle("⌕", "搜索", 2, -1175065614, 0);
        search.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                Activity activity = owner;
                activity.startActivity(new Intent(activity, (Class<?>) OnlineActivity.class).putExtra("page", "search"));
            }
        });
        this.dock.addView(search, new LinearLayout.LayoutParams(dp(58), dp(58)));
        owner.getApplication().registerActivityLifecycleCallbacks(this);
        this.requested = owner.bindService(new Intent(owner, (Class<?>) PlaybackService.class), this.service, 1);
        this.backdrop.post(new Runnable() { // from class: local.qingyin.mobile.MusicOverlayDock$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MusicOverlayDock.this.m188lambda$new$7$localqingyinmobileMusicOverlayDock();
            }
        });
        this.resumed = true;
        this.ui.post(this.ticker);
    }

    /* JADX INFO: renamed from: lambda$new$1$local-qingyin-mobile-MusicOverlayDock, reason: not valid java name */
    /* synthetic */ void m183lambda$new$1$localqingyinmobileMusicOverlayDock(View v) {
        if (this.bound && this.player != null && this.player.current() != null) {
            Intent svc = new Intent(this.activity, (Class<?>) PlaybackService.class);
            this.activity.startForegroundService(svc);
            this.player.toggle();
            update();
        }
    }

    /* JADX INFO: renamed from: lambda$new$2$local-qingyin-mobile-MusicOverlayDock, reason: not valid java name */
    /* synthetic */ void m184lambda$new$2$localqingyinmobileMusicOverlayDock(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$new$3$local-qingyin-mobile-MusicOverlayDock, reason: not valid java name */
    /* synthetic */ void m185lambda$new$3$localqingyinmobileMusicOverlayDock(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$new$4$local-qingyin-mobile-MusicOverlayDock, reason: not valid java name */
    /* synthetic */ void m186lambda$new$4$localqingyinmobileMusicOverlayDock(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$new$5$local-qingyin-mobile-MusicOverlayDock, reason: not valid java name */
    /* synthetic */ void m187lambda$new$5$localqingyinmobileMusicOverlayDock(View v) {
        openPlayer();
    }

    /* JADX INFO: renamed from: lambda$new$7$local-qingyin-mobile-MusicOverlayDock, reason: not valid java name */
    /* synthetic */ void m188lambda$new$7$localqingyinmobileMusicOverlayDock() {
        if (this.closed) {
            return;
        }
        ViewTreeObserver obs = this.backdrop.getViewTreeObserver();
        if (obs.isAlive() && !this.blurRegistered) {
            obs.addOnScrollChangedListener(this.scrollListener);
            this.blurRegistered = true;
        }
        refresh();
    }

    private FrameLayout circle(String icon, String hint, int index, int tint, int unused) {
        FrameLayout host = new FrameLayout(this.activity);
        host.setBackground(glass(host, index, dp(30), tint));
        host.setElevation(dp(11));
        NavIconView shape = new NavIconView(this.activity, icon, -13686464);
        host.addView(shape, new FrameLayout.LayoutParams(dp(30), dp(30), 17));
        host.setContentDescription(hint);
        return host;
    }

    private FrostedGlassDrawable glass(View host, int index, float corner, int tint) {
        FrostedGlassDrawable bg = new FrostedGlassDrawable(this.backdrop, host, corner, tint);
        this.surfaces[index] = bg;
        return bg;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void update() {
        if (this.closed || this.miniBox == null) {
            return;
        }
        Track current = (!this.bound || this.player == null) ? null : this.player.current();
        this.miniBox.setVisibility(current == null ? 4 : 0);
        if (current == null) {
            this.lastCover = "";
            refresh();
            return;
        }
        if (!current.coverUrl.equals(this.lastCover)) {
            this.lastCover = current.coverUrl;
            Artwork.bind(this.cover, current.coverUrl);
        }
        this.title.setText(current.title);
        this.artist.setText(current.artist);
        boolean playing = this.player.isPlaying() || this.player.isPreparing();
        this.play.setImageResource(playing ? 17301539 : 17301540);
        this.play.setContentDescription(playing ? "暂停" : "播放");
        refresh();
    }

    private void openPlayer() {
        if (this.bound && this.player != null && this.player.current() != null) {
            this.activity.startActivity(new Intent(this.activity, (Class<?>) NowPlayingActivity.class));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refresh() {
        if (this.closed) {
            return;
        }
        for (FrostedGlassDrawable bg : this.surfaces) {
            if (bg != null) {
                bg.refresh();
            }
        }
    }

    private void dispose() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.resumed = false;
        this.ui.removeCallbacks(this.ticker);
        if (this.requested) {
            this.activity.unbindService(this.service);
        }
        if (this.blurRegistered && this.backdrop.getViewTreeObserver().isAlive()) {
            this.backdrop.getViewTreeObserver().removeOnScrollChangedListener(this.scrollListener);
        }
        for (FrostedGlassDrawable bg : this.surfaces) {
            if (bg != null) {
                bg.release();
            }
        }
        this.activity.getApplication().unregisterActivityLifecycleCallbacks(this);
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityResumed(Activity owner) {
        if (owner != this.activity || this.closed) {
            return;
        }
        this.resumed = true;
        this.ui.removeCallbacks(this.ticker);
        this.ui.post(this.ticker);
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityPaused(Activity owner) {
        if (owner != this.activity) {
            return;
        }
        this.resumed = false;
        this.ui.removeCallbacks(this.ticker);
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityDestroyed(Activity owner) {
        if (owner == this.activity) {
            dispose();
        }
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityCreated(Activity a, Bundle state) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityStarted(Activity a) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityStopped(Activity a) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivitySaveInstanceState(Activity a, Bundle state) {
    }
}
