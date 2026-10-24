package dev.spaceclient.hud;

/** Put controls opposite the selected widget, so the editor never covers its focus. */
public final class HudEditorLayout {
    public static final int HEIGHT = 92;
    private HudEditorLayout() { }
    public static int toolbarY(int height, HudPlacement.Bounds selected) {
        return selected.y + selected.height / 2 > height / 2.0 ? 8 : height - HEIGHT - 8;
    }
}
