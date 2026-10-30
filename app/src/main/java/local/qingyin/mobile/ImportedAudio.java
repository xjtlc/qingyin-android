package local.qingyin.mobile;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** 0.7.19 document import, with decompiled resource scopes restored. */
public final class ImportedAudio {
    private ImportedAudio() { }

    public static Track inspect(Context context, Uri uri) {
        if (uri == null || !"content".equalsIgnoreCase(uri.getScheme())) return null;
        try {
            try (ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, "r")) {
                if (fd == null) return null;
            }
            String display = "导入音频";
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) display = cursor.getString(0);
            } catch (Exception ignored) { }
            String title = display.replaceAll("(?i)\\.(flac|wav|wave|aiff|aif|alac|mp3|m4a|aac|ogg|opus|wma)$", "");
            String artist = "未知歌手";
            String mime = context.getContentResolver().getType(uri);
            if (mime == null || mime.isEmpty() || mime.equals("application/octet-stream")) {
                String ext = display.toLowerCase(Locale.ROOT);
                mime = ext.endsWith(".flac") ? "audio/flac" : ext.endsWith(".wav") ? "audio/wav"
                    : ext.endsWith(".mp3") ? "audio/mpeg" : ext.endsWith(".ogg") ? "audio/ogg"
                    : ext.endsWith(".m4a") || ext.endsWith(".alac") ? "audio/mp4" : "audio/*";
            }
            int bitrate = 0, sampleRate = 0;
            long duration = 0;
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            try {
                metadata.setDataSource(context, uri);
                String rawDuration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (rawDuration != null) duration = Math.max(0, Long.parseLong(rawDuration));
                String songTitle = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String author = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (songTitle != null && !songTitle.trim().isEmpty()) title = songTitle;
                if (author != null && !author.trim().isEmpty()) artist = author;
                bitrate = positiveInt(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
                if (Build.VERSION.SDK_INT >= 29)
                    sampleRate = positiveInt(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE));
            } catch (Exception ignored) {
            } finally {
                try { metadata.release(); } catch (Exception ignored) { }
            }
            long hash = -3750763034362895579L;
            for (byte ch : uri.toString().getBytes(StandardCharsets.UTF_8)) hash = 1099511628211L * (hash ^ (ch & 255));
            Track result = new Track(hash | Long.MIN_VALUE, title, artist, mime, duration, uri, 0);
            result.updateSourceFormat(bitrate, sampleRate);
            return result;
        } catch (Exception ignored) { return null; }
    }

    private static int positiveInt(String raw) {
        try { return Math.max(0, Integer.parseInt(raw)); }
        catch (Exception ignored) { return 0; }
    }
}
