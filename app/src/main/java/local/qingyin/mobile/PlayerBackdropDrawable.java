package local.qingyin.mobile;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Static layered lighting. Only the existing short track-change transition animates. */
final class PlayerBackdropDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int accent=0xff535762,top=0xff292C35,bottom=0xff171920,opacity=255;
    private Shader base,halo,edge,shade;
    private boolean dirty=true;
    void setAccent(int color){if(accent==color&&base!=null)return;accent=color;dirty=true;invalidateSelf();}
    int topColor(){prepare();return top;}
    int bottomColor(){prepare();return bottom;}
    @Override protected void onBoundsChange(Rect bounds){dirty=true;}
    private static int tint(float hue,float saturation,float value){return Color.HSVToColor(new float[]{hue,saturation,value});}
    private static int alpha(int color,int value){return(color&0x00ffffff)|(value<<24);}
    private void prepare(){
        if(!dirty)return;dirty=false;
        float[] hsv=new float[3];Color.colorToHSV(accent,hsv);
        float saturation=Math.min(.66f,hsv[1]*.84f);
        float glowSaturation=Math.min(.64f,hsv[1]*.92f);
        float light=Math.max(.24f,Math.min(.34f,.22f+hsv[2]*.18f));
        top=tint(hsv[0],saturation*.78f,light);
        int middle=tint(hsv[0],saturation*.9f,light*.83f);
        bottom=tint(hsv[0],saturation*.68f,Math.max(.105f,light*.47f));
        int glow=tint(hsv[0],glowSaturation,Math.max(.50f,Math.min(.76f,.48f+hsv[2]*.32f)));
        int soft=tint(hsv[0],glowSaturation*.65f,.48f);
        Rect bounds=getBounds();float width=Math.max(1,bounds.width()),height=Math.max(1,bounds.height());
        base=new LinearGradient(0,0,0,height,new int[]{top,middle,bottom},new float[]{0,.56f,1},Shader.TileMode.CLAMP);
        int glowAlpha=hsv[1]<.10f?78:132;
        halo=new RadialGradient(width*.38f,height*.34f,Math.max(width*.88f,height*.42f),
            new int[]{alpha(glow,glowAlpha),alpha(glow,glowAlpha/3),alpha(glow,0)},new float[]{0,.48f,1},Shader.TileMode.CLAMP);
        edge=new RadialGradient(width*1.04f,height*.44f,width*.83f,
            new int[]{alpha(soft,42),alpha(soft,0)},null,Shader.TileMode.CLAMP);
        shade=new LinearGradient(0,0,0,height,new int[]{0x00000000,0x05000000,0x48000000},new float[]{0,.55f,1},Shader.TileMode.CLAMP);
    }
    @Override public void draw(Canvas canvas){
        prepare();Rect bounds=getBounds();int saved=canvas.save();canvas.translate(bounds.left,bounds.top);
        paint.setAlpha(opacity);
        for(Shader layer:new Shader[]{base,halo,edge,shade}){paint.setShader(layer);canvas.drawRect(0,0,bounds.width(),bounds.height(),paint);}
        paint.setShader(null);canvas.restoreToCount(saved);
    }
    @Override public void setAlpha(int value){opacity=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
