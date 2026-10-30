package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/* JADX INFO: loaded from: classes2.dex */
final class Artwork {
    private static final int MAX_BYTES = 2500000;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(24576) { // from class: local.qingyin.mobile.Artwork.1
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.util.LruCache
        public int sizeOf(String key, Bitmap bitmap) {
            return Math.max(1, bitmap.getByteCount() / 1024);
        }
    };
    private static final ExecutorService WORK = Executors.newFixedThreadPool(3);
    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static final HashMap<String, ArrayList<WeakReference<ImageView>>> IN_FLIGHT = new HashMap<>();

    private Artwork() {
    }

    static FrameLayout frame(Context c, int corner) {
        FrameLayout frame = new FrameLayout(c);
        frame.setBackground(QingyinUi.gradient(c, -1451545, -2696715, corner));
        frame.setClipToOutline(true);
        TextView fallback = QingyinUi.text(c, "♫", 23, -5149300, true);
        fallback.setGravity(17);
        frame.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
        ImageView image = new ImageView(c);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setVisibility(8);
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    static void bind(FrameLayout frame, String source) {
        bindSized(frame, source, 780, source);
    }

    /** Editorial cards request a real large cover, independently of thumbnail cache entries. */
    static void bindLarge(FrameLayout frame, String source) {
        int width = frame.getWidth() > 0 ? frame.getWidth() : frame.getResources().getDisplayMetrics().widthPixels;
        int size = width <= 760 ? 800 : 1200;
        String large = source == null ? "" : source.replace("{size}", Integer.toString(size))
            .replaceFirst("/(soft/collection|custom)/[0-9]{2,4}/", "/$1/" + size + "/");
        bindSized(frame, large, 1280, source);
    }

    private static void bindSized(FrameLayout frame, String source, int maxEdge, String fallback) {
        ImageView image = (ImageView) frame.getChildAt(1);
        final String url = approved(source) ? source.trim() : "";
        final String key = url.isEmpty() ? "" : maxEdge == 780 ? url : url + "#decode=" + maxEdge;
        if (key.equals(image.getTag()) && image.getVisibility() == 0 && image.getDrawable() != null) {
            return;
        }
        image.setTag(key);
        if (url.isEmpty()) {
            image.setImageDrawable(null);
            image.setVisibility(8);
            return;
        }
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            image.setImageBitmap(cached);
            image.setVisibility(0);
            return;
        }
        image.setImageDrawable(null);
        image.setVisibility(8);
        synchronized (IN_FLIGHT) {
            ArrayList<WeakReference<ImageView>> waiters = IN_FLIGHT.get(key);
            if (waiters != null) {
                for (WeakReference<ImageView> ref : waiters) {
                    if (ref.get() == image) {
                        return;
                    }
                }
                waiters.add(new WeakReference<>(image));
                return;
            }
            ArrayList<WeakReference<ImageView>> waiters2 = new ArrayList<>();
            waiters2.add(new WeakReference<>(image));
            IN_FLIGHT.put(key, waiters2);
            WORK.execute(() -> {
                Bitmap decoded = download(url, maxEdge);
                if (decoded == null && !url.equals(fallback) && approved(fallback)) decoded = download(fallback, maxEdge);
                final Bitmap result = decoded;
                if (result != null) CACHE.put(key, result);
                UI.post(() -> Artwork.lambda$bind$0(key, result));
            });
        }
    }

