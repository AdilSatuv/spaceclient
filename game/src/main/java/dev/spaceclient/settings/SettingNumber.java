package dev.spaceclient.settings;

/** UI increments never wrap. Existing off-grid values survive until the user adjusts them. */
public final class SettingNumber {
    public final int min, max, step;
    public SettingNumber(int min, int max, int step) {
        if (max <= min || step < 1) throw new IllegalArgumentException("Invalid control range");
        this.min = min; this.max = max; this.step = step;
    }
    public int clamp(int value) { return Math.max(min, Math.min(max, value)); }
    public int adjust(int value, int direction, boolean coarse) {
        return (int) Math.max(min, Math.min(max, (long) value + (long) direction * step * (coarse ? 5 : 1)));
    }
    public int at(double fraction) {
        if (!Double.isFinite(fraction)) return min;
        double bounded = Math.max(0, Math.min(1, fraction));
        return clamp(min + (int) Math.round(bounded * (max - min) / step) * step);
    }
    public double fraction(int value) { return (clamp(value) - min) / (double) (max - min); }
}
