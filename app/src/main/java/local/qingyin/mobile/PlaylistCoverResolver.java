package local.qingyin.mobile;

import android.content.Context;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
final class PlaylistCoverResolver {
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private PlaylistCoverResolver() {
    }

    static boolean missing(JSONObject list) {
        return list != null && list.optString("cover", "").trim().isEmpty();
    }

    static boolean needsResolution(Context context, JSONObject list) {
        if (list == null) {
            return false;
        }
        if (PlaylistSnapshot.important(list)) {
            if (list.optInt("count", 0) > 0) {
                return PlaylistSnapshot.cover(context, list).isEmpty() || PlaylistSnapshot.coverCountChanged(context, list);
            }
            return false;
        }
        return missing(list);
    }

    static String preferred(Context context, JSONObject list) {
        String stored = PlaylistSnapshot.cover(context, list);
        return stored.isEmpty() ? list.optString("cover", "") : stored;
    }

    /** Recover the most recently added cover with at most two bounded page requests. */
    static String resolve(Context context, JSONObject list) {
        if(list==null)return "";
        String stored=PlaylistSnapshot.cover(context,list);
        if(!needsResolution(context,list))return stored.isEmpty()?list.optString("cover",""):stored;
        String listId=list.optString("listId",""),globalId=list.optString("globalId",""),id=list.optString("id","");
        if(id.isEmpty()&&listId.isEmpty()&&globalId.isEmpty())return "";
        String key=listId.isEmpty()?"public:"+(globalId.isEmpty()?id:globalId):"private:"+listId;
        String cached=CACHE.get(key);
        if(cached!=null&&!PlaylistSnapshot.coverCountChanged(context,list))return cached;
        int count=Math.max(0,list.optInt("count",0));
        int last=(int)Math.max(1L,((long)count+99L)/100L);
        long newest=0;
        String result="";
        int[] pages=last==1?new int[]{1}:new int[]{last,1};
        for(int page:pages){
            try{
                JSONObject params=new JSONObject().put("id",id).put("listId",listId).put("globalId",globalId)
                    .put("type",list.optInt("type",0)).put("page",page).put("pageSize",100);
                JSONArray entries=OnlineApi.call(context,"playlistSongs",params).optJSONArray("songs");
                if(entries==null)continue;
                for(int i=0;i<entries.length();i++){
                    JSONObject song=entries.optJSONObject(i);
                    if(song==null)continue;
                    String art=song.optString("cover","");
                    if(!art.startsWith("https://"))continue;
                    long added=song.optLong("addedAt",0);
                    if(result.isEmpty()||(added>0&&added>=newest)||(newest==0&&page==last)){
                        result=art;newest=added;
                    }
                }
                if(!result.isEmpty()&&newest==0&&page==last)break;
            }catch(Exception unavailable){ /* Try the other boundary page, then use existing artwork. */ }
        }
        if(!result.isEmpty()){
            CACHE.put(key,result);PlaylistSnapshot.rememberCover(context,list,result);return result;
        }
        return list.optString("cover","");
    }
}
