package dev.spaceclient.hud;

/** Put controls opposite the selected widget, so the editor never covers its focus. */
public final class HudEditorLayout {
    public static final int HEIGHT = 92;
    private HudEditorLayout() { }
    /** Bound the inspector so large screens keep a spacious editing canvas. */
    public static int panelWidth(int screenWidth) {
        if (screenWidth < 320) throw new IllegalArgumentException("Editor requires at least 320 units");
        return Math.min(360, screenWidth - 16);
    }
    public static int panelX(int screenWidth) { return (screenWidth - panelWidth(screenWidth)) / 2; }
    public static int toolbarY(int height, HudPlacement.Bounds selected) {
        return selected.y + selected.height / 2 > height / 2.0 ? 8 : height - HEIGHT - 8;
    }
    public static int nextModuleIndex(int current, int delta, int count) {
        if (count < 1 || current < 0 || current >= count) throw new IllegalArgumentException("Invalid module selection");
        return Math.floorMod(current + delta, count);
    }
}
