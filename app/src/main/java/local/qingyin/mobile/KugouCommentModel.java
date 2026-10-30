package local.qingyin.mobile;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
final class KugouCommentModel {

    static final class Comment {
        final String avatar;
        final ArrayList<String> images;
        final long likes;
        final String name;
        final int replyCount;
        final String specialId;
        final String text;
        final String tid;
        final String when;

        Comment(String tid, String specialId, String name, String avatar, String text, String when, long likes, int replyCount, ArrayList<String> images) {
            this.tid = tid;
            this.specialId = specialId;
            this.name = name;
            this.avatar = avatar;
            this.text = text;
            this.when = when;
            this.likes = likes;
            this.replyCount = replyCount;
            this.images = images;
        }

        String key() {
            return !this.tid.isEmpty() ? this.specialId + "/" + this.tid : "anonymous/" + this.name.hashCode() + "/" + this.when.hashCode() + "/" + this.text.hashCode();
        }
    }

    static final class Result {
        final ArrayList<Comment> items;
        final int rawCount;
        final boolean recognized;
        final String specialId;
        final int total;

        Result(ArrayList<Comment> rows, boolean ok, int count, int originalRows, String id) {
            this.items = rows;
            this.recognized = ok;
            this.total = count;
            this.rawCount = originalRows;
            this.specialId = id;
        }
    }

