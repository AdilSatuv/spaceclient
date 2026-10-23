package dev.spaceclient;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class HudConfigTest {
    @Test public void roundTripPreservesDisabledModules() throws Exception {
        Path file = Files.createTempDirectory("space-config").resolve("hud.properties");
        HudConfig original = new HudConfig();
        original.enabled = false;
        original.coordinates = false;
        original.cps = false;
        original.save(file);
        HudConfig restored = HudConfig.load(file);
        assertFalse(restored.enabled);
        assertFalse(restored.coordinates);
        assertFalse(restored.cps);
        assertTrue(restored.fps);
    }
    @Test public void existingSchemaDefaultsNewCpsSettingToEnabled() throws Exception {
        Path file = Files.createTempFile("space-legacy", ".properties");
        Files.write(file, "schemaVersion=1\nenabled=false\nfps=false\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HudConfig restored = HudConfig.load(file);
        assertFalse(restored.enabled);
        assertFalse(restored.fps);
        assertTrue(restored.cps);
    }
    @Test(expected = java.io.IOException.class) public void invalidBooleanIsRejected() throws Exception {
        Path file = Files.createTempFile("space-invalid", ".properties");
        Files.write(file, "enabled=perhaps".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HudConfig.load(file);
    }
    @Test(expected = java.io.IOException.class) public void futureSchemaIsRejected() throws Exception {
        Path file = Files.createTempFile("space-future", ".properties");
        Files.write(file, "schemaVersion=2".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HudConfig.load(file);
    }
}
