package local.qingyin.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.TextView;

/** Shared presentation primitives; no playback, login or networking dependencies. */
final class QingyinUi {
    static final int BACKGROUND = 0xffF7F7F9;
    static final int INK = 0xff1B1B1F;
    static final int MUTED = 0xff797B85;
    static final int BLUE = 0xffE44C6A;
    static final int SOFT = 0xffFBE8EE;

    private QingyinUi() {}
    static int dp(Context c, float n) {
        return Math.round(n * c.getResources().getDisplayMetrics().density);
    }
    static GradientDrawable rounded(Context c, int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c,radius));
        return g;
    }
    static GradientDrawable gradient(Context c, int start, int end, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});
        g.setCornerRadius(dp(c,radius));
        return g;
    }
    /** Consistent Android touch feedback with a clipped rounded ripple. */
    static Drawable touch(Context c, int fill, float radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x27E44C6A),
            rounded(c,fill,radius),rounded(c,Color.WHITE,radius));
    }
    static TextView text(Context c, String title, int size, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(title); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    static Button button(Context c, String caption, boolean accent) {
        Button b = new Button(c);
        b.setText(caption); b.setTextSize(12); b.setAllCaps(false);
        b.setTextColor(accent?Color.WHITE:BLUE);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(accent?0x36FFFFFF:0x27E44C6A),
            rounded(c,accent?BLUE:SOFT,13),rounded(c,Color.WHITE,13)));
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
        b.setPadding(dp(c,8),0,dp(c,8),0);
        return b;
    }

    static final class Sheet extends ActionSheet {
        Sheet(android.app.Activity host,String title,String subtitle,boolean dark){super(host,title,subtitle,dark);}
    }
}
