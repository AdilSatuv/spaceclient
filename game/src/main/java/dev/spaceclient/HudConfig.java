package dev.spaceclient;

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
    public boolean enabled = true;
    public boolean fps = true;
    public boolean coordinates = true;
    public boolean keystrokes = true;
    public boolean cps = true;

    public static HudConfig load(Path file) throws IOException {
        HudConfig config = new HudConfig();
        if (!Files.exists(file)) return config;
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
        String schema = properties.getProperty("schemaVersion", "1");
        if (!schema.matches("[1-9][0-9]*")) throw new InvalidConfigException("Invalid HUD schema version");
        if (!schema.equals("1")) throw new UnsupportedSchemaException();
        config.enabled = read(properties, "enabled");
        config.fps = read(properties, "fps");
        config.coordinates = read(properties, "coordinates");
        config.keystrokes = read(properties, "keystrokes");
        config.cps = read(properties, "cps");
        return config;
    }

    private static boolean read(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key, "true");
        if (!value.equals("true") && !value.equals("false")) throw new InvalidConfigException("Invalid HUD setting: " + key);
        return Boolean.parseBoolean(value);
    }

    public void save(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", "1");
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("fps", Boolean.toString(fps));
        properties.setProperty("coordinates", Boolean.toString(coordinates));
        properties.setProperty("keystrokes", Boolean.toString(keystrokes));
        properties.setProperty("cps", Boolean.toString(cps));
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
