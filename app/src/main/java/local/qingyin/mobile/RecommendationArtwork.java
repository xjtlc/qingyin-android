package local.qingyin.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Preview artwork only: personalized song lists always come from the service. */
final class RecommendationArtwork {
    private static final String PREFS = "recommendation_artwork";
    private RecommendationArtwork() {}
    private static String key(String method) {
        return "dailyRecommendations".equals(method) ? "daily" : "guessYouLike".equals(method) ? "guess" : "";
    }
    private static SharedPreferences state(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private static String cleanAccount(String account) { return account == null ? "" : account.trim(); }
    private static int today() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR);
    }
    static synchronized String accountId(Context context) { return state(context).getString("account", ""); }
    static synchronized long epoch(Context context) { return state(context).getLong("epoch", 0); }
    /** A changed account or logout invalidates cached previews and pending writes. */
    static synchronized boolean setAccount(Context context, String account) {
        String selected = cleanAccount(account);
        SharedPreferences prefs = state(context);
        if (selected.equals(prefs.getString("account", ""))) return false;
        long next = prefs.getLong("epoch", 0) + 1;
        prefs.edit().clear().putString("account", selected).putLong("epoch", next).apply();
        return true;
    }
    static synchronized boolean save(Context context, String method, JSONArray songs,
                                     String responseAccountId, long expectedEpoch) {
        String selected = cleanAccount(responseAccountId), itemKey = key(method);
        SharedPreferences prefs = state(context);
        if (itemKey.isEmpty() || songs == null || selected.isEmpty()
                || !selected.equals(prefs.getString("account", ""))
                || expectedEpoch != prefs.getLong("epoch", 0)) return false;
        JSONArray urls = new JSONArray();
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < songs.length() && urls.length() < 3; i++) {
            JSONObject item = songs.optJSONObject(i);
            if (item == null) continue;
            String url = item.optString("cover", "").trim();
            if (url.startsWith("https://") && unique.add(url)) urls.put(url);
        }
        if (urls.length() == 0) return false;
        prefs.edit().putString(itemKey, urls.toString()).putString(itemKey + "_account", selected)
            .putLong(itemKey + "_time", System.currentTimeMillis()).putInt(itemKey + "_day", today()).apply();
        return true;
    }
    static synchronized List<String> load(Context context, String method) {
        ArrayList<String> urls = new ArrayList<>();
        String itemKey = key(method);
        SharedPreferences prefs = state(context);
        String selected = prefs.getString("account", "");
        if (itemKey.isEmpty() || selected.isEmpty() || !selected.equals(prefs.getString(itemKey + "_account", ""))) return urls;
        long now = System.currentTimeMillis(), time = prefs.getLong(itemKey + "_time", 0);
        if (time <= 0 || time > now + 60000) return urls;
        if (prefs.getInt(itemKey + "_day", 0) != today()) return urls;
        if ("guess".equals(itemKey) && now - time > 86400000) return urls;
        try {
            JSONArray covers = new JSONArray(prefs.getString(itemKey, "[]"));
            for (int i = 0; i < Math.min(3, covers.length()); i++) {
                String url = covers.optString(i, "");
                if (url.startsWith("https://")) urls.add(url);
            }
        } catch (Exception ignored) {}
        return urls;
    }
    static synchronized boolean fresh(Context context, String method) {
        String itemKey = key(method);
        if (itemKey.isEmpty() || load(context, method).isEmpty()) return false;
        return "daily".equals(itemKey) || System.currentTimeMillis() - state(context).getLong(itemKey + "_time", 0) < 1200000;
    }
    /** Explicit refresh preserves identity and rejects responses started before refresh. */
    static synchronized void clear(Context context) {
        SharedPreferences prefs = state(context);
        String selected = prefs.getString("account", "");
        long next = prefs.getLong("epoch", 0) + 1;
        prefs.edit().clear().putString("account", selected).putLong("epoch", next).apply();
    }
}
