package local.qingyin.mobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Native discovery shelves. The owner supplies data and handles navigation/playback. */
final class DiscoveryFeed extends LinearLayout {
    interface Listener {
        void onOpen(DiscoveryContent.Section section, DiscoveryContent.Item item);
        void onItemMore(DiscoveryContent.Section section, DiscoveryContent.Item item);
        void onRetry(DiscoveryContent.Section section);
    }

    private final Listener listener;
    private final LinkedHashMap<String, SectionView> sections = new LinkedHashMap<>();
    private final Rect visibleRect = new Rect();
    private ViewTreeObserver observedTree;
    private boolean visibilityCheckPosted;
    private boolean released;
    private final Runnable checkCovers = new Runnable() {
        @Override public void run() {
            visibilityCheckPosted = false;
            if (released || !isAttachedToWindow() || !isShown()) return;
            int starts = 0;
            boolean visiblePending = false;
            for (SectionView section : sections.values()) {
                for (CoverRequest request : section.covers) {
                    if (request.started || !request.cover.isShown()) continue;
                    if (!request.cover.getGlobalVisibleRect(visibleRect) || visibleRect.isEmpty()) continue;
                    if (starts >= 4) { visiblePending = true; continue; }
                    request.started = true;
                    if (request.large) Artwork.bindLarge(request.cover, request.url);
                    else Artwork.bind(request.cover, request.url);
                    starts++;
                }
            }
            if (visiblePending) scheduleCoverCheck();
        }
    };
    private final ViewTreeObserver.OnScrollChangedListener scrollListener = this::scheduleCoverCheck;
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::scheduleCoverCheck;

    DiscoveryFeed(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        setPadding(0, dp(6), 0, 0);
    }

    /** Sets order as well as content; existing shelves retain their horizontal position. */
    void setSections(List<DiscoveryContent.Section> content) {
        if (released) return;
        LinkedHashMap<String, SectionView> ordered = new LinkedHashMap<>();
        if (content != null) {
            for (DiscoveryContent.Section section : content) {
                if (section == null || section.key.isEmpty() || ordered.containsKey(section.key)) continue;
                SectionView view = sections.get(section.key);
                if (view == null) view = new SectionView();
                view.bind(section);
                ordered.put(section.key, view);
            }
        }
        sections.clear();
        sections.putAll(ordered);
        removeAllViews();
        for (SectionView view : sections.values()) addView(view, matchWrap());
        applyTheme();
        scheduleCoverCheck();
    }

    /** Updates one independent request without rebuilding the other shelves. */
    void update(DiscoveryContent.Section section) {
        if (released || section == null || section.key.isEmpty()) return;
        SectionView view = sections.get(section.key);
        if (view == null) {
            view = new SectionView();
            sections.put(section.key, view);
            addView(view, matchWrap());
        }
        view.bind(section);
        boolean dark = AppearancePreferences.dark(getContext());
        for (TintedSurface surface : view.surfaces) surface.apply(dark);
        AppearanceStyler.apply(view, dark);
        scheduleCoverCheck();
    }

    void applyTheme() {
        boolean dark = AppearancePreferences.dark(getContext());
        for (SectionView section : sections.values()) {
            for (TintedSurface surface : section.surfaces) surface.apply(dark);
        }
        AppearanceStyler.apply(this, dark);
    }

    void release() {
        released = true;
        stopObserving();
        removeCallbacks(checkCovers);
        visibilityCheckPosted = false;
        for (SectionView view : sections.values()) view.covers.clear();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (released) return;
        observedTree = getViewTreeObserver();
        observedTree.addOnScrollChangedListener(scrollListener);
        observedTree.addOnGlobalLayoutListener(layoutListener);
        scheduleCoverCheck();
    }

