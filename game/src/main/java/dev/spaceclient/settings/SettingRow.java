package dev.spaceclient.settings;

import java.util.function.IntFunction;

/** Presentation metadata only; settings owners retain validation and persistence. */
public final class SettingRow {
    public enum Kind { TOGGLE, CHOICE, NUMBER, ACTION, TEXT }
    public final int id;
    public final String title, detail, value;
    public final Kind kind;
    public final boolean checked, enabled;
    public final SettingNumber range;
    public final int number;
    public final IntFunction<String> format;

    private SettingRow(int id, String title, String detail, String value, Kind kind, boolean checked,
            boolean enabled, SettingNumber range, int number, IntFunction<String> format) {
        this.id=id; this.title=title; this.detail=detail; this.value=value; this.kind=kind;
        this.checked=checked; this.enabled=enabled; this.range=range; this.number=number; this.format=format;
    }
    public static SettingRow toggle(int id, String title, String detail, boolean value) {
        return new SettingRow(id,title,detail,value?"On":"Off",Kind.TOGGLE,value,true,null,0,null);
    }
    public static SettingRow choice(int id, String title, String detail, String value) {
        return new SettingRow(id,title,detail,value,Kind.CHOICE,false,true,null,0,null);
    }
    public static SettingRow action(int id, String title, String detail, String value) {
        return new SettingRow(id,title,detail,value,Kind.ACTION,false,true,null,0,null);
    }
    public static SettingRow text(int id, String title, String detail, String value) {
        return new SettingRow(id,title,detail,value,Kind.TEXT,false,true,null,0,null);
    }
    public static SettingRow number(int id, String title, String detail, int value, int min, int max, int step, IntFunction<String> format) {
        return new SettingRow(id,title,detail,format.apply(value),Kind.NUMBER,false,true,new SettingNumber(min,max,step),value,format);
    }
    public SettingRow disabled(String reason) {
        return new SettingRow(id,title,reason,value,kind,checked,false,range,number,format);
    }
}
