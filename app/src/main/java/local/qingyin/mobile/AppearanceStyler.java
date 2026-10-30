package local.qingyin.mobile;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes2.dex */
final class AppearanceStyler {
    private static final WeakHashMap<TextView, Integer> BASE_TEXT = new WeakHashMap<>();
    private static final WeakHashMap<View, Drawable> BASE_CARD = new WeakHashMap<>();
    private static final WeakHashMap<View, Boolean> PRESERVE_BACKGROUND = new WeakHashMap<>();
    static void preserveBackground(View view){PRESERVE_BACKGROUND.put(view,Boolean.TRUE);}

    private AppearanceStyler() {
    }

    static void apply(View content, boolean dark) {
        if (content == null) {
            return;
        }
        recolor(content, dark);
    }

    private static void recolor(View v, boolean dark) {
        if ((v instanceof TextView) && !(v.getBackground() instanceof FrostedGlassDrawable)) {
            TextView text = (TextView) v;
            if (!BASE_TEXT.containsKey(text)) {
                BASE_TEXT.put(text, Integer.valueOf(text.getCurrentTextColor()));
            }
            Integer original = BASE_TEXT.get(text);
            if (original != null) {
                int target = original.intValue();
                if (dark) {
                    target = lighten(target);
                }
                if (text.getCurrentTextColor() != target) {
                    text.setTextColor(target);
                }
            }
        }
        if (!PRESERVE_BACKGROUND.containsKey(v) && ((v instanceof LinearLayout) || (v instanceof TextView)) && v.getBackground() != null && !(v.getBackground() instanceof FrostedGlassDrawable)) {
            if (!BASE_CARD.containsKey(v)) {
                BASE_CARD.put(v, v.getBackground());
            }
            Drawable original2 = BASE_CARD.get(v);
            if (dark) {
                v.setBackground(QingyinUi.touch(v.getContext(), v instanceof TextView ? -1002226603 : -1288030646, 17.0f));
            } else if (original2 != null && v.getBackground() != original2) {
                v.setBackground(original2);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) v;
            for (int i = 0; i < parent.getChildCount(); i++) {
                recolor(parent.getChildAt(i), dark);
            }
        }
    }

    private static int lighten(int color) {
        if (Color.alpha(color) < 180) {
            return color;
        }
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        if (r > g * 1.24f && r > b * 1.14f && r > 145) {
            return -29526;
        }
        float brightness = (((r * 0.2126f) + (g * 0.7152f)) + (b * 0.0722f)) / 255.0f;
        if (brightness >= 0.74f) {
            return color;
        }
        return brightness < 0.42f ? -592902 : -3619886;
    }
}
