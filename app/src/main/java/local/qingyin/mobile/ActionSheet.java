package local.qingyin.mobile;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Outline;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/** A compact, themed surface with a persistent context header. */
class ActionSheet {
    private static final ArrayList<WeakReference<ActionSheet>> SHOWN=new ArrayList<>();
    final Activity host; final Dialog dialog; final LinearLayout content; final boolean dark;
    private final LinearLayout panel; private final FrameLayout artwork; private final TextView title,subtitle;
    private final ScrollView contentScroll;
    private final View backdrop;
    private final ArrayList<ViewTreeObserver> observers=new ArrayList<>();
    private FrostedGlassDrawable glass;
    private boolean glassActive,refreshQueued,gestureActive,contentScrollBusy;
    private long lastRefresh;
    private long refreshIntervalMs=160;
    private final Runnable updateGlass=()->{
        refreshQueued=false;
        if(glassActive&&glass!=null&&!gestureActive&&!contentScrollBusy){lastRefresh=SystemClock.uptimeMillis();glass.refresh();}
    };
    private final ViewTreeObserver.OnDrawListener backgroundDraw=()->scheduleGlass();
    private final ViewTreeObserver.OnScrollChangedListener backgroundScroll=()->scheduleGlass();
    private final ViewTreeObserver.OnGlobalLayoutListener backgroundLayout=()->scheduleGlass();
    ActionSheet(Activity activity,String heading,String detail,boolean useDark){
        host=activity;backdrop=activity.getWindow().getDecorView();
        dark=useDark||FrostedGlassDrawable.isDarkBackdrop(backdrop,AppearancePreferences.dark(activity));
        int ink=inkColor(),muted=mutedColor();
        dialog=new Dialog(activity){
            @Override protected void onStart(){super.onStart();UiPerformance.bindDialog(this);startGlass();}
            @Override protected void onStop(){stopGlass();UiPerformance.unbindDialog(this);super.onStop();}
        };dialog.setCanceledOnTouchOutside(true);
        panel=new LinearLayout(activity){
            @Override protected void onMeasure(int width,int height){
                int cap=(int)(getResources().getDisplayMetrics().heightPixels*.80f);
                int limit=MeasureSpec.getMode(height)==MeasureSpec.UNSPECIFIED?cap:Math.min(cap,MeasureSpec.getSize(height));
                super.onMeasure(width,MeasureSpec.makeMeasureSpec(limit,MeasureSpec.AT_MOST));
            }
            @Override public boolean dispatchTouchEvent(MotionEvent event){
                if(event.getActionMasked()==MotionEvent.ACTION_DOWN){gestureActive=true;updateCapturePause();}
                boolean handled=super.dispatchTouchEvent(event);
                if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){gestureActive=false;updateCapturePause();}
                return handled;
            }
        };
        panel.setOrientation(1);panel.setPadding(dp(16),dp(2),dp(16),dp(12));
        panel.setBackground(QingyinUi.rounded(activity,dark?0x58181B22:0x58FFFFFF,24));panel.setClipToOutline(true);
        panel.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View v,Outline outline){outline.setRoundRect(0,0,v.getWidth(),v.getHeight(),dp(24));}});
        panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            @Override public void onViewAttachedToWindow(View v){}
            @Override public void onViewDetachedFromWindow(View v){stopGlass();}
        });
        FrameLayout handle=new FrameLayout(activity);View line=new View(activity);
        line.setBackground(QingyinUi.rounded(activity,dark?0x72FFFFFF:0x483A4250,2));
        handle.addView(line,new FrameLayout.LayoutParams(dp(32),dp(4),Gravity.CENTER));
        handle.setOnTouchListener(new View.OnTouchListener(){float down;public boolean onTouch(View v,MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){down=e.getRawY();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_UP&&e.getRawY()-down>dp(24))dialog.dismiss();return true;
        }});panel.addView(handle,new LinearLayout.LayoutParams(-1,dp(20)));
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(4),0,0,0);header.setMinimumHeight(dp(60));
        artwork=Artwork.frame(activity,10);artwork.setVisibility(View.GONE);
        LinearLayout.LayoutParams artParams=new LinearLayout.LayoutParams(dp(44),dp(44));artParams.rightMargin=dp(12);header.addView(artwork,artParams);
        LinearLayout words=new LinearLayout(activity);words.setOrientation(1);
        title=QingyinUi.text(activity,heading,17,ink,true);title.setIncludeFontPadding(false);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(title);
        subtitle=QingyinUi.text(activity,detail==null?"":detail,12,muted,false);subtitle.setIncludeFontPadding(false);subtitle.setSingleLine(true);subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);subtitle.setPadding(0,dp(4),0,0);subtitle.setVisibility(detail==null||detail.isEmpty()?View.GONE:View.VISIBLE);words.addView(subtitle);
        header.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout close=new FrameLayout(activity);close.setContentDescription("关闭面板");close.setBackground(QingyinUi.touch(activity,dark?0x18FFFFFF:0x50FFFFFF,22));
        close.addView(new ActionGlyph(activity,"close",muted),new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));close.setOnClickListener(v->dialog.dismiss());header.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout.LayoutParams headerParams=new LinearLayout.LayoutParams(-1,-2);headerParams.bottomMargin=dp(7);panel.addView(header,headerParams);
        content=new LinearLayout(activity);content.setOrientation(1);
        contentScroll=new ScrollView(activity);contentScroll.setFillViewport(false);contentScroll.setVerticalScrollBarEnabled(false);contentScroll.setClipToPadding(false);contentScroll.setNestedScrollingEnabled(true);
        contentScroll.addView(content,new ScrollView.LayoutParams(-1,-2));panel.addView(contentScroll,new LinearLayout.LayoutParams(-1,-2));dialog.setContentView(panel);
    }
    private int dp(float n){return QingyinUi.dp(host,n);}
    int inkColor(){return dark?0xffFAFAFC:0xff171A21;}
    int mutedColor(){return dark?0xffD1D3DD:0xff5B616D;}
    int accentColor(){return dark?0xffFFC0D3:0xffB62F54;}
    /** ListView owns its own touch, fling and recycling; header/tool content stays outside it. */
    void setScrollableContent(View scrollingView,int heightPx){
        contentScroll.removeView(content);panel.removeView(contentScroll);
        panel.addView(content,new LinearLayout.LayoutParams(-1,-2));
        if(scrollingView.getParent() instanceof ViewGroup)((ViewGroup)scrollingView.getParent()).removeView(scrollingView);
        // Weight only shrinks the viewport if a small/landscape window leaves less room.
        panel.addView(scrollingView,new LinearLayout.LayoutParams(-1,heightPx,1f));
    }
    void setBackdropRefreshInterval(long intervalMs){refreshIntervalMs=Math.max(160,intervalMs);}
    void setContentScrollBusy(boolean busy){contentScrollBusy=busy;updateCapturePause();}
    void refreshBackdrop(){if(glassActive&&glass!=null){glass.refresh();scheduleGlass();}}
    private void updateCapturePause(){
        boolean paused=gestureActive||contentScrollBusy;
        if(glass!=null)glass.setCapturePaused(paused);
        if(paused){panel.removeCallbacks(updateGlass);refreshQueued=false;}
        else scheduleGlass();
    }
    private void observe(View source){
        ViewTreeObserver observer=source.getViewTreeObserver();
        if(!observer.isAlive()||observers.contains(observer))return;
        observer.addOnDrawListener(backgroundDraw);observer.addOnScrollChangedListener(backgroundScroll);observer.addOnGlobalLayoutListener(backgroundLayout);observers.add(observer);
    }
    private void startGlass(){
        if(glassActive)return;
        ArrayList<View> layers=new ArrayList<>();
        for(Iterator<WeakReference<ActionSheet>> it=SHOWN.iterator();it.hasNext();){
            ActionSheet previous=it.next().get();
            if(previous==null||!previous.dialog.isShowing()){it.remove();continue;}
            if(previous!=this&&previous.host==host&&previous.dialog.getWindow()!=null)layers.add(previous.dialog.getWindow().getDecorView());
        }
        glass=new FrostedGlassDrawable(backdrop,panel,dp(24),dark?0x58181B22:0x58FFFFFF,true,layers.toArray(new View[0]));
        panel.setBackground(glass);glassActive=true;lastRefresh=0;
        glass.setCapturePaused(gestureActive||contentScrollBusy);
        SHOWN.add(new WeakReference<>(this));observe(backdrop);for(View layer:layers)observe(layer);
        panel.post(this::scheduleGlass);
    }
    private void scheduleGlass(){
        if(!glassActive||refreshQueued||gestureActive||contentScrollBusy)return;
        refreshQueued=true;panel.postDelayed(updateGlass,Math.max(0,refreshIntervalMs-(SystemClock.uptimeMillis()-lastRefresh)));
    }
    private void stopGlass(){
        glassActive=false;refreshQueued=false;gestureActive=false;contentScrollBusy=false;panel.removeCallbacks(updateGlass);panel.animate().cancel();
        for(ViewTreeObserver observer:observers)if(observer.isAlive()){
            observer.removeOnDrawListener(backgroundDraw);observer.removeOnScrollChangedListener(backgroundScroll);observer.removeOnGlobalLayoutListener(backgroundLayout);
        }
        observers.clear();if(glass!=null){glass.release();glass=null;}
        for(Iterator<WeakReference<ActionSheet>> it=SHOWN.iterator();it.hasNext();){ActionSheet sheet=it.next().get();if(sheet==null||sheet==this)it.remove();}
    }
    void setSong(Track song){if(song==null)return;title.setText(song.title);subtitle.setText(song.artist);subtitle.setVisibility(View.VISIBLE);artwork.setVisibility(View.VISIBLE);Artwork.bind(artwork,song.coverUrl);}
    void setHeading(CharSequence heading){title.setText(heading);subtitle.setVisibility(View.GONE);}
    void addSeparator(){View line=new View(host);line.setBackgroundColor(dark?0x12FFFFFF:0x10000000);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,1);p.leftMargin=dp(48);p.topMargin=dp(5);p.bottomMargin=dp(5);content.addView(line,p);}
    void addAction(String glyph,String name,String detail,Runnable action){
        LinearLayout row=new LinearLayout(host);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(7),0,dp(6),0);row.setBackground(QingyinUi.touch(host,Color.TRANSPARENT,12));
        row.addView(new ActionGlyph(host,glyph,accentColor()),new LinearLayout.LayoutParams(dp(24),dp(24)));
        LinearLayout words=new LinearLayout(host);words.setOrientation(1);words.setPadding(dp(13),0,0,0);
        TextView label=QingyinUi.text(host,name,15,inkColor(),false);label.setIncludeFontPadding(false);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(label);
        boolean described=detail!=null&&!detail.isEmpty();
        if(described){TextView hint=QingyinUi.text(host,detail,11,mutedColor(),false);hint.setSingleLine(true);hint.setEllipsize(android.text.TextUtils.TruncateAt.END);hint.setIncludeFontPadding(false);hint.setPadding(0,dp(3),0,0);words.addView(hint);}
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));row.setContentDescription(name+(described?"，"+detail:""));row.setOnClickListener(v->{dialog.dismiss();action.run();});
        float fontScale=host.getResources().getConfiguration().fontScale;
        content.addView(row,new LinearLayout.LayoutParams(-1,dp((described?54:48)*Math.max(1f,fontScale/1.2f))));
    }
    void show(){if(host.isFinishing()||host.isDestroyed())return;dialog.show();Window window=dialog.getWindow();if(window==null)return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams p=window.getAttributes();p.width=Math.min(host.getResources().getDisplayMetrics().widthPixels-dp(20),dp(560));p.height=-2;p.gravity=Gravity.BOTTOM;p.y=dp(10);p.dimAmount=.14f;window.setAttributes(p);
        panel.setAlpha(.65f);panel.setTranslationY(dp(16));panel.animate().alpha(1f).translationY(0).setDuration(180).withEndAction(this::refreshBackdrop).start();
    }
}
