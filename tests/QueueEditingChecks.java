package local.qingyin.mobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Queue choices are checked by song identity, independent of the selected mode. */
public final class QueueEditingChecks {
    private static void require(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }
    private static QueueNavigator queue(QueueNavigator.Mode mode, int count, int selected, int seed) {
        QueueNavigator subject = new QueueNavigator(new Random(seed));
        subject.setMode(mode); subject.load(count, selected); return subject;
    }
    private static List<String> names(QueueNavigator subject, List<String> songs) {
        List<String> result = new ArrayList<>();
        for (int position : subject.upcoming(1000)) result.add(songs.get(position));
        return result;
    }
    private static void valid(QueueNavigator subject) {
        List<Integer> shown = subject.upcoming(subject.size());
        require(!shown.isEmpty() && shown.get(0) == subject.index(), "current song must lead the displayed order");
        require(new HashSet<>(shown).size() == shown.size(), "displayed queue repeats a slot");
        for (int position : shown) require(position >= 0 && position < subject.size(), "displayed slot outside edited queue");
    }

    public static void main(String[] ignored) {
        for (QueueNavigator.Mode mode : QueueNavigator.Mode.values()) {
            QueueNavigator subject = queue(mode, 3, 0, 3);
            List<String> songs = new ArrayList<>(Arrays.asList("current", "B", "C"));
            songs.add(1, "first choice"); subject.insertQueued(1, true);
            songs.add(1, "latest choice"); subject.insertQueued(1, true);
            require(songs.get(subject.index()).equals("current"), "queuing interrupted current song under " + mode);
            require(names(subject, songs).subList(0, 3).equals(Arrays.asList("current", "latest choice", "first choice")), "displayed priority differs from actual intention");
            require(songs.get(subject.step(1, true)).equals("latest choice"), "latest next choice ignored under " + mode);
            require(songs.get(subject.step(1, true)).equals("first choice"), "earlier next choice was lost under " + mode);
            require(subject.mode() == mode, "insertion changed mode");
            if (mode == QueueNavigator.Mode.ONE)
                require(songs.get(subject.step(1, true)).equals("first choice"), "single-repeat did not resume after explicit choices");
            valid(subject);
        }

        QueueNavigator shuffled = queue(QueueNavigator.Mode.SHUFFLE, 10, 6, 25);
        List<String> songs = new ArrayList<>();
        for (int i = 0; i < 10; i++) songs.add("song-" + i);
        shuffled.step(1, true); shuffled.step(1, true); // Edits in the middle of an existing shuffle cycle.
        String current = songs.get(shuffled.index());
        List<String> originalOrder = names(shuffled, songs);
        int insertion = shuffled.index() + 1;
        songs.add(insertion, "explicit next"); shuffled.insertQueued(insertion, true);
        require(songs.get(shuffled.index()).equals(current), "shuffle insertion selected new audio");
        require(songs.get(shuffled.step(1, true)).equals("explicit next"), "shuffle skipped priority");
        require(songs.get(shuffled.step(-1, false)).equals(current), "shuffle previous lost real playback history");
        require(songs.get(shuffled.step(1, false)).equals("explicit next"), "shuffle forward history changed");
        List<String> expected = new ArrayList<>(originalOrder.subList(1, originalOrder.size()));
        List<String> remaining = new ArrayList<>(names(shuffled, songs)); remaining.remove(0);
        require(remaining.equals(expected), "explicit next reshuffled the unplayed songs");
        songs.add("appended"); shuffled.insertQueued(songs.size() - 1, false);
        remaining = new ArrayList<>(names(shuffled, songs)); remaining.remove(0);
        expected.add("appended");
        require(remaining.equals(expected), "append reordered existing random choices");

        QueueNavigator single = queue(QueueNavigator.Mode.ONE, 4, 2, 0);
        single.insertQueued(3, true);
        require(single.removeQueued(3), "pending next cannot be removed");
        require(single.step(1, true) == 2, "removed next still overrode single-repeat");
        require(!single.removeQueued(2) && single.index() == 2, "current item was removed");
        require(single.removeQueued(0) && single.index() == 1, "removal before current lost its index");
        require(single.keepCurrentOnly() == 2 && single.index() == 0 && single.size() == 1,
            "clear upcoming did not keep exactly the current song");
        require(single.step(1, true) == 0 && single.mode() == QueueNavigator.Mode.ONE, "clear changed current playback mode");

        QueueNavigator ordered = queue(QueueNavigator.Mode.ORDER, 3, 2, 0);
        ordered.insertQueued(3, true);
        require(ordered.step(1, true) == 3 && ordered.step(1, true) == -1, "ordered queue did not stop after added next");

        int trials = 0;
        for (QueueNavigator.Mode mode : QueueNavigator.Mode.values()) for (int seed = 0; seed < 80; seed++) {
            Random edits = new Random(seed);
            QueueNavigator subject = queue(mode, 12, 5, seed);
            for (int action = 0; action < 24; action++) {
                int before = subject.index();
                if (edits.nextBoolean() || subject.size() < 2) {
                    int at = edits.nextInt(subject.size() + 1);
                    subject.insertQueued(at, edits.nextBoolean());
                    require(subject.index() == (before >= at ? before + 1 : before), "insert changed current identity");
                } else {
                    int at = edits.nextInt(subject.size());
                    if (at == before) continue;
                    require(subject.removeQueued(at), "unplayed slot removal failed");
                    require(subject.index() == (before > at ? before - 1 : before), "remove changed current identity");
                }
                require(subject.mode() == mode, "queue edits changed mode"); valid(subject);
            }
            // A queue edited while priorities exist must still stop after a bounded, unique recovery pass.
            subject.resetFailurePass(); Set<Integer> failed = new HashSet<>();
            for (int attempt = 0; ; attempt++) {
                require(attempt < subject.size(), "edited queue failure recovery looped");
                require(failed.add(subject.index()), "edited recovery retried a failed song");
                if (subject.nextAfterFailure() < 0) break;
            }
            require(failed.size() == subject.failureCount(), "edited failure count differs from attempted songs");
            trials++;
        }

        QueueNavigator recovering = queue(QueueNavigator.Mode.LOOP, 3, 0, 1);
        require(recovering.nextAfterFailure() == 1 && recovering.failureCount() == 1, "initial source failure");
        recovering.insertQueued(2, true);
        require(recovering.nextAfterFailure() == 2 && recovering.failureCount() == 2, "source failure ignored newly queued next");
        require(recovering.removeQueued(0) && recovering.failureCount() == 1, "deleted failure was not remapped");
        require(recovering.nextAfterFailure() == 2 && recovering.nextAfterFailure() == -1,
            "recovery after queue changes did not remain bounded");
        System.out.println("QUEUE_EDITING_TESTS_PASS " + trials + " edited recovery queues; explicit-next/ONE/SHUFFLE/history/append/remove/clear");
    }
}
