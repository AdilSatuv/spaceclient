package dev.spaceclient;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/** Counts physical button presses in the half-open window (now - 1 second, now]. */
public final class ClickCounter {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private final LongSupplier clock;
    private final Deque<Long> left = new ArrayDeque<>();
    private final Deque<Long> right = new ArrayDeque<>();
    private boolean leftHeld;
    private boolean rightHeld;

    public ClickCounter() { this(System::nanoTime); }
    ClickCounter(LongSupplier clock) { this.clock = clock; }

    public void onMouseEvent(int button, boolean pressed, boolean gameplay) {
        if (!gameplay) {
            clear();
            return;
        }
        if (button != 0 && button != 1) return;
        boolean held = button == 0 ? leftHeld : rightHeld;
        if (button == 0) leftHeld = pressed;
        else rightHeld = pressed;
        long now = clock.getAsLong();
        prune(left, now);
        prune(right, now);
        if (pressed && !held) (button == 0 ? left : right).addLast(now);
    }

    public int getLeftCps() { return count(left); }
    public int getRightCps() { return count(right); }

    public void clear() {
        left.clear();
        right.clear();
        leftHeld = false;
        rightHeld = false;
    }

    private int count(Deque<Long> clicks) {
        prune(clicks, clock.getAsLong());
        return clicks.size();
    }

    private static void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() >= WINDOW_NANOS) clicks.removeFirst();
    }
}
