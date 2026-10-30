package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: classes2.dex */
final class RadioVisuals {
    private static final int[][] PALETTES = {new int[]{-7387537, -1468502}, new int[]{-13082739, -7887139}, new int[]{-10990708, -3889181}, new int[]{-14124423, -7548985}, new int[]{-5610927, -871013}, new int[]{-9872005, -4147497}};
    private static final LruCache<String, Bitmap> ICONS = new LruCache<String, Bitmap>(6144) { // from class: local.qingyin.mobile.RadioVisuals.1
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.util.LruCache
        public int sizeOf(String url, Bitmap b) {
            return Math.max(1, b.getByteCount() / 1024);
        }
    };
    private static final ExecutorService WORK = Executors.newFixedThreadPool(3);
    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static final HashMap<String, ArrayList<WeakReference<ImageView>>> WAITERS = new HashMap<>();

    private RadioVisuals() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int dp(Context c, float n) {
        return QingyinUi.dp(c, n);
    }

    static FrameLayout cover(Context context, RadioDirectory.Station station) {
        FrameLayout frame = new FrameLayout(context);
        frame.setBackground(QingyinUi.rounded(context, -1122328, 14.0f));
        frame.setClipToOutline(true);
        frame.addView(new RadioPicture(context, station.uuid, false), new FrameLayout.LayoutParams(-1, -1));
        ImageView logo = new ImageView(context);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setVisibility(8);
        frame.addView(logo, new FrameLayout.LayoutParams(-1, -1));
        showLogo(logo, station.favicon);
        return frame;
    }

    static View headerArt(Context context) {
        return new RadioPicture(context, "hero", true);
    }

    private static final class RadioPicture extends View {
        private final boolean banner;
        private final Paint p;
        private final int[] tones;

