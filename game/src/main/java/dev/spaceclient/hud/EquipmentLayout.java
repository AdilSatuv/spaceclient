package dev.spaceclient.hud;

import java.util.function.ToIntFunction;

/** Content-sized armor cells and bounded effect cards, shared by HUD and editor. */
public final class EquipmentLayout {
    public static final int ARMOR_HEIGHT=38,EFFECT_WIDTH=128;
    private EquipmentLayout(){}
    public static int armorCell(String[] widestValues,ToIntFunction<String> measure) {
        int width=28;for(String value:widestValues)width=Math.max(width,Math.min(68,measure.applyAsInt(value)+6));return width;
    }
    public static int armorWidth(int count,int cell){if(count<1||cell<1)throw new IllegalArgumentException();return count*cell+(count-1)*2;}
    public static int effectHeight(int count,int limit,boolean time) {
        if(count<0||limit<1||limit>5)throw new IllegalArgumentException();
        if(count==0)return 24;
        return Math.min(count,limit)*((time?24:22)+2)-2+(count>limit?12:0);
    }
}