    static Result parse(JSONObject raw) {
        ArrayList<Comment> items = new ArrayList<>();
        JSONArray rows = rows(raw, 0);
        if (rows != null) {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    Comment parsed = comment(row);
                    if (!parsed.text.isEmpty() || !parsed.images.isEmpty()) {
                        items.add(parsed);
                    }
                }
            }
        }
        return new Result(items, rows != null, total(raw, 0), rows == null ? 0 : rows.length(), specialId(raw, 0));
    }

    static String value(JSONObject src, String... fields) {
        if (src == null) {
            return "";
        }
        for (String key : fields) {
            Object value = src.opt(key);
            if ((value instanceof String) && !((String) value).trim().isEmpty()) {
                return ((String) value).trim();
            }
            if (value instanceof Number) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private static JSONObject nested(JSONObject src, String... keys) {
        if (src == null) {
            return null;
        }
        for (String k : keys) {
            JSONObject o = src.optJSONObject(k);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    static JSONArray rows(JSONObject src, int depth) {
        if (src == null || depth > 5) {
            return null;
        }
        String[] strArr = {"list", "comments", "comment_list", "info", "rows", "items", "hotlist", "hot_comments", "hotcomments", "reply_list", "replies", "records"};
        for (int i = 0; i < 12; i++) {
            JSONArray arr = src.optJSONArray(strArr[i]);
            if (arr != null) {
                return arr;
            }
        }
        String[] strArr2 = {"data", "body", "result", "comment", "reply_data"};
        for (int i2 = 0; i2 < 5; i2++) {
            String k = strArr2[i2];
            JSONArray ar = src.optJSONArray(k);
            if (ar != null) {
                return ar;
            }
            JSONArray inner = rows(src.optJSONObject(k), depth + 1);
            if (inner != null) {
                return inner;
            }
        }
        return null;
    }

    static int total(JSONObject src, int depth) {
        if (src == null || depth > 5) {
            return -1;
        }
        String s = value(src, "total", "comment_total", "comment_count", "commentcount", "comments_num", "total_count", "totalCount", "count", "num");
        if (!s.isEmpty()) {
            try {
                long value = Long.parseLong(s);
                if (value >= 0 && value < 2147483647L) {
                    return (int) value;
                }
            } catch (Exception e) {
            }
        }
        String[] strArr = {"data", "body", "result"};
        for (int i = 0; i < 3; i++) {
            String k = strArr[i];
            int inner = total(src.optJSONObject(k), depth + 1);
            if (inner >= 0) {
                return inner;
            }
        }
        return -1;
    }

    static String specialId(JSONObject src, int depth) {
        if (src == null || depth > 5) {
            return "";
        }
        String value = value(src, "special_child_id", "childrenid", "special_id");
        if (!value.isEmpty()) {
            return value;
        }
        String[] strArr = {"data", "body", "result"};
        for (int i = 0; i < 3; i++) {
            String key = strArr[i];
            String inner = specialId(src.optJSONObject(key), depth + 1);
            if (!inner.isEmpty()) {
                return inner;
            }
        }
        return "";
    }

    static Comment comment(JSONObject item) {
        JSONObject person = nested(item, "user", "userinfo", "user_info", "uinfo", "author");
        String name = value(item, "user_name", "nickname", "username", "nick_name", "usernick", "userName");
        if (name.isEmpty()) {
            name = value(person, "nickname", "name", "user_name", "username");
        }
        if (name.isEmpty()) {
            name = "酷狗用户";
        }
        String avatar = value(item, "user_pic", "avatar", "headpic", "headimg", "pic", "user_avatar");
        if (avatar.isEmpty()) {
            avatar = value(person, "user_pic", "pic", "avatar", "headimg", "head_pic");
        }
        String avatar2 = avatar;
        String content = value(item, "content", "text", "comment", "content_text", "comment_content");
        String date = value(item, "time", "addtime", "add_time", "create_time", "created_at", "pub_time", "dateline");
        String tid = value(item, "tid", "comment_id", "commentid", "id", "cid");
        String special = value(item, "special_child_id", "childrenid", "special_id");
        long likes = number(value(item, "like", "likecount", "like_count", "likenum", "praise_count", "like_num", "praise"));
        int replies = (int) Math.min(1000000L, number(value(item, "reply_count", "reply_num", "replynum", "replycnt", "replyCount", "sub_count", "reply_total")));
        return new Comment(tid, special, name, avatar2, content, date, likes, replies, images(item));
    }

    static long number(String value) {
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    static ArrayList<String> images(JSONObject item) {
        int i;
        char c;
        char c2;
        char c3;
        char c4;
        String url;
        ArrayList<String> result = new ArrayList<>();
        if (item == null) {
            return result;
        }
        int i2 = 6;
        char c5 = 4;
        char c6 = 5;
        String[] strArr = {"images", "pics", "pic_list", "img_list", "image_list", "image"};
        int i3 = 0;
        while (i3 < i2) {
            String key = strArr[i3];
            JSONArray arr = item.optJSONArray(key);
            if (arr == null) {
                i = i2;
                c = c6;
                c5 = c5;
            } else {
                int i4 = 0;
                while (true) {
                    if (i4 >= arr.length()) {
                        c = c6;
                        i = 6;
                        break;
                    }
                    if (result.size() >= 9) {
                        i = 6;
                        c = 5;
                        break;
                    }
                    Object value = arr.opt(i4);
                    if (value instanceof JSONObject) {
                        c2 = 4;
                        c4 = 5;
                        c3 = 6;
                        url = value((JSONObject) value, "url", "pic", "image", "src", "original", "imgurl", "big_pic");
                    } else {
                        c2 = c5;
                        c3 = 6;
                        c4 = 5;
                        url = value instanceof String ? (String) value : "";
                    }
                    if (url.startsWith("https://") && !result.contains(url)) {
                        result.add(url);
                    }
                    i4++;
                    c6 = c4;
                    c5 = c2;
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
            String direct = value(item, key);
            if (direct.startsWith("https://")) {
                result.add(direct);
            }
            if (!result.isEmpty()) {
                return result;
            }
            i3++;
            i2 = i;
            c6 = c;
            c5 = c5;
        }
        return result;
    }

    static long timeMillis(String raw) {
        long j = 0;
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }
        String input = raw.trim();
        try {
            long value = Long.parseLong(input);
            if (value > 1000000000 && value < 100000000000L) {
                value *= 1000;
            }
            if (value < 100000000000L || value >= 4102444800000L) {
                return 0L;
            }
            return value;
        } catch (NumberFormatException e) {
            String candidate = input.replace('T', ' ');
            String[] strArr = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd"};
            for (int i = 0; i < 4; i++) {
                String fmt = strArr[i];
                try {
                    SimpleDateFormat parser = new SimpleDateFormat(fmt, Locale.CHINA);
                    parser.setLenient(false);
                    ParsePosition position = new ParsePosition(0);
                    Date parsed = parser.parse(candidate, position);
                    if (parsed != null && (position.getIndex() == candidate.length() || candidate.substring(position.getIndex()).matches("[.]\\d+(Z|[+-]\\d\\d:?\\d\\d)?"))) {
                        return parsed.getTime();
                    }
                } catch (Exception e2) {
                }
            }
            return j;
        }
    }

    static String date(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        long timestamp = timeMillis(raw);
        if (timestamp > 0) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(timestamp));
        }
        String text = raw.trim();
        return text.length() > 25 ? text.substring(0, 25) : text;
    }

    static String compact(long n) {
        return n >= 10000 ? String.format(Locale.CHINA, "%.1f万", Double.valueOf(n / 10000.0d)) : String.valueOf(n);
    }

    private KugouCommentModel() {
    }
}
