package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/* JADX INFO: loaded from: classes2.dex */
final class ArtistTheme {
    private ArtistTheme() {
    }

    static int color(Bitmap bitmap) {
        Bitmap bitmap2 = bitmap;
        if (bitmap2 == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            return -13356985;
        }
        float xx = 0.0f;
        float yy = 0.0f;
        float weightSum = 0.0f;
        float sv = 0.0f;
        float bv = 0.0f;
        float[] hsv = new float[3];
        int y = 1;
        while (true) {
            int i = 18;
            char c = 2;
            int i2 = 1;
            if (y > 18) {
                break;
            }
            int x = 1;
            while (x <= i) {
                int px = bitmap2.getPixel(Math.min(bitmap.getWidth() - i2, (bitmap.getWidth() * x) / 19), Math.min(bitmap.getHeight() - i2, (bitmap.getHeight() * y) / 19));
                Color.colorToHSV(px, hsv);
                if (hsv[c] < 0.12f || hsv[c] > 0.96f) {
                    xx = xx;
                } else {
                    float wt = (hsv[i2] * hsv[i2] * 2.0f) + 0.17f;
                    double a = Math.toRadians(hsv[0]);
                    xx += ((float) Math.cos(a)) * wt;
                    yy += ((float) Math.sin(a)) * wt;
                    sv += hsv[1] * wt;
                    bv += hsv[2] * wt;
                    weightSum += wt;
                }
                x++;
                i = 18;
                c = 2;
                i2 = 1;
                bitmap2 = bitmap;
            }
            y++;
            bitmap2 = bitmap;
        }
        if (weightSum < 1.0f) {
            return -13356985;
        }
        float hue = (float) Math.toDegrees(Math.atan2(yy, xx));
        if (hue < 0.0f) {
            hue += 360.0f;
        }
        float saturation = Math.min(0.67f, Math.max(0.25f, (sv / weightSum) * 0.73f));
        float value = Math.min(0.39f, Math.max(0.19f, (bv / weightSum) * 0.48f));
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }

    static GradientDrawable gradient(int first, int second, int corner) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{first, second});
        d.setCornerRadius(corner);
        return d;
    }

    static int darker(int col, float scale) {
        return Color.rgb(Math.round(Color.red(col) * scale), Math.round(Color.green(col) * scale), Math.round(Color.blue(col) * scale));
    }

    static GradientDrawable glass(Context c, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(671088639);
        d.setCornerRadius(QingyinUi.dp(c, radius));
        d.setStroke(Math.max(1, QingyinUi.dp(c, 1.0f)), 905969663);
        return d;
    }
}
