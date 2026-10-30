package local.qingyin.mobile;

import android.content.Context;
import android.net.Uri;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

/** Phone-only library. Never stores signed CDN URLs or account tokens. */
final class OnlineLibraryStore {
    private OnlineLibraryStore(){}
    private static final String PREFS="qingyin_online_library";
    private static final String FAVORITES="favorites";
    private static final String RECENTS="recents";
    private static final String PLAYLISTS="saved_playlists";
    private static final int LIMIT=80;

    private static String key(Track t){return t==null?"":t.uri;}
    private static JSONArray read(Context context,String list){
        String raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(list,"[]");
        try{return new JSONArray(raw);}catch(Exception ignored){return new JSONArray();}
    }
    private static JSONObject encode(Track t){
        JSONObject row=new JSONObject();
        try{
            row.put("uri",t.uri);row.put("title",t.title);row.put("artist",t.artist);
            row.put("duration",t.durationMs);row.put("id",t.id);
            row.put("cover",t.coverUrl);
            row.put("album",t.albumTitle);
        }catch(Exception ignored){}
        return row;
    }
    private static void put(Context context,String list,JSONArray value){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putString(list,value.toString()).apply();
    }
    static boolean isFavorite(Context context,Track song){
        if(song==null||!song.isOnline())return false;
        JSONArray entries=read(context,FAVORITES);
        for(int i=0;i<entries.length();i++)
            if(key(song).equals(entries.optJSONObject(i)==null?"":entries.optJSONObject(i).optString("uri")))return true;
        return false;
    }
    static void toggle(Context context,Track song){
        if(song==null||!song.isOnline())return;
        boolean existing=isFavorite(context,song);
        JSONArray old=read(context,FAVORITES),out=new JSONArray();
        if(!existing)out.put(encode(song));
        for(int i=0;i<old.length()&&out.length()<LIMIT;i++){
            JSONObject row=old.optJSONObject(i);
            if(row!=null&&!key(song).equals(row.optString("uri")))out.put(row);
        }
        put(context,FAVORITES,out);
        // Keep main player's heart and the online heart in agreement.
        SharedPreferences shared=context.getSharedPreferences("library",Context.MODE_PRIVATE);
        Set<String> ids=new HashSet<>(shared.getStringSet("favorites",Collections.emptySet()));
        if(existing)ids.remove(Long.toString(song.id));else ids.add(Long.toString(song.id));
        shared.edit().putStringSet("favorites",ids).apply();
    }
    static void remember(Context context,Track song){
        if(song==null||!song.isOnline())return;
        JSONArray old=read(context,RECENTS),out=new JSONArray();
        out.put(encode(song));
        for(int i=0;i<old.length()&&out.length()<LIMIT;i++){
            JSONObject row=old.optJSONObject(i);
            if(row!=null&&!key(song).equals(row.optString("uri")))out.put(row);
        }
        put(context,RECENTS,out);
    }
    static ArrayList<Track> tracks(Context context,boolean favorites){
        JSONArray src=read(context,favorites?FAVORITES:RECENTS);
        ArrayList<Track> result=new ArrayList<>();
        for(int i=0;i<src.length();i++){
            JSONObject o=src.optJSONObject(i);if(o==null)continue;
            try{
                String uri=o.optString("uri","");
                if(!uri.startsWith("qingyin-online://track/"))continue;
                Track restored=new Track(o.optLong("id"),o.optString("title","在线歌曲"),
                    o.optString("artist","未知歌手"),"audio/online",
                    Math.max(0,o.optLong("duration")),Uri.parse(uri),0);
                restored.coverUrl=o.optString("cover","");
                restored.albumTitle=o.optString("album","");
                result.add(restored);
            }catch(Exception ignored){}
        }
        return result;
    }

    private static String playlistKey(JSONObject playlist){
        if(playlist==null)return "";
        for(String field:new String[]{"globalId","listId","id"}){
            String value=playlist.optString(field,"").trim();
            if(!value.isEmpty()&&!"null".equals(value))
                return field+":"+playlist.optInt("type",0)+":"+value;
        }
        return "";
    }
    /** Playlist collection is local metadata, independent of the provider account. */
    static boolean isPlaylistSaved(Context context,JSONObject playlist){
        String identity=playlistKey(playlist);
        if(identity.isEmpty())return false;
        JSONArray entries=read(context,PLAYLISTS);
        for(int i=0;i<entries.length();i++)
            if(identity.equals(playlistKey(entries.optJSONObject(i))))return true;
        return false;
    }
    static boolean togglePlaylist(Context context,JSONObject playlist){
        String identity=playlistKey(playlist);
        if(identity.isEmpty())return false;
        boolean saved=isPlaylistSaved(context,playlist);
        JSONArray previous=read(context,PLAYLISTS),next=new JSONArray();
        if(!saved){
            JSONObject metadata=new JSONObject();
            // Persist only identifiers and display metadata, never raw API responses.
            for(String field:new String[]{"id","globalId","listId","type","title","cover","count","creator"}){
                Object value=playlist.opt(field);
                if(value instanceof String||value instanceof Number||value instanceof Boolean
                        ||value==JSONObject.NULL){
                    try{metadata.put(field,value);}catch(Exception ignored){}
                }
            }
            next.put(metadata);
        }
        for(int i=0;i<previous.length()&&next.length()<LIMIT;i++){
            JSONObject metadata=previous.optJSONObject(i);
            String key=playlistKey(metadata);
            if(!key.isEmpty()&&!identity.equals(key))next.put(metadata);
        }
        put(context,PLAYLISTS,next);
        return !saved;
    }
    static ArrayList<JSONObject> savedPlaylists(Context context){
        JSONArray entries=read(context,PLAYLISTS);
        ArrayList<JSONObject> result=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(int i=0;i<entries.length()&&result.size()<LIMIT;i++){
            JSONObject metadata=entries.optJSONObject(i);
            String identity=playlistKey(metadata);
            if(!identity.isEmpty()&&seen.add(identity))result.add(metadata);
        }
        return result;
    }
}
