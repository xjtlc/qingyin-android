package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;

/* JADX INFO: loaded from: classes2.dex */
final class DefaultPlaylistArt extends View {
    private final Paint paint;

    DefaultPlaylistArt(Context c) {
        super(c);
        this.paint = new Paint(1);
        setClickable(false);
    }

    static void decorate(FrameLayout cover, boolean show) {
        boolean already = false;
        int i = cover.getChildCount();
        while (true) {
            i--;
            if (i < 2) {
                break;
            }
            if (cover.getChildAt(i) instanceof DefaultPlaylistArt) {
                if (show) {
                    already = true;
                } else {
                    cover.removeViewAt(i);
                }
            }
        }
        if (!show || already) {
            return;
        }
        cover.addView(new DefaultPlaylistArt(cover.getContext()), new FrameLayout.LayoutParams(-1, -1));
    }

    @Override // android.view.View
    protected void onDraw(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        c.drawColor(-1450773);
        this.paint.setShader(new LinearGradient(0.0f, 0.0f, w, h, new int[]{-1121803, -4405266, -1653801}, (float[]) null, Shader.TileMode.CLAMP));
        c.drawRect(0.0f, 0.0f, w, h, this.paint);
        this.paint.setShader(null);
        c.save();
        c.translate(w * 0.52f, h * 0.54f);
        float s = Math.min(w, h) / 160.0f;
        c.scale(s, s);
        this.paint.setColor(1275068415);
        this.paint.setStyle(Paint.Style.FILL);
        c.drawCircle(-11.0f, -5.0f, 65.0f, this.paint);
        this.paint.setColor(1799965548);
        c.drawCircle(-11.0f, -5.0f, 49.0f, this.paint);
        this.paint.setColor(1525078764);
        c.drawCircle(-11.0f, -5.0f, 25.0f, this.paint);
        this.paint.setColor(-5742704);
        c.drawCircle(-11.0f, -5.0f, 8.0f, this.paint);
        this.paint.setColor(-1);
        this.paint.setStrokeWidth(6.0f);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStyle(Paint.Style.STROKE);
        c.drawLine(29.0f, -49.0f, 29.0f, 20.0f, this.paint);
        c.drawLine(50.0f, -56.0f, 50.0f, 12.0f, this.paint);
        c.drawLine(29.0f, -49.0f, 50.0f, -56.0f, this.paint);
        c.drawOval(12.0f, 15.0f, 29.0f, 29.0f, this.paint);
        c.drawOval(33.0f, 7.0f, 50.0f, 21.0f, this.paint);
        c.restore();
    }
}
