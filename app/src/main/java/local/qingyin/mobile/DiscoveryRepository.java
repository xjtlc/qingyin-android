package local.qingyin.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Public, unauthenticated display metadata. Playback still uses the original music engine. */
final class DiscoveryRepository {
    interface Callback { void onSection(DiscoveryContent.Section section); }

    private static final long FRESH_MS = 20L * 60 * 1000;
    private static final long MAX_STALE_MS = 7L * 24 * 60 * 60 * 1000;
    private static final int MAX_RESPONSE = 1024 * 1024;
    private static final int MAX_CACHE = 160 * 1024;
    private static final String ORIGIN = "https://m.kugou.com";
    private static final String PLAYLIST_PATH = "/plist/index&json=true&page=1";
    private static final String RANK_PATH = "/rank/list&json=true";
    private static final Set<String> IMAGE_HOSTS = new HashSet<>(Arrays.asList(
        "imgessl.kugou.com", "imge.kugou.com", "singerimg.kugou.com", "c1.kgimg.com"));
    private static final Spec[] SPECS = {
        new Spec("focus", "今日精选", "hero", ""),
        new Spec("new", "新歌精选", "songs", "74534"),
        new Spec("trending", "正在流行", "songs", "8888"),
        new Spec("rising", "热度飙升", "songs", "6666"),
        new Spec("playlists", "热门歌单", "cards", ""),
        new Spec("original", "原创发现", "songs", "30972"),
        new Spec("styles", "风格漫游", "cards", ""),
        new Spec("global", "全球声音", "cards", ""),
        new Spec("dj", "电音与 DJ", "cards", "")
    };
    private static final String[] STYLE_IDS = {"51341", "59896", "59895", "44412", "59897", "59900", "59899", "33162"};
    private static final String[] GLOBAL_IDS = {"31310", "31311", "31312", "33165", "31313", "54848", "4681", "4680"};
    private static final String[] DJ_IDS = {"24971", "33160", "25028"};

    private final File cacheDirectory;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private final Object playlistLock = new Object();
    private final Object directoryLock = new Object();
    private final Map<String, DiscoveryContent.Section> memory = new HashMap<>();
    private final Map<String, ArrayList<Callback>> pending = new HashMap<>();
    private final Set<HttpsURLConnection> activeConnections = new HashSet<>();
    private final ExecutorService workers;
    private volatile boolean released;
    private List<DiscoveryContent.Item> playlistSnapshot;
    private List<DiscoveryContent.Item> chartSnapshot;
    private long playlistFetchedAt, directoryFetchedAt;
    private volatile long sourcesInvalidatedAt;

