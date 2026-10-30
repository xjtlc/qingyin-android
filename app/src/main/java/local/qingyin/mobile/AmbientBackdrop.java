package local.qingyin.mobile;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import java.util.HashMap;
import java.util.function.Consumer;

/* JADX INFO: loaded from: classes2.dex */
final class AmbientBackdrop extends View {
    private static final int BASE = -329477;
    private static final HashMap<String, Palette> CACHE = new HashMap<>();
    private static final int NIGHT = -15197657;
    private ValueAnimator animation;
    private final Paint background;
    private float blend;
    private Palette current;
    private final Palette[] fallback;
    private Runnable glassRefresh;
    private long lastFrameNotified;
    private final Palette[] nights;
    private final Palette pearl;
    private Palette previous;
    private String selection;
    private String style;
    private int ticket;

    private static final class Palette {
        final Paint[] paints;

        Palette(int... shades) {
            this(false, shades);
        }

        Palette(boolean restrained, int... shades) {
            this.paints = new Paint[3];
            for (int i = 0; i < 3; i++) {
                int shade = shades[i];
                Paint paint = new Paint(1);
                paint.setShader(new RadialGradient(0.0f, 0.0f, 1.0f, new int[]{AmbientBackdrop.withAlpha(shade, restrained ? 112 : 208), AmbientBackdrop.withAlpha(shade, restrained ? 53 : 110), AmbientBackdrop.withAlpha(shade, 0)}, new float[]{0.0f, 0.49f, 1.0f}, Shader.TileMode.CLAMP));
                this.paints[i] = paint;
            }
        }
    }

    AmbientBackdrop(Context ctx) {
        super(ctx);
        Palette palette;
        this.fallback = new Palette[]{new Palette(-1463874, -4404747, -731977), new Palette(-3426832, -4531223, -1260327), new Palette(-5322251, -4990757, -2506261), new Palette(-1456687, -3814168, -1450809), new Palette(-3031572, -4925721, -1324075)};
        this.pearl = new Palette(true, -1460017, -3815445, -1125432);
        this.nights = new Palette[]{new Palette(true, -7443028, -11375206, -7707269), new Palette(true, -9278028, -12612979, -7646083), new Palette(true, -11174726, -12812922, -9149782), new Palette(true, -6917212, -11437164, -6460542), new Palette(true, -8946505, -12092271, -6527863)};
        this.style = "B";
        this.background = new Paint(1);
        this.blend = 1.0f;
        this.selection = "";
        this.ticket = 0;
        this.lastFrameNotified = 0L;
        setClickable(false);
        setImportantForAccessibility(2);
        this.style = AppearancePreferences.get(ctx);
        if ("C".equals(this.style)) {
            palette = this.nights[0];
        } else {
            palette = "A".equals(this.style) ? this.pearl : this.fallback[0];
        }
        this.current = palette;
    }

    static int baseColor() {
        return -329477;
    }

    static int baseColor(Context context) {
        return AppearancePreferences.dark(context) ? -15197657 : -329477;
    }

    void onGlassRefresh(Runnable value) {
        this.glassRefresh = value;
    }

