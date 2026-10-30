package local.qingyin.mobile;

import android.content.Context;

/* JADX INFO: loaded from: classes2.dex */
final class AppearancePreferences {
    static final String A = "A";
    static final String B = "B";
    static final String C = "C";
    private static final String KEY = "background_style";
    static final String[] LABELS = {"A  ·  珍珠白渐变", "B  ·  专辑动态柔光", "C  ·  深色沉浸"};
    private static final String PREF = "qingyin_appearance";

    private AppearancePreferences() {
    }

    static String get(Context context) {
        String value = context.getSharedPreferences("qingyin_appearance", 0).getString("background_style", "B");
        return ("A".equals(value) || "C".equals(value)) ? value : "B";
    }

    static int index(Context c) {
        String mode = get(c);
        if ("A".equals(mode)) {
            return 0;
        }
        return "C".equals(mode) ? 2 : 1;
    }

    static void set(Context context, String selected) {
        String mode = ("A".equals(selected) || "C".equals(selected)) ? selected : "B";
        context.getSharedPreferences("qingyin_appearance", 0).edit().putString("background_style", mode).apply();
    }

    static String label(Context c) {
        return LABELS[index(c)];
    }

    static boolean dark(Context context) {
        return "C".equals(get(context));
    }
}
