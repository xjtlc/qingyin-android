package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Choreographer;
import android.view.View;

/** Hardware-rendered record. Static grooves/shadows are rasterized once, not every frame. */
final class VinylRecordView extends View {
    private static final float DEGREES_PER_MILLISECOND = .009f;
    private static final int MAX_BACKDROP_PIXELS = 1024;
    private final Paint solid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint image = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix imageMatrix = new Matrix();
    private final Path arm = new Path();
    private final RectF backdropBounds = new RectF();
    private final RectF coverBounds = new RectF();
    private final RectF cartridge = new RectF();
    private final RectF cartridgeInset = new RectF();
    private Bitmap art;
    // Owned cache only. Artwork's shared cover bitmap is never recycled or modified.
    private Bitmap backdropCache;
    private BitmapShader artShader;
    private Shader platterShader, placeholderShader;
    private String requested = "";
    private boolean spinning, coverStyle, attached, framePosted, animationActive;
    private boolean geometryDirty = true, imageDirty = true, cacheAttempted;
    private long lastFrameNanos;
    private float angle, radius, centerX, centerY, innerRadius;
    private float pivotX, pivotY, headX, headY, coverCorner;

    private final Choreographer.FrameCallback frame = new Choreographer.FrameCallback() {
        @Override public void doFrame(long frameTimeNanos) {
            framePosted = false;
            if (!shouldAnimate()) { syncAnimation(); return; }
            if (lastFrameNanos != 0) {
                angle = (angle + (frameTimeNanos - lastFrameNanos) / 1_000_000f
                        * DEGREES_PER_MILLISECOND) % 360f;
            }
            lastFrameNanos = frameTimeNanos;
            // Choreographer invokes this before traversal, so this draw uses the same VSYNC.
            invalidate();
            framePosted = true;
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    VinylRecordView(Context context) {
        super(context);
        // Keep the default hardware-accelerated canvas. Blur shadows are baked into our cache.
        detail.setStyle(Paint.Style.STROKE);
        detail.setStrokeCap(Paint.Cap.ROUND);
        detail.setStrokeJoin(Paint.Join.ROUND);
    }

    void setCoverStyle(boolean enabled) {
        if (coverStyle == enabled) return;
        coverStyle = enabled;
        geometryDirty = imageDirty = true;
        cacheAttempted = false;
        backdropCache = null;
        syncAnimation();
        invalidate();
    }

    void setCover(String url) {
        final String incoming = url == null ? "" : url;
        if (incoming.equals(requested)) return;
        requested = incoming;
        art = null;
        artShader = null;
        imageDirty = true;
        invalidate();
        Artwork.loadBitmap(incoming, bitmap -> m459lambda$setCover$0$localqingyinmobileVinylRecordView(incoming, bitmap));
    }

    void m459lambda$setCover$0$localqingyinmobileVinylRecordView(String incoming, Bitmap bitmap) {
        if (!incoming.equals(requested)) return;
        art = bitmap;
        artShader = null;
        imageDirty = true;
        invalidate();
    }

    void setSpinning(boolean value) {
        if (spinning == value) return;
        spinning = value;
        syncAnimation();
        invalidate();
    }

    private boolean shouldAnimate() {
        return attached && spinning && !coverStyle && getWindowVisibility() == VISIBLE
                && hasWindowFocus() && isShown() && getWidth() > 0 && getHeight() > 0;
    }

    private void syncAnimation() {
        // View construction may dispatch visibility callbacks before our fields initialize.
        if (frame == null) return;
        boolean running = shouldAnimate();
        if (running) {
            if (!animationActive) {
                animationActive = true;
                lastFrameNanos = 0;
                UiPerformance.setAnimationActive(this, true);
            }
            if (!framePosted) {
                framePosted = true;
                Choreographer.getInstance().postFrameCallback(frame);
            }
        } else {
            if (framePosted) {
                Choreographer.getInstance().removeFrameCallback(frame);
                framePosted = false;
            }
            lastFrameNanos = 0;
            if (animationActive) {
                animationActive = false;
                UiPerformance.setAnimationActive(this, false);
            }
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        cacheAttempted = false;
        syncAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        syncAnimation();
        // Drop only our own raster. Let RenderThread/GC release it after pending draws finish.
        backdropCache = null;
        cacheAttempted = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        syncAnimation();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        syncAnimation();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        syncAnimation();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        geometryDirty = imageDirty = true;
        cacheAttempted = false;
        backdropCache = null;
        syncAnimation();
    }

    private void prepareGeometry() {
        if (!geometryDirty) return;
        geometryDirty = false;
        float width = getWidth(), height = getHeight();
        float density = getResources().getDisplayMetrics().density;
        radius = Math.min(Math.min(.385f * width, .315f * height), density * 146f);
        centerX = width / 2f;
        centerY = Math.min(height - 1.02f * radius, Math.max(1.55f * radius, .56f * height));
        innerRadius = .61f * radius;
        float side = Math.min(Math.min(width * .88f, height * .79f), density * 300f);
        coverBounds.set((width - side) / 2f, (height - side) / 2f,
                (width + side) / 2f, (height + side) / 2f);
        coverCorner = side * .035f;
        if (coverStyle) {
            float margin = side * .10f;
            backdropBounds.set(coverBounds.left - margin, coverBounds.top - margin,
                    coverBounds.right + margin, coverBounds.bottom + margin);
            placeholderShader = new LinearGradient(coverBounds.left, coverBounds.top,
                    coverBounds.right, coverBounds.bottom, 0xff9A8498, 0xff44394E, Shader.TileMode.CLAMP);
        } else {
            float extent = radius * 1.14f;
            backdropBounds.set(centerX - extent, centerY - extent, centerX + extent, centerY + extent);
            platterShader = new RadialGradient(centerX - radius * .27f, centerY - radius * .21f,
                    radius * 1.26f, new int[]{-12895424, -15658732, -14211285, -15592940},
                    new float[]{0, .47f, .79f, 1}, Shader.TileMode.CLAMP);
            placeholderShader = new LinearGradient(centerX - innerRadius, centerY - innerRadius,
                    centerX + innerRadius, centerY + innerRadius, -6013859, -13032127, Shader.TileMode.CLAMP);
        }
        pivotX = centerX + .025f * radius;
        pivotY = centerY - 1.47f * radius;
        arm.reset();
        arm.moveTo(pivotX, pivotY + .05f * radius);
        arm.cubicTo(pivotX + .08f * radius, pivotY + .62f * radius,
                pivotX + .14f * radius, pivotY + .82f * radius,
                pivotX + .25f * radius, pivotY + radius);
        arm.cubicTo(pivotX + .34f * radius, pivotY + 1.17f * radius,
                pivotX + .49f * radius, pivotY + 1.22f * radius,
                pivotX + .66f * radius, pivotY + 1.35f * radius);
        headX = pivotX + .68f * radius;
        headY = pivotY + 1.37f * radius;
        cartridge.set(headX - .045f * radius, headY - .1f * radius,
                headX + .19f * radius, headY + .035f * radius);
        cartridgeInset.set(headX + .12f * radius, headY - .067f * radius,
                headX + .19f * radius, headY + .001f * radius);
        imageDirty = true;
    }

    private void prepareImage() {
        if (!imageDirty) return;
        imageDirty = false;
        artShader = null;
        if (art == null || art.isRecycled()) return;
        artShader = new BitmapShader(art, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float side = coverStyle ? coverBounds.width() : innerRadius * 2;
        float scale = side / Math.min(art.getWidth(), art.getHeight());
        float x = coverStyle ? coverBounds.centerX() : centerX;
        float y = coverStyle ? coverBounds.centerY() : centerY;
        imageMatrix.setScale(scale, scale);
        imageMatrix.postTranslate(x - art.getWidth() * scale / 2,
                y - art.getHeight() * scale / 2);
        artShader.setLocalMatrix(imageMatrix);
    }

    private void circle(Canvas canvas, float x, float y, float size, int color) {
        solid.setShader(null);
        solid.setColor(color);
        canvas.drawCircle(x, y, size, solid);
    }

    private void renderBackdrop(Canvas canvas) {
        if (coverStyle) {
            shadow.setColor(0xff474550);
            shadow.setShadowLayer(coverBounds.width() * .045f, 0, coverBounds.width() * .025f, 0x66000000);
            canvas.drawRoundRect(coverBounds, coverCorner, coverCorner, shadow);
            shadow.clearShadowLayer();
            return;
        }
        shadow.setShadowLayer(.055f * radius, 0, .033f * radius, 956301312);
        shadow.setColor(839909376);
        canvas.drawCircle(centerX, centerY, 1.025f * radius, shadow);
        shadow.clearShadowLayer();
        circle(canvas, centerX, centerY, radius * 1.014f, 285212672);
        circle(canvas, centerX, centerY, radius, -13619147);
        circle(canvas, centerX, centerY, radius * .981f, -15724267);
        solid.setShader(platterShader);
        canvas.drawCircle(centerX, centerY, radius * .958f, solid);
        solid.setShader(null);
        detail.setStrokeWidth(Math.max(.55f, .0022f * radius));
        for (int i = 0; i < 20; i++) {
            detail.setColor(i % 4 == 0 ? 419430399 : 285212672);
            canvas.drawCircle(centerX, centerY, (.66f + i * .014f) * radius, detail);
        }
    }

    private void drawBackdrop(Canvas canvas) {
        if (!cacheAttempted) {
            cacheAttempted = true;
            int size = Math.max(1, Math.min(MAX_BACKDROP_PIXELS, (int) Math.ceil(backdropBounds.width())));
            try {
                Bitmap generated = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas raster = new Canvas(generated);
                raster.scale(size / backdropBounds.width(), size / backdropBounds.height());
                raster.translate(-backdropBounds.left, -backdropBounds.top);
                renderBackdrop(raster);
                backdropCache = generated;
            } catch (OutOfMemoryError exhausted) {
                // Keep the geometry-only fallback usable; do not retry allocation every frame.
                backdropCache = null;
            }
        }
        if (backdropCache != null) canvas.drawBitmap(backdropCache, null, backdropBounds, bitmapPaint);
        else renderBackdrop(canvas);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        prepareGeometry();
        prepareImage();
        drawBackdrop(canvas);
        if (coverStyle) { drawCover(canvas); return; }
        int rotating = canvas.save();
        canvas.rotate(angle, centerX, centerY);
        circle(canvas, centerX, centerY, radius * .626f, -15263977);
        boolean hasArt = artShader != null && art != null && !art.isRecycled();
        image.setShader(hasArt ? artShader : placeholderShader);
        canvas.drawCircle(centerX, centerY, innerRadius, image);
        if (!hasArt) {
            solid.setColor(-1426063361);
            solid.setTextAlign(Paint.Align.CENTER);
            solid.setTextSize(.74f * innerRadius);
            canvas.drawText("♫", centerX, centerY + .27f * innerRadius, solid);
        }
        detail.setColor(1442840575);
        detail.setStrokeWidth(Math.max(1, .008f * radius));
        canvas.drawCircle(centerX, centerY, innerRadius, detail);
        canvas.restoreToCount(rotating);
        circle(canvas, centerX, centerY, radius * .043f, -1073741825);
        circle(canvas, centerX, centerY, radius * .02f, -2762531);
        drawArm(canvas);
        // Snapshot/blur draws must not advance angle or request another frame.
    }

    private void drawArm(Canvas canvas) {
        circle(canvas, pivotX, pivotY, radius * .095f, 855638016);
        circle(canvas, pivotX, pivotY, radius * .066f, -1315859);
        circle(canvas, pivotX, pivotY, radius * .028f, -5789782);
        detail.setStrokeWidth(Math.max(3, .031f * radius));
        detail.setColor(905969664);
        int shifted = canvas.save();
        canvas.translate(radius * .017f, radius * .017f);
        canvas.drawPath(arm, detail);
        canvas.restoreToCount(shifted);
        detail.setColor(-1382936);
        canvas.drawPath(arm, detail);
        int rotated = canvas.save();
        canvas.rotate(-45, headX, headY);
        solid.setShader(null);
        solid.setColor(-988435);
        canvas.drawRoundRect(cartridge, radius * .018f, radius * .018f, solid);
        solid.setColor(-5328202);
        canvas.drawRoundRect(cartridgeInset, radius * .01f, radius * .01f, solid);
        canvas.restoreToCount(rotated);
    }

    private void drawCover(Canvas canvas) {
        boolean hasArt = artShader != null && art != null && !art.isRecycled();
        image.setShader(hasArt ? artShader : placeholderShader);
        canvas.drawRoundRect(coverBounds, coverCorner, coverCorner, image);
        if (!hasArt) {
            solid.setShader(null);
            solid.setColor(0xffEFE9F1);
            solid.setTextAlign(Paint.Align.CENTER);
            solid.setTextSize(coverBounds.width() * .3f);
            canvas.drawText("♫", coverBounds.centerX(), coverBounds.top + coverBounds.height() * .6f, solid);
        }
        detail.setStrokeWidth(1);
        detail.setColor(0x36FFFFFF);
        canvas.drawRoundRect(coverBounds, coverCorner, coverCorner, detail);
    }
}
