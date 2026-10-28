package dev.spaceclient;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class HudConfigTest {
    @Test public void roundTripPreservesDisabledModules() throws Exception {
        Path file = Files.createTempDirectory("space-config").resolve("hud.properties");
        HudConfig original = new HudConfig();
        original.active().enabled = false;
        original.active().coordinates = false;
        original.active().cps = false;
        original.save(file);
        HudConfig restored = HudConfig.load(file);
        assertFalse(restored.active().enabled);
        assertFalse(restored.active().coordinates);
        assertFalse(restored.active().cps);
        assertTrue(restored.active().fps);
    }
    @Test public void existingSchemaDefaultsNewCpsSettingToEnabled() throws Exception {
        Path file = Files.createTempFile("space-legacy", ".properties");
        Files.write(file, "schemaVersion=1\nenabled=false\nfps=false\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HudConfig restored = HudConfig.load(file);
        assertFalse(restored.active().enabled);
        assertFalse(restored.active().fps);
        assertTrue(restored.active().cps);
    }
    @Test(expected = java.io.IOException.class) public void invalidBooleanIsRejected() throws Exception {
        Path file = Files.createTempFile("space-invalid", ".properties");
        Files.write(file, "enabled=perhaps".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HudConfig.load(file);
    }
    @Test(expected = java.io.IOException.class) public void futureSchemaIsRejected() throws Exception {
        Path file = Files.createTempFile("space-future", ".properties");
        Files.write(file, "schemaVersion=9".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HudConfig.load(file);
    }
}
