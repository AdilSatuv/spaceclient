package dev.spaceclient.hud;

/** A logical canvas keeps rendering and pointer coordinates in the same space. */
final class HudViewport {
    final float scale;
    final int width, height;
    HudViewport(int width, int height) {
        float fit = Math.min(1f, Math.min(width / 830f, height / 465f));
        scale = Math.max(.55f, fit);
        this.width = Math.max(1, (int) (width / scale));
        this.height = Math.max(1, (int) (height / scale));
    }
    int toUi(int value) { return (int) Math.floor(value / scale); }
}
