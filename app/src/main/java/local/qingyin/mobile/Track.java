package local.qingyin.mobile;

import android.net.Uri;
import java.util.Locale;

/* JADX INFO: loaded from: classes2.dex */
public final class Track {
    public final String artist;
    public final int bitrate;
    public final long durationMs;
    public final long id;
    private volatile int measuredBitrate;
    public final String mime;
    private volatile int sampleRateHz;
    public final String title;
    public final String uri;
    private volatile String onlineSpec = "";
    private volatile String actualAudioFormat = "";
    private volatile SourceQuality sourceQuality = new SourceQuality(false, "", 0, false);
    public String coverUrl = "";
    public String albumTitle = "";
    public long artistId = 0;
    public long addedAt = 0;

    public Track(long id, String title, String artist, String mime, long durationMs, Uri uri, int bitrate) {
        this.id = id;
        this.title = (title == null || title.isEmpty()) ? "未知歌曲" : title;
        this.artist = (artist == null || artist.isEmpty() || "<unknown>".equals(artist)) ? "未知歌手" : artist;
        this.mime = mime != null ? mime : "";
        this.durationMs = Math.max(0L, durationMs);
        this.uri = uri.toString();
        this.bitrate = bitrate;
    }

    public void updateSourceFormat(int measuredBitrate, int sampleRateHz) {
        if (measuredBitrate > 0) {
            this.measuredBitrate = measuredBitrate;
        }
        if (sampleRateHz > 0) {
            this.sampleRateHz = sampleRateHz;
        }
    }

    public boolean isOnline() {
        return this.uri.startsWith("qingyin-online://track/");
    }

    public boolean isRadio() {
        return "audio/radio".equals(this.mime);
    }

    public void setOnlineSpec(String text) {
        this.onlineSpec = text == null ? "" : text;
    }

    public void setActualAudioFormat(String value) {
        this.actualAudioFormat = value == null ? "" : value;
    }

    /** The current stream's verified properties, independent of preferred quality or labels. */
    public void setCurrentSourceQuality(boolean verified, String format, double bitrateKbps, boolean preview) {
        this.sourceQuality = new SourceQuality(verified, format, bitrateKbps, preview);
    }

    private static final class SourceQuality {
        final boolean verified, preview;
        final String format;
        final double bitrateKbps;

        SourceQuality(boolean verified, String format, double bitrateKbps, boolean preview) {
            this.verified = verified;
            this.preview = preview;
            this.format = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
            this.bitrateKbps = Double.isFinite(bitrateKbps) && bitrateKbps > 0 ? bitrateKbps : 0;
        }
    }

    private static boolean losslessFormat(String format) {
        return "flac".equals(format) || "wav".equals(format) || "wave".equals(format)
                || "alac".equals(format) || "aiff".equals(format) || "aif".equals(format)
                || "pcm".equals(format);
    }

    private static String lossyQualityLabel(double bitrateKbps) {
        if (bitrateKbps <= 0) return "音质待确认";
        return bitrateKbps >= 256 ? "高品音质" : "标准音质";
    }

    /** Compact quality of the actual source; detailed format labels remain available below. */
    public String currentQualityLabel() {
        if (isRadio()) return "直播电台";
        if (isOnline()) {
            SourceQuality quality = this.sourceQuality;
            if (!quality.verified) return "音质待确认";
            if (quality.preview) return "试听音质";
            if (losslessFormat(quality.format)) return "无损音质";
            return lossyQualityLabel(quality.bitrateKbps);
        }
        String type = this.mime.toLowerCase(Locale.ROOT);
        if (type.contains("flac") || type.contains("wav") || type.contains("alac")
                || type.contains("aiff") || type.contains("pcm") || "audio/l16".equals(type)
                || "audio/l24".equals(type)) return "无损音质";
        int bps = this.measuredBitrate > 0 ? this.measuredBitrate : this.bitrate;
        return lossyQualityLabel(bps / 1000.0);
    }

    public String actualAudioFormat() {
        if (isRadio()) {
            return "LIVE · 电台音源";
        }
        if (isOnline()) {
            return this.actualAudioFormat.isEmpty() ? "音质待确认" : this.actualAudioFormat;
        }
        return qualityLabel();
    }

    public String qualityLabel() {
        String format;
        if (isRadio()) {
            return "直播中 · 音质由电台提供";
        }
        if (isOnline()) {
            return this.onlineSpec.isEmpty() ? "在线音质 · 尚未核验" : this.onlineSpec;
        }
        String type = this.mime.toLowerCase(Locale.ROOT);
        if (type.contains("flac")) {
            format = "FLAC";
        } else if (type.contains("mpeg")) {
            format = "MP3";
        } else if (type.contains("mp4")) {
            format = "M4A";
        } else if (type.contains("ogg")) {
            format = "OGG";
        } else {
            format = type.contains("wav") ? "WAV" : "本地音频";
        }
        int bps = this.measuredBitrate > 0 ? this.measuredBitrate : this.bitrate;
        String specs = bps > 0 ? " · 源文件约 " + Math.round(bps / 1000.0f) + " kbps" : " · 原文件";
        if (this.sampleRateHz > 0) {
            specs = specs + " · " + String.format(Locale.ROOT, "%.1f kHz", Float.valueOf(this.sampleRateHz / 1000.0f));
        }
        return format + specs;
    }
}
