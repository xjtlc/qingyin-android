package local.qingyin.mobile;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
/** Reconcile only download IDs previously enqueued by this app. */
public final class PlayerDownloadReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()))return;
        PendingResult pending=goAsync();
        new Thread(()->{try{PlayerSongActions.reconcileDownloads(context.getApplicationContext());}finally{pending.finish();}},"Qingyin-download-complete").start();
    }
}
