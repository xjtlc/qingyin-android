package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/* JADX INFO: loaded from: classes2.dex */
final class ArtistActionIcon extends View {
    private final Paint brush;
    private final int color;
    private boolean filled;
    private final String type;

    ArtistActionIcon(Context c, String type, int color) {
        super(c);
        this.brush = new Paint(1);
        this.type = type;
        this.color = color;
        setImportantForAccessibility(2);
    }

    void setFilled(boolean value) {
        this.filled = value;
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float factor = Math.min(getWidth(), getHeight()) / 48.0f;
        int save = canvas.save();
        canvas.translate((getWidth() - (factor * 48.0f)) / 2.0f, (getHeight() - (48.0f * factor)) / 2.0f);
        canvas.scale(factor, factor);
        this.brush.setColor(this.color);
        this.brush.setStrokeWidth(2.8f);
        this.brush.setStrokeCap(Paint.Cap.ROUND);
        this.brush.setStrokeJoin(Paint.Join.ROUND);
        this.brush.setStyle(Paint.Style.STROKE);
        if ("back".equals(this.type)) {
            Path arrow = new Path();
            arrow.moveTo(28.0f, 14.0f);
            arrow.lineTo(19.0f, 24.0f);
            arrow.lineTo(28.0f, 34.0f);
            canvas.drawPath(arrow, this.brush);
        } else if ("info".equals(this.type)) {
            this.brush.setStyle(Paint.Style.FILL);
            canvas.drawCircle(24.0f, 15.0f, 2.4f, this.brush);
            canvas.drawRoundRect(22.6f, 21.0f, 25.4f, 34.0f, 1.3f, 1.3f, this.brush);
        } else if ("play".equals(this.type)) {
            this.brush.setStyle(Paint.Style.FILL);
            Path triangle = new Path();
            triangle.moveTo(17.0f, 12.0f);
            triangle.lineTo(17.0f, 36.0f);
            triangle.lineTo(35.0f, 24.0f);
            triangle.close();
            canvas.drawPath(triangle, this.brush);
        } else if ("heart".equals(this.type)) {
            Path heart = new Path();
            heart.moveTo(24.0f, 36.0f);
            heart.cubicTo(19.0f, 31.0f, 9.0f, 24.0f, 9.0f, 18.0f);
            heart.cubicTo(9.0f, 9.0f, 20.0f, 7.0f, 24.0f, 15.0f);
            heart.cubicTo(28.0f, 7.0f, 39.0f, 9.0f, 39.0f, 18.0f);
            heart.cubicTo(39.0f, 24.0f, 29.0f, 31.0f, 24.0f, 36.0f);
            heart.close();
            this.brush.setStyle(this.filled ? Paint.Style.FILL : Paint.Style.STROKE);
            canvas.drawPath(heart, this.brush);
        }
        canvas.restoreToCount(save);
    }
}
