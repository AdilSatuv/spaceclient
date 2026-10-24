package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Client-thread commands; every failed mutation restores the complete two-profile snapshot. */
public final class HudSettings {
    private final Path file;
    private final HudConfig config;
    private final boolean readOnly;
    private final String notice;
    private String actionNotice = "";
    private String error = "";

    private HudSettings(Path file, HudConfig config, boolean readOnly, String notice) {
        this.file = file; this.config = config; this.readOnly = readOnly; this.notice = notice;
    }
    public static HudSettings open(Path file) {
        try {
            HudConfig config = HudConfig.load(file);
            if (config.needsMigrationBackup()) {
                Path backup = file.resolveSibling(file.getFileName() + ".v1-" + UUID.randomUUID() + ".bak");
                try { Files.copy(file, backup); }
                catch (IOException exception) {
                    return new HudSettings(file, config, true, "Migration backup failed. File kept; read-only.");
                }
                return new HudSettings(file, config, false, "Version 1 loaded. Backup kept; next save upgrades.");
            }
            return new HudSettings(file, config, false, "");
        } catch (HudConfig.InvalidConfigException exception) {
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
    public String getNotice() { return actionNotice.isEmpty() ? notice : actionNotice; }
    public String getError() { return error; }

    public boolean toggle(HudModule module) {
        return change(() -> module.setEnabled(config, !module.isEnabled(config)));
    }
    public boolean toggleHud() { return change(() -> config.active().enabled = !config.active().enabled); }
    public boolean selectProfile(int index) { return change(() -> config.select(index)); }
    public boolean renameProfile(String name) {
        try { HudProfile.validName(name); }
        catch (IllegalArgumentException exception) { error = "Use 1-32 plain characters for the profile name."; return false; }
        return change(() -> config.active().name = name);
    }
    public boolean place(HudModule module, HudPlacement placement) {
        return change(() -> config.active().place(module, placement));
    }
    public boolean reset(HudModule module) { return place(module, HudPlacement.defaults(module)); }
    public boolean resetLayout() { return change(() -> config.active().resetLayout()); }
    public boolean exportProfile() {
        if (readOnly) return false;
        try {
            HudConfig.exportProfile(file.resolveSibling("spaceclient-hud-export.properties"), config.active());
            error = ""; actionNotice = "Exported: config/spaceclient-hud-export.properties";
            return true;
        } catch (IOException exception) { error = "Could not export profile. Existing file kept."; return false; }
    }
    public boolean importProfile() {
        if (readOnly) return false;
        final HudProfile imported;
        try { imported = HudConfig.importProfile(file.resolveSibling("spaceclient-hud-import.properties")); }
        catch (IOException exception) {
            error = "Need valid config/spaceclient-hud-import.properties";
            return false;
        }
        if (!change(() -> config.replaceActive(imported))) return false;
        actionNotice = "Imported into the selected profile only.";
        return true;
    }
    private boolean change(Runnable mutation) {
        if (readOnly) return false;
        HudConfig previous = config.copy();
        try {
            mutation.run();
            config.save(file);
            error = ""; actionNotice = "";
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            config.restore(previous);
            error = "Could not save. Change reverted; try again.";
            return false;
        }
    }
}
