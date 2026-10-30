package local.qingyin.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

/** Pure-Java playback order logic; no Android state or file access. */
public final class QueueNavigator {
    public enum Mode { LOOP, ONE, ORDER, SHUFFLE }

    private final Random random;
    private Mode mode = Mode.LOOP;
    private int size;
    private int index = -1;
    private final ArrayList<Integer> shuffle = new ArrayList<>();
    private int shuffleCursor;
    private final Set<Integer> failedPass = new HashSet<>();
    private final ArrayList<Integer> priorityNext = new ArrayList<>();

    public QueueNavigator() { this(new Random()); }
    public QueueNavigator(Random random) { this.random = random; }

    public int index() { return index; }
    public Mode mode() { return mode; }
    public int size() { return size; }

    public void load(int count, int selected) {
        resetFailurePass();
        priorityNext.clear();
        size = Math.max(0, count);
        if (size == 0) { index = -1; shuffle.clear(); return; }
        if (selected < 0 || selected >= size) throw new IllegalArgumentException("invalid index");
        index = selected;
        rebuildShuffle();
    }

    public void setMode(Mode selected) {
        if (selected == null) throw new IllegalArgumentException("mode");
        if (mode == selected) return;
        mode = selected;
        if (mode == Mode.SHUFFLE) rebuildShuffle();
    }

    public void choose(int selected) {
        if (selected < 0 || selected >= size) throw new IllegalArgumentException("invalid index");
        index = selected;
        resetFailurePass();
        priorityNext.clear();
        if (mode == Mode.SHUFFLE) rebuildShuffle();
    }

    /** Insert a queued slot without selecting it or rebuilding the shuffled order. */
    public void insertQueued(int position, boolean next) {
        if (position < 0 || position > size) throw new IllegalArgumentException("invalid insert position");
        if (size == 0) { load(1, 0); return; }
        shiftInserted(shuffle, position);
        shiftInserted(priorityNext, position);
        Set<Integer> remapped = new HashSet<>();
        for (int failed : failedPass) remapped.add(failed >= position ? failed + 1 : failed);
        failedPass.clear(); failedPass.addAll(remapped);
        if (index >= position) index++;
        size++;
        shuffle.add(position); // Ordinary additions join the end of the unplayed permutation.
        if (next) priorityNext.add(0, position);
    }

    /** Current audio is protected; deleting another slot only remaps its indexes. */
    public boolean removeQueued(int position) {
        if (position < 0 || position >= size || position == index) return false;
        int shuffledPosition = shuffle.indexOf(position);
        if (shuffledPosition >= 0) {
            shuffle.remove(shuffledPosition);
            if (shuffledPosition <= shuffleCursor) shuffleCursor--;
        }
        shiftRemoved(shuffle, position);
        priorityNext.remove(Integer.valueOf(position));
        shiftRemoved(priorityNext, position);
        Set<Integer> remapped = new HashSet<>();
        for (int failed : failedPass)
            if (failed != position) remapped.add(failed > position ? failed - 1 : failed);
        failedPass.clear(); failedPass.addAll(remapped);
        if (index > position) index--;
        size--;
        return true;
    }

    public int keepCurrentOnly() {
        if (index < 0 || size == 0) return 0;
        int removed = size - 1;
        load(1, 0);
        return removed;
    }

