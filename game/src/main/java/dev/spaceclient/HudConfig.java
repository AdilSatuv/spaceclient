package dev.spaceclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class HudConfig {
    public boolean enabled = true;
    public boolean fps = true;
    public boolean coordinates = true;
    public boolean keystrokes = true;

    public static HudConfig load(Path file) throws IOException {
        HudConfig config = new HudConfig();
        if (!Files.exists(file)) return config;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) { properties.load(input); }
        if (!properties.getProperty("schemaVersion", "1").equals("1")) throw new IOException("Unsupported HUD schema version");
        config.enabled = read(properties, "enabled");
        config.fps = read(properties, "fps");
        config.coordinates = read(properties, "coordinates");
        config.keystrokes = read(properties, "keystrokes");
        return config;
    }

    private static boolean read(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key, "true");
        if (!value.equals("true") && !value.equals("false")) throw new IOException("Invalid HUD setting: " + key);
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
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) { properties.store(output, "Space Client HUD"); }
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
