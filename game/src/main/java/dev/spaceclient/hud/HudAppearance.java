package dev.spaceclient.hud;

/** Immutable per-module appearance stored independently in each HUD profile. */
public final class HudAppearance {
    public enum Color { AUTO, WHITE, GOLD, AQUA }
    public enum Backdrop { STRONG, SOFT, OFF }
    public final Color color;
    public final Backdrop backdrop;
    public final boolean shadow;

    public HudAppearance(Color color, Backdrop backdrop, boolean shadow) {
        if (color == null || backdrop == null) throw new IllegalArgumentException("Invalid HUD appearance");
        this.color = color;
        this.backdrop = backdrop;
        this.shadow = shadow;
    }
    public static HudAppearance defaults() { return new HudAppearance(Color.AUTO, Backdrop.STRONG, true); }
    public HudAppearance withColor(Color value) { return new HudAppearance(value, backdrop, shadow); }
    public HudAppearance withBackdrop(Backdrop value) { return new HudAppearance(color, value, shadow); }
    public HudAppearance withShadow(boolean value) { return new HudAppearance(color, backdrop, value); }
}