    @Override protected void onDetachedFromWindow() {
        stopObserving();
        removeCallbacks(checkCovers);
        visibilityCheckPosted = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && checkCovers != null) scheduleCoverCheck();
    }

    private void stopObserving() {
        if (observedTree != null && observedTree.isAlive()) {
            observedTree.removeOnScrollChangedListener(scrollListener);
            observedTree.removeOnGlobalLayoutListener(layoutListener);
        }
        observedTree = null;
    }

    private void scheduleCoverCheck() {
        if (released || visibilityCheckPosted || !isAttachedToWindow()) return;
        visibilityCheckPosted = true;
        postOnAnimation(checkCovers);
    }

    private final class SectionView extends LinearLayout {
        DiscoveryContent.Section section;
        final LinearLayout header;
        final TextView title;
        final TextView more;
        final TextView caption;
        final LinearLayout content;
        final LinearLayout state;
        final ArrayList<CoverRequest> covers = new ArrayList<>();
        final ArrayList<TintedSurface> surfaces = new ArrayList<>();
        String contentKey = "";
        ShelfRail rail;

        SectionView() {
            super(DiscoveryFeed.this.getContext());
            setOrientation(VERTICAL);
            setPadding(0, dp(11), 0, dp(15));
            setClipChildren(false);
            header = new LinearLayout(getContext());
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setMinimumHeight(dp(48));
            header.setBackground(QingyinUi.touch(getContext(), Color.TRANSPARENT, 10));
            AppearanceStyler.preserveBackground(header);
            header.setOnClickListener(v -> {
                if (listener != null && section != null && section.more != null) listener.onOpen(section, section.more);
            });
            title = text("", 21, QingyinUi.INK, true);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            more = text("›", 29, QingyinUi.MUTED, false);
            more.setGravity(Gravity.CENTER);
            more.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            header.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
            addView(header, matchWrap());
            caption = text("", 12, QingyinUi.MUTED, false);
            caption.setMaxLines(2);
            caption.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams captionParams = matchWrap();
            captionParams.bottomMargin = dp(10);
            addView(caption, captionParams);
            content = new LinearLayout(getContext());
            content.setOrientation(VERTICAL);
            content.setClipChildren(false);
            addView(content, matchWrap());
            state = new LinearLayout(getContext());
            state.setGravity(Gravity.CENTER_VERTICAL);
            state.setPadding(0, dp(7), 0, 0);
            addView(state, matchWrap());
        }

        void bind(DiscoveryContent.Section next) {
            DiscoveryContent.Section previous = section;
            section = next;
            title.setText(next.title);
            more.setVisibility(next.more == null ? GONE : VISIBLE);
            header.setClickable(next.more != null);
            header.setFocusable(next.more != null);
            header.setContentDescription(next.more == null ? null : "查看更多" + next.title);
            // Keep editorial scene guidance; source/update metadata stays outside the browse layout.
            boolean showCaption = "topics".equals(next.layout) || "explore".equals(next.layout);
            caption.setText(showCaption ? next.caption : "");
            caption.setVisibility(showCaption && !next.caption.isEmpty() ? VISIBLE : GONE);
            String newKey = contentKey(next);
            if (!newKey.equals(contentKey)) {
                int scrollX = rail == null ? 0 : rail.getScrollX();
                contentKey = newKey;
                content.removeAllViews();
                covers.clear();
                surfaces.clear();
                rail = null;
                if (!next.items.isEmpty()) {
                    if ("topics".equals(next.layout) || "explore".equals(next.layout)) buildTopics(this);
                    else {
                        rail = new ShelfRail();
                        if ("hero".equals(next.layout)) buildHeroes(this, rail);
                        else if ("songs".equals(next.layout)) buildSongPages(this, rail);
                        else buildCards(this, rail);
                        content.addView(rail, matchWrap());
                        if (scrollX > 0) {
                            ShelfRail updatedRail = rail;
                            updatedRail.post(() -> updatedRail.scrollTo(scrollX, 0));
                        }
                    }
                }
            } else if (previous != null && !next.loading && next.error.isEmpty() && !next.stale
                    && next.fetchedAt > 0 && previous.fetchedAt != next.fetchedAt) {
                // A user refresh can retry failed images even when the shelf metadata is unchanged.
                // Artwork's memory cache makes already successful covers an immediate no-op.
                for (CoverRequest request : covers) request.started = false;
            }
            state.removeAllViews();
            if (next.items.isEmpty()) {
                if (next.loading) showState("正在发现好音乐…", false);
                else showState(next.error.isEmpty() ? "暂时没有内容" : "暂时无法加载", true);
            } else if (!next.error.isEmpty()) showState("已保留上次内容", true);
            else state.setVisibility(GONE);
        }

        private void showState(String message, boolean retry) {
            state.setVisibility(VISIBLE);
            if (section.items.isEmpty()) state.setMinimumHeight(section.loading ? loadingHeight() : dp(80));
            else state.setMinimumHeight(0);
            TextView status = text(message, 13, QingyinUi.MUTED, false);
            state.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
            if (retry) {
                TextView button = text("重试", 13, QingyinUi.BLUE, true);
                button.setGravity(Gravity.CENTER);
                button.setPadding(dp(13), 0, dp(13), 0);
                button.setBackground(QingyinUi.touch(getContext(), Color.TRANSPARENT, 20));
                AppearanceStyler.preserveBackground(button);
                button.setOnClickListener(v -> { if (listener != null) listener.onRetry(section); });
                state.addView(button, new LinearLayout.LayoutParams(-2, dp(44)));
            }
        }

        private int loadingHeight() {
            int viewport = DiscoveryFeed.this.getWidth() > 0 ? DiscoveryFeed.this.getWidth()
                    : Math.max(dp(220), getResources().getDisplayMetrics().widthPixels - dp(38));
            if ("songs".equals(section.layout)) return songRowHeight() * 4 + dp(3);
            if ("hero".equals(section.layout)) {
                int tileWidth = Math.max(dp(230), Math.min(dp(480), viewport - dp(20)));
                return Math.max(dp(180), Math.round(tileWidth * .67f));
            }
            if ("topics".equals(section.layout) || "explore".equals(section.layout)) return dp(222);
            int coverSize = Math.max(dp(110), Math.min(dp(220), Math.round((viewport - dp(12)) / 2.12f)));
            return coverSize + dp(48 * Math.max(1f, getResources().getConfiguration().fontScale));
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width != oldWidth && section != null && section.loading && section.items.isEmpty())
                state.setMinimumHeight(loadingHeight());
        }
    }

    private void buildHeroes(SectionView holder, ShelfRail rail) {
        List<DiscoveryContent.Item> items = holder.section.items;
        for (int position = 0; position < Math.min(6, items.size()); position++) {
            DiscoveryContent.Item item = items.get(position);
            FrameLayout hero = cover(holder, item.cover, 15);
            hero.setMinimumHeight(dp(196));
            View shade = new View(getContext());
            GradientDrawable scrim = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x06000000, 0x15000000, 0xD911151C});
            scrim.setCornerRadius(dp(15));
            shade.setBackground(scrim);
            hero.addView(shade, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout info = new LinearLayout(getContext());
            info.setOrientation(VERTICAL);
            info.setPadding(dp(16), dp(24), dp(51), dp(17));
            TextView heading = text(item.title, 21, Color.WHITE, true);
            heading.setMaxLines(2);
            heading.setEllipsize(TextUtils.TruncateAt.END);
            info.addView(heading, matchWrap());
            if (!item.subtitle.isEmpty()) {
                TextView subtitle = text(item.subtitle, 12, 0xE6FFFFFF, false);
                subtitle.setMaxLines(2);
                subtitle.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams subtitleParams = matchWrap();
                subtitleParams.topMargin = dp(6);
                info.addView(subtitle, subtitleParams);
            }
            FrameLayout.LayoutParams infoParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            hero.addView(info, infoParams);
            ActionGlyph arrow = new ActionGlyph(getContext(), "chevron", Color.WHITE);
            arrow.setPadding(dp(8), dp(8), dp(8), dp(8));
            arrow.setBackground(QingyinUi.rounded(getContext(), 0x2AFFFFFF, 19));
            FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.BOTTOM | Gravity.END);
            arrowParams.rightMargin = dp(13);
            arrowParams.bottomMargin = dp(20);
            hero.addView(arrow, arrowParams);
            hero.setForeground(QingyinUi.touch(getContext(), Color.TRANSPARENT, 15));
            hero.setContentDescription(item.title + (item.subtitle.isEmpty() ? "" : "，" + item.subtitle));
            hero.setOnClickListener(v -> open(holder, item));
            rail.addTile(hero, hero, ShelfRail.HERO);
        }
    }

    private void buildCards(SectionView holder, ShelfRail rail) {
        List<DiscoveryContent.Item> items = holder.section.items;
        for (int position = 0; position < Math.min(12, items.size()); position++) {
            DiscoveryContent.Item item = items.get(position);
            LinearLayout tile = new LinearLayout(getContext());
            tile.setOrientation(VERTICAL);
            tile.setPadding(0, 0, 0, dp(3));
            FrameLayout art = cover(holder, item.cover, 10);
            tile.addView(art, new LinearLayout.LayoutParams(-1, dp(152)));
            TextView title = text(item.title, 14, QingyinUi.INK, false);
            title.setSingleLine();
            title.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams titleParams = matchWrap();
            titleParams.topMargin = dp(8);
            tile.addView(title, titleParams);
            if (!item.subtitle.isEmpty()) {
                TextView subtitle = text(item.subtitle, 12, QingyinUi.MUTED, false);
                subtitle.setSingleLine();
                subtitle.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams subtitleParams = matchWrap();
                subtitleParams.topMargin = dp(3);
                tile.addView(subtitle, subtitleParams);
            }
            tile.setBackground(QingyinUi.touch(getContext(), Color.TRANSPARENT, 10));
            AppearanceStyler.preserveBackground(tile);
            tile.setContentDescription(item.title + (item.subtitle.isEmpty() ? "" : "，" + item.subtitle));
            tile.setOnClickListener(v -> open(holder, item));
            rail.addTile(tile, art, ShelfRail.CARD);
        }
    }

    private void buildSongPages(SectionView holder, ShelfRail rail) {
        List<DiscoveryContent.Item> songs = holder.section.items;
        int count = Math.min(12, songs.size());
        for (int start = 0; start < count; start += 4) {
            LinearLayout page = new LinearLayout(getContext());
            page.setOrientation(VERTICAL);
            int end = Math.min(start + 4, count);
            for (int position = start; position < end; position++) {
                DiscoveryContent.Item item = songs.get(position);
                LinearLayout row = new LinearLayout(getContext());
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(songRowHeight());
                // Keep the menu beside the playback area, rather than inside a clickable row.
                // Its full-height target cannot fall through to the song's play action.
                LinearLayout playArea = new LinearLayout(getContext());
                playArea.setGravity(Gravity.CENTER_VERTICAL);
                playArea.setPadding(0, dp(4), 0, dp(4));
                playArea.setMinimumHeight(songRowHeight());
                FrameLayout art = cover(holder, item.cover, 7);
                LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(dp(44), dp(44));
                artParams.rightMargin = dp(11);
                playArea.addView(art, artParams);
                LinearLayout labels = new LinearLayout(getContext());
                labels.setOrientation(VERTICAL);
                TextView title = text(item.title, 15, QingyinUi.INK, false);
                title.setSingleLine();
                title.setEllipsize(TextUtils.TruncateAt.END);
                labels.addView(title, matchWrap());
                TextView artist = text(item.subtitle, 12, QingyinUi.MUTED, false);
                artist.setSingleLine();
                artist.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams artistParams = matchWrap();
                artistParams.topMargin = dp(2);
                labels.addView(artist, artistParams);
                playArea.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
                playArea.setBackground(QingyinUi.touch(getContext(), Color.TRANSPARENT, 8));
                AppearanceStyler.preserveBackground(playArea);
                playArea.setContentDescription("播放" + item.title + (item.subtitle.isEmpty() ? "" : "，" + item.subtitle));
                playArea.setOnClickListener(v -> open(holder, item));
                row.addView(playArea, new LinearLayout.LayoutParams(0, -1, 1));
                TextView menu = text("···", 22, QingyinUi.MUTED, true);
                menu.setGravity(Gravity.CENTER);
                menu.setContentDescription(item.title + "，更多操作");
                menu.setBackground(QingyinUi.touch(getContext(), Color.TRANSPARENT, 22));
                AppearanceStyler.preserveBackground(menu);
                menu.setOnClickListener(v -> { if (listener != null) listener.onItemMore(holder.section, currentItem(holder, item)); });
                menu.setMinimumHeight(songRowHeight());
                row.addView(menu, new LinearLayout.LayoutParams(dp(48), -1));
                page.addView(row, matchWrap());
                if (position + 1 < end) {
                    View separator = new View(getContext());
                    separator.setBackgroundColor(0x18797B85);
                    LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, dp(1));
                    lineParams.leftMargin = dp(55);
                    page.addView(separator, lineParams);
                }
            }
            rail.addTile(page, null, ShelfRail.SONGS);
        }
    }

    private void buildTopics(SectionView holder) {
        List<DiscoveryContent.Item> items = holder.section.items;
        int count = Math.min(12, items.size());
        for (int offset = 0; offset < count; offset += 2) {
            LinearLayout row = new LinearLayout(getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            for (int column = 0; column < 2; column++) {
                int position = offset + column;
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                if (column == 0) params.rightMargin = dp(9);
                if (position >= count) {
                    row.addView(new View(getContext()), params);
                    continue;
                }
                DiscoveryContent.Item item = items.get(position);
                LinearLayout tile = new LinearLayout(getContext());
                tile.setOrientation(VERTICAL);
                tile.setGravity(Gravity.CENTER_VERTICAL);
                tile.setMinimumHeight(dp(65));
                tile.setPadding(dp(14), dp(11), dp(11), dp(11));
                TextView title = text(item.title, 15, QingyinUi.INK, true);
                title.setSingleLine();
                title.setEllipsize(TextUtils.TruncateAt.END);
                tile.addView(title, matchWrap());
                if (!item.subtitle.isEmpty()) {
                    TextView subtitle = text(item.subtitle, 11, QingyinUi.MUTED, false);
                    subtitle.setSingleLine();
                    subtitle.setEllipsize(TextUtils.TruncateAt.END);
                    LinearLayout.LayoutParams subtitleParams = matchWrap();
                    subtitleParams.topMargin = dp(4);
                    tile.addView(subtitle, subtitleParams);
                }
                TintedSurface surface = new TintedSurface(tile, position % 4);
                holder.surfaces.add(surface);
                surface.apply(AppearancePreferences.dark(getContext()));
                tile.setContentDescription("探索" + item.title);
                tile.setOnClickListener(v -> open(holder, item));
                row.addView(tile, params);
            }
            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.bottomMargin = dp(9);
            holder.content.addView(row, rowParams);
        }
    }

    private FrameLayout cover(SectionView holder, String url, int radius) {
        FrameLayout art = Artwork.frame(getContext(), radius);
        if (url != null && !url.isEmpty()) holder.covers.add(new CoverRequest(art, url, "hero".equals(holder.section.layout)));
        return art;
    }

    private void open(SectionView holder, DiscoveryContent.Item item) {
        if (listener != null) listener.onOpen(holder.section, currentItem(holder, item));
    }

    private DiscoveryContent.Item currentItem(SectionView holder, DiscoveryContent.Item original) {
        for (DiscoveryContent.Item item : holder.section.items) {
            if (original.id.equals(item.id) && original.kind.equals(item.kind)) return item;
        }
        return original;
    }

    private int songRowHeight() {
        float fontScale = getResources().getConfiguration().fontScale;
        return dp(56 * Math.max(1f, Math.min(fontScale, 1.6f)));
    }

    private static String contentKey(DiscoveryContent.Section section) {
        StringBuilder key = new StringBuilder(section.layout);
        int count = Math.min("hero".equals(section.layout) ? 6 : 12, section.items.size());
        for (int position = 0; position < count; position++) {
            DiscoveryContent.Item item = section.items.get(position);
            key.append('\u0001').append(item.kind).append('\u0002').append(item.id)
                    .append('\u0002').append(item.title).append('\u0002').append(item.subtitle)
                    .append('\u0002').append(item.cover);
        }
        return key.toString();
    }

    private static final class CoverRequest {
        final FrameLayout cover;
        final String url;
        final boolean large;
        boolean started;
        CoverRequest(FrameLayout cover, String url, boolean large) { this.cover = cover; this.url = url; this.large = large; }
    }

    private final class TintedSurface {
        final View view;
        final int tone;
        TintedSurface(View view, int tone) {
            this.view = view; this.tone = tone;
            AppearanceStyler.preserveBackground(view);
        }
        void apply(boolean dark) {
            int[] light = {0x168D789C, 0x147D9FA1, 0x16C39576, 0x148991BE};
            int[] night = {0x258E7FA4, 0x23729394, 0x25AD8B75, 0x237B8AA5};
            view.setBackground(QingyinUi.touch(getContext(), dark ? night[tone] : light[tone], 13));
        }
    }

    /** Horizontal shelves leave vertical gestures to the existing page scroll view. */
    private final class ShelfRail extends HorizontalScrollView {
        static final int CARD = 0, HERO = 1, SONGS = 2;
        final LinearLayout strip;
        final ArrayList<Tile> tiles = new ArrayList<>();
        final int touchSlop;
        float downX, downY;
        int direction;

        ShelfRail() {
            super(DiscoveryFeed.this.getContext());
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(OVER_SCROLL_NEVER);
            setClipToPadding(false);
            setFocusable(false);
            setFillViewport(false);
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            strip = new LinearLayout(getContext());
            strip.setOrientation(HORIZONTAL);
            strip.setGravity(Gravity.TOP);
            addView(strip, new HorizontalScrollView.LayoutParams(-2, -2));
        }

        void addTile(View tile, FrameLayout art, int kind) {
            Tile entry = new Tile(tile, art, kind);
            tiles.add(entry);
            strip.addView(tile, new LinearLayout.LayoutParams(tileWidth(kind), -2));
            if (tiles.size() > 1) resizeTile(tiles.get(tiles.size() - 2), tiles.size() - 2);
            resizeTile(entry, tiles.size() - 1);
        }

        private int viewport() {
            return getWidth() > 0 ? getWidth() : Math.max(dp(220), getResources().getDisplayMetrics().widthPixels - dp(38));
        }

        private int tileWidth(int kind) {
            int viewport = viewport();
            if (kind == HERO) return Math.max(dp(230), Math.min(dp(480), viewport - dp(20)));
            if (kind == SONGS) return Math.max(dp(225), viewport - dp(15));
            return Math.max(dp(110), Math.min(dp(220), Math.round((viewport - dp(12)) / 2.12f)));
        }

        private void resizeTile(Tile tile, int position) {
            int width = tileWidth(tile.kind);
            LinearLayout.LayoutParams tileParams = (LinearLayout.LayoutParams) tile.view.getLayoutParams();
            int height = tile.kind == HERO ? Math.max(dp(180), Math.round(width * .67f)) : -2;
            int gap = position == tiles.size() - 1 ? 0 : dp(tile.kind == SONGS ? 16 : 12);
            if (tileParams.width != width || tileParams.height != height || tileParams.rightMargin != gap) {
                tileParams.width = width; tileParams.height = height; tileParams.rightMargin = gap;
                tile.view.setLayoutParams(tileParams);
            }
            if (tile.kind == CARD && tile.art != null) {
                ViewGroup.LayoutParams artParams = tile.art.getLayoutParams();
                if (artParams.height != width) { artParams.height = width; tile.art.setLayoutParams(artParams); }
            }
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width != oldWidth) for (int i = 0; i < tiles.size(); i++) resizeTile(tiles.get(i), i);
            scheduleCoverCheck();
        }

        @Override protected void onScrollChanged(int x, int y, int oldX, int oldY) {
            super.onScrollChanged(x, y, oldX, oldY);
            scheduleCoverCheck();
        }

        private void resolveGesture(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX(); downY = event.getY(); direction = 0;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = Math.abs(event.getX() - downX), dy = Math.abs(event.getY() - downY);
                    if (direction == 0 && Math.max(dx, dy) > touchSlop) direction = dx > dy ? 1 : 2;
                    if (getParent() != null && direction != 0) getParent().requestDisallowInterceptTouchEvent(direction == 1);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
        }

        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            resolveGesture(event);
            return direction != 2 && super.onInterceptTouchEvent(event);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            resolveGesture(event);
            return direction != 2 && super.onTouchEvent(event);
        }

        final class Tile {
            final View view;
            final FrameLayout art;
            final int kind;
            Tile(View view, FrameLayout art, int kind) { this.view = view; this.art = art; this.kind = kind; }
        }
    }

    private int dp(float value) { return QingyinUi.dp(getContext(), value); }
    private TextView text(String title, int size, int color, boolean bold) {
        return QingyinUi.text(getContext(), title, size, color, bold);
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
}
