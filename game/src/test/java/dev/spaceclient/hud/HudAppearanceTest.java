package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class HudAppearanceTest {
    @Rule public TemporaryFolder dir = new TemporaryFolder();
    private Path file() { return dir.getRoot().toPath().resolve("hud.properties"); }
    private HudAppearance configured() {
        return new HudAppearance(HudAppearance.Color.AQUA, HudAppearance.Backdrop.OFF, false);
    }
    private void assertDefault(HudAppearance appearance) {
        assertEquals(HudAppearance.Color.AUTO, appearance.color);
        assertEquals(HudAppearance.Backdrop.STRONG, appearance.backdrop);
        assertTrue(appearance.shadow);
    }
    private void assertConfigured(HudAppearance appearance) {
        assertEquals(HudAppearance.Color.AQUA, appearance.color);
        assertEquals(HudAppearance.Backdrop.OFF, appearance.backdrop);
        assertFalse(appearance.shadow);
    }
    @Test public void appearancePersistsByModuleAndProfileAndExports() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.appearance(HudModule.FPS, configured()));
        assertDefault(settings.getConfig().profile(0).appearance(HudModule.CPS));
        assertDefault(settings.getConfig().profile(1).appearance(HudModule.FPS));
        assertTrue(settings.selectProfile(1));
        assertTrue(settings.appearance(HudModule.CPS, configured()));
        HudConfig saved = HudConfig.load(file());
        assertConfigured(saved.profile(0).appearance(HudModule.FPS));
        assertDefault(saved.profile(1).appearance(HudModule.FPS));
        assertConfigured(saved.profile(1).appearance(HudModule.CPS));
        assertDefault(saved.profile(0).appearance(HudModule.CPS));
        Path exported = dir.getRoot().toPath().resolve("profile.properties");
        HudConfig.exportProfile(exported, saved.profile(0));
        assertConfigured(HudConfig.importProfile(exported).appearance(HudModule.FPS));
        assertDefault(HudConfig.importProfile(exported).appearance(HudModule.CPS));
    }
    @Test public void placementAndAppearanceResetsAreIndependent() throws Exception {
        HudSettings settings = HudSettings.open(file());
        HudPlacement moved = new HudPlacement(HudPlacement.Horizontal.RIGHT, HudPlacement.Vertical.BOTTOM, 24, 36, 1.5);
        assertTrue(settings.place(HudModule.FPS, moved));
        assertTrue(settings.appearance(HudModule.FPS, configured()));
        assertTrue(settings.reset(HudModule.FPS));
        assertConfigured(settings.getConfig().active().appearance(HudModule.FPS));
        assertTrue(settings.place(HudModule.FPS, moved));
        assertTrue(settings.resetLayout());
        assertConfigured(settings.getConfig().active().appearance(HudModule.FPS));
        assertTrue(settings.place(HudModule.FPS, moved));
        assertTrue(settings.appearance(HudModule.FPS, HudAppearance.defaults()));
        HudPlacement actual = HudConfig.load(file()).active().placement(HudModule.FPS);
        assertEquals(moved.horizontal, actual.horizontal);
        assertEquals(moved.vertical, actual.vertical);
        assertEquals(moved.x, actual.x, 0);
        assertEquals(moved.y, actual.y, 0);
        assertEquals(moved.scale, actual.scale, 0);
    }
    @Test public void schemaSixIsBackedUpWithoutChangingFileUntilFirstSave() throws Exception {
        byte[] old = "schemaVersion=6\nprofile.0.fps=false\nprofile.1.cps=false\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file(), old);
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.isReadOnly());
        assertArrayEquals(old, Files.readAllBytes(file()));
        try (Stream<Path> paths = Files.list(dir.getRoot().toPath())) {
            Path backup = paths.filter(p -> p.getFileName().toString().startsWith("hud.properties.v6-")).findFirst().get();
            assertArrayEquals(old, Files.readAllBytes(backup));
        }
        assertFalse(settings.getConfig().profile(0).fps);
        assertFalse(settings.getConfig().profile(1).cps);
        for (HudModule module : HudModule.values()) {
            assertDefault(settings.getConfig().profile(0).appearance(module));
            assertDefault(settings.getConfig().profile(1).appearance(module));
        }
        assertTrue(settings.appearance(HudModule.FPS, configured()));
        assertConfigured(HudConfig.load(file()).profile(0).appearance(HudModule.FPS));
        Properties properties = new Properties();
        try (java.io.InputStream in = Files.newInputStream(file())) { properties.load(in); }
        assertEquals("8", properties.getProperty("schemaVersion"));
    }
    @Test public void failedAppearanceWriteRestoresSnapshot() throws Exception {
        HudSettings settings = HudSettings.open(file());
        Files.createDirectory(file());
        Files.write(file().resolve("obstacle"), new byte[] {1});
        assertFalse(settings.appearance(HudModule.FPS, configured()));
        assertDefault(settings.getConfig().profile(0).appearance(HudModule.FPS));
        assertDefault(settings.getConfig().profile(1).appearance(HudModule.FPS));
    }
    @Test public void versionTwoImportsRejectMissingOrInvalidAppearanceWithoutChangingProfiles() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.appearance(HudModule.FPS, configured()));
        byte[] original = Files.readAllBytes(file());
        Path imported = file().resolveSibling("spaceclient-hud-import.properties");
        HudConfig.exportProfile(imported, settings.getConfig().active());
        Properties properties = new Properties();
        try (java.io.InputStream in = Files.newInputStream(imported)) { properties.load(in); }
        properties.setProperty("schemaVersion", "2");
        properties.remove("content.coordinates.precision");
        properties.remove("content.clock.format");
        properties.remove("content.clock.seconds");
        properties.remove("content.fps.label");
        properties.remove("content.cps.mode");
        for (String invalid : new String[] {"missing", "invalid-color", "invalid-shadow"}) {
            Properties broken = new Properties(); broken.putAll(properties);
            if (invalid.equals("missing")) broken.remove("appearance.fps.backdrop");
            else if (invalid.equals("invalid-color")) broken.setProperty("appearance.fps.color", "PINK");
            else broken.setProperty("appearance.fps.shadow", "yes");
            try (java.io.OutputStream out = Files.newOutputStream(imported)) { broken.store(out, "invalid"); }
            assertFalse(settings.importProfile());
            assertArrayEquals(original, Files.readAllBytes(file()));
            assertConfigured(settings.getConfig().profile(0).appearance(HudModule.FPS));
            assertDefault(settings.getConfig().profile(1).appearance(HudModule.FPS));
        }
    }
    @Test public void schemaSevenRejectsMissingOrInvalidAppearanceInsteadOfSilentlyResetting() throws Exception {
        HudConfig config = new HudConfig(); config.save(file());
        Properties properties = new Properties();
        try (java.io.InputStream in = Files.newInputStream(file())) { properties.load(in); }
        toSchemaSeven(properties);
        for (String invalid : new String[] {"missing", "invalid-backdrop", "invalid-shadow"}) {
            Properties broken = new Properties(); broken.putAll(properties);
            if (invalid.equals("missing")) broken.remove("profile.0.appearance.fps.color");
            else if (invalid.equals("invalid-backdrop")) broken.setProperty("profile.0.appearance.fps.backdrop", "UNKNOWN");
            else broken.setProperty("profile.0.appearance.fps.shadow", "yes");
            try (java.io.OutputStream out = Files.newOutputStream(file())) { broken.store(out, "invalid"); }
            try { HudConfig.load(file()); fail("Invalid appearance must fail validation"); }
            catch (HudConfig.InvalidConfigException expected) { }
        }
    }
    @Test public void schemaSevenMissingCoreFieldsIsRejectedWithoutRewriting() throws Exception {
        HudConfig config = new HudConfig(); config.save(file());
        Properties properties = new Properties();
        try (java.io.InputStream in = Files.newInputStream(file())) { properties.load(in); }
        toSchemaSeven(properties);
        for (String key : new String[] {"activeProfile", "profile.0.fps", "profile.1.name", "profile.0.layout.fps.x"}) {
            Properties broken = new Properties(); broken.putAll(properties); broken.remove(key);
            try (java.io.OutputStream out = Files.newOutputStream(file())) { broken.store(out, "missing"); }
            byte[] bytes = Files.readAllBytes(file());
            try { HudConfig.load(file()); fail("Missing " + key + " must fail validation"); }
            catch (HudConfig.InvalidConfigException expected) { }
            assertArrayEquals(bytes, Files.readAllBytes(file()));
        }
    }

    private static void toSchemaSeven(Properties properties) {
        properties.setProperty("schemaVersion", "7");
        for (int i = 0; i < 2; i++) {
            properties.remove("profile." + i + ".content.coordinates.precision");
            properties.remove("profile." + i + ".content.clock.format");
            properties.remove("profile." + i + ".content.clock.seconds");
            properties.remove("profile." + i + ".content.fps.label");
            properties.remove("profile." + i + ".content.cps.mode");
        }
    }
}
