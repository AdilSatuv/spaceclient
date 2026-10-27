package dev.spaceclient.hud;

/** Put controls opposite the selected widget, so the editor never covers its focus. */
public final class HudEditorLayout {
    public static final int HEIGHT = 92;
    public static final int NAV_WIDTH = 18, TITLE_WIDTH = 68, NAV_GAP = 2;
    private HudEditorLayout() { }
    public static int toolbarY(int height, HudPlacement.Bounds selected) {
        return selected.y + selected.height / 2 > height / 2.0 ? 8 : height - HEIGHT - 8;
    }
    public static int profileWidth(int screenWidth) {
        if (screenWidth < 320) throw new IllegalArgumentException("Editor requires at least 320 units");
        return Math.min(96, 52 + (screenWidth - 320) / 2);
    }
    public static int previousModuleX(int screenWidth) { return 22 + profileWidth(screenWidth) * 2; }
    public static int moduleTitleX(int screenWidth) { return previousModuleX(screenWidth) + NAV_WIDTH + NAV_GAP; }
    public static int nextModuleX(int screenWidth) { return moduleTitleX(screenWidth) + TITLE_WIDTH + NAV_GAP; }
    public static int doneX(int screenWidth) { return screenWidth - 68; }
    public static int nextModuleIndex(int current, int delta, int count) {
        if (count < 1 || current < 0 || current >= count) throw new IllegalArgumentException("Invalid module selection");
        return Math.floorMod(current + delta, count);
    }
}
