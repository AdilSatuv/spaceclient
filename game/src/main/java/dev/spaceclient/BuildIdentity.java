package dev.spaceclient;

import java.io.InputStream;
import java.util.Properties;

/** A development source identifier, not a signature or a runtime verification result. */
public final class BuildIdentity {
    private static final String LABEL = load();
    private BuildIdentity() { }

    public static String label() { return LABEL; }

    public static String fromProperties(Properties stamp) {
        if (stamp == null || !"1".equals(stamp.getProperty("schemaVersion"))
                || !"development".equals(stamp.getProperty("channel"))) return "dev-unknown";
        String tree = stamp.getProperty("gameSourceTree", "");
        return tree.matches("[a-f0-9]{40}") ? "dev-" + tree.substring(0, 12) : "dev-unknown";
    }

    private static String load() {
        try (InputStream stream = BuildIdentity.class.getResourceAsStream("/spaceclient-build.properties")) {
            if (stream == null) return "dev-unknown";
            Properties stamp = new Properties();
            stamp.load(stream);
            return fromProperties(stamp);
        } catch (Exception failure) { return "dev-unknown"; }
    }
}
