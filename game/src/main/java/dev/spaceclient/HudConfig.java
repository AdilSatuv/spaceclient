package dev.spaceclient;

import dev.spaceclient.hud.HudModule;
import dev.spaceclient.hud.HudPlacement;
import dev.spaceclient.hud.HudProfile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class HudConfig {
    public static final int MAX_BYTES = 65536;
    private final HudProfile[] profiles = { new HudProfile("Profile 1"), new HudProfile("Profile 2") };
    private int activeProfile;
    private boolean migratedFromV1;

    public HudProfile active() { return profiles[activeProfile]; }
    public HudProfile profile(int index) {
        if (index < 0 || index >= profiles.length) throw new IllegalArgumentException("Unknown HUD profile");
        return profiles[index];
    }
    public int activeIndex() { return activeProfile; }
    public void select(int index) { profile(index); activeProfile = index; }
    public void replaceActive(HudProfile profile) { profiles[activeProfile] = profile.copy(); }
    public boolean needsMigrationBackup() { return migratedFromV1; }
    public HudConfig copy() { HudConfig copy = new HudConfig(); copy.restore(this); return copy; }
    public void restore(HudConfig source) {
        for (int i = 0; i < profiles.length; i++) profiles[i] = source.profiles[i].copy();
        activeProfile = source.activeProfile;
        migratedFromV1 = source.migratedFromV1;
    }

    public static HudConfig load(Path file) throws IOException {
        HudConfig config = new HudConfig();
        if (!Files.exists(file)) return config;
        Properties properties = readProperties(file);
        String schema = properties.getProperty("schemaVersion", "1");
        if (!schema.matches("[1-9][0-9]*")) throw new InvalidConfigException("Invalid HUD schema version");
        if (!schema.equals("1") && !schema.equals("2")) throw new UnsupportedSchemaException();
        if (schema.equals("1")) {
            config.profiles[0] = readProfile(properties, "", "Profile 1");
            config.profiles[1] = config.profiles[0].copy();
            config.profiles[1].name = "Profile 2";
            config.migratedFromV1 = true;
        } else {
            try { config.select(Integer.parseInt(properties.getProperty("activeProfile", "0"))); }
            catch (IllegalArgumentException exception) { throw new InvalidConfigException("Invalid active HUD profile", exception); }
            for (int i = 0; i < config.profiles.length; i++)
                config.profiles[i] = readProfile(properties, "profile." + i + ".", "Profile " + (i + 1));
        }
        return config;
    }

    private static Properties readProperties(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (bytes.size() + count > MAX_BYTES) throw new InvalidConfigException("HUD config exceeds 64 KiB");
                bytes.write(buffer, 0, count);
            }
            try { properties.load(new ByteArrayInputStream(bytes.toByteArray())); }
            catch (IllegalArgumentException exception) { throw new InvalidConfigException("Malformed HUD properties", exception); }
        }
        return properties;
    }

    private static HudProfile readProfile(Properties properties, String prefix, String defaultName) throws IOException {
        try {
            HudProfile profile = new HudProfile(properties.getProperty(prefix + "name", defaultName));
            profile.enabled = readBoolean(properties, prefix + "enabled");
            profile.fps = readBoolean(properties, prefix + "fps");
            profile.coordinates = readBoolean(properties, prefix + "coordinates");
            profile.keystrokes = readBoolean(properties, prefix + "keystrokes");
            profile.cps = readBoolean(properties, prefix + "cps");
            for (HudModule module : HudModule.values()) {
                HudPlacement defaults = HudPlacement.defaults(module);
                String key = prefix + "layout." + module.getId() + ".";
                profile.place(module, new HudPlacement(
                    HudPlacement.Horizontal.valueOf(properties.getProperty(key + "horizontal", "LEFT")),
                    HudPlacement.Vertical.valueOf(properties.getProperty(key + "vertical", "TOP")),
                    Double.parseDouble(properties.getProperty(key + "x", Double.toString(defaults.x))),
                    Double.parseDouble(properties.getProperty(key + "y", Double.toString(defaults.y))),
                    Double.parseDouble(properties.getProperty(key + "scale", "1.0"))));
            }
            return profile;
        } catch (IllegalArgumentException exception) { throw new InvalidConfigException("Invalid HUD profile", exception); }
    }

    private static boolean readBoolean(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key, "true");
        if (!value.equals("true") && !value.equals("false")) throw new InvalidConfigException("Invalid HUD setting: " + key);
        return Boolean.parseBoolean(value);
    }

    private static void writeProfile(Properties properties, String prefix, HudProfile profile) throws IOException {
        try { HudProfile.validName(profile.name); }
        catch (IllegalArgumentException exception) { throw new InvalidConfigException("Invalid HUD profile name", exception); }
        properties.setProperty(prefix + "name", profile.name);
        properties.setProperty(prefix + "enabled", Boolean.toString(profile.enabled));
        properties.setProperty(prefix + "fps", Boolean.toString(profile.fps));
        properties.setProperty(prefix + "coordinates", Boolean.toString(profile.coordinates));
        properties.setProperty(prefix + "keystrokes", Boolean.toString(profile.keystrokes));
        properties.setProperty(prefix + "cps", Boolean.toString(profile.cps));
        for (HudModule module : HudModule.values()) {
            HudPlacement placement = profile.placement(module);
            String key = prefix + "layout." + module.getId() + ".";
            properties.setProperty(key + "horizontal", placement.horizontal.name());
            properties.setProperty(key + "vertical", placement.vertical.name());
            properties.setProperty(key + "x", Double.toString(placement.x));
            properties.setProperty(key + "y", Double.toString(placement.y));
            properties.setProperty(key + "scale", Double.toString(placement.scale));
        }
    }

    public void save(Path file) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", "2");
        properties.setProperty("activeProfile", Integer.toString(activeProfile));
        for (int i = 0; i < profiles.length; i++) writeProfile(properties, "profile." + i + ".", profiles[i]);
        writeAtomic(file, properties);
    }

    public static void exportProfile(Path file, HudProfile profile) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("format", "spaceclient-hud-profile");
        properties.setProperty("schemaVersion", "1");
        writeProfile(properties, "", profile);
        writeAtomic(file, properties);
    }

    public static HudProfile importProfile(Path file) throws IOException {
        Properties properties = readProperties(file);
        if (!"spaceclient-hud-profile".equals(properties.getProperty("format"))
                || !"1".equals(properties.getProperty("schemaVersion")))
            throw new InvalidConfigException("Unsupported HUD profile export");
        HudProfile profile = readProfile(properties, "", "Imported");
        Properties expected = new Properties();
        expected.setProperty("format", "spaceclient-hud-profile");
        expected.setProperty("schemaVersion", "1");
        writeProfile(expected, "", profile);
        if (!properties.stringPropertyNames().equals(expected.stringPropertyNames()))
            throw new InvalidConfigException("Incomplete or unknown HUD profile fields");
        return profile;
    }

    private static void writeAtomic(Path file, Properties properties) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "space-hud-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) { properties.store(output, "Space Client HUD"); }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public static final class InvalidConfigException extends IOException {
        public InvalidConfigException(String message) { super(message); }
        public InvalidConfigException(String message, Throwable cause) { super(message, cause); }
    }
    public static final class UnsupportedSchemaException extends IOException {
        public UnsupportedSchemaException() { super("Unsupported HUD schema version"); }
    }
}
