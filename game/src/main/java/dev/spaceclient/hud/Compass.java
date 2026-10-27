package dev.spaceclient.hud;

/** Minecraft yaw: zero faces south, positive angles turn west. */
public final class Compass {
    private static final String[] DIRECTIONS = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };
    private Compass() { }

    public static String direction(double yaw) {
        if (!Double.isFinite(yaw)) return "--";
        double normalized = ((yaw % 360) + 360) % 360;
        int sector = (int) Math.floor((normalized + 22.5) / 45) % DIRECTIONS.length;
        return DIRECTIONS[sector];
    }
}