        RadioPicture(Context context, String stationId, boolean hero) {
            super(context);
            this.p = new Paint(1);
            this.banner = hero;
            this.tones = RadioVisuals.PALETTES[(stationId.hashCode() & Integer.MAX_VALUE) % RadioVisuals.PALETTES.length];
            setImportantForAccessibility(2);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w > 0.0f && h > 0.0f) {
                this.p.setShader(new LinearGradient(0.0f, 0.0f, w, h, this.banner ? -14210218 : this.tones[0], this.banner ? -2526565 : this.tones[1], Shader.TileMode.CLAMP));
                canvas.drawRect(0.0f, 0.0f, w, h, this.p);
                this.p.setShader(null);
                float w2 = 0.06f;
                float f = 0.065f;
                float f2 = 2.0f;
                if (!this.banner) {
                    this.p.setColor(654311423);
                    canvas.drawCircle(w * 0.96f, (-h) * 0.05f, 0.53f * w, this.p);
                    canvas.drawCircle((-w) * 0.15f, h * 0.85f, w * 0.38f, this.p);
                    float cx = w * 0.5f;
                    float cy = h * 0.5f;
                    float r = Math.min(w, h) * 0.34f;
                    this.p.setColor(-1107692289);
                    canvas.drawCircle(cx, cy, r, this.p);
                    this.p.setColor(this.tones[0]);
                    canvas.drawCircle(cx, cy, 0.78f * r, this.p);
                    this.p.setStyle(Paint.Style.STROKE);
                    this.p.setStrokeWidth(Math.max(1, RadioVisuals.dp(getContext(), 1.1f)));
                    this.p.setColor(-1191182337);
                    canvas.drawCircle(cx, cy, 0.61f * r, this.p);
                    canvas.drawCircle(cx, cy, 0.43f * r, this.p);
                    this.p.setStyle(Paint.Style.FILL);
                    this.p.setColor(-333327);
                    canvas.drawCircle(cx, cy, 0.23f * r, this.p);
                    this.p.setColor(this.tones[0]);
                    canvas.drawCircle(cx, cy, 0.075f * r, this.p);
                    this.p.setColor(-1711276033);
                    int i = 0;
                    while (i < 4) {
                        float x = w * ((i * f) + 0.16f);
                        float mid = h * 0.83f;
                        float height = h * (((i % 3) * 0.04f) + 0.06f);
                        canvas.drawRoundRect(x, mid - height, x + Math.max(2, RadioVisuals.dp(getContext(), 2.0f)), mid + height, RadioVisuals.dp(getContext(), 1.0f), RadioVisuals.dp(getContext(), 1.0f), this.p);
                        i++;
                        cy = cy;
                        r = r;
                        f = 0.065f;
                    }
                    return;
                }
                this.p.setColor(419430399);
                canvas.drawCircle(0.93f * w, (-h) * 0.26f, 1.14f * h, this.p);
                this.p.setColor(587202559);
                canvas.drawCircle(0.82f * w, h * 0.51f, 0.49f * h, this.p);
                float cx2 = w * 0.79f;
                float cy2 = h * 0.51f;
                float r2 = h * 0.38f;
                this.p.setColor(-1107496459);
                canvas.drawCircle(cx2, cy2, r2, this.p);
                this.p.setColor(-12499090);
                canvas.drawCircle(cx2, cy2, 0.91f * r2, this.p);
                this.p.setStyle(Paint.Style.STROKE);
                this.p.setStrokeWidth(RadioVisuals.dp(getContext(), 1.0f));
                for (int i2 = 0; i2 < 5; i2++) {
                    this.p.setColor(1291845631);
                    canvas.drawCircle(cx2, cy2, ((i2 * 0.11f) + 0.34f) * r2, this.p);
                }
                this.p.setStyle(Paint.Style.FILL);
                this.p.setColor(-936754);
                canvas.drawCircle(cx2, cy2, 0.24f * r2, this.p);
                this.p.setColor(-11979685);
                canvas.drawCircle(cx2, cy2, 0.065f * r2, this.p);
                this.p.setColor(-1996488705);
                int i3 = 0;
                while (i3 < 9) {
                    float x2 = (0.46f * w) + (i3 * w * 0.025f);
                    float len = h * (((i3 % 4) * 0.035f) + w2);
                    canvas.drawRoundRect(x2, cy2 - len, x2 + RadioVisuals.dp(getContext(), 3.0f), cy2 + len, RadioVisuals.dp(getContext(), f2), RadioVisuals.dp(getContext(), f2), this.p);
                    i3++;
                    w = w;
                    h = h;
                    r2 = r2;
                    w2 = 0.06f;
                    f2 = 2.0f;
                }
            }
        }
    }

    private static boolean approved(String raw) {
        String host;
        if (raw == null || raw.length() < 12 || raw.length() > 1500) {
            return false;
        }
        try {
            Uri u = Uri.parse(raw);
            if ("https".equalsIgnoreCase(u.getScheme()) && u.getUserInfo() == null && u.getPort() == -1 && (host = u.getHost()) != null && host.length() <= 240) {
                String host2 = host.toLowerCase(Locale.ROOT);
                if (host2.equals("localhost") || host2.endsWith(".localhost") || host2.endsWith(".local") || host2.endsWith(".internal") || host2.matches("[0-9.]+") || host2.contains(":") || !host2.contains(".")) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean publicHost(String url) {
        try {
            String host = new URL(url).getHost();
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (!address.isAnyLocalAddress() && !address.isLoopbackAddress() && !address.isSiteLocalAddress() && !address.isLinkLocalAddress() && !address.isMulticastAddress()) {
                    byte[] bytes = address.getAddress();
                    if (bytes.length == 16 && (bytes[0] & 254) == 252) {
                        return false;
                    }
                    if (bytes.length == 4) {
                        int x = bytes[0] & 255;
                        int y = bytes[1] & 255;
                        if (x == 0 || ((x == 100 && y >= 64 && y <= 127) || (x == 198 && y == 18))) {
                            return false;
                        }
                    }
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX WARN: Code duplicated, block: B:83:0x0105  */
    private static Bitmap fetchLogo(String url) {
        if (!approved(url) || !publicHost(url)) return null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(4000); connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "QingyinRadioArtwork/0.7.8");
            if (connection.getResponseCode() != 200) return null;
            String type = connection.getContentType();
            if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("image/") || connection.getContentLength() > 550000) return null;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] bytes = new byte[4096]; int n;
                while ((n = stream.read(bytes)) != -1) {
                    if (result.size() + n > 550000) return null;
                    result.write(bytes, 0, n);
                }
            }
            byte[] data = result.toByteArray();
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            if (bounds.outWidth < 4 || bounds.outHeight < 4 || bounds.outWidth > 6000 || bounds.outHeight > 6000) return null;
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 1;
            while (bounds.outWidth / options.inSampleSize > 240 || bounds.outHeight / options.inSampleSize > 240) options.inSampleSize *= 2;
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception ignored) { return null; }
        finally { if (connection != null) connection.disconnect(); }
    }

    private static void showLogo(ImageView target, String source) {
        if (approved(source)) {
            final String url = source.trim();
            target.setTag(url);
            Bitmap cached = ICONS.get(url);
            if (cached != null) {
                target.setImageBitmap(cached);
                target.setVisibility(0);
                return;
            }
            synchronized (WAITERS) {
                ArrayList<WeakReference<ImageView>> waiting = WAITERS.get(url);
                if (waiting != null) {
                    waiting.add(new WeakReference<>(target));
                    return;
                }
                ArrayList<WeakReference<ImageView>> waiting2 = new ArrayList<>();
                waiting2.add(new WeakReference<>(target));
                WAITERS.put(url, waiting2);
                WORK.execute(new Runnable() { // from class: local.qingyin.mobile.RadioVisuals$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        RadioVisuals.lambda$showLogo$1(url);
                    }
                });
            }
        }
    }

    static /* synthetic */ void lambda$showLogo$1(final String url) {
        final Bitmap bitmap = fetchLogo(url);
        if (bitmap != null) {
            ICONS.put(url, bitmap);
        }
        UI.post(new Runnable() { // from class: local.qingyin.mobile.RadioVisuals$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RadioVisuals.lambda$showLogo$0(url, bitmap);
            }
        });
    }

    static /* synthetic */ void lambda$showLogo$0(String url, Bitmap bitmap) {
        ArrayList<WeakReference<ImageView>> list;
        synchronized (WAITERS) {
            list = WAITERS.remove(url);
        }
        if (list == null || bitmap == null) {
            return;
        }
        for (WeakReference<ImageView> ref : list) {
            ImageView view = ref.get();
            if (view != null && url.equals(view.getTag())) {
                view.setImageBitmap(bitmap);
                view.setVisibility(0);
            }
        }
    }
}
