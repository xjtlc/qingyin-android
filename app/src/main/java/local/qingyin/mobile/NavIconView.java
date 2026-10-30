package local.qingyin.mobile;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Resolution-independent pictograms for the main music tab bar. */
final class NavIconView extends View {
    private final String type;
    private int tone;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    NavIconView(Context c,String type,int color){
        super(c);this.type=type;this.tone=color;
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    void tint(int value){tone=value;invalidate();}
    private void line(Canvas c,float x1,float y1,float x2,float y2){
        c.drawLine(x1,y1,x2,y2,paint);
    }
    @Override protected void onDraw(Canvas original){
        super.onDraw(original);
        Canvas c=original;
        int save=c.save();
        float scale=Math.min(getWidth(),getHeight())/28f;
        c.translate((getWidth()-28*scale)/2f,(getHeight()-28*scale)/2f);
        c.scale(scale,scale);
        paint.setColor(tone);paint.setStrokeWidth(2.3f);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        if("⌂".equals(type)){
            Path p=new Path();p.moveTo(4,12);p.lineTo(14,4);p.lineTo(24,12);p.lineTo(24,23);
            p.lineTo(17,23);p.lineTo(17,16);p.lineTo(11,16);p.lineTo(11,23);p.lineTo(4,23);p.close();
            c.drawPath(p,paint);
        }else if("◈".equals(type)){
            // Discover is four app-style music tiles, not a font-dependent diamond.
            paint.setStyle(Paint.Style.FILL);
            for(int row=0;row<2;row++)for(int col=0;col<2;col++)
                c.drawRoundRect(4+col*11,4+row*11,12+col*11,12+row*11,2.3f,2.3f,paint);
        }else if("◎".equals(type)){
            c.drawCircle(14,14,3.5f,paint);
            c.drawArc(6,6,22,22,-55,110,false,paint);
            c.drawArc(6,6,22,22,125,110,false,paint);
            c.drawArc(1.5f,1.5f,26.5f,26.5f,-55,110,false,paint);
            c.drawArc(1.5f,1.5f,26.5f,26.5f,125,110,false,paint);
        }else if("▤".equals(type)){
            c.drawRoundRect(6,5,22,24,2,2,paint);
            line(c,10,10,19,10);line(c,10,14,17,14);
            paint.setStyle(Paint.Style.FILL);
            c.drawCircle(11,20,1.4f,paint);
            c.drawRect(14,16,17,22,paint);
        }else if("⌕".equals(type)){
            c.drawCircle(12,12,7.7f,paint);
            line(c,18.1f,18.1f,24,24);
        }
        c.restoreToCount(save);
    }
}

