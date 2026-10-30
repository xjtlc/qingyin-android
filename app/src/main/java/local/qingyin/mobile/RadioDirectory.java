package local.qingyin.mobile;

import android.net.Uri;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
final class RadioDirectory {
    private static long cachedAt;
    private static ArrayList<Record> cachedRecords;
    private static final String[] MIRRORS = {"https://de1.api.radio-browser.info", "https://nl1.api.radio-browser.info", "https://at1.api.radio-browser.info"};
    private static final String[] FEATURED = {"中国之声", "北京新闻广播", "广东珠江经济台", "广东股市广播", "上海经典音乐广播", "上海新闻广播", "上海动感101", "北京交通广播", "广东交通之声", "北京文艺广播", "济南故事广播", "广东音乐之声", "四川新闻广播", "上海经典金曲广播", "江苏经典流行音乐广播", "国际新闻"};

    RadioDirectory() {
    }

    static final class Station {
        final int bitrate;
        final String codec;
        final String country;
        final String favicon;
        final String name;
        final String stream;
        final String uuid;

        Station(String uuid, String name, String country, String codec, String stream, int bitrate, String favicon) {
            this.uuid = uuid;
            this.name = name;
            this.country = country;
            this.codec = codec;
            this.stream = stream;
            this.bitrate = bitrate;
            this.favicon = favicon == null ? "" : favicon.trim();
        }

        Track asTrack() {
            long id = (((long) this.uuid.hashCode()) & 268435455) + 1862270976;
            return new Track(id, this.name, "直播电台 · " + this.country, "audio/radio", 0L, Uri.parse(this.stream), Math.max(0, this.bitrate) * 1000);
        }
    }

