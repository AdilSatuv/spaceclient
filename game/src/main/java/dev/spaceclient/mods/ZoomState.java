package dev.spaceclient.mods;

/** Frame-rate independent zoom; saved Minecraft FOV and sensitivity remain untouched. */
public final class ZoomState {
    private double divisor = 1;
    private long previous;
    public double update(long nanos, boolean eligible, boolean held, int factor, boolean smooth) {
        if (!eligible) { divisor = 1; previous = nanos; return divisor; }
        double target = held ? factor : 1;
        if (previous == 0 || !smooth) divisor = target;
        else {
            double seconds = Math.max(0, Math.min(.1, (nanos - previous) / 1_000_000_000.0));
            divisor += (target - divisor) * (1 - Math.exp(-seconds * 18));
            if (Math.abs(divisor - target) < .001) divisor = target;
        }
        previous = nanos;
        return divisor;
    }
    public double divisor() { return divisor; }
}
