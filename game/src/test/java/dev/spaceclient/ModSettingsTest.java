package dev.spaceclient;

import dev.spaceclient.mods.ModSettings;
import dev.spaceclient.mods.CrosshairStyle;
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
    @Test public void legacySettingsKeepBothModulesDuringMigration() throws Exception {
        Path file = Files.createTempFile("space-mods-v1", ".properties");
        byte[] original = "schemaVersion=1\nautoSprint=true\nfreeLook=true\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);
        ModSettings s = ModSettings.open(file);
        assertFalse(s.isReadOnly()); assertTrue(s.autoSprint()); assertTrue(s.freeLook());
        assertArrayEquals(original, Files.readAllBytes(file));
        assertTrue(s.toggleZoom());
        ModSettings restored = ModSettings.open(file);
        assertFalse(restored.isReadOnly()); assertTrue(restored.autoSprint()); assertTrue(restored.freeLook()); assertTrue(restored.zoom());
        assertFalse(restored.crosshair()); assertEquals(ModSettings.SprintMode.AUTO, restored.sprintMode());
    }
    @Test public void newOptionsPersistAndFailuresKeepPreviousValues() throws Exception {
        Path file = Files.createTempDirectory("space-mods-v2").resolve("mods.properties");
        ModSettings s = ModSettings.open(file);
        assertTrue(s.sprintMode(ModSettings.SprintMode.TOGGLE)); assertTrue(s.freeLookKey(48)); assertTrue(s.zoomKey(34));
        assertTrue(s.zoomFactor(8)); assertTrue(s.zoomSmooth(false)); assertTrue(s.toggleCrosshair());
        assertTrue(s.crosshairStyle(CrosshairStyle.defaults().withShape(CrosshairStyle.Shape.RING).withColor(CrosshairStyle.Color.AQUA).withSize(11).withGap(0).withThickness(3).withDot(true).withOutline(false)));
        ModSettings r = ModSettings.open(file);
        assertFalse(r.isReadOnly()); assertEquals(8,r.zoomFactor()); assertFalse(r.zoomSmooth());
        assertEquals(48,r.freeLookKey()); assertEquals(34,r.zoomKey()); assertEquals(ModSettings.SprintMode.TOGGLE,r.sprintMode());
        assertTrue(r.crosshair()); assertEquals(CrosshairStyle.Shape.RING,r.crosshairStyle().shape);
        assertEquals(11,r.crosshairStyle().size); assertTrue(r.crosshairStyle().dot); assertFalse(r.crosshairStyle().outline);
        byte[] saved=Files.readAllBytes(file);
        assertFalse(r.zoomFactor(99)); assertFalse(r.freeLookKey(-1));
        assertArrayEquals(saved,Files.readAllBytes(file)); assertEquals(8,r.zoomFactor());
    }
}
