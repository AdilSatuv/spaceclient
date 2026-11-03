package dev.spaceclient.settings;

/** Whole-row scrolling keeps focused controls clear of the fixed header and footer. */
public final class SettingsLayout {
    public static final int ROW_HEIGHT = 44;
    public final int left, top, width, height, bodyTop, slots;
    public SettingsLayout(int width, int height, int header) {
        this.width=Math.min(660,Math.max(0,width-24));
        this.height=Math.min(430,Math.max(0,height-24));
        left=(width-this.width)/2; top=(height-this.height)/2;
        bodyTop=top+header;
        slots=Math.max(1,(this.height-header-80)/ROW_HEIGHT);
    }
    public int offset(int requested, int count) { return Math.max(0,Math.min(Math.max(0,count-slots),requested)); }
    public int reveal(int offset, int index, int count) {
        return offset(index<offset?index:index>=offset+slots?index-slots+1:offset,count);
    }
    public int rowY(int index, int offset) { return bodyTop+(index-offset)*ROW_HEIGHT; }
    public int controlX() { return left+width-220; }
    public int trackX() { return controlX()+32; }
    public int trackWidth() { return 138; }
}