    static /* synthetic */ void lambda$bind$1(final String url) {
        final Bitmap data = download(url);
        if (data != null) {
            CACHE.put(url, data);
        }
        UI.post(new Runnable() { // from class: local.qingyin.mobile.Artwork$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Artwork.lambda$bind$0(url, data);
            }
        });
    }

    static /* synthetic */ void lambda$bind$0(String url, Bitmap result) {
        ArrayList<WeakReference<ImageView>> waiting;
        synchronized (IN_FLIGHT) {
            waiting = IN_FLIGHT.remove(url);
        }
        if (waiting == null || result == null) {
            return;
        }
        for (WeakReference<ImageView> ref : waiting) {
            ImageView target = ref.get();
            if (target != null && url.equals(target.getTag())) {
                target.setImageBitmap(result);
                target.setVisibility(0);
            }
        }
    }

    static void loadBitmap(String source, final Consumer<Bitmap> done) {
        final String url = approved(source) ? source.trim() : "";
        if (url.isEmpty()) {
            UI.post(new Runnable() { // from class: local.qingyin.mobile.Artwork$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    done.accept(null);
                }
            });
            return;
        }
        final Bitmap cached = CACHE.get(url);
        if (cached != null) {
            UI.post(new Runnable() { // from class: local.qingyin.mobile.Artwork$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    done.accept(cached);
                }
            });
        } else {
            WORK.execute(new Runnable() { // from class: local.qingyin.mobile.Artwork$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    Artwork.lambda$loadBitmap$5(url, done);
                }
            });
        }
    }

    static /* synthetic */ void lambda$loadBitmap$5(String url, final Consumer done) {
        final Bitmap bitmap = download(url);
        if (bitmap != null) {
            CACHE.put(url, bitmap);
        }
        UI.post(new Runnable() { // from class: local.qingyin.mobile.Artwork$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                done.accept(bitmap);
            }
        });
    }

    static Bitmap glassCover(Bitmap source) {
        if (source == null || source.isRecycled()) {
            return null;
        }
        Bitmap result = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        RectF art = new RectF(7.0f, 7.0f, 313.0f, 313.0f);
        Path clip = new Path();
        clip.addRoundRect(art, 38.0f, 38.0f, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        int w = source.getWidth();
        int h = source.getHeight();
        int crop = Math.min(w, h);
        Rect src = new Rect((w - crop) / 2, (h - crop) / 2, (w + crop) / 2, (h + crop) / 2);
        Paint imagePaint = new Paint(3);
        canvas.drawBitmap(source, src, art, imagePaint);
        Paint shine = new Paint(1);
        shine.setShader(new LinearGradient(5.0f, 0.0f, 250.0f, 230.0f, new int[]{1392508927, 335544319, 16777215}, new float[]{0.0f, 0.43f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(art, 38.0f, 38.0f, shine);
        Paint edge = new Paint(1);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(4.5f);
        edge.setShader(new LinearGradient(0.0f, 0.0f, 320.0f, 320.0f, new int[]{-1073741825, 671088639, 268435455, -2097152001}, new float[]{0.0f, 0.32f, 0.75f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(10.0f, 10.0f, 310.0f, 310.0f), 35.0f, 35.0f, edge);
        canvas.restore();
        return result;
    }

    static void addGlassLayer(FrameLayout frame) {
        View overlay = new View(frame.getContext()) { // from class: local.qingyin.mobile.Artwork.2
            final Paint rim = new Paint(1);

            @Override // android.view.View
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }
                float d = getResources().getDisplayMetrics().density;
                float corner = 20.0f * d;
                float stroke = Math.max(1.5f, 2.0f * d);
                RectF edgeRect = new RectF(stroke, stroke, w - stroke, h - stroke);
                this.rim.setStyle(Paint.Style.STROKE);
                this.rim.setStrokeWidth(stroke);
                this.rim.setShader(new LinearGradient(0.0f, 0.0f, w, h, new int[]{-671088641, 704643071, 352321535, -1493172225}, new float[]{0.0f, 0.29f, 0.69f, 1.0f}, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(edgeRect, corner, corner, this.rim);
                this.rim.setShader(new LinearGradient(0.0f, 0.0f, w * 0.8f, h * 0.45f, 1124073471, 16777215, Shader.TileMode.CLAMP));
                this.rim.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(new RectF(stroke, stroke, w - stroke, h * 0.24f), corner, corner, this.rim);
                this.rim.setShader(null);
            }
        };
        overlay.setContentDescription(null);
        overlay.setImportantForAccessibility(2);
        overlay.setClickable(false);
        frame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private static boolean approved(String url) {
        if (url == null || url.length() > 1300) {
            return false;
        }
        try {
            Uri parsed = Uri.parse(url.trim());
            String host = parsed.getHost();
            if ("https".equalsIgnoreCase(parsed.getScheme()) && host != null && parsed.getUserInfo() == null && parsed.getPort() == -1) {
                String host2 = host.toLowerCase(Locale.ROOT);
                return host2.equals("kugou.com") || host2.endsWith(".kugou.com") || host2.equals("kgimg.com") || host2.endsWith(".kgimg.com") || host2.equals("kugoucdn.com") || host2.endsWith(".kugoucdn.com");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static Bitmap download(String url) {
        return download(url, 780);
    }

    private static Bitmap download(String url, int maxEdge) {
        HttpURLConnection connection = null;
        String current = url;
        for (int redirect = 0; redirect < 3; redirect++) {
            try {
                if (!approved(current)) {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    return null;
                }
                HttpURLConnection connection2 = (HttpURLConnection) new URL(current).openConnection();
                connection2.setInstanceFollowRedirects(false);
                connection2.setConnectTimeout(5000);
                connection2.setReadTimeout(7000);
                connection2.setRequestProperty("Accept", "image/avif,image/webp,image/jpeg,image/png,image/*");
                connection2.setRequestProperty("User-Agent", "QingyinAndroid/0.6");
                int status = connection2.getResponseCode();
                if (status < 300 || status >= 400) {
                    if (status != 200) {
                        if (connection2 != null) {
                            connection2.disconnect();
                        }
                        return null;
                    }
                    String type = connection2.getContentType();
                    if (type != null && type.toLowerCase(Locale.ROOT).startsWith("image/")) {
                        if (connection2.getContentLength() > 2500000) {
                            if (connection2 != null) {
                                connection2.disconnect();
                            }
                            return null;
                        }
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        InputStream stream = connection2.getInputStream();
                        try {
                            byte[] chunk = new byte[8192];
                            while (true) {
                                int bytes = stream.read(chunk);
                                if (bytes == -1) {
                                    if (stream != null) {
                                        stream.close();
                                    }
                                    byte[] body = output.toByteArray();
                                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                                    bounds.inJustDecodeBounds = true;
                                    BitmapFactory.decodeByteArray(body, 0, body.length, bounds);
                                    if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                                        BitmapFactory.Options opts = new BitmapFactory.Options();
                                        int sample = 1;
                                        while (true) {
                                            if (bounds.outWidth / sample <= maxEdge && bounds.outHeight / sample <= maxEdge) {
                                                break;
                                            }
                                            sample *= 2;
                                        }
                                        opts.inSampleSize = sample;
                                        Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(body, 0, body.length, opts);
                                        if (connection2 != null) {
                                            connection2.disconnect();
                                        }
                                        return bitmapDecodeByteArray;
                                    }
                                    if (connection2 != null) {
                                        connection2.disconnect();
                                    }
                                    return null;
                                }
                                if (output.size() + bytes > 2500000) {
                                    if (stream != null) {
                                        stream.close();
                                    }
                                    if (connection2 != null) {
                                        connection2.disconnect();
                                    }
                                    return null;
                                }
                                output.write(chunk, 0, bytes);
                            }
                        } catch (Throwable th) {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (Throwable th2) {
                                    th.addSuppressed(th2);
                                }
                            }
                            throw th;
                        }
                    }
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    return null;
                }
                String next = connection2.getHeaderField("Location");
                if (next == null) {
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    return null;
                }
                String full = new URL(new URL(current), next).toString();
                connection2.disconnect();
                connection = null;
                current = full;
            } catch (Exception e) {
                if (connection != null) {
                }
            } catch (Throwable th3) {
                if (connection != null) {
                    connection.disconnect();
                }
                throw th3;
            }
        }
        if (connection != null) {
            connection.disconnect();
        }
        return null;
    }
}
