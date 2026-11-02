package dev.spaceclient.hud;

import java.util.function.ToIntFunction;

/** Compact and bounded labels for the player's actual movement bindings. */
public final class KeystrokesHud {
    private static final int MAX_LABEL_CODEPOINTS = 18;

    private KeystrokesHud() { }
    public static final class Key {
        public final String label;
        public final boolean pressed;
        public Key(String label,boolean pressed){this.label=safeLabel(label);this.pressed=pressed;}
        public String text(){return key(label,pressed);}
    }
    public static String text(Key[] keys) {
        StringBuilder out=new StringBuilder();
        for(Key key:keys){if(out.length()>0)out.append(' ');out.append(key.text());}
        return out.toString();
    }
    /** Reserve the wider of both states so a preceding pressed key cannot shift its neighbours. */
    public static int cellWidth(Key key,ToIntFunction<String> measure) {
        return Math.max(measure.applyAsInt(key(key.label,false)),measure.applyAsInt(key(key.label,true)))+2;
    }

    public static String text(String forward, boolean forwardPressed, String left, boolean leftPressed,
                              String back, boolean backPressed, String right, boolean rightPressed) {
        return key(forward, forwardPressed) + " " + key(left, leftPressed) + " "
            + key(back, backPressed) + " " + key(right, rightPressed);
    }

    private static String key(String name, boolean pressed) {
        String label = safeLabel(name);
        return pressed ? "[" + label + "]" : " " + label + " ";
    }

    static String safeLabel(String name) {
        if (name == null) return "?";
        StringBuilder out = new StringBuilder();
        name.codePoints().filter(cp -> !Character.isISOControl(cp) && cp != 0x00a7
                && Character.getType(cp) != Character.FORMAT && Character.getType(cp) != Character.SURROGATE)
            .limit(MAX_LABEL_CODEPOINTS).forEach(out::appendCodePoint);
        String label = out.toString().trim();
        return label.isEmpty() ? "?" : label;
    }
}
