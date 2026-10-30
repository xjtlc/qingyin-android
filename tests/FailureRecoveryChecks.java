package local.qingyin.mobile;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Observable queue recovery behavior; runnable without Android or a test framework. */
public final class FailureRecoveryChecks {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static QueueNavigator queue(int count, int selected, QueueNavigator.Mode mode, long seed) {
        QueueNavigator navigator = new QueueNavigator(new Random(seed));
        navigator.setMode(mode);
        navigator.load(count, selected);
        return navigator;
    }
    private static void exhaust(QueueNavigator navigator, int expected) {
        Set<Integer> attempted = new HashSet<>();
        QueueNavigator.Mode originalMode = navigator.mode();
        int count = 0;
        while (true) {
            int failed = navigator.index();
            require(attempted.add(failed), "a failed slot was retried within one pass");
            count++;
            int next = navigator.nextAfterFailure();
            require(navigator.mode() == originalMode, "recovery changed the user's playback mode");
            require(navigator.failureCount() == count, "failure count does not match actual failed songs");
            if (next < 0) {
                require(count == expected, "stopped before checking all eligible songs: " + count + "/" + expected);
                require(navigator.index() == failed, "terminal error discarded the current song");
                require(navigator.nextAfterFailure() == -1, "exhaustion re-entered a failed queue");
                require(navigator.failureCount() == count, "terminal callbacks inflated failure count");
                break;
            }
            require(next != failed, "recovery repeated the failed song");
            require(count < expected, "recovery exceeded its bounded pass");
        }
    }
    public static void main(String[] ignored) {
        QueueNavigator empty = queue(0, 0, QueueNavigator.Mode.LOOP, 0);
        require(empty.nextAfterFailure() == -1 && empty.failureCount() == 0, "empty queue recovery");
        for (QueueNavigator.Mode mode : QueueNavigator.Mode.values())
            exhaust(queue(1, 0, mode, 2), 1);

        QueueNavigator one = queue(3, 0, QueueNavigator.Mode.ONE, 0);
        require(one.step(1, true) == 0, "normal single-repeat completion changed");
        require(one.nextAfterFailure() == 1, "single-repeat did not leave an unplayable song");
        require(one.step(1, true) == 1, "single-repeat mode was lost after recovery");

        QueueNavigator ordered = queue(5, 3, QueueNavigator.Mode.ORDER, 0);
        require(ordered.nextAfterFailure() == 4, "ordered failure did not select the next song");
        require(ordered.nextAfterFailure() == -1 && ordered.index() == 4, "ordered failure wrapped/repeated its end");

        QueueNavigator recovered = queue(3, 0, QueueNavigator.Mode.LOOP, 0);
        require(recovered.nextAfterFailure() == 1 && recovered.failureCount() == 1, "first failure");
        recovered.resetFailurePass(); // Called by service only after real playback progress or completion.
        require(recovered.failureCount() == 0, "successful playback did not reset recovery");
        exhaust(recovered, 3);
        recovered.choose(0); // An explicit user selection permits a fresh attempt.
        require(recovered.failureCount() == 0 && recovered.nextAfterFailure() == 1, "manual retry inherited exhaustion");
        recovered.load(2, 1);
        require(recovered.failureCount() == 0 && recovered.nextAfterFailure() == 0, "new queue inherited exhaustion");

        int passes = 0;
        for (int size = 1; size <= 48; size++) {
            for (int seed = 0; seed < 24; seed++) {
                int selected = seed % size;
                for (QueueNavigator.Mode mode : QueueNavigator.Mode.values()) {
                    exhaust(queue(size, selected, mode, seed), mode == QueueNavigator.Mode.ORDER ? size - selected : size);
                    passes++;
                }
                // Begin failing near an existing shuffled cycle's end, where
                // a bounded N calls to ordinary step() can otherwise miss slots.
                QueueNavigator lateShuffle = queue(size, selected, QueueNavigator.Mode.SHUFFLE, seed);
                for (int before = 0; before < Math.max(0, size - 2); before++) lateShuffle.step(1, true);
                exhaust(lateShuffle, size);
                passes++;
            }
        }
        System.out.println("FAILURE_RECOVERY_TESTS_PASS " + passes +
            " recovery passes; 1-48 songs; ONE/ORDER/LOOP/SHUFFLE; late shuffle cycles; bounded exhaustion; success/manual/new-queue reset");
    }
}
