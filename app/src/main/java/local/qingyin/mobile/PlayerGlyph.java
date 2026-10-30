package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class PlayerGlyph extends View {
    private final int color;
    private float iconScale;
    private float visualSize = 28f;

    void setVisualSize(float size) { visualSize = Math.max(16f, Math.min(48f, size)); invalidate(); }
    private final Paint pen;
    private String state;

    PlayerGlyph(Context ctx, String kind, int ink) {
        super(ctx);
        this.iconScale = 1.0f;
        this.pen = new Paint(1);
        this.state = kind;
        this.color = ink;
    }

    void setState(String kind) {
        if(this.state.equals(kind))return;
        this.state = kind;
        invalidate();
    }

    void setIconScale(float factor) {
        this.iconScale = Math.max(0.45f, Math.min(1.0f, factor));
        invalidate();
    }

    /** Round the filled silhouette itself, so transport corners stay soft at every scale. */
    private void drawSoftTriangle(Canvas canvas, Path path, float left, float top,
                                  float right, float bottom, boolean pointsLeft, float corner) {
        float halfHeight = (bottom - top) / 2f;
        float diagonal = (float) Math.hypot(right - left, halfHeight);
        float dx = corner * (right - left) / diagonal;
        float dy = corner * halfHeight / diagonal;
        float back = pointsLeft ? right : left;
        float tip = pointsLeft ? left : right;
        float direction = pointsLeft ? -1f : 1f;
        float middle = (top + bottom) / 2f;
        path.reset();
        path.moveTo(back, top + corner);
        path.quadTo(back, top, back + direction * dx, top + dy);
        path.lineTo(tip - direction * dx, middle - dy);
        path.quadTo(tip, middle, tip - direction * dx, middle + dy);
        path.lineTo(back + direction * dx, bottom - dy);
        path.quadTo(back, bottom, back, bottom - corner);
        path.close();
        canvas.drawPath(path, this.pen);
    }

    @Override 
    protected void onDraw(Canvas target) {
        super.onDraw(target);
        int save = target.save();
        float viewport = Math.min(Math.min(getWidth(), getHeight()), QingyinUi.dp(getContext(), visualSize * 1.8f));
        float scale = viewport / 56.0f * this.iconScale;
        target.translate((getWidth() - (scale * 56.0f)) / 2.0f, (getHeight() - (56.0f * scale)) / 2.0f);
        target.scale(scale, scale);
        this.pen.setColor(this.color);
        this.pen.setStrokeWidth(2.35f);
        this.pen.setStyle(Paint.Style.STROKE);
        this.pen.setStrokeCap(Paint.Cap.ROUND);
        this.pen.setStrokeJoin(Paint.Join.ROUND);
        Path p = new Path();
        switch (this.state) {
            case "collapse":
            case "down":
                p.moveTo(16.0f, 23.0f);
                p.lineTo(28.0f, 35.0f);
                p.lineTo(40.0f, 23.0f);
                target.drawPath(p, this.pen);
                break;
            case "shuffle":
                target.drawLine(14.0f, 18.0f, 20.0f, 18.0f, this.pen);
                p.moveTo(20.0f, 18.0f);
                p.cubicTo(30.0f, 18.0f, 27.0f, 38.0f, 39.0f, 38.0f);
                target.drawPath(p, this.pen);
                target.drawLine(14.0f, 38.0f, 20.0f, 38.0f, this.pen);
                p.reset();
                p.moveTo(20.0f, 38.0f);
                p.cubicTo(30.0f, 38.0f, 27.0f, 18.0f, 39.0f, 18.0f);
                target.drawPath(p, this.pen);
                target.drawLine(35.0f, 14.0f, 40.0f, 18.0f, this.pen);
                target.drawLine(35.0f, 22.0f, 40.0f, 18.0f, this.pen);
                target.drawLine(35.0f, 34.0f, 40.0f, 38.0f, this.pen);
                target.drawLine(35.0f, 42.0f, 40.0f, 38.0f, this.pen);
                break;
            case "repeat":
            case "repeat-one":
            case "order":
                this.pen.setStrokeWidth(2.1f);
                p.moveTo(16.0f, 24.0f);
                p.lineTo(16.0f, 22.0f);
                p.quadTo(16.0f, 18.0f, 20.0f, 18.0f);
                p.lineTo(39.0f, 18.0f);
                p.moveTo(35.5f, 14.5f);
                p.lineTo(39.0f, 18.0f);
                p.lineTo(35.5f, 21.5f);
                p.moveTo(40.0f, 32.0f);
                p.lineTo(40.0f, 34.0f);
                p.quadTo(40.0f, 38.0f, 36.0f, 38.0f);
                p.lineTo(17.0f, 38.0f);
                p.moveTo(20.5f, 34.5f);
                p.lineTo(17.0f, 38.0f);
                p.lineTo(20.5f, 41.5f);
                target.drawPath(p, this.pen);
                if ("repeat-one".equals(this.state)) {
                    this.pen.setStyle(Paint.Style.FILL);
                    this.pen.setTextSize(18.0f);
                    this.pen.setTextAlign(Paint.Align.CENTER);
                    target.drawText("1", 28.0f, 33.0f, this.pen);
                    break;
                } else {
                    if ("order".equals(this.state)) {
                        target.drawLine(23.0f, 28.0f, 33.0f, 28.0f, this.pen);
                    }
                    break;
                }
            case "comment":
                p.moveTo(17.0f, 16.0f);
                p.lineTo(39.0f, 16.0f);
                p.quadTo(44.0f, 16.0f, 44.0f, 21.0f);
                p.lineTo(44.0f, 33.0f);
                p.quadTo(44.0f, 38.0f, 39.0f, 38.0f);
                p.lineTo(25.0f, 38.0f);
                p.lineTo(17.0f, 44.0f);
                p.lineTo(17.0f, 38.0f);
                p.quadTo(12.0f, 38.0f, 12.0f, 33.0f);
                p.lineTo(12.0f, 21.0f);
                p.quadTo(12.0f, 16.0f, 17.0f, 16.0f);
                p.close();
                target.drawPath(p, this.pen);
                target.drawLine(20.0f, 25.0f, 36.0f, 25.0f, this.pen);
                target.drawLine(20.0f, 31.0f, 31.0f, 31.0f, this.pen);
                break;
            case "close":
                target.drawLine(17.0f, 17.0f, 39.0f, 39.0f, this.pen);
                target.drawLine(39.0f, 17.0f, 17.0f, 39.0f, this.pen);
                break;
            case "queue":
                this.pen.setStrokeWidth(2.1f);
                for (int y = 18; y <= 38; y += 10) {
                    this.pen.setStyle(Paint.Style.FILL);
                    target.drawCircle(15.5f, y, 1.25f, this.pen);
                    this.pen.setStyle(Paint.Style.STROKE);
                    target.drawLine(24.0f, y, 41.0f, y, this.pen);
                }
                break;
            case "more":
                this.pen.setStyle(Paint.Style.FILL);
                for (int x = 17; x <= 39; x += 11) {
                    target.drawCircle(x, 28.0f, 2.6f, this.pen);
                }
                break;
            case "heart-fill":
            case "heart":
                p.moveTo(28.0f, 43.0f);
                p.cubicTo(21.0f, 37.0f, 12.0f, 30.0f, 12.0f, 22.0f);
                p.cubicTo(12.0f, 12.0f, 23.0f, 10.0f, 28.0f, 19.0f);
                p.cubicTo(33.0f, 10.0f, 44.0f, 12.0f, 44.0f, 22.0f);
                p.cubicTo(44.0f, 30.0f, 35.0f, 37.0f, 28.0f, 43.0f);
                p.close();
                this.pen.setStyle("heart-fill".equals(this.state) ? Paint.Style.FILL : Paint.Style.STROKE);
                target.drawPath(p, this.pen);
                break;
            case "previous":
            case "next":
                boolean left = "previous".equals(this.state);
                this.pen.setStyle(Paint.Style.FILL);
                if (left) {
                    drawSoftTriangle(target, p, 22.0f, 16.0f, 39.0f, 40.0f, true, 2.8f);
                    target.drawRoundRect(16.0f, 16.0f, 21.0f, 40.0f, 2.5f, 2.5f, this.pen);
                    break;
                } else {
                    drawSoftTriangle(target, p, 17.0f, 16.0f, 34.0f, 40.0f, false, 2.8f);
                    target.drawRoundRect(35.0f, 16.0f, 40.0f, 40.0f, 2.5f, 2.5f, this.pen);
                    break;
                }
            case "pause":
                this.pen.setStyle(Paint.Style.FILL);
                target.drawRoundRect(19.0f, 15.0f, 25.0f, 41.0f, 3.0f, 3.0f, this.pen);
                target.drawRoundRect(31.0f, 15.0f, 37.0f, 41.0f, 3.0f, 3.0f, this.pen);
                break;
            case "loading":
                target.drawArc(18.0f, 18.0f, 38.0f, 38.0f, 40.0f, 260.0f, false, this.pen);
                break;
            default:
                this.pen.setStyle(Paint.Style.FILL);
                drawSoftTriangle(target, p, 21.0f, 15.0f, 40.0f, 41.0f, false, 3.2f);
                break;
        }
        target.restoreToCount(save);
    }
}
