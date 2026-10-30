package local.qingyin.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Keeps a compact local playback checkpoint. Never stores files or account tokens. */
public final class PlaybackBookmark {
    private PlaybackBookmark() {}
    private static final String KEY = "last_session_v1";
    private static final String OFFSET_KEY = "last_position_ms";
    private static final int MAX_SAVED = 240;

    public static final class Snapshot {
        public final ArrayList<Track> songs;
        public final int index;
        public final int offsetMs;
        Snapshot(ArrayList<Track> songs, int index, int offsetMs) {
            this.songs = songs; this.index = index; this.offsetMs = offsetMs;
        }
    }

    public static void save(Context context, List<Track> queue, int index, int offsetMs) {
        if (queue == null || index < 0 || index >= queue.size()) return;
        int start = Math.max(0, index - MAX_SAVED / 2);
        if (start + MAX_SAVED > queue.size()) start = Math.max(0, queue.size() - MAX_SAVED);
        int end = Math.min(queue.size(), start + MAX_SAVED);
        try {
            JSONArray songs = new JSONArray();
            for (int i = start; i < end; i++) {
                Track t = queue.get(i);
                JSONObject item = new JSONObject();
                item.put("id", t.id);
                item.put("title", t.title);
                item.put("artist", t.artist);
                item.put("mime", t.mime);
                item.put("duration", t.durationMs);
                item.put("uri", t.uri);
                songs.put(item);
            }
            JSONObject session = new JSONObject();
            session.put("songs", songs);
            session.put("index", index - start);
            session.put("offset", Math.max(0, offsetMs));
            context.getSharedPreferences("playback", Context.MODE_PRIVATE).edit()
                .putString(KEY, session.toString())
                .putInt(OFFSET_KEY, Math.max(0, offsetMs)).apply();
        } catch (Exception ignored) { }
    }

    public static void savePosition(Context context, int positionMs) {
        SharedPreferences prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE);
        if (prefs.contains(KEY)) prefs.edit().putInt(OFFSET_KEY, Math.max(0, positionMs)).apply();
    }

    public static Snapshot load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "");
        if (raw == null || raw.isEmpty()) return null;
        try {
            JSONObject snapshot = new JSONObject(raw);
            JSONArray songs = snapshot.getJSONArray("songs");
            if (songs.length() == 0 || songs.length() > MAX_SAVED) return null;
            ArrayList<Track> restored = new ArrayList<>();
            for (int i = 0; i < songs.length(); i++) {
                JSONObject item = songs.getJSONObject(i);
                String link = item.getString("uri");
                Uri uri = Uri.parse(link);
                if (!"content".equalsIgnoreCase(uri.getScheme())) return null;
                restored.add(new Track(item.getLong("id"), item.optString("title"),
                    item.optString("artist"), item.optString("mime"),
                    item.optLong("duration"), uri, 0));
            }
            int index = snapshot.optInt("index", -1);
            if (index < 0 || index >= restored.size()) return null;
            return new Snapshot(restored, index, Math.max(0, prefs.getInt(OFFSET_KEY, snapshot.optInt("offset"))));
        } catch (Exception invalid) { return null; }
    }
}
