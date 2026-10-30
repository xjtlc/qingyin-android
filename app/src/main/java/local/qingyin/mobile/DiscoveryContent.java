package local.qingyin.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

/** Display metadata only. No signed stream URLs, sessions or account secrets. */
final class DiscoveryContent {
    private DiscoveryContent(){}
    static final class Item {
        final String kind,id,title,subtitle,cover;
        final JSONObject data;
        final Track song;
        Item(String kind,String id,String title,String subtitle,String cover,JSONObject data,Track song){
            this.kind=clean(kind);this.id=clean(id);this.title=clean(title);this.subtitle=clean(subtitle);this.cover=clean(cover);
            this.data=data==null?new JSONObject():data;this.song=song;
        }
    }
    static final class Section {
        final String key,title,caption,layout,error;
        final List<Item> items;
        final Item more;
        final boolean loading,stale;
        final long fetchedAt;
        Section(String key,String title,String caption,String layout,List<Item> items,Item more,boolean loading,String error,long fetchedAt,boolean stale){
            this.key=clean(key);this.title=clean(title);this.caption=clean(caption);this.layout=clean(layout);
            this.items=Collections.unmodifiableList(new ArrayList<>(items==null?Collections.emptyList():items));
            this.more=more;this.loading=loading;this.error=clean(error);this.fetchedAt=fetchedAt;this.stale=stale;
        }
        static Section loading(String key,String title,String layout){return new Section(key,title,"",layout,null,null,true,"",0,false);}
    }
    private static String clean(String value){return value==null?"":value.trim();}
}