    void show(int section, String artworkUrl) {
        Palette cached;
        final int tab = Math.max(0, Math.min(section, this.fallback.length - 1));
        final String art = artworkUrl == null ? "" : artworkUrl.trim();
        String selected = AppearancePreferences.get(getContext());
        String key = selected + "|" + tab + "|" + ("B".equals(selected) ? art : "");
        if (key.equals(this.selection)) {
            return;
        }
        this.selection = key;
        final int version = this.ticket + 1;
        this.ticket = version;
        this.style = selected;
        if ("A".equals(selected)) {
            transition(this.pearl);
            invalidate();
            return;
        }
        if ("C".equals(selected)) {
            transition(this.nights[tab]);
            invalidate();
        } else {
            if (art.isEmpty()) {
                transition(this.fallback[tab]);
                return;
            }
            synchronized (CACHE) {
                cached = CACHE.get(art);
            }
            if (cached != null) {
                transition(cached);
            } else {
                Artwork.loadBitmap(art, new Consumer() { // from class: local.qingyin.mobile.AmbientBackdrop$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AmbientBackdrop.this.m12lambda$show$0$localqingyinmobileAmbientBackdrop(version, tab, art, (Bitmap) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: renamed from: lambda$show$0$local-qingyin-mobile-AmbientBackdrop, reason: not valid java name */
    /* synthetic */ void m12lambda$show$0$localqingyinmobileAmbientBackdrop(int version, int tab, String art, Bitmap bitmap) {
        if (version != this.ticket) {
            return;
        }
        if (bitmap == null) {
            transition(this.fallback[tab]);
            return;
        }
        Palette palette = palette(bitmap);
        synchronized (CACHE) {
            if (CACHE.size() >= 24) {
                CACHE.clear();
            }
            CACHE.put(art, palette);
        }
        transition(palette);
    }

    private void transition(Palette next) {
        if (next == this.current) {
            return;
        }
        if (this.animation != null) {
            this.animation.cancel();
        }
        this.previous = this.current;
        this.current = next;
        this.blend = 0.0f;
        this.animation = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.animation.setDuration(820L);
        this.animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: local.qingyin.mobile.AmbientBackdrop$$ExternalSyntheticLambda1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                AmbientBackdrop.this.m13lambda$transition$1$localqingyinmobileAmbientBackdrop(valueAnimator);
            }
        });
        this.animation.start();
    }

    /* JADX INFO: renamed from: lambda$transition$1$local-qingyin-mobile-AmbientBackdrop, reason: not valid java name */
    /* synthetic */ void m13lambda$transition$1$localqingyinmobileAmbientBackdrop(ValueAnimator value) {
        this.blend = ((Float) value.getAnimatedValue()).floatValue();
        invalidate();
        long now = SystemClock.uptimeMillis();
        if (this.glassRefresh != null && now - this.lastFrameNotified >= 210) {
            this.lastFrameNotified = now;
            this.glassRefresh.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int withAlpha(int color, int alpha) {
        return (16777215 & color) | (alpha << 24);
    }

    private static int pastel(float hue, float sat, float value) {
        return Color.HSVToColor(new float[]{(hue + 360.0f) % 360.0f, sat, value});
    }

    private static Palette palette(Bitmap image) {
        float x = 0.0f;
        float y = 0.0f;
        float sum = 0.0f;
        float[] hsv = new float[3];
        int width = image.getWidth();
        int height = image.getHeight();
        char c = 1;
        if (width < 1 || height < 1) {
            return new Palette(-1588774, -4207379, -1452349);
        }
        int row = 1;
        while (true) {
            int i = 15;
            float f = 0.96f;
            if (row > 15) {
                break;
            }
            int col = 1;
            while (col <= i) {
                int pixel = image.getPixel(Math.min(width - 1, (col * width) / 16), Math.min(height - 1, (row * height) / 16));
                Color.colorToHSV(pixel, hsv);
                if (hsv[2] < 0.16f || hsv[2] > f || hsv[c] < 0.1f) {
                    y = y;
                } else {
                    float weight = (hsv[c] * hsv[c]) + 0.18f;
                    double angle = Math.toRadians(hsv[0]);
                    row = row;
                    float x2 = (float) (((double) x) + (Math.cos(angle) * ((double) weight)));
                    y = (float) (((double) y) + (Math.sin(angle) * ((double) weight)));
                    sum += weight;
                    x = x2;
                }
                col++;
                row = row;
                c = 1;
                i = 15;
                f = 0.96f;
            }
            row++;
            c = 1;
        }
        float hue = sum < 0.01f ? 305.0f : (float) ((Math.toDegrees(Math.atan2(y, x)) + 360.0d) % 360.0d);
        return new Palette(pastel(hue, 0.34f, 0.96f), pastel(68.0f + hue, 0.26f, 0.96f), pastel(hue - 74.0f, 0.27f, 0.97f));
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor("C".equals(this.style) ? -15197657 : -329477);
        if (this.previous != null && this.blend < 1.0f) {
            drawPalette(canvas, this.previous, 1.0f - this.blend);
        }
        drawPalette(canvas, this.current, this.blend);
    }

    private void drawPalette(Canvas canvas, Palette palette, float opacity) {
        if (palette == null || opacity <= 0.0f) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float[][] geometry = {new float[]{0.05f, 0.07f, 0.92f, 0.52f}, new float[]{0.99f, 0.79f, 0.81f, 0.63f}, new float[]{0.94f, 0.28f, 0.64f, 0.54f}};
        for (int i = 0; i < palette.paints.length; i++) {
            Paint paint = palette.paints[i];
            paint.setAlpha(Math.round(255.0f * opacity));
            int save = canvas.save();
            float[] g = geometry[i];
            canvas.translate(g[0] * width, g[1] * height);
            canvas.scale(g[2] * width, g[3] * height);
            canvas.drawCircle(0.0f, 0.0f, 1.0f, paint);
            canvas.restoreToCount(save);
        }
    }

    void release() {
        this.ticket++;
        if (this.animation != null) {
            this.animation.cancel();
            this.animation = null;
        }
        this.glassRefresh = null;
    }
}
