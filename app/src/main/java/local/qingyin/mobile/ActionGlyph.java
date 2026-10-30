package local.qingyin.mobile;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Matched outline icons, independent of the device's symbol font. */
final class ActionGlyph extends View {
    private final String kind; private final int color;
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    ActionGlyph(Context context,String kind,int color){super(context);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);int save=c.save();float scale=Math.min(getWidth(),getHeight())/24f;
        c.translate((getWidth()-24*scale)/2,(getHeight()-24*scale)/2);c.scale(scale,scale);
        pen.setColor(color);pen.setStrokeWidth(1.8f);pen.setStyle(Paint.Style.STROKE);pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);Path p=new Path();
        switch(kind){
            case "queue":case "≡":case "▤":for(int y=6;y<=18;y+=6)c.drawLine(4,y,18,y,pen);c.drawLine(21,6,21,18,pen);break;
            case "artist":case "◉":c.drawCircle(12,8,3.2f,pen);c.drawArc(5,12,19,24,205,130,false,pen);break;
            case "history":case "↻":c.drawArc(4,4,20,20,-70,295,false,pen);c.drawLine(4,4,4,9,pen);c.drawLine(4,9,9,9,pen);c.drawLine(12,7,12,12,pen);c.drawLine(12,12,16,14,pen);break;
            case "heart":case "♡":case "♥":p.moveTo(12,20);p.cubicTo(9,17,3,13,3,8);p.cubicTo(3,3,9,2,12,7);p.cubicTo(15,2,21,3,21,8);p.cubicTo(21,13,15,17,12,20);c.drawPath(p,pen);break;
            case "download":case "↓":c.drawLine(12,3,12,15,pen);c.drawLine(7,11,12,16,pen);c.drawLine(12,16,17,11,pen);p.moveTo(4,17);p.lineTo(4,21);p.lineTo(20,21);p.lineTo(20,17);c.drawPath(p,pen);break;
            case "next":case "↳":p.moveTo(5,5);p.lineTo(5,12);p.lineTo(18,12);p.moveTo(13,7);p.lineTo(18,12);p.lineTo(13,17);c.drawPath(p,pen);c.drawLine(21,7,21,17,pen);break;
            case "add":case "+":c.drawLine(4,6,16,6,pen);c.drawLine(4,12,13,12,pen);c.drawLine(4,18,11,18,pen);c.drawLine(18,12,18,20,pen);c.drawLine(14,16,22,16,pen);break;
            case "play":case "▶":p.moveTo(7,4);p.lineTo(20,12);p.lineTo(7,20);p.close();c.drawPath(p,pen);break;
            case "player":case "□":c.drawRoundRect(3,4,21,20,3,3,pen);p.moveTo(10,8);p.lineTo(16,12);p.lineTo(10,16);p.close();c.drawPath(p,pen);break;
            case "close":case "×":c.drawLine(6,6,18,18,pen);c.drawLine(18,6,6,18,pen);break;
            case "chevron":case "›":c.drawLine(9,6,15,12,pen);c.drawLine(15,12,9,18,pen);break;
            case "remove":case "−":c.drawLine(5,12,19,12,pen);break;
            case "trash":c.drawLine(4,6,20,6,pen);c.drawLine(9,3,15,3,pen);p.moveTo(6,9);p.lineTo(7,21);p.lineTo(17,21);p.lineTo(18,9);c.drawPath(p,pen);c.drawLine(10,10,10,17,pen);c.drawLine(14,10,14,17,pen);break;
            case "playing":pen.setStrokeWidth(3);c.drawLine(6,9,6,20,pen);c.drawLine(12,4,12,20,pen);c.drawLine(18,13,18,20,pen);break;
            case "sort":case "↕":c.drawLine(8,4,8,20,pen);c.drawLine(4,8,8,4,pen);c.drawLine(8,4,12,8,pen);c.drawLine(17,4,17,20,pen);c.drawLine(13,16,17,20,pen);c.drawLine(17,20,21,16,pen);break;
            case "info":case "ⓘ":c.drawCircle(12,12,9,pen);c.drawLine(12,11,12,17,pen);c.drawPoint(12,7,pen);break;
            case "quality":case "◇":p.moveTo(12,3);p.lineTo(21,12);p.lineTo(12,21);p.lineTo(3,12);p.close();c.drawPath(p,pen);break;
            case "save":c.drawLine(6,3,18,3,pen);c.drawRoundRect(4,7,20,21,3,3,pen);c.drawLine(12,11,12,17,pen);c.drawLine(9,14,15,14,pen);break;
            case "share":p.moveTo(5,10);p.lineTo(5,21);p.lineTo(19,21);p.lineTo(19,10);p.moveTo(12,16);p.lineTo(12,3);p.moveTo(7,8);p.lineTo(12,3);p.lineTo(17,8);c.drawPath(p,pen);break;
            case "listen":c.drawCircle(8,7,3,pen);c.drawArc(2,11,14,23,200,140,false,pen);c.drawCircle(18,13,4.5f,pen);c.drawLine(18,10.5f,18,15.5f,pen);c.drawLine(15.5f,13,20.5f,13,pen);break;
            case "album":c.drawCircle(12,12,9,pen);c.drawCircle(12,12,2,pen);c.drawArc(6,6,18,18,205,55,false,pen);c.drawArc(6,6,18,18,25,55,false,pen);break;
            case "effects":c.drawLine(5,3,5,8,pen);c.drawLine(5,14,5,21,pen);c.drawCircle(5,11,3,pen);c.drawLine(12,3,12,13,pen);c.drawLine(12,19,12,21,pen);c.drawCircle(12,16,3,pen);c.drawLine(19,3,19,5,pen);c.drawLine(19,11,19,21,pen);c.drawCircle(19,8,3,pen);break;
            case "style":c.drawRoundRect(3,3,21,21,4,4,pen);c.drawCircle(12,10,4,pen);c.drawCircle(12,10,1,pen);c.drawLine(7,17,17,17,pen);break;
            case "sparkle":case "heart_effect":p.moveTo(10,20);p.cubicTo(7,17,2,13,3,8);p.cubicTo(4,4,8,4,10,8);p.cubicTo(13,4,18,5,18,9);p.cubicTo(18,13,14,17,10,20);c.drawPath(p,pen);p.reset();p.moveTo(19,2);p.lineTo(20,5);p.lineTo(23,6);p.lineTo(20,7);p.lineTo(19,10);p.lineTo(18,7);p.lineTo(15,6);p.lineTo(18,5);p.close();c.drawPath(p,pen);break;
            case "♫":case "♬":c.drawLine(10,6,10,17,pen);c.drawLine(10,6,20,3,pen);c.drawLine(20,3,20,14,pen);c.drawOval(4,15,10,20,pen);c.drawOval(14,12,20,17,pen);break;
            default:for(int x=5;x<=19;x+=7)c.drawCircle(x,12,1,pen);break;
        }c.restoreToCount(save);
    }
}
