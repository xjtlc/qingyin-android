package local.qingyin.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import org.json.JSONObject;
import java.net.InetAddress;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/**
 * Debug-only diagnostic entrypoint. Accessible only to senders with the Android
 * signature-level DUMP permission; never outputs credentials, audio or user data.
 */
public final class DebugProbeReceiver extends BroadcastReceiver {
    private static final String TAG = "QingyinProbe";

    @Override public void onReceive(Context context, Intent intent) {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return;
        if (!"local.qingyin.mobile.DEBUG_PROBE".equals(intent.getAction())) return;
        final String mode = intent.getStringExtra("mode");
        if (!"javaNetwork".equals(mode) && !"nodeNetwork".equals(mode)
                && !"recommendations".equals(mode) && !"status".equals(mode)
                && !"probeSong".equals(mode) && !"playbackState".equals(mode)
                && !"openOnlineUi".equals(mode) && !"openPlayerUi".equals(mode)
                && !"skipFailures".equals(mode)) return;
        final Context app = context.getApplicationContext();
        if ("skipFailures".equals(mode)) {
            PlaybackFailureProbe.run(app, intent.getStringExtra("scenario"));
            return;
        }
        if ("openPlayerUi".equals(mode)) {
            app.startActivity(new Intent(app,NowPlayingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Log.i(TAG,"OPEN_PLAYER_UI_REQUESTED=true");
            return;
        }
        if ("openOnlineUi".equals(mode)) {
            app.startActivity(new Intent(app,OnlineActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Log.i(TAG,"OPEN_ONLINE_UI_REQUESTED=true");
            return;
        }
        if ("playbackState".equals(mode)) {
            final ServiceConnection connection = new ServiceConnection(){
                @Override public void onServiceConnected(ComponentName component, IBinder binder){
                    try {
                        PlaybackService service=((PlaybackService.LocalBinder)binder).service();
                        String error=service.lastError();
                        Log.i(TAG,"PLAYBACK_STATE track="+(service.current()!=null)
                            +" online="+(service.current()!=null&&service.current().isOnline())
                            +" queue="+service.queueSnapshot().size()+" playing="+service.isPlaying()
                            +" preparing="+service.isPreparing()+" errorPresent="+(error!=null&&!error.isEmpty())
                            +" errorKind="+(error!=null&&error.contains("音源")?"SOURCE":"OTHER"));
                    } finally {app.unbindService(this);}
                }
                @Override public void onServiceDisconnected(ComponentName component){}
            };
            app.bindService(new Intent(app,PlaybackService.class),connection,Context.BIND_AUTO_CREATE);
            return;
        }
        Thread worker = new Thread(() -> {
            long start = android.os.SystemClock.elapsedRealtime();
            Log.i(TAG,"START " + mode);
            try {
                if ("javaNetwork".equals(mode)) {
                    InetAddress.getByName("gateway.kugou.com");
                    Log.i(TAG,"JAVA_DNS=OK");
                    HttpsURLConnection connection=(HttpsURLConnection)
                        new URL("https://gateway.kugou.com/").openConnection();
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(6500);
                    connection.setReadTimeout(6500);
                    try {
                        Log.i(TAG,"JAVA_HTTPS_STATUS=" + connection.getResponseCode());
                    } finally { connection.disconnect(); }
                } else if ("probeSong".equals(mode)) {
                    JSONObject found=OnlineApi.call(app,"search",new JSONObject().put("keyword","晴天").put("page",1));
                    org.json.JSONArray songs=found.optJSONArray("songs");
                    Log.i(TAG,"PROBE_SEARCH_COUNT="+(songs==null?0:songs.length()));
                    int tried=0;
                    if(songs!=null) for(int i=0;i<songs.length()&&tried<3;i++) {
                        JSONObject entry=songs.optJSONObject(i);
                        if(entry==null)continue;
                        Track track=OnlineApi.asTrack(entry);
                        if(track==null)continue;
                        tried++;
                        try {
                            JSONObject stream=OnlineApi.call(app,"songUrl",OnlineApi.songParams(track,"128"));
                            String url=stream.optString("url","");
                            android.net.Uri parsed=android.net.Uri.parse(url);
                            String host=parsed.getHost()==null?"":parsed.getHost().toLowerCase(java.util.Locale.ROOT);
                            boolean serviceHostOk=host.equals("kugou.com")||host.endsWith(".kugou.com")
                                ||host.equals("kugoucdn.com")||host.endsWith(".kugoucdn.com");
                            Log.i(TAG,"PROBE_SOURCE_HOST="+host+" ACCEPTED_BY_SERVICE="+serviceHostOk);
                            JSONObject quality=stream.optJSONObject("quality");
                            boolean good=url.startsWith("https://")||url.startsWith("http://");
                            Log.i(TAG,"PROBE_SOURCE_SUCCESS="+good
                                +" transport="+(url.startsWith("https://")?"HTTPS":url.startsWith("http://")?"HTTP":"INVALID")
                                +" quality="+(quality==null?"unknown":quality.optString("format","unknown")));
                            if(good) {
                                java.util.concurrent.CountDownLatch wait=new java.util.concurrent.CountDownLatch(1);
                                android.media.MediaPlayer probe=new android.media.MediaPlayer();
                                try {
                                    probe.setAudioAttributes(new android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build());
                                    java.util.Map<String,String> hdr=new java.util.HashMap<>();
                                    hdr.put("User-Agent","Mozilla/5.0 (Linux; Android) QingyinMobile/0.5");
                                    hdr.put("Accept-Encoding","identity");
                                    probe.setOnPreparedListener(p->{
                                        Log.i(TAG,"PROBE_MEDIA_PREPARED=true");
                                        try {
                                            p.setVolume(0f,0f);
                                            p.start();
                                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
                                                try { Log.i(TAG,"PROBE_MEDIA_STARTED="+p.isPlaying()
                                                    +" POSITION_ADVANCED="+(p.getCurrentPosition()>1500)); }
                                                catch(Exception failure){Log.w(TAG,"PROBE_MEDIA_PROGRESS_ERROR");}
                                                wait.countDown();
                                            },4000L);
                                        } catch(Exception failure) {
                                            Log.w(TAG,"PROBE_MEDIA_START_ERROR kind="+failure.getClass().getSimpleName());
                                            wait.countDown();
                                        }
                                    });
                                    probe.setOnErrorListener((p,what,extra)->{
                                        Log.w(TAG,"PROBE_MEDIA_ERROR what="+what+" extra="+extra);
                                        wait.countDown();
                                        return true;
                                    });
                                    probe.setDataSource(app,android.net.Uri.parse(url),hdr);
                                    probe.prepareAsync();
                                    if(!wait.await(14000,java.util.concurrent.TimeUnit.MILLISECONDS))
                                        Log.w(TAG,"PROBE_MEDIA_PREPARE_TIMEOUT");
                                }finally {probe.release();}
                                break;
                            }
                        }catch(OnlineApi.ApiException failed){
                            Log.w(TAG,"PROBE_SOURCE_REJECT code="+failed.code);
                        }catch(Exception failure){
                            Log.e(TAG,"PROBE_SOURCE_ERROR kind="+failure.getClass().getSimpleName());
                        }
                    }
                } else {
                    JSONObject result=OnlineApi.call(app,
                        "nodeNetwork".equals(mode) ? "diagNetwork" : mode,new JSONObject());
                    if ("nodeNetwork".equals(mode)) {
                        Log.i(TAG,"NODE_DNS="+result.optString("dns","UNKNOWN"));
                        Log.i(TAG,"NODE_HTTPS="+result.optString("https","UNKNOWN"));
                    } else if ("status".equals(mode)) {
                        Log.i(TAG,"NODE_API_STATUS=OK");
                    } else if ("recommendations".equals(mode)) {
                        Log.i(TAG,"RECOMMENDATION_COUNT="+
                            (result.optJSONArray("playlists") == null ? -1 :
                            result.optJSONArray("playlists").length()));
                    }
                }
                Log.i(TAG,"COMPLETE "+mode+" MS="+
                    (android.os.SystemClock.elapsedRealtime()-start));
            } catch (Exception failure) {
                // Never print the remote API response or login/session information.
                Log.e(TAG,"FAIL "+mode+" CLASS="+failure.getClass().getSimpleName()
                    +" MS="+(android.os.SystemClock.elapsedRealtime()-start));
            }
        },"Qingyin-Private-DebugProbe");
        worker.setDaemon(true);
        worker.start();
    }
}
