package dev.spaceclient.hud;

/** Monotonic world-session timer. Pauses and disconnects do not accumulate playtime. */
public final class SessionTimer {
    private long previous, elapsed;
    private boolean active;
    public void update(long nanos, boolean inWorld, boolean paused) {
        if (!inWorld) { previous = nanos; elapsed = 0; active = false; return; }
        if (active && !paused && previous != 0) elapsed += Math.max(0, Math.min(1_000_000_000L, nanos - previous));
        previous = nanos; active = !paused;
    }
    public long seconds() { return elapsed / 1_000_000_000L; }
}
