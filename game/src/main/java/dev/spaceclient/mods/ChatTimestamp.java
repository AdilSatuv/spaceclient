package dev.spaceclient.mods;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Local arrival time, captured once when a new message enters native chat. */
public final class ChatTimestamp {
    private static final DateTimeFormatter[] FORMATS;
    static {
        ModSettings.ChatTimeFormat[] modes=ModSettings.ChatTimeFormat.values();
        FORMATS=new DateTimeFormatter[modes.length];
        for(int i=0;i<modes.length;i++)FORMATS[i]=DateTimeFormatter.ofPattern(modes[i].pattern,Locale.ROOT);
    }
    private ChatTimestamp() { }
    public static String prefix(LocalTime time,ModSettings.ChatTimeFormat format) {
        return "["+FORMATS[format.ordinal()].format(time)+"] ";
    }
}
