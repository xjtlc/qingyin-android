package local.qingyin.mobile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;

/* JADX INFO: loaded from: classes2.dex */
final class FrostedGlassDrawable extends Drawable {
    private static final int BLUR_RADIUS = 4;
    private static final int DOWNSAMPLE = 5;
    private static final ThreadLocal<Boolean> IN_CAPTURE = new ThreadLocal<>();
    private static final long MIN_UPDATE_MS = 75;
    private int[] buffer;
    private final float cornerPx;
    private boolean disposed;
    private final View host;
    private long lastUpdate;
    private boolean queued;
    private boolean capturePaused;
    private Bitmap snapshot;
    private Canvas snapshotCanvas;
    private final View source;
    private final int tintColor;
    private final boolean screenCoordinates;
    private final View[] overlays;
    private final int fallbackColor;
    private LinearGradient sheen;
    private int sheenHeight;
    private int[] work;
    private final Paint paint = new Paint(3);
    private final Path rounded = new Path();
    private final int[] location = new int[2];
    private final int[] origin = new int[2];
    private final int[] overlayLocation = new int[2];
    private boolean dirty = true;
    private final Runnable retryCapture=()->m46lambda$draw$0$localqingyinmobileFrostedGlassDrawable();

    FrostedGlassDrawable(View source, View host, float cornerPx, int tintColor) {
        this(source,host,cornerPx,tintColor,false,new View[0]);
    }

    FrostedGlassDrawable(View source, View host, float cornerPx, int tintColor,
                         boolean screenCoordinates, View[] overlays) {
        this.source = source;
        this.host = host;
        this.cornerPx = cornerPx;
        this.tintColor = tintColor;
        this.screenCoordinates=screenCoordinates;
        this.overlays=overlays==null?new View[0]:overlays.clone();
        this.fallbackColor=screenCoordinates?(Color.luminance(tintColor)<.45?0xff252933:0xffF2F4F7):-593420;
    }

    /** Sample only the underlying scene, before this separate dialog window is shown. */
    static boolean isDarkBackdrop(View source,boolean fallback){
        if(source==null||source.getWidth()<=0||source.getHeight()<=0)return fallback;
        Bitmap tiny=null;Boolean previous=IN_CAPTURE.get();
        try{
            tiny=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(tiny);canvas.drawColor(fallback?0xff252933:Color.WHITE);
            canvas.scale(16f/source.getWidth(),16f/source.getHeight());
            IN_CAPTURE.set(true);source.draw(canvas);
            double light=0;int count=0;
            for(int y=6;y<16;y++)for(int x=0;x<16;x++){
                int pixel=tiny.getPixel(x,y);
                light+=(Color.red(pixel)*.2126+Color.green(pixel)*.7152+Color.blue(pixel)*.0722)/255.;count++;
            }
            return light/count<.55;
        }catch(RuntimeException unavailable){return fallback;}
        finally{IN_CAPTURE.set(previous);if(tiny!=null)tiny.recycle();}
    }

    void refresh() {
        if (this.disposed) {
            return;
        }
        this.dirty = true;
        if(!capturePaused)invalidateSelf();
    }

    /** Keep the cached glass texture stable during touch/fling; list rendering stays independent. */
    void setCapturePaused(boolean paused){
        if(capturePaused==paused||disposed)return;
        capturePaused=paused;
        if(paused){host.removeCallbacks(retryCapture);queued=false;}
        else if(dirty)invalidateSelf();
    }

    void release() {
        this.disposed = true;
        host.removeCallbacks(retryCapture);queued=false;
        if(snapshotCanvas!=null){snapshotCanvas.setBitmap(null);snapshotCanvas=null;}
        if (this.snapshot != null) {
            this.snapshot.recycle();
            this.snapshot = null;
        }
        this.buffer = null;
        this.work = null;
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        if (this.disposed) {
            return;
        }
        boolean nestedCapture=Boolean.TRUE.equals(IN_CAPTURE.get());
        // Other dialog windows may composite our cached surface, without recapturing it.
        if(nestedCapture&&(!screenCoordinates||snapshot==null||snapshot.isRecycled()))return;
        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (this.dirty && !nestedCapture && !capturePaused) {
            long now = SystemClock.uptimeMillis();
            if (this.snapshot == null || now - this.lastUpdate >= MIN_UPDATE_MS) {
                boolean captured=capture(width, height);
                this.lastUpdate = now;
                this.dirty = !captured;
            } else if (!this.queued) {
                this.queued = true;
                this.host.postDelayed(retryCapture, Math.max(1L, MIN_UPDATE_MS - (now - this.lastUpdate)));
            }
        }
        float x = bounds.left;
        float y = bounds.top;
        canvas.save();
        canvas.clipPath(this.rounded);
        if (this.snapshot != null && !this.snapshot.isRecycled()) {
            this.paint.setColor(-1);
            this.paint.setAlpha(255);
            this.paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(this.snapshot, (Rect) null, bounds, this.paint);
        } else {
            canvas.drawColor(fallbackColor);
        }
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(this.tintColor);
        canvas.drawRoundRect(x, y, x + width, y + height, this.cornerPx, this.cornerPx, this.paint);
        if(screenCoordinates){
            if(sheen==null||sheenHeight!=height){
                sheenHeight=height;
                sheen=new LinearGradient(0,y,0,y+height,new int[]{0x20FFFFFF,0x04FFFFFF,0x04000000},new float[]{0,.42f,1},Shader.TileMode.CLAMP);
            }
            paint.setShader(sheen);canvas.drawRoundRect(x,y,x+width,y+height,cornerPx,cornerPx,paint);paint.setShader(null);
        }
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(Math.max(1.0f, this.host.getResources().getDisplayMetrics().density*(screenCoordinates?.35f:1f)));
        this.paint.setColor(screenCoordinates?0x60FFFFFF:-1191182337);
        canvas.drawRoundRect(x + 0.5f, y + 0.5f, (x + width) - 0.5f, (y + height) - 0.5f, Math.max(1.0f, this.cornerPx - 0.5f), Math.max(1.0f, this.cornerPx - 0.5f), this.paint);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setAlpha(255);
        canvas.restore();
    }

