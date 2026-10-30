package local.qingyin.mobile;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/** Request a native high-refresh mode during interaction/visible animation; release when idle. */
final class UiPerformance {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final WeakHashMap<Window,Controller> WINDOWS=new WeakHashMap<>();
    private static final WeakHashMap<Activity,Boolean> RESUMED=new WeakHashMap<>();
    private static boolean installed,powerSaving;
    private UiPerformance(){}
    static void install(Application app){
        if(installed)return;installed=true;
        PowerManager power=(PowerManager)app.getSystemService(Context.POWER_SERVICE);
        powerSaving=power!=null&&power.isPowerSaveMode();
        BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
            powerSaving=power!=null&&power.isPowerSaveMode();
            for(Controller controller:new ArrayList<>(WINDOWS.values()))controller.reconcile();
        }};
        IntentFilter filter=new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        if(Build.VERSION.SDK_INT>=33)app.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else app.registerReceiver(receiver,filter);
        DisplayManager displays=(DisplayManager)app.getSystemService(Context.DISPLAY_SERVICE);
        if(displays!=null)displays.registerDisplayListener(new DisplayManager.DisplayListener(){
            public void onDisplayAdded(int id){}
            public void onDisplayRemoved(int id){}
            public void onDisplayChanged(int id){for(Controller c:new ArrayList<>(WINDOWS.values())){c.modeChecked=0;c.reconcile();}}
        },MAIN);
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity a,Bundle state){bind(a,a.getWindow());}
            public void onActivityStarted(Activity a){}
            public void onActivityResumed(Activity a){RESUMED.put(a,true);bind(a,a.getWindow());for(Controller c:new ArrayList<>(WINDOWS.values()))if(c.owner==a){c.active=true;c.attachScroll();c.boost();}}
            public void onActivityPaused(Activity a){RESUMED.put(a,false);for(Controller c:new ArrayList<>(WINDOWS.values()))if(c.owner==a){c.active=false;c.reconcile();}}
            public void onActivityStopped(Activity a){}
            public void onActivitySaveInstanceState(Activity a,Bundle state){}
            public void onActivityDestroyed(Activity a){RESUMED.remove(a);for(Controller c:new ArrayList<>(WINDOWS.values()))if(c.owner==a){WINDOWS.remove(c.window);c.dispose();}}
        });
    }
    private static Activity activity(Context context){
        while(context instanceof ContextWrapper){if(context instanceof Activity)return(Activity)context;Context next=((ContextWrapper)context).getBaseContext();if(next==context)break;context=next;}return null;
    }
    private static Controller bind(Activity owner,Window window){
        if(owner==null||window==null)return null;
        Controller controller=WINDOWS.get(window);
        if(controller==null){controller=new Controller(owner,window);WINDOWS.put(window,controller);}
        return controller;
    }
    static void bindDialog(Dialog dialog){
        Activity owner=dialog.getOwnerActivity();if(owner==null)owner=activity(dialog.getContext());
        Controller controller=bind(owner,dialog.getWindow());if(controller!=null){controller.active=Boolean.TRUE.equals(RESUMED.get(owner));controller.boost();}
    }
    static void unbindDialog(Dialog dialog){Controller controller=WINDOWS.remove(dialog.getWindow());if(controller!=null)controller.dispose();}
    static void setAnimationActive(View view,boolean enabled){
        Activity owner=activity(view.getContext());if(owner==null)return;
        Controller controller=WINDOWS.get(owner.getWindow());
        View root=view.getRootView();
        for(Controller candidate:WINDOWS.values())if(candidate.owner==owner&&candidate.window.peekDecorView()==root){controller=candidate;break;}
        if(controller==null)return;
        if(enabled)controller.animations.put(view,true);else controller.animations.remove(view);
        controller.reconcile();
    }
    private static final class Controller {
        final Activity owner;final Window window;final Window.Callback original;final Forwarder callback;
        final int originalMode;final float originalRate;
        final WeakHashMap<View,Boolean> animations=new WeakHashMap<>();
        ViewTreeObserver scrollObserver;
        final ViewTreeObserver.OnScrollChangedListener scrolling=this::boost;
        boolean active,disposed,decayQueued;
        long boostedUntil,modeChecked;int bestMode;float bestRate;
        final Runnable decay=()->{decayQueued=false;reconcile();};
        Controller(Activity owner,Window window){
            this.owner=owner;this.window=window;original=window.getCallback();
            WindowManager.LayoutParams attrs=window.getAttributes();originalMode=attrs.preferredDisplayModeId;originalRate=attrs.preferredRefreshRate;
            callback=new Forwarder(original,this);window.setCallback(callback);active=Boolean.TRUE.equals(RESUMED.get(owner));
            window.getDecorView().post(()->{if(!disposed){attachScroll();reconcile();}});
        }
        void attachScroll(){
            if(disposed||!window.getDecorView().isAttachedToWindow())return;ViewTreeObserver next=window.getDecorView().getViewTreeObserver();
            if(next==scrollObserver&&next.isAlive())return;
            if(scrollObserver!=null&&scrollObserver.isAlive())scrollObserver.removeOnScrollChangedListener(scrolling);
            scrollObserver=next;if(next.isAlive())next.addOnScrollChangedListener(scrolling);
        }
        void detachScroll(){if(scrollObserver!=null&&scrollObserver.isAlive())scrollObserver.removeOnScrollChangedListener(scrolling);scrollObserver=null;}
        void boost(){if(disposed||!active)return;boostedUntil=SystemClock.uptimeMillis()+1400;reconcile();}
        void findMode(){
            long now=SystemClock.uptimeMillis();if(modeChecked!=0&&now-modeChecked<2000)return;modeChecked=now;
            Display display=window.getDecorView().getDisplay();if(display==null)display=owner.getWindowManager().getDefaultDisplay();
            Display.Mode current=display.getMode();bestMode=current.getModeId();bestRate=current.getRefreshRate();
            for(Display.Mode candidate:display.getSupportedModes())if(candidate.getPhysicalWidth()==current.getPhysicalWidth()&&candidate.getPhysicalHeight()==current.getPhysicalHeight()&&candidate.getRefreshRate()>bestRate){bestMode=candidate.getModeId();bestRate=candidate.getRefreshRate();}
        }
        void reconcile(){
            if(disposed)return;
            boolean boosted=active&&!powerSaving&&(!animations.isEmpty()||SystemClock.uptimeMillis()<boostedUntil);
            if(boosted)findMode();
            apply(boosted?bestMode:originalMode,boosted?bestRate:originalRate);
            if(!boosted){MAIN.removeCallbacks(decay);decayQueued=false;}
            else if(animations.isEmpty()&&!decayQueued){decayQueued=true;MAIN.postDelayed(decay,Math.max(100,boostedUntil-SystemClock.uptimeMillis()+20));}
        }
        void apply(int mode,float rate){
            WindowManager.LayoutParams attrs=window.getAttributes();
            if(attrs.preferredDisplayModeId==mode&&Math.abs(attrs.preferredRefreshRate-rate)<.01f)return;
            attrs.preferredDisplayModeId=mode;attrs.preferredRefreshRate=rate;window.setAttributes(attrs);
        }
        void dispose(){
            if(disposed)return;disposed=true;MAIN.removeCallbacks(decay);animations.clear();
            detachScroll();
            if(window.getCallback()==callback)window.setCallback(original);apply(originalMode,originalRate);
        }
    }
    /** Explicit forwarding preserves framework callbacks without reflection on the input path. */
    private static final class Forwarder implements Window.Callback {
        final Window.Callback base;final Controller controller;
        Forwarder(Window.Callback base,Controller controller){this.base=base;this.controller=controller;}
        public boolean dispatchKeyEvent(KeyEvent e){controller.boost();return base.dispatchKeyEvent(e);}
        public boolean dispatchKeyShortcutEvent(KeyEvent e){controller.boost();return base.dispatchKeyShortcutEvent(e);}
        public boolean dispatchTouchEvent(MotionEvent e){controller.boost();return base.dispatchTouchEvent(e);}
        public boolean dispatchTrackballEvent(MotionEvent e){controller.boost();return base.dispatchTrackballEvent(e);}
        public boolean dispatchGenericMotionEvent(MotionEvent e){controller.boost();return base.dispatchGenericMotionEvent(e);}
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent e){return base.dispatchPopulateAccessibilityEvent(e);}
        public View onCreatePanelView(int id){return base.onCreatePanelView(id);}
        public boolean onCreatePanelMenu(int id,Menu m){return base.onCreatePanelMenu(id,m);}
        public boolean onPreparePanel(int id,View v,Menu m){return base.onPreparePanel(id,v,m);}
        public boolean onMenuOpened(int id,Menu m){return base.onMenuOpened(id,m);}
        public boolean onMenuItemSelected(int id,MenuItem m){return base.onMenuItemSelected(id,m);}
        public void onWindowAttributesChanged(WindowManager.LayoutParams p){base.onWindowAttributesChanged(p);}
        public void onContentChanged(){base.onContentChanged();controller.attachScroll();}
        public void onWindowFocusChanged(boolean focus){base.onWindowFocusChanged(focus);if(focus)controller.boost();}
        public void onAttachedToWindow(){base.onAttachedToWindow();controller.attachScroll();}
        public void onDetachedFromWindow(){controller.detachScroll();base.onDetachedFromWindow();}
        public void onPanelClosed(int id,Menu m){base.onPanelClosed(id,m);}
        public boolean onSearchRequested(){return base.onSearchRequested();}
        public boolean onSearchRequested(SearchEvent e){return base.onSearchRequested(e);}
        public ActionMode onWindowStartingActionMode(ActionMode.Callback c){return base.onWindowStartingActionMode(c);}
        public ActionMode onWindowStartingActionMode(ActionMode.Callback c,int type){return base.onWindowStartingActionMode(c,type);}
        public void onActionModeStarted(ActionMode m){base.onActionModeStarted(m);}
        public void onActionModeFinished(ActionMode m){base.onActionModeFinished(m);}
        public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> data,Menu m,int device){base.onProvideKeyboardShortcuts(data,m,device);}
        public void onPointerCaptureChanged(boolean captured){base.onPointerCaptureChanged(captured);}
    }
}
