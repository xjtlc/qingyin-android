package local.qingyin.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.json.JSONObject;

/** User-initiated song actions. Never shares signed audio URLs or account credentials. */
final class PlayerSongActions {
    private static final java.util.WeakHashMap<Activity,AlertDialog> RESOLVING=new java.util.WeakHashMap<>();
    private PlayerSongActions() {}
    private static void say(Activity host, String message) { Toast.makeText(host,message,Toast.LENGTH_LONG).show(); }
    private static String songInfo(Track song) { return "《"+song.title+"》 — "+song.artist; }
    private static String shareText(Track song) {
        String text=songInfo(song);
        if(song.isOnline()) {
            Uri uri=Uri.parse(song.uri);
            String hash=uri.getLastPathSegment();
            if(hash!=null && hash.matches("(?i)[0-9a-f]{32}"))
                text+="\nhttps://www.kugou.com/song/#hash="+hash+"&album_id="+Math.max(0,albumId(song));
        }
        return text;
    }
    static long albumId(Track song) {
        try {return Long.parseLong(Uri.parse(song.uri).getQueryParameter("albumId"));}
        catch(Exception ignored){return 0;}
    }
    private static void sendShare(Activity host,Track song,boolean invite) {
        Intent intent=new Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT,song.title)
            .putExtra(Intent.EXTRA_TEXT,(invite?"想和你一起听这首歌：\n":"")+shareText(song));
        try {host.startActivity(Intent.createChooser(intent,invite?"邀请朋友听歌":"分享歌曲"));}
        catch(android.content.ActivityNotFoundException unavailable){say(host,"设备上暂时没有可分享的应用");}
    }
    static void showShare(Activity host,Track song) {
        ActionSheet sheet=new ActionSheet(host,"分享歌曲",songInfo(song),false);
        sheet.setSong(song);
        sheet.addAction("share","系统分享",song.isOnline()?"发送歌曲链接给朋友":"分享歌名和歌手信息",()->sendShare(host,song,false));
        sheet.addAction("info",song.isOnline()?"复制歌曲链接":"复制歌曲信息","保存到剪贴板",()->{
            ClipboardManager clipboard=(ClipboardManager)host.getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText(song.title,shareText(song)));
            say(host,"已复制");
        });
        sheet.show();
    }
    static void showListenTogether(Activity host,Track song) {
        new AlertDialog.Builder(host).setTitle("邀请朋友一起听")
            .setMessage(songInfo(song)+"\n\n把这首歌分享给朋友，"+(song.isOnline()?"对方可打开链接收听。":"对方可按歌名搜索收听。")+"当前提供歌曲邀请，不会同步双方的播放进度。")
            .setPositiveButton("邀请朋友",(dialog,which)->sendShare(host,song,true))
            .setNegativeButton("取消",null).show();
    }
    static void showFavorite(Activity host,Track song,Runnable changed){
        boolean saved=song.isOnline()?OnlineLibraryStore.isFavorite(host,song):host.getSharedPreferences("library",Context.MODE_PRIVATE).getStringSet("favorites",Collections.emptySet()).contains(Long.toString(song.id));
        new AlertDialog.Builder(host).setTitle("收藏歌曲").setMessage(songInfo(song))
            .setPositiveButton(saved?"取消收藏":"加入我喜欢",(d,w)->{
                if(song.isOnline())OnlineLibraryStore.toggle(host,song);
                else{
                    SharedPreferences prefs=host.getSharedPreferences("library",Context.MODE_PRIVATE);
                    Set<String> ids=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));
                    if(!ids.add(Long.toString(song.id)))ids.remove(Long.toString(song.id));prefs.edit().putStringSet("favorites",ids).apply();
                }
                if(changed!=null)changed.run();
            }).setNegativeButton("取消",null).show();
    }
    static void showDownload(Activity host,Track song) {
        if(!song.isOnline()) {
            new AlertDialog.Builder(host).setTitle(song.isRadio()?"电台直播":"已在本机")
                .setMessage(song.isRadio()?"直播流不提供歌曲文件下载。":"这首歌曲已来自本机，可在“已下载”中查看。")
                .setPositiveButton("查看已下载",(d,w)->host.startActivity(new Intent(host,MainActivity.class)))
                .setNegativeButton("关闭",null).show();
            return;
        }
        new AlertDialog.Builder(host).setTitle("下载歌曲").setMessage(songInfo(song)+
            "\n\n使用当前账号可用的音源和音质，完成后可在资料库的“已下载”中播放。")
            .setPositiveButton("开始下载",(dialog,which)->resolveDownload(host,song))
            .setNeutralButton("查看已下载",(d,w)->host.startActivity(new Intent(host,MainActivity.class)))
            .setNegativeButton("取消",null).show();
    }
    private static void resolveDownload(Activity host,Track song) {
        closePending(host);
        AlertDialog busy=new AlertDialog.Builder(host).setTitle("正在获取下载音源")
            .setMessage("正在确认当前账号的可用音源…").setNegativeButton("取消",null).create();
        RESOLVING.put(host,busy);
        busy.setOnDismissListener(d->{if(RESOLVING.get(host)==busy)RESOLVING.remove(host);});
        busy.show();
        long epoch=RecommendationArtwork.epoch(host);
        new Thread(()->{
            try {
                JSONObject result=OnlineApi.call(host.getApplicationContext(),"songUrl",OnlineApi.songParams(song,OnlineApi.quality(host)));
                String url=result.optString("url","").trim();
                Uri uri=Uri.parse(url);String domain=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);
                boolean trusted=domain.equals("kugou.com")||domain.endsWith(".kugou.com")||domain.equals("kugoucdn.com")||domain.endsWith(".kugoucdn.com");
                if(!trusted||url.length()>8192||uri.getUserInfo()!=null||!("https".equals(uri.getScheme())||"http".equals(uri.getScheme())))
                    throw new IllegalStateException("当前歌曲没有可用的下载音源");
                JSONObject quality=result.optJSONObject("quality");
                String format=quality==null?"":quality.optString("format","").toLowerCase(Locale.ROOT);
                boolean preview=result.optBoolean("preview",false);
                host.runOnUiThread(()->{
                    if(host.isFinishing()||host.isDestroyed()||!busy.isShowing())return;
                    busy.dismiss();
                    if(epoch!=RecommendationArtwork.epoch(host)){say(host,"账号已更改，请重新下载");return;}
                    if(preview)new AlertDialog.Builder(host).setTitle("当前仅提供试听片段")
                        .setMessage("当前账号获得的是试听音源，不是完整歌曲。是否仅下载试听片段？")
                        .setPositiveButton("下载试听",(d,w)->enqueue(host,song,uri,format,true)).setNegativeButton("取消",null).show();
                    else enqueue(host,song,uri,format,false);
                });
            } catch(Exception failure) {
                host.runOnUiThread(()->{if(host.isFinishing()||host.isDestroyed()||!busy.isShowing())return;busy.dismiss();say(host,"获取下载音源失败，请检查账号权限和网络后重试");});
            }
        },"Qingyin-download-resolver").start();
    }
    static void closePending(Activity host){AlertDialog dialog=RESOLVING.remove(host);if(dialog!=null)dialog.dismiss();}
    private static void enqueue(Activity host,Track song,Uri uri,String format,boolean preview) {
        try {
            String extension=format.matches("flac|wav|mp3|m4a|aac|ogg")?format:"audio";
            String name=(song.artist+" - "+song.title).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_");
            if(name.length()>90)name=name.substring(0,90);
            String filename=name+(preview?"-试听":"")+"-"+System.currentTimeMillis()+"."+extension;
            DownloadManager manager=(DownloadManager)host.getSystemService(Context.DOWNLOAD_SERVICE);
            if(manager==null)throw new IllegalStateException("Download service unavailable");
            DownloadManager.Request request=new DownloadManager.Request(uri).setTitle(song.title+(preview?"（试听片段）":""))
                .setDescription(song.artist+" · 青音下载").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(host,Environment.DIRECTORY_MUSIC,filename)
                .addRequestHeader("User-Agent","Mozilla/5.0 (Linux; Android) QingyinMobile/0.5")
                .addRequestHeader("Accept-Encoding","identity");
            request.setMimeType("flac".equals(extension)?"audio/flac":"wav".equals(extension)?"audio/wav":"m4a".equals(extension)?"audio/mp4":"ogg".equals(extension)?"audio/ogg":"aac".equals(extension)?"audio/aac":"audio/mpeg");
            long id=manager.enqueue(request);
            synchronized(PlayerSongActions.class){
                SharedPreferences prefs=host.getSharedPreferences("player_downloads",Context.MODE_PRIVATE);
                Set<String> pending=new HashSet<>(prefs.getStringSet("pending",Collections.emptySet()));pending.add(Long.toString(id));
                prefs.edit().putStringSet("pending",pending).apply();
            }
            say(host,"已加入下载，完成后可在“已下载”中查看");
        }catch(Exception failure){say(host,"下载未能启动，请确认设备存储空间充足");}
    }
    static synchronized void reconcileDownloads(Context context) {
        SharedPreferences prefs=context.getSharedPreferences("player_downloads",Context.MODE_PRIVATE);
        Set<String> pending=new HashSet<>(prefs.getStringSet("pending",Collections.emptySet()));
        if(pending.isEmpty())return;
        DownloadManager manager=(DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        if(manager==null)return;
        SharedPreferences library=context.getSharedPreferences("library",Context.MODE_PRIVATE);
        Set<String> imported=new HashSet<>(library.getStringSet("imported_uris",Collections.emptySet()));
        for(String entry:new HashSet<>(pending))try{
            long id=Long.parseLong(entry);
            try(Cursor cursor=manager.query(new DownloadManager.Query().setFilterById(id))){
                if(cursor==null||!cursor.moveToFirst()){pending.remove(entry);continue;}
                int status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if(status==DownloadManager.STATUS_SUCCESSFUL){Uri file=manager.getUriForDownloadedFile(id);if(file!=null)imported.add(file.toString());pending.remove(entry);}
                else if(status==DownloadManager.STATUS_FAILED)pending.remove(entry);
            }
        }catch(Exception ignored){}
        library.edit().putStringSet("imported_uris",imported).apply();
        prefs.edit().putStringSet("pending",pending).apply();
    }
}
