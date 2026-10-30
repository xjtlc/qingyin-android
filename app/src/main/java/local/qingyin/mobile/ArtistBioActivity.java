package local.qingyin.mobile;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/* JADX INFO: loaded from: classes2.dex */
public final class ArtistBioActivity extends Activity {
    private int dp(int value) {
        return QingyinUi.dp(this, value);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        return QingyinUi.text(this, value, size, color, bold);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle state) {
        super.onCreate(state);
        String artist = getIntent().getStringExtra("artist_name");
        String image = getIntent().getStringExtra("artist_cover");
        String description = getIntent().getStringExtra("artist_bio");
        String birthday = getIntent().getStringExtra("artist_birthday");
        int theme = getIntent().getIntExtra("theme", -13356985);
        if (artist == null) {
            artist = "";
        }
        if (description == null) {
            description = "";
        }
        if (birthday == null) {
            birthday = "";
        }
        getWindow().setStatusBarColor(ArtistTheme.darker(theme, 0.8f));
        getWindow().setNavigationBarColor(ArtistTheme.darker(theme, 0.57f));
        getWindow().getDecorView().setSystemUiVisibility(0);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ArtistTheme.darker(theme, 0.57f));
        scrollView.setVerticalScrollBarEnabled(false);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(theme);
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        setContentView(scrollView);
        MusicOverlayDock.attach(this);
        FrameLayout header = new FrameLayout(this);
        linearLayout.addView(header, new LinearLayout.LayoutParams(-1, dp(360)));
        FrameLayout imageFrame = Artwork.frame(this, 0);
        header.addView(imageFrame, new FrameLayout.LayoutParams(-1, -1));
        if (image != null && !image.isEmpty()) {
            Artwork.bind(imageFrame, image);
        }
        View wash = new View(this);
        GradientDrawable overlay = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0, 0, 1358954496, theme, theme});
        overlay.setDither(true);
        wash.setBackground(overlay);
        header.addView(wash, new FrameLayout.LayoutParams(-1, -1));
        TextView back = text("‹", 34, -1, true);
        back.setGravity(17);
        back.setContentDescription("返回歌手主页");
        back.setBackground(ArtistTheme.glass(this, 28));
        back.setOnClickListener(new View.OnClickListener() { // from class: local.qingyin.mobile.ArtistBioActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ArtistBioActivity.this.m36lambda$onCreate$0$localqingyinmobileArtistBioActivity(view);
            }
        });
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(45), dp(45), 51);
        bp.topMargin = dp(13);
        bp.leftMargin = dp(18);
        header.addView(back, bp);
        TextView artistName = text(artist, 32, -1, true);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, -2, 80);
        np.leftMargin = dp(22);
        np.rightMargin = dp(20);
        np.bottomMargin = dp(26);
        header.addView(artistName, np);
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(1);
        details.setPadding(dp(22), dp(5), dp(22), dp(114));
        details.setMinimumHeight(Math.max(dp(520), getResources().getDisplayMetrics().heightPixels - dp(348)));
        details.setBackground(ArtistTheme.gradient(theme, ArtistTheme.darker(theme, 0.57f), 0));
        linearLayout.addView(details, new LinearLayout.LayoutParams(-1, -2));
        if (!birthday.trim().isEmpty()) {
            TextView heading = text("出生日期", 12, -2369828, false);
            details.addView(heading);
            String date = birthday.trim();
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] chunks = date.split("-");
                date = chunks[0] + "年" + Integer.parseInt(chunks[1]) + "月" + Integer.parseInt(chunks[2]) + "日";
            }
            TextView value = text(date, 17, -1, true);
            LinearLayout.LayoutParams vd = new LinearLayout.LayoutParams(-1, -2);
            vd.topMargin = dp(6);
            vd.bottomMargin = dp(24);
            details.addView(value, vd);
        }
        TextView heading2 = text("简介", 17, -1, true);
        details.addView(heading2);
        TextView info = text(description.trim().isEmpty() ? "暂无歌手简介" : description.trim(), 14, -1120019, false);
        info.setLineSpacing(dp(6), 1.08f);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(12);
        details.addView(info, detailLp);
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$local-qingyin-mobile-ArtistBioActivity, reason: not valid java name */
    /* synthetic */ void m36lambda$onCreate$0$localqingyinmobileArtistBioActivity(View v) {
        finish();
    }
}