    @Override protected void onBoundsChange(Rect bounds){
        rounded.reset();
        rounded.addRoundRect(bounds.left,bounds.top,bounds.right,bounds.bottom,cornerPx,cornerPx,Path.Direction.CW);
        sheen=null;dirty=true;
    }

    /* JADX INFO: renamed from: lambda$draw$0$local-qingyin-mobile-FrostedGlassDrawable, reason: not valid java name */
    /* synthetic */ void m46lambda$draw$0$localqingyinmobileFrostedGlassDrawable() {
        this.queued = false;
        if (this.disposed || !this.dirty || capturePaused) {
            return;
        }
        invalidateSelf();
    }

    private boolean capture(int width, int height) {
        if (this.source.getWidth() == 0 || this.source.getHeight() == 0 || this.host.getWindowToken() == null) {
            return false;
        }
        int downsample=screenCoordinates?8:5;
        int blurRadius=screenCoordinates?6:4;
        int sw = Math.max(1, ((width + downsample) - 1) / downsample);
        int sh = Math.max(1, ((height + downsample) - 1) / downsample);
        if (this.snapshot == null || this.snapshot.getWidth() != sw || this.snapshot.getHeight() != sh) {
            if(snapshotCanvas!=null)snapshotCanvas.setBitmap(null);
            if (this.snapshot != null) {
                this.snapshot.recycle();
            }
            this.snapshot = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            this.snapshotCanvas=new Canvas(this.snapshot);
            this.buffer = new int[sw * sh];
            this.work = new int[sw * sh];
        }
        if(screenCoordinates){source.getLocationOnScreen(origin);host.getLocationOnScreen(location);}
        else{source.getLocationInWindow(origin);host.getLocationInWindow(location);}
        float ox = this.location[0] - this.origin[0];
        float oy = this.location[1] - this.origin[1];
        Canvas backdrop = this.snapshotCanvas;
        backdrop.drawColor(screenCoordinates?fallbackColor:-1);
        int save = backdrop.save();
        backdrop.scale(1f/downsample, 1f/downsample);
        backdrop.translate(-ox, -oy);
        backdrop.clipRect(ox, oy, width + ox, height + oy);
        Boolean previous = IN_CAPTURE.get();
        IN_CAPTURE.set(true);
        try {
            this.source.draw(backdrop);
            if(screenCoordinates){
                for(View overlay:overlays){
                    if(overlay==null||overlay.getWindowToken()==null)continue;
                    overlay.getLocationOnScreen(overlayLocation);int layer=backdrop.save();
                    try{backdrop.translate(overlayLocation[0]-origin[0],overlayLocation[1]-origin[1]);overlay.draw(backdrop);}
                    finally{backdrop.restoreToCount(layer);}
                }
            }
        } finally {
            IN_CAPTURE.set(previous);
            backdrop.restoreToCount(save);
        }
        this.snapshot.getPixels(this.buffer, 0, sw, 0, 0, sw, sh);
        for (int pass = 0; pass < 2; pass++) {
            boxHorizontal(this.buffer, this.work, sw, sh, blurRadius);
            boxVertical(this.work, this.buffer, sw, sh, blurRadius);
        }
        this.snapshot.setPixels(this.buffer, 0, sw, 0, 0, sw, sh);
        return true;
    }

    private static void boxHorizontal(int[] src, int[] dst, int w, int h, int r) {
        for (int y = 0; y < h; y++) {
            int base = y * w;
            int rs=0,gs=0,bs=0,count=Math.min(w,r+1);
            for(int n=0;n<count;n++){int c=src[base+n];rs+=(c>>16)&255;gs+=(c>>8)&255;bs+=c&255;}
            for (int x = 0; x < w; x++) {
                dst[base+x]=Color.rgb(rs/count,gs/count,bs/count);
                int remove=x-r,add=x+r+1;
                if(remove>=0){int c=src[base+remove];rs-=(c>>16)&255;gs-=(c>>8)&255;bs-=c&255;count--;}
                if(add<w){int c=src[base+add];rs+=(c>>16)&255;gs+=(c>>8)&255;bs+=c&255;count++;}
            }
        }
    }

    private static void boxVertical(int[] src, int[] dst, int w, int h, int r) {
        for (int x = 0; x < w; x++) {
            int rs=0,gs=0,bs=0,count=Math.min(h,r+1);
            for(int n=0;n<count;n++){int c=src[n*w+x];rs+=(c>>16)&255;gs+=(c>>8)&255;bs+=c&255;}
            for (int y = 0; y < h; y++) {
                dst[y*w+x]=Color.rgb(rs/count,gs/count,bs/count);
                int remove=y-r,add=y+r+1;
                if(remove>=0){int c=src[remove*w+x];rs-=(c>>16)&255;gs-=(c>>8)&255;bs-=c&255;count--;}
                if(add<h){int c=src[add*w+x];rs+=(c>>16)&255;gs+=(c>>8)&255;bs+=c&255;count++;}
            }
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int alpha) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter filter) {
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }
}
