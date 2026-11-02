package dev.spaceclient.mods;

/** Exact threshold comparison: rounded percentages must not warn early. */
public final class DurabilityAlert {
    private DurabilityAlert() { }
    public static int remaining(int damage,int maximum) { return Math.max(0,maximum-Math.max(0,damage)); }
    public static boolean low(boolean damageable,int damage,int maximum,int threshold) {
        return damageable&&maximum>0&&threshold>=1&&threshold<=100
            &&(long)remaining(damage,maximum)*100<=(long)maximum*threshold;
    }
}
