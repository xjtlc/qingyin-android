package local.qingyin.mobile;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class OnlineApi {
    private static final String PREFS = "online_bridge";
    public static final String[] QUALITIES = {"auto", "lossless", "lossless_only", "320", "128", "viper_clear", "viper_tape", "viper_atmos"};
    public static final String[] QUALITY_NAMES = {"自动最高可用（可降级）", "优先无损（可降级）", "仅播放无损（不降级）", "320 kbps", "128 kbps", "蝰蛇超清（需权益）", "蝰蛇母带（需权益）", "蝰蛇全景声（需权益）"};

    private OnlineApi() {
    }

    public static boolean paired(Context context) {
        return true;
    }

    public static String address(Context context) {
        return "手机独立音乐引擎";
    }

    public static void pair(Context context, String input) {
        throw new UnsupportedOperationException("独立版不需要电脑配对");
    }

    public static String quality(Context c) {
        String value = c.getSharedPreferences("online_bridge", 0).getString("quality", "auto");
        for (String q : QUALITIES) {
            if (q.equals(value)) {
                return value;
            }
        }
        return "auto";
    }

    public static void setQuality(Context c, String value) {
        for (String q : QUALITIES) {
            if (q.equals(value)) {
                c.getSharedPreferences("online_bridge", 0).edit().putString("quality", value).apply();
                return;
            }
        }
    }

    public static final class ApiException extends IOException {
        public final String code;

        public ApiException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    public static JSONObject call(Context context, String method, JSONObject params) throws Exception {
        File socketPath = new File(new File(context.getFilesDir(), "node-runtime"), "qingyin.sock");
        JSONObject request = new JSONObject().put("method", method).put("params", params == null ? new JSONObject() : params);
        byte[] bytes = (request.toString() + "\n").getBytes(StandardCharsets.UTF_8);
        IOException startup = null;
        for (int attempt = 0; attempt < 12; attempt++) {
            try (LocalSocket socket = new LocalSocket()) {
                try { socket.connect(new LocalSocketAddress(socketPath.getAbsolutePath(), LocalSocketAddress.Namespace.FILESYSTEM)); }
                catch (IOException notReady) {
                    startup = notReady; if (attempt >= 11) break;
                    Thread.sleep(400); continue;
                }
                socket.setSoTimeout(45000);
                OutputStream output = socket.getOutputStream(); output.write(bytes); output.flush();
                ByteArrayOutputStream response = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int n;
                try (InputStream input = socket.getInputStream()) {
                    while ((n = input.read(buffer)) != -1) {
                        if (response.size() + n > 2000000) throw new IOException("概念版接口响应过大");
                        response.write(buffer, 0, n);
                    }
                }
                JSONObject decoded = new JSONObject(response.toString(StandardCharsets.UTF_8.name()).trim());
                if (!decoded.optBoolean("ok")) throw new ApiException(decoded.optString("code", "UNKNOWN"), decoded.optString("error", "概念版服务未能处理请求"));
                JSONObject data = decoded.optJSONObject("data"); return data == null ? new JSONObject() : data;
            }
        }
        throw new IOException("手机独立音乐引擎启动失败，请重新打开应用", startup);
    }

    public static String playlistCountLabel(JSONObject item) {
        Object raw;
        if (item == null || !item.has("count") || item.isNull("count") || (raw = item.opt("count")) == null || raw == JSONObject.NULL) {
            return "点击查看歌曲";
        }
        String value = String.valueOf(raw).trim();
        if (!value.matches("0|[1-9][0-9]{0,6}")) {
            return "点击查看歌曲";
        }
        try {
            return Integer.parseInt(value) + " 首歌曲";
        } catch (NumberFormatException e) {
            return "点击查看歌曲";
        }
    }

    public static Track asTrack(JSONObject item) {
        String hash = item.optString("hash", "");
        if (!hash.matches("(?i)[a-f0-9]{32}")) {
            return null;
        }
        long id = Long.parseUnsignedLong(hash.substring(0, 16), 16);
        long albumId = item.optLong("albumId", item.optLong("album_id", 0L));
        long audioId = item.optLong("albumAudioId", item.optLong("album_audio_id", 0L));
        Uri uri = new Uri.Builder().scheme("qingyin-online").authority("track").appendPath(hash).appendQueryParameter("albumId", Long.toString(albumId)).appendQueryParameter("audioId", Long.toString(audioId)).build();
        Track result = new Track(id, item.optString("title", "在线歌曲"), item.optString("artist", "未知歌手"), "audio/online", Math.max(0L, item.optLong("duration") * 1000), uri, 0);
        result.coverUrl = item.optString("cover", "");
        result.albumTitle = item.optString("album", "");
        result.artistId = Math.max(0L, item.optLong("artistId", 0L));
        result.addedAt = Math.max(0L, item.optLong("addedAt", 0L));
        return result;
    }

    public static JSONObject songParams(Track track, String quality) {
        Uri uri = Uri.parse(track.uri);
        JSONObject params = new JSONObject();
        try {
            params.put("hash", uri.getLastPathSegment());
            params.put("albumId", Long.parseLong(uri.getQueryParameter("albumId")));
            params.put("albumAudioId", Long.parseLong(uri.getQueryParameter("audioId")));
            params.put("quality", quality);
        } catch (Exception e) {
        }
        return params;
    }
}
