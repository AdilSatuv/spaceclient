package dev.spaceclient.hud;

/** Bounded durability formatting without Minecraft or rendering dependencies. */
public final class ArmorDurability {
    private ArmorDurability() { }

    /** Empty slot: -, non-damageable/unbreakable item: n/a, invalid metadata: ?. */
    public static String value(boolean present, boolean damageable, int damage, int maximum) {
        if (!present) return "-";
        if (!damageable) return "n/a";
        if (maximum <= 0 || damage < 0 || damage > maximum) return "?";
        long remaining = (long) maximum - damage;
        // Round up so an item with durability remaining does not misleadingly show 0%.
        long percent = (remaining * 100 + maximum - 1) / maximum;
        return percent + "%";
    }
}