    DiscoveryRepository(Context context) {
        cacheDirectory = new File(context.getApplicationContext().getCacheDir(), "discovery-public-v1");
        AtomicInteger serial = new AtomicInteger();
        // Executors create threads only when load/retry submits work. Detail callers use no pool threads.
        workers = Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "discovery-public-" + serial.incrementAndGet());
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
    }

    static List<DiscoveryContent.Section> placeholders() {
        ArrayList<DiscoveryContent.Section> result = new ArrayList<>();
        for (Spec spec : SPECS) result.add(DiscoveryContent.Section.loading(spec.key, spec.title, spec.layout));
        return Collections.unmodifiableList(result);
    }

    void load(boolean force, Callback callback) {
        if (released || callback == null) return;
        if (force) invalidateSources();
        for (Spec spec : SPECS) request(spec, force, callback);
    }

    void retry(String key, Callback callback) {
        if (released || callback == null) return;
        for (Spec spec : SPECS) if (spec.key.equals(key)) {
            invalidateSources();
            request(spec, true, callback);
            return;
        }
    }

    private void invalidateSources() {
        // Called by the UI: never wait for a worker that holds a source lock during I/O.
        sourcesInvalidatedAt = System.currentTimeMillis();
    }

    private void request(Spec spec, boolean force, Callback callback) {
        synchronized (lock) {
            if (released) return;
            DiscoveryContent.Section existing = memory.get(spec.key);
            if (existing != null && !force && !existing.stale && existing.error.isEmpty() && fresh(existing.fetchedAt)) {
                deliver(callback, existing);
                return;
            }
            ArrayList<Callback> listeners = pending.get(spec.key);
            if (listeners != null) {
                if (!listeners.contains(callback)) listeners.add(callback);
                return;
            }
            listeners = new ArrayList<>();
            listeners.add(callback);
            pending.put(spec.key, listeners);
        }
        try { workers.execute(() -> perform(spec, force)); }
        catch (RejectedExecutionException stopped) { synchronized (lock) { pending.remove(spec.key); } }
    }

    private void perform(Spec spec, boolean force) {
        DiscoveryContent.Section cached;
        synchronized (lock) { cached = memory.get(spec.key); }
        if (cached == null) cached = readCache(spec);
        if (released) return;
        if (cached != null && !force && !cached.stale && cached.error.isEmpty() && fresh(cached.fetchedAt)) {
            finish(spec.key, copy(cached, false, "", false));
            return;
        }
        if (cached != null) publish(spec.key, copy(cached, true, "", true));
        else publish(spec.key, DiscoveryContent.Section.loading(spec.key, spec.title, spec.layout));
        try {
            checkOpen();
            DiscoveryContent.Section result = fetchSection(spec);
            if (result.items.isEmpty()) throw new IOException("Empty public metadata");
            checkOpen();
            writeCache(result);
            finish(spec.key, result);
        } catch (Exception unavailable) {
            if (released) return;
            String message = "暂时无法获取，点击重试";
            DiscoveryContent.Section result = cached == null
                ? new DiscoveryContent.Section(spec.key, spec.title, "", spec.layout, null, null, false, message, 0, false)
                : copy(cached, false, "暂时无法更新，显示上次内容", true);
            finish(spec.key, result);
        }
    }

    private void publish(String key, DiscoveryContent.Section section) {
        ArrayList<Callback> listeners;
        synchronized (lock) {
            if (released) return;
            ArrayList<Callback> current = pending.get(key);
            listeners = current == null ? new ArrayList<>() : new ArrayList<>(current);
        }
        for (Callback callback : listeners) deliver(callback, section);
    }

    private void finish(String key, DiscoveryContent.Section section) {
        ArrayList<Callback> listeners;
        synchronized (lock) {
            if (released) return;
            memory.put(key, section);
            listeners = pending.remove(key);
        }
        if (listeners != null) for (Callback callback : listeners) deliver(callback, section);
    }

    private void deliver(Callback callback, DiscoveryContent.Section section) {
        main.post(() -> { if (!released) callback.onSection(section); });
    }

    private DiscoveryContent.Section fetchSection(Spec spec) throws IOException, JSONException {
        if (!spec.rankId.isEmpty()) {
            ChartSongs chart = loadChart(spec.rankId);
            List<DiscoveryContent.Item> items = new ArrayList<>();
            for (int i = 0; i < chart.songs.length(); i++) {
                JSONObject data = chart.songs.getJSONObject(i);
                Track track = OnlineApi.asTrack(data);
                if (track != null) items.add(new DiscoveryContent.Item("song", data.optString("hash"),
                    data.optString("title"), data.optString("artist"), data.optString("cover"), data, track));
            }
            JSONObject data = new JSONObject().put("title", spec.title).put("rankid", spec.rankId)
                .put("id", spec.rankId).put("songs", chart.songs).put("sourceTitle", chart.title)
                .put("sourceUpdatedAt", chart.updatedAt);
            DiscoveryContent.Item more = new DiscoveryContent.Item("collection", spec.rankId, spec.title,
                chart.title, firstCover(items), data, null);
            return section(spec, chartCaption(chart.title, chart.updatedAt), items, more, chart.fetchedAt);
        }
        if ("focus".equals(spec.key) || "playlists".equals(spec.key)) {
            List<DiscoveryContent.Item> all = playlists();
            int start = "focus".equals(spec.key) ? 0 : Math.min(6, all.size());
            int end = "focus".equals(spec.key) ? Math.min(6, all.size()) : Math.min(18, all.size());
            List<DiscoveryContent.Item> items = new ArrayList<>(all.subList(start, end));
            if (items.isEmpty()) items.addAll(all);
            long fetched;
            synchronized (playlistLock) { fetched = playlistFetchedAt; }
            return section(spec, "酷狗热门歌单", items, null, fetched);
        }
        List<DiscoveryContent.Item> directory = charts();
        String[] selected = "styles".equals(spec.key) ? STYLE_IDS : "global".equals(spec.key) ? GLOBAL_IDS : DJ_IDS;
        List<DiscoveryContent.Item> items = new ArrayList<>();
        for (String id : selected) for (DiscoveryContent.Item item : directory) {
            if (id.equals(item.id)) { items.add(item); break; }
        }
        long fetched;
        synchronized (directoryLock) { fetched = directoryFetchedAt; }
        return section(spec, "酷狗官方榜单", items, null, fetched);
    }

    private static DiscoveryContent.Section section(Spec spec, String caption, List<DiscoveryContent.Item> items,
        DiscoveryContent.Item more, long fetchedAt) {
        return new DiscoveryContent.Section(spec.key, spec.title, caption, spec.layout, items, more, false, "", fetchedAt, false);
    }

    /**
     * Read public song metadata from Kugou's HTTPS playlist page.
     * This never requests stream URLs, cookies or account credentials.
     */
    JSONArray fetchPlaylistSongs(String specialId) throws IOException {
        checkOpen();
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IOException("Playlist metadata requires a worker thread");
        if (specialId == null || !specialId.matches("[1-9][0-9]{0,14}")) throw new IOException("Invalid playlist identifier");
        URL target = new URL("https://www.kugou.com/yy/special/single/" + specialId + ".html");
        HttpsURLConnection connection = (HttpsURLConnection) target.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/html");
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36");
        synchronized (lock) {
            if (released) { connection.disconnect(); throw new IOException("Repository closed"); }
            activeConnections.add(connection);
        }
        try {
            if (connection.getResponseCode() != 200 || connection.getContentLengthLong() > MAX_RESPONSE)
                throw new IOException("Playlist metadata unavailable");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            long deadline = SystemClock.elapsedRealtime() + 22_000;
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    checkOpen();
                    if (SystemClock.elapsedRealtime() > deadline || output.size() + n > MAX_RESPONSE)
                        throw new IOException("Playlist metadata limit exceeded");
                    output.write(buffer, 0, n);
                }
            }
            String html = new String(output.toByteArray(), StandardCharsets.UTF_8);
            java.util.regex.Matcher embedded = java.util.regex.Pattern.compile(
                "(?s)\\bvar\\s+data\\s*=\\s*(\\[.*?\\])\\s*,\\s*specialData\\s*=").matcher(html);
            if (!embedded.find()) throw new IOException("Playlist song data not present");
            JSONArray songs = normalizeSongs(new JSONArray(embedded.group(1)), 300);
            if (songs.length() == 0) throw new IOException("Playlist has no usable songs");
            return songs;
        } catch (JSONException invalid) {
            throw new IOException("Invalid playlist metadata", invalid);
        } finally {
            synchronized (lock) { activeConnections.remove(connection); }
            connection.disconnect();
        }
    }

    /** Blocking metadata request. Call from the detail page's existing executor, never the UI thread. */
    JSONArray fetchChartSongs(String rankId) throws IOException {
        checkOpen();
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IOException("Chart metadata requires a worker thread");
        try { return loadChart(rankId).songs; }
        catch (JSONException invalid) { throw new IOException("Invalid chart metadata", invalid); }
    }

    private ChartSongs loadChart(String rankId) throws IOException, JSONException {
        if (rankId == null || !rankId.matches("[1-9][0-9]{0,7}")) throw new IOException("Invalid chart identifier");
        JSONObject root = getJson("/rank/info/?rankid=" + rankId + "&page=1&json=true");
        JSONObject info = root.optJSONObject("info");
        JSONObject songBox = root.optJSONObject("songs");
        if (info == null || !rankId.equals(info.optString("rankid")) || songBox == null)
            throw new IOException("Unexpected chart metadata");
        JSONArray raw = songBox.optJSONArray("list");
        JSONArray songs = normalizeSongs(raw);
        if (songs.length() == 0) throw new IOException("Empty chart metadata");
        return new ChartSongs(songs, clean(info.optString("rankname"), 120),
            sourceDate(info.optString("rank_id_publish_date")), System.currentTimeMillis());
    }

    private List<DiscoveryContent.Item> playlists() throws IOException, JSONException {
        synchronized (playlistLock) {
            checkOpen();
            if (playlistSnapshot != null && playlistFetchedAt >= sourcesInvalidatedAt
                && System.currentTimeMillis() - playlistFetchedAt < 60_000) return playlistSnapshot;
            JSONObject root = getJson(PLAYLIST_PATH);
            JSONObject plist = root.optJSONObject("plist");
            JSONObject box = plist == null ? null : plist.optJSONObject("list");
            JSONArray rows = box == null ? null : box.optJSONArray("info");
            LinkedHashMap<String, DiscoveryContent.Item> result = new LinkedHashMap<>();
            for (int i = 0; rows != null && i < Math.min(rows.length(), 30); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                // The original playlist_track_all expects numeric specialid, not collection_3_* identifiers.
                String id = row.optString("specialid");
                String title = clean(row.optString("specialname"), 180);
                if (!id.matches("[1-9][0-9]{0,14}") || title.isEmpty()) continue;
                String creator = clean(row.optString("username"), 100);
                String cover = image(row.optString("imgurl"));
                int count = Math.max(0, Math.min(1_000_000, row.optInt("songcount")));
                JSONObject data = new JSONObject().put("id", id).put("globalId", id).put("listId", "")
                    .put("type", 0).put("title", title).put("cover", cover).put("count", count)
                    .put("creator", creator).put("description", clean(row.optString("intro"), 600));
                String subtitle = creator.isEmpty() ? count + " 首" : creator + " · " + count + " 首";
                result.put(id, new DiscoveryContent.Item("playlist", id, title, subtitle, cover, data, null));
            }
            if (result.isEmpty()) throw new IOException("Empty playlist metadata");
            playlistSnapshot = new ArrayList<>(result.values());
            playlistFetchedAt = System.currentTimeMillis();
            return playlistSnapshot;
        }
    }

    private List<DiscoveryContent.Item> charts() throws IOException, JSONException {
        synchronized (directoryLock) {
            checkOpen();
            if (chartSnapshot != null && directoryFetchedAt >= sourcesInvalidatedAt
                && System.currentTimeMillis() - directoryFetchedAt < 60_000) return chartSnapshot;
            JSONObject root = getJson(RANK_PATH);
            JSONObject rank = root.optJSONObject("rank");
            JSONArray rows = rank == null ? null : rank.optJSONArray("list");
            LinkedHashMap<String, DiscoveryContent.Item> result = new LinkedHashMap<>();
            for (int i = 0; rows != null && i < Math.min(rows.length(), 150); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                String id = row.optString("rankid");
                String title = clean(row.optString("rankname"), 120);
                if (!id.matches("[1-9][0-9]{0,7}") || title.isEmpty()) continue;
                String cover = image(row.optString("imgurl"));
                if (cover.isEmpty()) cover = image(row.optString("img_9"));
                if (cover.isEmpty()) cover = image(row.optString("album_img_9"));
                String updated = sourceDate(row.optString("rank_id_publish_date"));
                String frequency = clean(row.optString("update_frequency"), 60);
                JSONObject data = new JSONObject().put("id", id).put("rankid", id).put("title", title)
                    .put("cover", cover).put("sourceUpdatedAt", updated).put("frequency", frequency)
                    .put("description", clean(row.optString("intro"), 600));
                String subtitle = !updated.isEmpty() ? displayDate(updated) + "更新" : frequency.isEmpty() ? "酷狗榜单" : frequency + "更新";
                result.put(id, new DiscoveryContent.Item("chart", id, title, subtitle, cover, data, null));
            }
            if (result.isEmpty()) throw new IOException("Empty chart directory");
            chartSnapshot = new ArrayList<>(result.values());
            directoryFetchedAt = System.currentTimeMillis();
            return chartSnapshot;
        }
    }

    private static JSONArray normalizeSongs(JSONArray rows) throws JSONException {
        return normalizeSongs(rows, 30);
    }

    private static JSONArray normalizeSongs(JSONArray rows, int maxItems) throws JSONException {
        JSONArray result = new JSONArray();
        Set<String> seen = new HashSet<>();
        for (int i = 0; rows != null && i < Math.min(Math.min(300, maxItems), rows.length()); i++) {
            JSONObject raw = rows.optJSONObject(i);
            if (raw == null) continue;
            String hash = raw.optString("hash").trim().toLowerCase(Locale.ROOT);
            if (!hash.matches("[a-f0-9]{32}") || !seen.add(hash)) continue;
            String filename = clean(raw.optString("filename"), 300);
            int separator = filename.indexOf(" - ");
            String title = clean(raw.optString("songname"), 220);
            if (title.isEmpty()) title = separator >= 0 ? filename.substring(separator + 3).trim() : filename;
            if (title.isEmpty()) continue;
            JSONArray authors = raw.optJSONArray("authors");
            StringBuilder names = new StringBuilder();
            long artistId = 0;
            for (int a = 0; authors != null && a < Math.min(12, authors.length()); a++) {
                JSONObject author = authors.optJSONObject(a);
                if (author == null) continue;
                String name = clean(author.optString("author_name"), 100);
                if (name.isEmpty()) continue;
                if (names.length() > 0) names.append("、");
                names.append(name);
                if (artistId == 0) artistId = Math.max(0, author.optLong("author_id"));
            }
            String artist = names.toString();
            if (artist.isEmpty()) artist = clean(raw.optString("singer_name"), 180);
            if (artist.isEmpty()) artist = clean(raw.optString("singername"), 180);
            if (artist.isEmpty() && separator > 0) artist = filename.substring(0, separator).trim();
            if (artist.isEmpty()) artist = clean(raw.optString("h5_author_name"), 180);
            if (artist.isEmpty()) artist = "未知歌手";
            String cover = image(raw.optString("album_sizable_cover"));
            JSONObject trans = raw.optJSONObject("trans_param");
            if (cover.isEmpty() && trans != null) cover = image(trans.optString("union_cover"));
            JSONObject data = new JSONObject().put("id", hash).put("hash", hash).put("title", title)
                .put("artist", artist).put("artistId", artistId).put("album", clean(raw.optString("album_name"), 220))
                .put("albumId", positiveId(raw.optString("album_id"))).put("albumAudioId", positiveId(raw.optString("album_audio_id")))
                .put("cover", cover).put("duration", Math.max(0, Math.min(86_400,
                    raw.optLong("duration") > 86_400 ? (raw.optLong("duration") + 500) / 1000 : raw.optLong("duration"))));
            result.put(data);
        }
        return result;
    }

    private JSONObject getJson(String path) throws IOException, JSONException {
        URL target = new URL(ORIGIN + path);
        long deadline = SystemClock.elapsedRealtime() + 22_000;
        for (int redirects = 0; redirects <= 2; redirects++) {
            checkOpen();
            long remainingBeforeConnect = deadline - SystemClock.elapsedRealtime();
            if (remainingBeforeConnect <= 0) throw new IOException("Public metadata timeout");
            if (!"https".equals(target.getProtocol()) || !"m.kugou.com".equals(target.getHost())
                || target.getUserInfo() != null || (target.getPort() != -1 && target.getPort() != 443))
                throw new IOException("Unexpected public metadata host");
            HttpsURLConnection connection = (HttpsURLConnection) target.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout((int) Math.min(8_000, remainingBeforeConnect));
            connection.setReadTimeout((int) Math.min(8_000, remainingBeforeConnect));
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("User-Agent", "Qingyin-Discovery/1.0 (Android)");
            connection.setRequestProperty("Cookie", "");
            connection.setRequestProperty("Authorization", "");
            synchronized (lock) {
                if (released) { connection.disconnect(); throw new IOException("Repository closed"); }
                activeConnections.add(connection);
            }
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status <= 399) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || redirects == 2) throw new IOException("Public metadata redirect unavailable");
                    target = new URL(target, location);
                    continue;
                }
                if (status != 200) throw new IOException("Public metadata unavailable");
                if (connection.getContentLengthLong() > MAX_RESPONSE) throw new IOException("Public metadata too large");
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    while (true) {
                        checkOpen();
                        long remaining = deadline - SystemClock.elapsedRealtime();
                        if (remaining <= 0) throw new IOException("Public metadata timeout");
                        connection.setReadTimeout((int) Math.min(8_000, remaining));
                        int length = input.read(buffer);
                        if (length < 0) break;
                        if (output.size() + length > MAX_RESPONSE) throw new IOException("Public metadata too large");
                        output.write(buffer, 0, length);
                    }
                    return new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
                }
            } finally {
                synchronized (lock) { activeConnections.remove(connection); }
                connection.disconnect();
            }
        }
        throw new IOException("Public metadata unavailable");
    }

    private DiscoveryContent.Section readCache(Spec spec) {
        File file = new File(cacheDirectory, spec.key + ".json");
        if (!file.isFile() || file.length() > MAX_CACHE) return null;
        try (InputStream input = new AtomicFile(file).openRead(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (output.size() + count > MAX_CACHE) return null;
                output.write(buffer, 0, count);
            }
            JSONObject root = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            long fetchedAt = root.optLong("fetchedAt");
            long age = System.currentTimeMillis() - fetchedAt;
            if (root.optInt("schema") != 1 || !spec.key.equals(root.optString("key")) || fetchedAt <= 0
                || age < -300_000 || age > MAX_STALE_MS || !day(fetchedAt).equals(root.optString("day"))) return null;
            List<DiscoveryContent.Item> items = new ArrayList<>();
            JSONArray entries = root.optJSONArray("items");
            for (int i = 0; entries != null && i < Math.min(40, entries.length()); i++) {
                DiscoveryContent.Item item = readItem(entries.optJSONObject(i));
                if (item != null) items.add(item);
            }
            if (items.isEmpty()) return null;
            DiscoveryContent.Item more = readItem(root.optJSONObject("more"));
            return new DiscoveryContent.Section(spec.key, spec.title, clean(root.optString("caption"), 180), spec.layout,
                items, more, false, "", fetchedAt, !fresh(fetchedAt));
        } catch (Exception ignored) { return null; }
    }

    private void writeCache(DiscoveryContent.Section section) {
        FileOutputStream output = null;
        AtomicFile file = new AtomicFile(new File(cacheDirectory, section.key + ".json"));
        try {
            JSONObject root = new JSONObject().put("schema", 1).put("key", section.key)
                .put("fetchedAt", section.fetchedAt).put("day", day(section.fetchedAt)).put("caption", section.caption);
            JSONArray items = new JSONArray();
            for (DiscoveryContent.Item item : section.items) items.put(writeItem(item));
            root.put("items", items);
            if (section.more != null) root.put("more", writeItem(section.more));
            byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_CACHE || (!cacheDirectory.isDirectory() && !cacheDirectory.mkdirs())) return;
            checkOpen();
            output = file.startWrite();
            output.write(bytes);
            file.finishWrite(output);
        } catch (Exception ignored) { if (output != null) file.failWrite(output); }
    }

    private static JSONObject writeItem(DiscoveryContent.Item item) throws JSONException {
        return new JSONObject().put("kind", item.kind).put("id", item.id).put("title", item.title)
            .put("subtitle", item.subtitle).put("cover", item.cover).put("data", item.data);
    }

    private static DiscoveryContent.Item readItem(JSONObject object) {
        if (object == null) return null;
        String kind = object.optString("kind");
        if (!Arrays.asList("song", "playlist", "chart", "collection").contains(kind)) return null;
        JSONObject data = object.optJSONObject("data");
        if (data == null) return null;
        Track song = "song".equals(kind) ? OnlineApi.asTrack(data) : null;
        if ("song".equals(kind) && song == null) return null;
        return new DiscoveryContent.Item(kind, clean(object.optString("id"), 80), clean(object.optString("title"), 220),
            clean(object.optString("subtitle"), 220), image(object.optString("cover")), data, song);
    }

    private static DiscoveryContent.Section copy(DiscoveryContent.Section source, boolean loading, String error, boolean stale) {
        return new DiscoveryContent.Section(source.key, source.title, source.caption, source.layout,
            source.items, source.more, loading, error, source.fetchedAt, stale);
    }

    private static boolean fresh(long timestamp) {
        long now = System.currentTimeMillis();
        return timestamp > 0 && timestamp <= now + 300_000 && now - timestamp < FRESH_MS && day(timestamp).equals(day(now));
    }

    private static String day(long timestamp) { return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(timestamp)); }
    private static String clean(String value, int limit) {
        if (value == null) return "";
        String text = value.replaceAll("[\\p{Cc}&&[^\\n\\t]]", "").trim();
        return text.substring(0, Math.min(text.length(), limit));
    }
    private static String positiveId(String value) { return value != null && value.matches("[0-9]{1,17}") ? value : "0"; }
    private static String sourceDate(String value) {
        return value != null && value.matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}( [0-9]{2}:[0-9]{2}:[0-9]{2})?") ? value : "";
    }
    private static String displayDate(String date) { return date.length() >= 10 ? date.substring(5, 7) + "月" + date.substring(8, 10) + "日" : ""; }
    private static String chartCaption(String name, String date) { return "酷狗" + name + (date.isEmpty() ? "" : " · " + displayDate(date) + "更新"); }
    private static String firstCover(List<DiscoveryContent.Item> items) { return items.isEmpty() ? "" : items.get(0).cover; }

    private static String image(String value) {
        try {
            String raw = value == null ? "" : value.trim().replace("{size}", "400").replace("\\/", "/");
            if (raw.startsWith("//")) raw = "https:" + raw;
            URL url = new URL(raw);
            if ((!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) || !IMAGE_HOSTS.contains(url.getHost())
                || url.getUserInfo() != null || url.getPort() != -1 || url.getPath().length() > 500) return "";
            String host = "imge.kugou.com".equals(url.getHost()) ? "imgessl.kugou.com" : url.getHost();
            // Cover URLs need no query parameters. Do not retain signed links or arbitrary navigation URLs.
            return new URL("https", host, url.getPath()).toString();
        } catch (Exception invalid) { return ""; }
    }

    private void checkOpen() throws IOException {
        if (released || Thread.currentThread().isInterrupted()) throw new IOException("Repository closed");
    }

    void release() {
        List<HttpsURLConnection> connections;
        synchronized (lock) {
            if (released) return;
            released = true;
            connections = new ArrayList<>(activeConnections);
            activeConnections.clear();
            pending.clear();
            memory.clear();
        }
        workers.shutdownNow();
        for (HttpsURLConnection connection : connections) connection.disconnect();
    }

    private static final class Spec {
        final String key, title, layout, rankId;
        Spec(String key, String title, String layout, String rankId) { this.key = key; this.title = title; this.layout = layout; this.rankId = rankId; }
    }
    private static final class ChartSongs {
        final JSONArray songs;
        final String title, updatedAt;
        final long fetchedAt;
        ChartSongs(JSONArray songs, String title, String updatedAt, long fetchedAt) {
            this.songs = songs; this.title = title; this.updatedAt = updatedAt; this.fetchedAt = fetchedAt;
        }
    }
}
