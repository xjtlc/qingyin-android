package local.qingyin.mobile;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.ViewGroup;

/** Brief, non-interactive vector hearts; removed as soon as the animation ends. */
final class HeartBurstView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path heart=new Path();
    private final float x,y,density;
    private float progress;
    private ValueAnimator animator;
    HeartBurstView(Context context,float x,float y){
        super(context);this.x=x;this.y=y;density=getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);
        heart.moveTo(0,8);heart.cubicTo(-5,4,-11,-1,-9,-6);heart.cubicTo(-7,-11,-2,-10,0,-5);
        heart.cubicTo(2,-10,7,-11,9,-6);heart.cubicTo(11,-1,5,4,0,8);heart.close();
    }
    void start(){
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(650);
        animator.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidate();});
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(getParent() instanceof ViewGroup)((ViewGroup)getParent()).removeView(HeartBurstView.this);}});animator.start();
    }
    @Override protected void onDraw(Canvas canvas){
        paint.setColor(0xffFF7699);paint.setAlpha((int)(255*(1-progress)));
        for(int i=0;i<7;i++){
            double angle=Math.PI*(1.1+i*.13);float distance=(16+progress*48)*density;
            int saved=canvas.save();canvas.translate(x+(float)Math.cos(angle)*distance,y+(float)Math.sin(angle)*distance-progress*20*density);
            float scale=density*(.45f+(i%3)*.12f)*(1-progress*.45f);canvas.scale(scale,scale);canvas.drawPath(heart,paint);canvas.restoreToCount(saved);
        }
    }
    @Override protected void onDetachedFromWindow(){if(animator!=null){animator.removeAllListeners();animator.cancel();animator=null;}super.onDetachedFromWindow();}
}
