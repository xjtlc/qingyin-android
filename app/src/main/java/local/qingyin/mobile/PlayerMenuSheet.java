package local.qingyin.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Song-specific player menu. All actions are supplied by the player, never simulated here. */
final class PlayerMenuSheet {
    interface Listener { void onAction(String action); }

    private final Activity host;
    private final Dialog dialog;
    private final Listener listener;
    private final LinearLayout panel, rows;
    private final boolean dark;
    private final int ink, muted, accent, surface;

    PlayerMenuSheet(Activity activity, Track song, boolean favorite, Listener listener) {
        if (listener == null) throw new IllegalArgumentException("A player menu needs action handlers");
        this.host = activity;
        this.listener = listener;
        dark = false; // The song menu follows the reference's clean white surface.
        ink = dark ? 0xffF6F6FA : 0xff282A3E;
        muted = dark ? 0xffA9ADBC : 0xff888B9B;
        accent = dark ? 0xffFF9FB8 : 0xffEE476C;
        surface = dark ? 0xff20212B : Color.WHITE;
        dialog = new Dialog(activity);
        dialog.setCanceledOnTouchOutside(true);

        panel = new LinearLayout(activity) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int cap = (int) (getResources().getDisplayMetrics().heightPixels * .86f);
                int limit = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED
                        ? cap : Math.min(cap, MeasureSpec.getSize(heightSpec));
                super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST));
            }
        };
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), 0, dp(20), dp(16));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(surface);
        float corner = dp(24);
        shape.setCornerRadii(new float[]{corner, corner, corner, corner, 0, 0, 0, 0});
        panel.setBackground(shape);
        panel.setClipToOutline(true);

        FrameLayout handleArea = new FrameLayout(activity);
        handleArea.setContentDescription("下滑关闭歌曲菜单");
        View handle = new View(activity);
        handle.setBackground(QingyinUi.rounded(activity, dark ? 0xff626571 : 0xffE5E5E8, 3));
        handleArea.addView(handle, new FrameLayout.LayoutParams(dp(34), dp(4), Gravity.CENTER));
        handleArea.setOnTouchListener(new View.OnTouchListener() {
            private float downY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { downY = event.getRawY(); return true; }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (event.getRawY() - downY > dp(24)) dismiss();
                    return true;
                }
                return true;
            }
        });
        panel.addView(handleArea, new LinearLayout.LayoutParams(-1, dp(24)));

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setMinimumHeight(dp(68));
        FrameLayout cover = Artwork.frame(activity, 8);
        Artwork.bind(cover, song == null ? "" : song.coverUrl);
        header.addView(cover, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout words = new LinearLayout(activity);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(13), 0, 0, 0);
        TextView name = text(song == null ? "正在播放" : song.title, 17, ink, true);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(name, new LinearLayout.LayoutParams(-1, -2));
        TextView artist = text(song == null ? "青音" : song.artist, 13, muted, false);
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        artist.setPadding(0, dp(6), 0, 0);
        words.addView(artist, new LinearLayout.LayoutParams(-1, -2));
        header.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
        headerLp.bottomMargin = dp(12);
        panel.addView(header, headerLp);

        LinearLayout quick = new LinearLayout(activity);
        quick.setGravity(Gravity.CENTER_VERTICAL);
        quick.addView(shortcut("favorite", favorite ? "heart" : "save", favorite ? "已收藏" : "收藏", favorite), new LinearLayout.LayoutParams(0, dp(88), 1));
        quick.addView(shortcut("download", "download", "下载", false), new LinearLayout.LayoutParams(0, dp(88), 1));
        quick.addView(shortcut("share", "share", "分享", false), new LinearLayout.LayoutParams(0, dp(88), 1));
        quick.addView(shortcut("listen", "listen", "一起听", false), new LinearLayout.LayoutParams(0, dp(88), 1));
        panel.addView(quick, new LinearLayout.LayoutParams(-1, -2));

        View separator = new View(activity);
        separator.setBackgroundColor(dark ? 0x12FFFFFF : 0xffF2F2F5);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(-1, 1);
        lineLp.topMargin = dp(4);
        lineLp.bottomMargin = dp(10);
        panel.addView(separator, lineLp);

        rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(false);
        scroll.setNestedScrollingEnabled(true);
        scroll.addView(rows, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        dialog.setContentView(panel);
        dialog.setOnDismissListener(ignored -> {panel.animate().cancel();UiPerformance.unbindDialog(dialog);});
    }

    private int dp(float value) { return QingyinUi.dp(host, value); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = QingyinUi.text(host, value, size, color, bold);
        view.setIncludeFontPadding(false);
        return view;
    }

    private View shortcut(String action, String icon, String label, boolean selected) {
        LinearLayout item = new LinearLayout(host);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), dp(8), dp(2), dp(8));
        item.setBackground(QingyinUi.touch(host, Color.TRANSPARENT, 14));
        item.setFocusable(true);
        item.setContentDescription(label);
        item.setSelected(selected);
        item.addView(new ActionGlyph(host, icon, selected ? accent : ink), new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView caption = text(label, 14, selected ? accent : ink, false);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setGravity(Gravity.CENTER);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-1, -2);
        labelLp.topMargin = dp(10);
        item.addView(caption, labelLp);
        item.setOnClickListener(v -> perform(action));
        return item;
    }

    PlayerMenuSheet addItem(String action, String icon, String label, String detail) {
        String value = detail == null ? "" : detail.trim();
        LinearLayout row = new LinearLayout(host);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(52));
        row.setPadding(0, dp(12), dp(1), dp(12));
        row.setBackground(QingyinUi.touch(host, Color.TRANSPARENT, 12));
        row.setFocusable(true);
        row.setContentDescription(value.isEmpty() ? label : label + "，" + value);
        row.addView(new ActionGlyph(host, icon, ink), new LinearLayout.LayoutParams(dp(23), dp(23)));
        TextView caption = text(value.isEmpty() ? label : label + "：" + value, 16, ink, false);
        caption.setMaxLines(2);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(0, -2, 1);
        captionLp.leftMargin = dp(14);
        captionLp.rightMargin = dp(8);
        row.addView(caption, captionLp);
        row.addView(new ActionGlyph(host, "chevron", muted), new LinearLayout.LayoutParams(dp(16), dp(20)));
        row.setOnClickListener(v -> perform(action));
        rows.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return this;
    }

    private void perform(String action) {
        if (!dialog.isShowing()) return;
        dialog.dismiss();
        if (!host.isFinishing() && !host.isDestroyed()) listener.onAction(action);
    }

    void dismiss() { dialog.dismiss(); }

    void show() {
        if (host.isFinishing() || host.isDestroyed()) return;
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(surface);
        window.getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = Math.min(host.getResources().getDisplayMetrics().widthPixels, dp(560));
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.BOTTOM;
        params.dimAmount = .38f;
        window.setAttributes(params);
        UiPerformance.bindDialog(dialog);
        panel.setAlpha(.85f);
        panel.setTranslationY(dp(28));
        panel.animate().alpha(1f).translationY(0).setDuration(190).start();
    }
}