    static boolean trustedStream(String url) {
        String host;
        if (url == null || url.length() < 12 || url.length() > 2048) {
            return false;
        }
        try {
            Uri u = Uri.parse(url);
            if ("https".equalsIgnoreCase(u.getScheme()) && u.getUserInfo() == null && (host = u.getHost()) != null && !host.isEmpty()) {
                String host2 = host.toLowerCase(Locale.ROOT);
                if (!host2.equals("localhost") && !host2.endsWith(".local") && !host2.endsWith(".internal") && !host2.endsWith(".localhost") && !host2.matches("[0-9.]+") && !host2.contains(":")) {
                    String path = u.getPath() == null ? "" : u.getPath().toLowerCase(Locale.ROOT);
                    return (path.endsWith(".m3u") || path.endsWith(".pls") || path.endsWith(".m3u8") || path.endsWith(".asx")) ? false : true;
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static final class Record {
        final Station station;
        final String tags;

        Record(Station station, String tags) {
            this.station = station;
            this.tags = tags;
        }
    }

    private static boolean unwanted(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.length() < 3 || n.contains("个人") || n.contains("私用") || n.contains("合集") || n.contains("首华语") || n.contains("50首") || n.contains("500首") || n.contains("歌单") || n.contains("回放") || n.contains("录播") || n.contains("replay") || n.contains("24小时循环") || n.contains("循环播放") || n.contains("2023.") || n.contains("测试") || n.contains("test");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:20:0x004e  */
    private static boolean suitable(Record record, String category) {
        String n = record.station.name.toLowerCase(Locale.ROOT);
        String tags = record.tags.toLowerCase(Locale.ROOT);
        switch (category) {
            case "news":
                if (n.contains("中国之声") || n.contains("新闻广播") || n.contains("国际新闻") || n.contains("资讯广播") || n.contains("时事")) {
                    return true;
                }
                return tags.contains("news") && (n.contains("广播") || n.contains("radio"));
            case "finance":
                return n.contains("经济") || n.contains("财经") || n.contains("股市") || n.contains("商业广播") || tags.contains("economics") || tags.contains("finance");
            case "city":
                return n.contains("交通") || n.contains("城市之声") || n.contains("生活广播") || n.contains("都市广播") || n.contains("cityfm") || tags.contains("traffic") || tags.contains("lifestyle");
            case "culture":
                return n.contains("文艺") || n.contains("故事") || n.contains("评书") || n.contains("戏曲") || n.contains("科学") || n.contains("科普") || n.contains("古典") || n.contains("教育") || tags.contains("culture") || tags.contains("educational") || tags.contains("storytelling") || tags.contains("classical");
            case "music":
                return n.contains("音乐") || n.contains("动感") || n.contains("流行") || n.contains("经典金曲") || n.contains("热歌") || n.contains("mixfm") || n.contains("cityfm") || tags.contains("music") || tags.contains("pop");
            default:
                return false;
        }
    }

    /* JADX WARN: Code duplicated, block: B:131:0x029c A[Catch: all -> 0x02c8, TRY_ENTER, TryCatch #6 {, blocks: (B:4:0x0003, B:6:0x000b, B:8:0x0013, B:10:0x001e, B:14:0x0023, B:16:0x002b, B:137:0x02ac, B:138:0x02af, B:131:0x029c, B:132:0x029f, B:108:0x0249, B:141:0x02be, B:143:0x02c7), top: B:157:0x0003 }] */
    /* JADX WARN: Code duplicated, block: B:137:0x02ac A[Catch: all -> 0x02c8, TryCatch #6 {, blocks: (B:4:0x0003, B:6:0x000b, B:8:0x0013, B:10:0x001e, B:14:0x0023, B:16:0x002b, B:137:0x02ac, B:138:0x02af, B:131:0x029c, B:132:0x029f, B:108:0x0249, B:141:0x02be, B:143:0x02c7), top: B:157:0x0003 }] */
    /* JADX WARN: Code duplicated, block: B:175:0x02af A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:66:0x015c A[Catch: all -> 0x0208, Exception -> 0x020d, TRY_ENTER, TryCatch #2 {Exception -> 0x020d, blocks: (B:48:0x00eb, B:50:0x00f2, B:53:0x0104, B:66:0x015c, B:68:0x0162), top: B:151:0x00eb }] */
    private static synchronized ArrayList<Record> directory() throws Exception {
        long now = SystemClock.elapsedRealtime();
        if (cachedRecords != null && !cachedRecords.isEmpty() && now - cachedAt < 600000) return cachedRecords;
        Exception last = null;
        for (String mirror : MIRRORS) {
            HttpURLConnection http = null;
            try {
                http = (HttpURLConnection) new URL(mirror + "/json/stations/search?countrycode=CN&hidebroken=true&order=votes&reverse=true&limit=280").openConnection();
                http.setInstanceFollowRedirects(false);
                http.setConnectTimeout(6500); http.setReadTimeout(8500);
                http.setRequestProperty("User-Agent", "QingyinAndroidRadio/0.7.9");
                http.setRequestProperty("Accept", "application/json");
                if (http.getResponseCode() != 200) throw new IOException("电台目录响应 " + http.getResponseCode());
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                try (InputStream input = http.getInputStream()) {
                    byte[] bytes = new byte[8192]; int n;
                    while ((n = input.read(bytes)) != -1) {
                        if (body.size() + n > 650000) throw new IOException("目录响应过大");
                        body.write(bytes, 0, n);
                    }
                }
                JSONArray raw = new JSONArray(body.toString("UTF-8"));
                ArrayList<Record> found = new ArrayList<>(); HashSet<String> urls = new HashSet<>();
                for (int i = 0; i < raw.length(); i++) {
                    JSONObject item = raw.optJSONObject(i);
                    if (item == null || item.optInt("lastcheckok", 0) != 1 || item.optBoolean("hls", false)) continue;
                    String stream = item.optString("url_resolved", item.optString("url", "")).trim();
                    String codec = item.optString("codec", "").toUpperCase(Locale.ROOT);
                    if (!(codec.equals("MP3") || codec.equals("AAC") || codec.equals("AAC+") || codec.equals("MPEG"))) continue;
                    if (!trustedStream(stream) || !urls.add(stream)) continue;
                    String uuid = item.optString("stationuuid", ""); String name = item.optString("name", "").trim();
                    if (uuid.isEmpty() || unwanted(name)) continue;
                    if (name.length() > 70) name = name.substring(0, 70);
                    found.add(new Record(new Station(uuid, name, item.optString("country", "中国"), codec, stream,
                        item.optInt("bitrate", 0), item.optString("favicon", "")), item.optString("tags", "")));
                }
                if (found.isEmpty()) throw new IOException("目录暂无有效直播频道");
                cachedRecords = found; cachedAt = SystemClock.elapsedRealtime(); return found;
            } catch (Exception error) { last = error; }
            finally { if (http != null) http.disconnect(); }
        }
        if (last != null) throw last;
        throw new IOException("电台目录不可用");
    }

    static List<Station> fetch(String category) throws Exception {
        ArrayList<Record> records = directory();
        ArrayList<Station> selected = new ArrayList<>();
        HashSet<String> used = new HashSet<>();
        if ("featured".equals(category) || "all".equals(category)) {
            for (String keyword : FEATURED) {
                for (Record record : records) {
                    if (record.station.name.contains(keyword) && used.add(record.station.stream)) {
                        selected.add(record.station);
                        break;
                    }
                }
                if (selected.size() >= 14) {
                    break;
                }
            }
            if (selected.size() < 8) {
                for (Record record2 : records) {
                    if ((suitable(record2, "news") || suitable(record2, "finance") || suitable(record2, "city") || suitable(record2, "culture")) && used.add(record2.station.stream)) {
                        selected.add(record2.station);
                    }
                    if (selected.size() >= 12) {
                        break;
                    }
                }
            }
        } else {
            for (Record record3 : records) {
                if (suitable(record3, category) && used.add(record3.station.stream)) {
                    selected.add(record3.station);
                }
                if (selected.size() >= 28) {
                    break;
                }
            }
        }
        if (selected.isEmpty()) {
            throw new IOException("本类别暂时没有可用的合适直播频道");
        }
        return selected;
    }
}