    private static void shiftInserted(List<Integer> slots, int position) {
        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i) >= position) slots.set(i, slots.get(i) + 1);
    }
    private static void shiftRemoved(List<Integer> slots, int position) {
        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i) > position) slots.set(i, slots.get(i) - 1);
    }

    /** Explicit next choices take precedence over single-repeat and random playback. */
    private int takePriority(boolean recovering) {
        while (!priorityNext.isEmpty()) {
            int candidate = priorityNext.remove(0);
            if (candidate < 0 || candidate >= size || candidate == index ||
                (recovering && failedPass.contains(candidate))) continue;
            if (mode == Mode.SHUFFLE) {
                int previousPosition = shuffle.indexOf(candidate);
                if (previousPosition >= 0) {
                    shuffle.remove(previousPosition);
                    if (previousPosition <= shuffleCursor) shuffleCursor--;
                }
                shuffleCursor = Math.max(0, Math.min(shuffleCursor + 1, shuffle.size()));
                shuffle.add(shuffleCursor, candidate);
            }
            return index = candidate;
        }
        return -1;
    }

    /** Returns -1 if ordered playback has reached its end. */
    public int step(int direction, boolean automatic) {
        if (size == 0 || index < 0) return -1;
        if (direction != -1 && direction != 1) throw new IllegalArgumentException("direction");
        if (direction == 1) {
            int priority = takePriority(false);
            if (priority >= 0) return priority;
        }
        if (automatic && direction == 1 && mode == Mode.ONE) return index;
        if (mode == Mode.SHUFFLE) {
            if (direction < 0) {
                if (shuffleCursor > 0) index = shuffle.get(--shuffleCursor);
                return index;
            }
            if (shuffle.size() == 1) return index;
            if (shuffleCursor + 1 >= shuffle.size()) {
                rebuildShuffle();
                return index = shuffle.get(++shuffleCursor);
            }
            return index = shuffle.get(++shuffleCursor);
        }
        int requested = index + direction;
        if (requested < 0 || requested >= size) {
            if (mode == Mode.ORDER) return automatic ? -1 : index;
            return index = requested < 0 ? size - 1 : 0;
        }
        return index = requested;
    }

    /**
     * Skip a failed song once per recovery pass. ONE must leave the failed song;
     * ORDER still ends at its boundary. Shuffle may rebuild its order only after
     * inspecting the full remaining permutation, and never retries failed slots.
     */
    public int nextAfterFailure() {
        if (size == 0 || index < 0) return -1;
        failedPass.add(index);
        if (failedPass.size() >= size) return -1;
        int priority = takePriority(true);
        if (priority >= 0) return priority;
        if (mode == Mode.SHUFFLE) {
            for (int cursor = shuffleCursor + 1; cursor < shuffle.size(); cursor++) {
                int candidate = shuffle.get(cursor);
                if (!failedPass.contains(candidate)) {
                    shuffleCursor = cursor;
                    return index = candidate;
                }
            }
            rebuildShuffle();
            for (int cursor = 1; cursor < shuffle.size(); cursor++) {
                int candidate = shuffle.get(cursor);
                if (!failedPass.contains(candidate)) {
                    shuffleCursor = cursor;
                    return index = candidate;
                }
            }
            return -1;
        }
        int previous = index;
        for (int candidate = previous + 1; candidate < size; candidate++)
            if (!failedPass.contains(candidate)) return index = candidate;
        if (mode != Mode.ORDER)
            for (int candidate = 0; candidate < previous; candidate++)
                if (!failedPass.contains(candidate)) return index = candidate;
        return -1;
    }

    public int failureCount() { return failedPass.size(); }
    public void resetFailurePass() { failedPass.clear(); }

    private void rebuildShuffle() {
        shuffle.clear();
        if (size == 0) return;
        // Current song is first, followed by every other song exactly once.
        shuffle.add(index);
        List<Integer> other = new ArrayList<>();
        for (int i = 0; i < size; i++) if (i != index) other.add(i);
        Collections.shuffle(other, random);
        shuffle.addAll(other);
        shuffleCursor = 0;
    }

    public List<Integer> upcoming(int limit) {
        ArrayList<Integer> result = new ArrayList<>();
        if (size == 0 || index < 0 || limit <= 0) return result;
        result.add(index);
        for (int priority : priorityNext)
            if (result.size() < limit && priority >= 0 && priority < size && !result.contains(priority)) result.add(priority);
        if (mode == Mode.SHUFFLE) {
            for (int j = shuffleCursor + 1; j < shuffle.size() && result.size() < limit; j++)
                if (!result.contains(shuffle.get(j))) result.add(shuffle.get(j));
        } else {
            for (int i = index + 1; i < size && result.size() < limit; i++)
                if (!result.contains(i)) result.add(i);
            if (mode == Mode.LOOP || mode == Mode.ONE)
                for (int i = 0; i < index && result.size() < limit; i++)
                    if (!result.contains(i)) result.add(i);
        }
        return result;
    }
}
