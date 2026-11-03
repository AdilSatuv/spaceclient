package dev.spaceclient.mods;

/** Clamped content offsets, independent of Minecraft and physical input. */
public final class TooltipScroll {
    public int x,y;
    public void scroll(int wheel,boolean horizontal,int speed,int contentW,int contentH,int viewW,int viewH){
        if(horizontal)x=clamp(x-Integer.signum(wheel)*speed,contentW-viewW);
        else y=clamp(y-Integer.signum(wheel)*speed,contentH-viewH);
    }
    private static int clamp(int value,int max){return Math.max(0,Math.min(Math.max(0,max),value));}
    public void reset(){x=y=0;}
}
