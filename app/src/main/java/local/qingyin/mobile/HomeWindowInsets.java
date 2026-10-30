package local.qingyin.mobile;

import android.graphics.Color;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Background scenes paint behind the status bar; only their foreground content is inset. */
final class HomeWindowInsets {
    private HomeWindowInsets() {}

    static void styleStatusBar(Window window, boolean dark) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        int flags = window.getDecorView().getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) window.setStatusBarContrastEnforced(false);
    }

    static void install(FrameLayout sceneRoot, View[] pages, View bottomChrome) {
        // Insets normally arrive before the first frame. The resource is only an initial
        // estimate for a not-yet-attached root, replaced by actual window/cutout insets.
        int resource = sceneRoot.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int initialTop = resource == 0 ? 0 : sceneRoot.getResources().getDimensionPixelSize(resource);
        placeContent(pages, bottomChrome, 0, initialTop, 0);
        sceneRoot.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = 0, top, right = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.displayCutout());
                left = safe.left; top = safe.top; right = safe.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        left = cutout.getSafeInsetLeft(); right = cutout.getSafeInsetRight();
                        top = Math.max(top, cutout.getSafeInsetTop());
                    }
                }
            }
            placeContent(pages, bottomChrome, left, top, right);
            // No layout-behind-navigation flag is set: the window still fits its system
            // navigation area as before. Keep the remaining insets available to children.
            return insets;
        });
        sceneRoot.requestApplyInsets();
    }

    private static void placeContent(View[] pages, View bottomChrome, int left, int top, int right) {
        for (View page : pages) margins(page, left, top, right);
        margins(bottomChrome, left, 0, right);
    }

    private static void margins(View view, int left, int top, int right) {
        if (view == null || !(view.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (params.leftMargin == left && params.topMargin == top && params.rightMargin == right) return;
        params.leftMargin = left; params.topMargin = top; params.rightMargin = right;
        view.setLayoutParams(params);
    }
}
