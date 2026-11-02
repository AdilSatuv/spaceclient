package dev.spaceclient.mods;

/** Immutable crosshair settings, independent of Minecraft and OpenGL. */
public final class CrosshairStyle {
    public enum Shape { CROSS, DOT, RING }
    public enum Color {
        WHITE(0xFFFFFF), GOLD(0xE7BF79), AQUA(0x80E4ED), LIME(0xB1EF7A), RED(0xFF7777);
        public final int rgb;
        Color(int rgb) { this.rgb = rgb; }
    }
    public final Shape shape;
    public final Color color;
    public final int size, gap, thickness;
    public final boolean dot, outline;
    public CrosshairStyle(Shape shape, Color color, int size, int gap, int thickness, boolean dot, boolean outline) {
        if (shape == null || color == null || size < 2 || size > 12 || gap < 0 || gap > 8
                || thickness < 1 || thickness > 4) throw new IllegalArgumentException("Invalid crosshair style");
        this.shape = shape; this.color = color; this.size = size; this.gap = gap;
        this.thickness = thickness; this.dot = dot; this.outline = outline;
    }
    public static CrosshairStyle defaults() { return new CrosshairStyle(Shape.CROSS, Color.WHITE, 5, 3, 1, false, true); }
    public float extent() { return (shape == Shape.DOT ? thickness / 2f : shape == Shape.RING ? size : size + gap) + (outline ? 1 : 0); }
    public CrosshairStyle withShape(Shape v) { return new CrosshairStyle(v, color, size, gap, thickness, dot, outline); }
    public CrosshairStyle withColor(Color v) { return new CrosshairStyle(shape, v, size, gap, thickness, dot, outline); }
    public CrosshairStyle withSize(int v) { return new CrosshairStyle(shape, color, v, gap, thickness, dot, outline); }
    public CrosshairStyle withGap(int v) { return new CrosshairStyle(shape, color, size, v, thickness, dot, outline); }
    public CrosshairStyle withThickness(int v) { return new CrosshairStyle(shape, color, size, gap, v, dot, outline); }
    public CrosshairStyle withDot(boolean v) { return new CrosshairStyle(shape, color, size, gap, thickness, v, outline); }
    public CrosshairStyle withOutline(boolean v) { return new CrosshairStyle(shape, color, size, gap, thickness, dot, v); }
}
