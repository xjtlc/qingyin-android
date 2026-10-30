package local.qingyin.mobile;

import android.app.Application;

/* JADX INFO: loaded from: classes2.dex */
public final class QingyinApp extends Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        UiPerformance.install(this);
        LocalNodeHost.start(this);
    }
}
