package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Client-thread settings commands. Failed writes roll back the visible change. */
public final class HudSettings {
    private final Path file;
    private final HudConfig config;
    private final boolean readOnly;
    private final String notice;
    private String error = "";

    private HudSettings(Path file, HudConfig config, boolean readOnly, String notice) {
        this.file = file;
        this.config = config;
        this.readOnly = readOnly;
        this.notice = notice;
    }

    public static HudSettings open(Path file) {
        try { return new HudSettings(file, HudConfig.load(file), false, ""); }
        catch (HudConfig.InvalidConfigException exception) {
            Path backup = file.resolveSibling(file.getFileName() + ".invalid-" + UUID.randomUUID() + ".bak");
            try {
                Files.move(file, backup);
                return new HudSettings(file, new HudConfig(), false, "Invalid config backed up. Defaults restored.");
            } catch (IOException backupError) {
                return new HudSettings(file, new HudConfig(), true, "Config recovery failed. File kept; read-only.");
            }
        } catch (HudConfig.UnsupportedSchemaException exception) {
            return new HudSettings(file, new HudConfig(), true, "Newer config detected. File kept; read-only.");
        } catch (IOException exception) {
            return new HudSettings(file, new HudConfig(), true, "Could not read config. File kept; read-only.");
        }
    }

    public HudConfig getConfig() { return config; }
    public boolean isReadOnly() { return readOnly; }
    public String getNotice() { return notice; }
    public String getError() { return error; }

    public boolean toggle(HudModule module) {
        if (readOnly) return false;
        boolean previous = module.isEnabled(config);
        module.setEnabled(config, !previous);
        if (persist()) return true;
        module.setEnabled(config, previous);
        return false;
    }

    public boolean toggleHud() {
        if (readOnly) return false;
        config.enabled = !config.enabled;
        if (persist()) return true;
        config.enabled = !config.enabled;
        return false;
    }

    private boolean persist() {
        try {
            config.save(file);
            error = "";
            return true;
        } catch (IOException exception) {
            error = "Could not save. Change reverted; try again.";
            return false;
        }
    }
}
