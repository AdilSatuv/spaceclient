package dev.spaceclient.hud;

/** Position the inspector without covering the selected widget or its focus border. */
public final class HudEditorLayout {
    public static final int HEIGHT = 92;
    public static final int SIDEBAR_WIDTH = 156, SIDEBAR_HEIGHT = 220;
    private HudEditorLayout() { }
    public static int panelWidth(int screenWidth) {
        if (screenWidth < 320) throw new IllegalArgumentException("Editor requires at least 320 units");
        return Math.min(360, screenWidth - 16);
    }
    public static int panelX(int screenWidth) { return (screenWidth - panelWidth(screenWidth)) / 2; }
    public static int toolbarY(int height, HudPlacement.Bounds selected) {
        return selected.y + selected.height / 2 > height / 2.0 ? 8 : height - HEIGHT - 8;
    }
    /** Wider screens use a side inspector when it can fit completely outside the selected bounds. */
    public static boolean useSidebar(int width, int height, HudPlacement.Bounds selected) {
        if (width < 500 || height < SIDEBAR_HEIGHT + 16) return false;
        return selected.x - 2 >= 8 + SIDEBAR_WIDTH || selected.x + selected.width + 2 <= width - 8 - SIDEBAR_WIDTH;
    }
    public static int sidebarX(int width, HudPlacement.Bounds selected) {
        int right = width - SIDEBAR_WIDTH - 8;
        return selected.x + selected.width + 2 <= right ? right : 8;
    }
    public static int sidebarY(int height) { return (height - SIDEBAR_HEIGHT) / 2; }
    public static int nextModuleIndex(int current, int delta, int count) {
        if (count < 1 || current < 0 || current >= count) throw new IllegalArgumentException("Invalid module selection");
        return Math.floorMod(current + delta, count);
    }
}
