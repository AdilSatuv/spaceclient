package dev.spaceclient.mods;

import java.awt.Color;
/** Shared cosmetic colors; rainbow time is monotonic and never saved to settings. */
public enum VisualPalette {
    GOLD(0xE7BF79), AQUA(0x49E8E0), VIOLET(0xBA79FF), LIME(0xA4F26A), RED(0xFF626E), WHITE(0xFFFFFF), RAINBOW(0);
    private final int color;
    VisualPalette(int color){this.color=color;}
    public int rgb(long millis){return this==RAINBOW?Color.HSBtoRGB(Math.floorMod(millis,6000L)/6000f,.65f,1)&0xFFFFFF:color;}
    public int rgb(){return rgb(System.nanoTime()/1000000L);}
}
