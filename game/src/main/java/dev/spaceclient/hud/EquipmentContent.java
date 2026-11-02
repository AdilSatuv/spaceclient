package dev.spaceclient.hud;

/** Per-profile formats for the player's equipment and status effects. */
public final class EquipmentContent {
    public enum Style { TEXT, ICONS }
    public enum Durability { PERCENT, REMAINING }
    public final Style armorStyle,effectStyle;
    public final Durability durability;
    public final boolean hand,effectTime;
    public final int maxEffects;
    public EquipmentContent(Style armorStyle,Durability durability,boolean hand,Style effectStyle,int maxEffects,boolean effectTime) {
        if(armorStyle==null||durability==null||effectStyle==null||maxEffects<1||maxEffects>5)throw new IllegalArgumentException("Invalid equipment format");
        this.armorStyle=armorStyle;this.durability=durability;this.hand=hand;this.effectStyle=effectStyle;this.maxEffects=maxEffects;this.effectTime=effectTime;
    }
    public static EquipmentContent defaults(){return new EquipmentContent(Style.TEXT,Durability.PERCENT,true,Style.TEXT,2,true);}
    public EquipmentContent armorStyle(Style v){return new EquipmentContent(v,durability,hand,effectStyle,maxEffects,effectTime);}
    public EquipmentContent durability(Durability v){return new EquipmentContent(armorStyle,v,hand,effectStyle,maxEffects,effectTime);}
    public EquipmentContent hand(boolean v){return new EquipmentContent(armorStyle,durability,v,effectStyle,maxEffects,effectTime);}
    public EquipmentContent effectStyle(Style v){return new EquipmentContent(armorStyle,durability,hand,v,maxEffects,effectTime);}
    public EquipmentContent maxEffects(int v){return new EquipmentContent(armorStyle,durability,hand,effectStyle,v,effectTime);}
    public EquipmentContent effectTime(boolean v){return new EquipmentContent(armorStyle,durability,hand,effectStyle,maxEffects,v);}
}
