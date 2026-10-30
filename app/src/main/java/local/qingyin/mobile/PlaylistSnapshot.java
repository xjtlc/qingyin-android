package local.qingyin.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
final class PlaylistSnapshot {
    private static final String PREF = "qingyin_complete_playlists_v1";

    static final class Result {
        final int count;
        final boolean hasMore;
        final int page;
        final ArrayList<Track> songs;

        Result(ArrayList<Track> songs, int page, int count, boolean more) {
            this.songs = songs;
            this.page = page;
            this.count = count;
            this.hasMore = more;
        }
    }

    private PlaylistSnapshot() {
    }

    static boolean important(JSONObject playlist) {
        String name = playlist != null ? playlist.optString("title", "") : "";
        return name.contains("我喜欢") || name.contains("默认收藏");
    }

    static String key(JSONObject list) {
        if (list == null) {
            return "";
        }
        String id = list.optString("listId", "");
        if (id.isEmpty()) {
            id = list.optString("globalId", "");
        }
        if (id.isEmpty()) {
            id = list.optString("id", "");
        }
        if (id.isEmpty()) {
            return "";
        }
        return Integer.toHexString((list.optInt("type", 0) + ":" + id).hashCode());
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("qingyin_complete_playlists_v1", 0);
    }

    static Result read(Context context, JSONObject list) {
        String key = key(list);
        if (key.isEmpty() || !important(list)) {
            return null;
        }
        String raw = preferences(context).getString("rows_" + key, "");
        if (raw.isEmpty()) {
            return null;
        }
        try {
            JSONObject state = new JSONObject(raw);
            JSONArray json = state.optJSONArray("songs");
            if (json != null && json.length() != 0 && json.length() <= 4000) {
                ArrayList<Track> rows = new ArrayList<>();
                for (int i = 0; i < json.length(); i++) {
                    Track song = OnlineApi.asTrack(json.optJSONObject(i));
                    if (song != null) {
                        rows.add(song);
                    }
                }
                int i2 = rows.size();
                if (i2 != json.length()) {
                    return null;
                }
                return new Result(rows, state.optInt("page", 1), state.optInt("count", rows.size()), state.optBoolean("hasMore", false));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    static boolean coverCountChanged(Context context, JSONObject list) {
        if (!important(list) || list == null) {
            return false;
        }
        String key = key(list);
        if (key.isEmpty()) {
            return false;
        }
        int count = list.optInt("count", 0);
        int remembered = preferences(context).getInt("cover_count_" + key, -1);
        return count > 0 && remembered >= 0 && remembered != count;
    }

    static String cover(Context context, JSONObject list) {
        if (!important(list)) {
            return "";
        }
        String key = key(list);
        return key.isEmpty() ? "" : preferences(context).getString("cover_" + key, "");
    }

    static void write(Context context, JSONObject list, ArrayList<Track> songs, int page, boolean more) {
        Uri u;
        String hash;
        if (!important(list) || more || songs.isEmpty()) {
            return;
        }
        if (songs.size() > 4000) {
            return;
        }
        String key = key(list);
        if (key.isEmpty()) {
            return;
        }
        JSONArray arr = new JSONArray();
        for (Track song : songs) {
            if (!song.isOnline() || (hash = (u = Uri.parse(song.uri)).getLastPathSegment()) == null || !hash.matches("(?i)[a-f0-9]{32}")) {
                return;
            }
            JSONObject item = new JSONObject();
            try {
                item.put("hash", hash);
                item.put("title", song.title);
                item.put("artist", song.artist);
                item.put("duration", song.durationMs / 1000);
                item.put("albumId", u.getQueryParameter("albumId"));
                item.put("albumAudioId", u.getQueryParameter("audioId"));
                item.put("artistId", song.artistId);
                item.put("addedAt", song.addedAt);
                item.put("cover", song.coverUrl);
                item.put("album", song.albumTitle);
                arr.put(item);
            } catch (Exception e) {
                return;
            }
        }
        String newestCover = "";
        long newestTimestamp = 0;
        for (int i = 0; i < songs.size(); i++) {
            Track song2 = songs.get(i);
            if (song2.coverUrl != null && song2.coverUrl.startsWith("https://") && (newestCover.isEmpty() || ((song2.addedAt > 0 && song2.addedAt >= newestTimestamp) || newestTimestamp == 0))) {
                newestCover = song2.coverUrl;
                newestTimestamp = song2.addedAt;
            }
        }
        try {
            try {
                JSONObject json = new JSONObject().put("songs", arr).put("page", page).put("count", list.optInt("count", songs.size())).put("hasMore", false);
                SharedPreferences.Editor editor = preferences(context).edit().putString("rows_" + key, json.toString());
                if (!newestCover.isEmpty()) {
                    editor.putString("cover_" + key, newestCover);
                    editor.putInt("cover_count_" + key, list.optInt("count", songs.size()));
                }
                editor.apply();
            } catch (Exception e2) {
            }
        } catch (Exception e3) {
        }
    }

    static void rememberCover(Context context, JSONObject list, String cover) {
        if (!important(list) || cover == null || !cover.startsWith("https://")) {
            return;
        }
        String identity = key(list);
        if (!identity.isEmpty()) {
            preferences(context).edit().putString("cover_" + identity, cover).putInt("cover_count_" + identity, list.optInt("count", 0)).apply();
        }
    }

    static void invalidate(Context context, JSONObject list) {
        String key = key(list);
        if (!key.isEmpty()) {
            preferences(context).edit().remove("rows_" + key).apply();
        }
    }
}
