package dev.spaceclient;

import dev.spaceclient.mods.ModSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModSettingsTest {
    @Test public void controlsAreOptInAndPersistIndependently() throws Exception {
        Path file = Files.createTempDirectory("space-mods").resolve("mods.properties");
        ModSettings settings = ModSettings.open(file);
        assertFalse(settings.autoSprint()); assertFalse(settings.freeLook());
        assertTrue(settings.toggleAutoSprint()); assertTrue(settings.toggleFreeLook());
        ModSettings reloaded = ModSettings.open(file);
        assertTrue(reloaded.autoSprint()); assertTrue(reloaded.freeLook());
        assertTrue(reloaded.toggleAutoSprint());
        assertFalse(ModSettings.open(file).autoSprint()); assertTrue(ModSettings.open(file).freeLook());
    }
    @Test public void unsupportedFileIsPreservedReadOnly() throws Exception {
        Path file = Files.createTempFile("space-mods-future", ".properties");
        byte[] original = "schemaVersion=2\nautoSprint=true\nfreeLook=true\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);
        ModSettings settings = ModSettings.open(file);
        assertTrue(settings.isReadOnly()); assertFalse(settings.autoSprint());
        assertFalse(settings.toggleFreeLook()); assertArrayEquals(original, Files.readAllBytes(file));
    }
    @Test public void failedWriteDoesNotChangeState() throws Exception {
        Path parent = Files.createTempFile("space-mods-parent", ".file");
        ModSettings settings = ModSettings.open(parent.resolve("settings.properties"));
        assertFalse(settings.toggleAutoSprint()); assertFalse(settings.autoSprint());
        assertFalse(settings.error().isEmpty());
    }
}
