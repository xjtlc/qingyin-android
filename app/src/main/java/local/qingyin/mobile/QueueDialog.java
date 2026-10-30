package local.qingyin.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;

/** One live queue panel shared by local, online and now-playing screens. */
final class QueueDialog {
    private QueueDialog() {}
    static void show(Activity host, PlaybackService player) {
        if (player == null || host.isFinishing()) return;
        new Panel(host, player).show();
    }
    private static final class Panel {
        final Activity host;
        final PlaybackService player;
        final Handler ui = new Handler(Looper.getMainLooper());
        final QingyinUi.Sheet sheet;
        final TextView summary;
        final ListView list;
        final Rows adapter = new Rows();
        final IdentityHashMap<Track,CharSequence> normalLabels=new IdentityHashMap<>();
        final IdentityHashMap<Track,CharSequence> selectedLabels=new IdentityHashMap<>();
        List<Track> entries = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        Track current;
        boolean open;
        int shownCount=-1,shownMode=-1;
        final Runnable tick = new Runnable() {
            @Override public void run() {
                if (!open || host.isDestroyed()) return;
                refresh(); ui.postDelayed(this, 700);
            }
        };
        Panel(Activity host, PlaybackService player) {
            this.host = host; this.player = player;
            sheet = new QingyinUi.Sheet(host, "当前播放", "", false);
            sheet.setBackdropRefreshInterval(500);
            LinearLayout tools = new LinearLayout(host);
            tools.setGravity(Gravity.CENTER_VERTICAL);
            summary = text("", 13, QingyinUi.MUTED);
            summary.setContentDescription("切换播放模式");
            summary.setOnClickListener(v -> { player.cycleMode(); refresh(); });
            summary.setPadding(dp(12),0,dp(12),0);
            summary.setBackground(QingyinUi.touch(host,sheet.dark?0x18FFFFFF:0x70FFFFFF,18));
            tools.addView(summary, new LinearLayout.LayoutParams(-2, dp(36)));
            tools.addView(new View(host),new LinearLayout.LayoutParams(0,dp(44),1));
            tools.addView(tool("download","下载当前歌曲",()->{Track song=player.current();if(song!=null)PlayerSongActions.showDownload(host,song);}));
            tools.addView(tool("save","收藏当前歌曲",()->{Track song=player.current();if(song!=null)PlayerSongActions.showFavorite(host,song,null);}));
            tools.addView(tool("trash","清空待播",()->new android.app.AlertDialog.Builder(host).setTitle("清空待播歌曲？")
                .setMessage("当前歌曲会保留，其余待播歌曲将从队列中移除。")
                .setPositiveButton("清空待播",(d,w)->{
                int removed = player.clearUpcoming();
                refresh(); Toast.makeText(host, removed > 0 ? "已清空待播，当前歌曲已保留" : "没有待播歌曲", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("取消",null).show()));
            sheet.content.addView(tools,new LinearLayout.LayoutParams(-1,dp(54)));
            list = new ListView(host); list.setDivider(null); list.setAdapter(adapter);
            list.setNestedScrollingEnabled(false);
            list.setScrollingCacheEnabled(false);
            list.setOnScrollListener(new AbsListView.OnScrollListener(){
                @Override public void onScrollStateChanged(AbsListView view,int state){
                    UiPerformance.setAnimationActive(list,state!=SCROLL_STATE_IDLE);
                    sheet.setContentScrollBusy(state!=SCROLL_STATE_IDLE);
                    if(state==SCROLL_STATE_IDLE&&open)refresh();
                }
                @Override public void onScroll(AbsListView view,int first,int visible,int total){}
            });
            list.setOnItemClickListener((parent, row, position, id) -> {
                play(at(position));
            });
            sheet.setScrollableContent(list,dp(50));
            sheet.dialog.setOnDismissListener(dialog -> { open = false; ui.removeCallbacks(tick);UiPerformance.setAnimationActive(list,false);normalLabels.clear();selectedLabels.clear(); });
        }
        int dp(int n) { return QingyinUi.dp(host, n); }
        View tool(String icon,String label,Runnable action){
            FrameLayout button=new FrameLayout(host);button.setContentDescription(label);button.setTooltipText(label);
            button.addView(new ActionGlyph(host,icon,sheet.mutedColor()),new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.CENTER));
            button.setBackground(QingyinUi.touch(host,Color.TRANSPARENT,18));button.setOnClickListener(v->action.run());
            button.setLayoutParams(new LinearLayout.LayoutParams(dp(44),dp(44)));return button;
        }
        TextView text(String value, int sp, int color) {
            if(color==QingyinUi.INK)color=sheet.inkColor();
            else if(color==QingyinUi.MUTED)color=sheet.mutedColor();
            else if(color==QingyinUi.BLUE)color=sheet.accentColor();
            TextView view = QingyinUi.text(host, value, sp, color, false);
            view.setGravity(Gravity.CENTER_VERTICAL); return view;
        }
        Track at(int position) {
            if (position < 0 || position >= order.size()) return null;
            int index = order.get(position);
            return index >= 0 && index < entries.size() ? entries.get(index) : null;
        }
        void play(Track chosen){int actual=player.queueSnapshot().indexOf(chosen);if(chosen==null||actual<0)return;host.startForegroundService(new android.content.Intent(host,PlaybackService.class));player.selectQueue(actual);refresh();}
        void refresh() {
            List<Track> next = player.queueSnapshot(); List<Integer> nextOrder = player.upcoming();
            Track selected = player.current();
            if (!entries.equals(next) || !order.equals(nextOrder) || selected != current) {
                boolean changedSong=selected!=current;
                boolean changedEntries=!entries.equals(next);
                if(changedEntries){normalLabels.clear();selectedLabels.clear();}
                entries = next; order = nextOrder; current = selected; adapter.notifyDataSetChanged();
                int height=Math.min(dp(390), Math.min(dp(Math.max(1, order.size()) * 50),
                    (int)(host.getResources().getDisplayMetrics().heightPixels * .46f)));
                if(list.getLayoutParams().height!=height){list.getLayoutParams().height=height;list.requestLayout();}
                if(changedSong)sheet.refreshBackdrop();
            }
            int mode=player.mode().ordinal();
            if(mode!=shownMode){
                shownMode=mode;String[] modes = {"列表循环", "单曲循环", "顺序播放", "随机播放"};
                summary.setText("↻  "+modes[mode]);
            }
            if(entries.size()!=shownCount){
                shownCount=entries.size();String heading="当前播放 "+shownCount;SpannableString title=new SpannableString(heading);
                title.setSpan(new RelativeSizeSpan(.65f),5,heading.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                title.setSpan(new SuperscriptSpan(),5,heading.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);sheet.setHeading(title);
            }
        }
        void show() { refresh(); open = true; sheet.show(); ui.post(tick); }
        void actions(Track song) {
            if (song == null) return;
            QingyinUi.Sheet actions = new QingyinUi.Sheet(host, song.title, song.artist, false);
            if (song != player.current()) {
                actions.addAction("↳", "下一首播放", "当前歌曲继续播放", () -> { player.playNext(song); refresh(); });
                actions.addAction("−", "从队列移除", "不影响当前歌曲", () -> {
                    int index = player.queueSnapshot().indexOf(song);
                    if (index >= 0) player.removeQueueItem(index);
                    refresh();
                });
            } else actions.addAction("info", "当前歌曲来源", "查看音源与当前音质", () -> source(song));
            actions.show();
        }
        void source(Track song){new android.app.AlertDialog.Builder(host).setTitle("歌曲来源")
            .setMessage(song.title+" · "+song.artist+"\n\n"+(song.isOnline()?"酷狗在线曲库":song.isRadio()?"电台直播":"本机音频")+"\n"+song.currentQualityLabel())
            .setPositiveButton("关闭",null).show();}
        final class Rows extends BaseAdapter {
            public int getCount() { return order.size(); }
            public Object getItem(int index) { return at(index); }
            public long getItemId(int index) { return index; }
            public View getView(int position, View reused, ViewGroup parent) {
                RowHolder holder;
                if(reused instanceof LinearLayout&&reused.getTag() instanceof RowHolder)holder=(RowHolder)reused.getTag();
                else holder=new RowHolder();
                Track song=at(position);if(song==null)return holder.row;
                boolean selected=song==current;
                if(holder.bound&&holder.song==song&&holder.selected==selected)return holder.row;
                holder.song=song;
                holder.mark.setVisibility(selected?View.VISIBLE:View.GONE);
                IdentityHashMap<Track,CharSequence> labels=selected?selectedLabels:normalLabels;
                CharSequence label=labels.get(song);
                if(label==null){
                    String value=song.title+" · "+song.artist;SpannableString words=new SpannableString(value);
                    words.setSpan(new ForegroundColorSpan(selected?sheet.accentColor():sheet.mutedColor()),song.title.length(),value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    label=words;labels.put(song,label);
                }
                holder.title.setText(label);holder.title.setTextColor(selected?sheet.accentColor():sheet.inkColor());
                holder.source.setVisibility(selected?View.VISIBLE:View.GONE);
                holder.remove.setVisibility(selected?View.INVISIBLE:View.VISIBLE);
                holder.remove.setContentDescription("从播放列表移除："+song.title);
                holder.row.setContentDescription((selected?"当前歌曲：":"播放：")+song.title+"，"+song.artist);
                if(!holder.bound||holder.selected!=selected){holder.row.setBackground(selected?holder.selectedBackground:holder.normalBackground);holder.selected=selected;holder.bound=true;}
                return holder.row;
            }
            final class RowHolder {
                final LinearLayout row;
                final View mark;
                final TextView title,source;
                final FrameLayout remove;
                final Drawable selectedBackground,normalBackground;
                Track song;boolean selected,bound;
                RowHolder(){
                    row = new LinearLayout(host); row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(3), 0, 0, 0);row.setMinimumHeight(dp(50));
                    mark=new ActionGlyph(host,"playing",sheet.accentColor());row.addView(mark,new LinearLayout.LayoutParams(dp(21),dp(44)));
                    title = text("", 15, QingyinUi.INK);
                    title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.END);
                    row.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));
                    source=text("来源",11,QingyinUi.MUTED);source.setGravity(Gravity.CENTER);
                    source.setBackground(QingyinUi.touch(host,sheet.dark?0x12FFFFFF:0x60FFFFFF,14));row.addView(source,new LinearLayout.LayoutParams(dp(42),dp(28)));
                    remove=new FrameLayout(host);remove.addView(new ActionGlyph(host,"close",sheet.mutedColor()),new FrameLayout.LayoutParams(dp(17),dp(17),Gravity.CENTER));
                    row.addView(remove,new LinearLayout.LayoutParams(dp(42),dp(48)));
                    selectedBackground=QingyinUi.touch(host,sheet.dark?0x18FFFFFF:0x12B62F54,10);
                    normalBackground=QingyinUi.touch(host,Color.TRANSPARENT,10);
                    source.setContentDescription("查看当前歌曲来源");source.setOnClickListener(v->{if(song!=null)Panel.this.source(song);});
                    remove.setOnClickListener(v->{if(song==null)return;int actual=player.queueSnapshot().indexOf(song);if(actual>=0)player.removeQueueItem(actual);refresh();});
                    row.setOnClickListener(v->play(song));
                    row.setOnLongClickListener(v->{actions(song);return true;});
                    row.setTag(this);
                }
            }
        }
    }
}
