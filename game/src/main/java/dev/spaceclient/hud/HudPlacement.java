package dev.spaceclient.hud;

/** Immutable placement in GUI units; no rendering or Minecraft dependencies. */
public final class HudPlacement {
    public enum Horizontal { LEFT, RIGHT }
    public enum Vertical { TOP, BOTTOM }
    public final Horizontal horizontal;
    public final Vertical vertical;
    public final double x, y, scale;

    public HudPlacement(Horizontal horizontal, Vertical vertical, double x, double y, double scale) {
        if (horizontal == null || vertical == null || !finite(x, 0, 16384) || !finite(y, 0, 16384)
                || !finite(scale, 0.5, 3)) throw new IllegalArgumentException("Invalid HUD placement");
        this.horizontal = horizontal; this.vertical = vertical;
        this.x = x; this.y = y; this.scale = scale;
    }
    private static boolean finite(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max;
    }
    public static HudPlacement defaults(HudModule module) {
        return new HudPlacement(Horizontal.LEFT, Vertical.TOP, 8, 20 + module.ordinal() * 12, 1);
    }
    public Bounds resolve(int screenWidth, int screenHeight, int textWidth, int textHeight) {
        if (screenWidth < 1 || screenHeight < 1 || textWidth < 1 || textHeight < 1)
            throw new IllegalArgumentException("Invalid HUD viewport");
        double fittedScale = Math.min(scale, Math.min((double) screenWidth / textWidth,
            (double) screenHeight / textHeight));
        double w = textWidth * fittedScale, h = textHeight * fittedScale;
        double px = horizontal == Horizontal.LEFT ? x : screenWidth - w - x;
        double py = vertical == Vertical.TOP ? y : screenHeight - h - y;
        return new Bounds(clamp(px, screenWidth - w), clamp(py, screenHeight - h), w, h, fittedScale);
    }
    /** Convert a dragged/nudged point to edge distances, snapping within six GUI units. */
    public HudPlacement at(double px, double py, int screenWidth, int screenHeight, int textWidth, int textHeight) {
        return at(px, py, screenWidth, screenHeight, textWidth, textHeight, true);
    }
    public HudPlacement at(double px, double py, int screenWidth, int screenHeight, int textWidth, int textHeight, boolean snap) {
        if (!Double.isFinite(px) || !Double.isFinite(py)) throw new IllegalArgumentException("Invalid HUD point");
        Bounds size = resolve(screenWidth, screenHeight, textWidth, textHeight);
        double maxX = screenWidth - size.width, maxY = screenHeight - size.height;
        px = clamp(px, maxX); py = clamp(py, maxY);
        Horizontal hx = px <= maxX - px ? Horizontal.LEFT : Horizontal.RIGHT;
        Vertical vy = py <= maxY - py ? Vertical.TOP : Vertical.BOTTOM;
        double dx = hx == Horizontal.LEFT ? px : maxX - px;
        double dy = vy == Vertical.TOP ? py : maxY - py;
        return new HudPlacement(hx, vy, snap && dx <= 6 ? 0 : Math.min(dx, 16384),
            snap && dy <= 6 ? 0 : Math.min(dy, 16384), scale);
    }
    public HudPlacement anchored(Horizontal hx, Vertical vy, int screenWidth, int screenHeight, int textWidth, int textHeight) {
        Bounds b = resolve(screenWidth, screenHeight, textWidth, textHeight);
        return new HudPlacement(hx, vy, clamp(hx == Horizontal.LEFT ? b.x : screenWidth - b.width - b.x, 16384),
            clamp(vy == Vertical.TOP ? b.y : screenHeight - b.height - b.y, 16384), scale);
    }
    public HudPlacement withScale(double value) { return new HudPlacement(horizontal, vertical, x, y, value); }
    private static double clamp(double value, double max) { return Math.max(0, Math.min(value, Math.max(0, max))); }
    public static final class Bounds {
        public final double x, y, width, height, scale;
        private Bounds(double x, double y, double width, double height, double scale) {
            this.x = x; this.y = y; this.width = width; this.height = height; this.scale = scale;
        }
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
