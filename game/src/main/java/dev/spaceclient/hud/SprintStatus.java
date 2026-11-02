package dev.spaceclient.hud;

/** The armed toggle and vanilla's actual movement state are deliberately shown separately. */
public final class SprintStatus {
    private SprintStatus() { }
    public static String text(boolean moduleEnabled, boolean requested, boolean sprinting) {
        if (!moduleEnabled) return "ToggleSprint: OFF | Disabled";
        return "ToggleSprint: " + (requested ? "ON" : "OFF")
            + (sprinting ? " | Sprinting" : requested ? " | Ready" : "");
    }
    public static String compact(boolean moduleEnabled, boolean requested, boolean sprinting) {
        if(!moduleEnabled)return "Sprint OFF";
        return "Sprint " + (requested?"ON":"OFF") + (sprinting?" / RUN":requested?" / READY":"");
    }
}
