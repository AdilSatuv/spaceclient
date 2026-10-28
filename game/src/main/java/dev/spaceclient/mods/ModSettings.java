package dev.spaceclient.mods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Settings are deliberately separate from HUD profiles: these change gameplay controls. */
public final class ModSettings {
    private final Path file;
    private boolean autoSprint, freeLook, readOnly;
    private String error = "";

    private ModSettings(Path file) { this.file = file; }

    public static ModSettings open(Path file) {
        ModSettings settings = new ModSettings(file);
        try {
            if (!Files.exists(file)) return settings;
            if (Files.size(file) > 4096) throw new IOException("File too large");
            Properties values = new Properties();
            try (InputStream input = Files.newInputStream(file)) { values.load(input); }
            if (!"1".equals(values.getProperty("schemaVersion")) || values.size() != 3
                    || !values.containsKey("autoSprint") || !values.containsKey("freeLook"))
                throw new IOException("Unsupported settings");
            settings.autoSprint = parseBoolean(values.getProperty("autoSprint"));
            settings.freeLook = parseBoolean(values.getProperty("freeLook"));
        } catch (IOException | IllegalArgumentException ex) {
            settings.autoSprint = false;
            settings.freeLook = false;
            settings.readOnly = true;
            settings.error = "Mod settings unreadable; original kept (read-only).";
        }
        return settings;
    }

    private static boolean parseBoolean(String value) throws IOException {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw new IOException("Invalid boolean");
    }
    public boolean autoSprint() { return autoSprint; }
    public boolean freeLook() { return freeLook; }
    public boolean isReadOnly() { return readOnly; }
    public String error() { return error; }
    public boolean toggleAutoSprint() { return save(!autoSprint, freeLook); }
    public boolean toggleFreeLook() { return save(autoSprint, !freeLook); }

    private boolean save(boolean sprint, boolean look) {
        if (readOnly) return false;
        Path tmp = null;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            tmp = Files.createTempFile(file.toAbsolutePath().getParent(), "space-mods-", ".tmp");
            Properties values = new Properties();
            values.setProperty("schemaVersion", "1");
            values.setProperty("autoSprint", Boolean.toString(sprint));
            values.setProperty("freeLook", Boolean.toString(look));
            try (OutputStream output = Files.newOutputStream(tmp)) { values.store(output, "Space Client controls"); }
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
            autoSprint = sprint; freeLook = look; error = "";
            return true;
        } catch (IOException ex) {
            error = "Could not save; change reverted.";
            return false;
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignored) { }
        }
    }
}
