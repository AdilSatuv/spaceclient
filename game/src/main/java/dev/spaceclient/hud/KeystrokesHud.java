package dev.spaceclient.hud;

/** Compact and bounded labels for the player's actual movement bindings. */
public final class KeystrokesHud {
    private static final int MAX_LABEL_CODEPOINTS = 18;

    private KeystrokesHud() { }

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
        name.codePoints().filter(cp -> !Character.isISOControl(cp) && cp != 0x00a7)
            .limit(MAX_LABEL_CODEPOINTS).forEach(out::appendCodePoint);
        String label = out.toString().trim();
        return label.isEmpty() ? "?" : label;
    }
}
