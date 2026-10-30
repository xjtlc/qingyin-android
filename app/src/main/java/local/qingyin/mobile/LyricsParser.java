package local.qingyin.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes2.dex */
final class LyricsParser {
    private static final Pattern TIME = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?\\]");
    private static final Pattern OFFSET = Pattern.compile("^\\[offset:([+-]?\\d+)\\]$", 2);

    static final class Line {
        final long millis;
        final String text;

        Line(long ms, String text) {
            this.millis = ms;
            this.text = text;
        }
    }

    private LyricsParser() {
    }

    static List<Line> parse(String raw) {
        int i;
        int i2;
        int i3;
        String part;
        Matcher times;
        long ms = 0;
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = raw.replace("\r", "").split("\n", -1);
        ArrayList<Line> out = new ArrayList<>();
        long offset = 0;
        int length = parts.length;
        int i4 = 0;
        int i5 = 0;
        while (true) {
            i = 1;
            if (i5 >= length) {
                break;
            }
            Matcher m = OFFSET.matcher(parts[i5].trim());
            if (m.matches()) {
                try {
                    offset = Long.parseLong(m.group(1));
                } catch (Exception e) {
                }
            }
            i5++;
        }
        int length2 = parts.length;
        int i6 = 0;
        while (i6 < length2) {
            String part2 = parts[i6];
            Matcher times2 = TIME.matcher(part2);
            ArrayList<Long> timestamps = new ArrayList<>();
            int end = 0;
            while (times2.find()) {
                try {
                    long mins = Long.parseLong(times2.group(i));
                    long secs = Long.parseLong(times2.group(2));
                    if (secs >= 60) {
                        i3 = i6;
                        part = part2;
                        times = times2;
                    } else {
                        String fraction = times2.group(3);
                        part = part2;
                        if (fraction == null) {
                            ms = 0;
                        } else {
                            try {
                                ms = Long.parseLong((fraction + "000").substring(i4, 3));
                            } catch (Exception e2) {
                                i3 = i6;
                                times = times2;
                            }
                        }
                        i3 = i6;
                        times = times2;
                        try {
                            timestamps.add(Long.valueOf(Math.max(0L, (60000 * mins) + (1000 * secs) + ms + offset)));
                            end = times.end();
                            i6 = i3;
                            part2 = part;
                            times2 = times;
                            i4 = 0;
                            i = 1;
                        } catch (Exception e3) {
                            i6 = i3;
                            part2 = part;
                            times2 = times;
                            i4 = 0;
                            i = 1;
                        }
                    }
                } catch (Exception e4) {
                    i3 = i6;
                    part = part2;
                    times = times2;
                }
                i6 = i3;
                part2 = part;
                times2 = times;
                i4 = 0;
                i = 1;
            }
            int i7 = i6;
            String part3 = part2;
            if (timestamps.isEmpty()) {
                i2 = 0;
            } else {
                String caption = part3.substring(Math.min(end, part3.length())).trim();
                if (caption.isEmpty()) {
                    i2 = 0;
                } else {
                    if (caption.length() > 260) {
                        i2 = 0;
                        caption = caption.substring(0, 260);
                    } else {
                        i2 = 0;
                    }
                    Iterator<Long> it = timestamps.iterator();
                    while (it.hasNext()) {
                        long time = it.next().longValue();
                        if (out.size() < 800) {
                            out.add(new Line(time, caption));
                        }
                    }
                }
            }
            i4 = i2;
            i = 1;
            i6 = i7 + 1;
        }
        out.sort(Comparator.comparingLong(new ToLongFunction() { // from class: local.qingyin.mobile.LyricsParser$$ExternalSyntheticLambda0
            @Override // java.util.function.ToLongFunction
            public final long applyAsLong(Object obj) {
                return ((LyricsParser.Line) obj).millis;
            }
        }));
        return out;
    }

    static int indexAt(List<Line> lines, long elapsed) {
        int lo = 0;
        int hi = lines.size() - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).millis <= elapsed) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found;
    }
}
