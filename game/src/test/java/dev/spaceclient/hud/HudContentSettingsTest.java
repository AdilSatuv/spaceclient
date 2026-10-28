package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class HudContentSettingsTest {
    @Rule public TemporaryFolder dir = new TemporaryFolder();
    private Path file() { return dir.getRoot().toPath().resolve("hud.properties"); }
    private HudContentSettings custom() { return new HudContentSettings(2, HudContentSettings.ClockFormat.H12, false).withFpsLabel(false).withCpsMode(HudContentSettings.CpsMode.RIGHT); }
    private void assertDefault(HudContentSettings c) {
        assertEquals(1, c.coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H24, c.clockFormat);
        assertTrue(c.clockSeconds);
        assertTrue(c.fpsLabel);
        assertEquals(HudContentSettings.CpsMode.BOTH, c.cpsMode);
    }
    private void assertCustom(HudContentSettings c) {
        assertEquals(2, c.coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H12, c.clockFormat);
        assertFalse(c.clockSeconds);
        assertFalse(c.fpsLabel);
        assertEquals(HudContentSettings.CpsMode.RIGHT, c.cpsMode);
    }
    private static Properties read(Path file) throws Exception {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        return p;
    }
    private static void write(Path file, Properties p) throws Exception {
        try (OutputStream out = Files.newOutputStream(file)) { p.store(out, "test"); }
    }
    private static void removeContent(Properties p, String prefix) {
        p.remove(prefix + "content.coordinates.precision");
        p.remove(prefix + "content.clock.format");
        p.remove(prefix + "content.clock.seconds");
        p.remove(prefix + "content.fps.label");
        p.remove(prefix + "content.cps.mode");
    }
    @Test public void changingOneModuleFormatPreservesAllOtherFormats() {
        HudContentSettings c = custom();
        HudContentSettings changedCoordinates = c.withCoordinatesPrecision(0);
        assertEquals(HudContentSettings.ClockFormat.H12, changedCoordinates.clockFormat);
        assertFalse(changedCoordinates.clockSeconds);
        assertFalse(changedCoordinates.fpsLabel);
        assertEquals(HudContentSettings.CpsMode.RIGHT, changedCoordinates.cpsMode);
        HudContentSettings changedClock = c.withClockFormat(HudContentSettings.ClockFormat.H24).withClockSeconds(true);
        assertEquals(2, changedClock.coordinatesPrecision);
        assertFalse(changedClock.fpsLabel);
        assertEquals(HudContentSettings.CpsMode.RIGHT, changedClock.cpsMode);
        HudContentSettings changedFps = c.withFpsLabel(true);
        assertEquals(2, changedFps.coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H12, changedFps.clockFormat);
        assertFalse(changedFps.clockSeconds);
        assertEquals(HudContentSettings.CpsMode.RIGHT, changedFps.cpsMode);
        HudContentSettings changedCps = c.withCpsMode(HudContentSettings.CpsMode.LEFT);
        assertEquals(2, changedCps.coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H12, changedCps.clockFormat);
        assertFalse(changedCps.clockSeconds);
        assertFalse(changedCps.fpsLabel);
        assertTrue(new HudContentSettings(1, HudContentSettings.ClockFormat.H24, true).fpsLabel);
        assertEquals(HudContentSettings.CpsMode.BOTH, new HudContentSettings(1, HudContentSettings.ClockFormat.H24, true).cpsMode);
    }
    @Test public void contentIsIndependentAcrossProfilesAndSurvivesRestartAndExport() throws Exception {
        HudSettings s = HudSettings.open(file());
        assertTrue(s.content(custom()));
        assertDefault(s.getConfig().profile(1).content());
        assertTrue(s.selectProfile(1));
        assertTrue(s.content(HudContentSettings.defaults().withCoordinatesPrecision(0)));
        HudConfig saved = HudConfig.load(file());
        assertCustom(saved.profile(0).content());
        assertEquals(0, saved.profile(1).content().coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H24, saved.profile(1).content().clockFormat);
        Path exported = dir.getRoot().toPath().resolve("export.properties");
        HudConfig.exportProfile(exported, saved.profile(0));
        assertEquals("3", read(exported).getProperty("schemaVersion"));
        assertCustom(HudConfig.importProfile(exported).content());
    }
    @Test public void resettingOneModuleContentKeepsTheOtherModuleSettings() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.content(custom()));
        assertTrue(settings.content(settings.getConfig().active().content().withCoordinatesPrecision(1)));
        HudContentSettings afterCoordinatesReset = HudConfig.load(file()).active().content();
        assertEquals(1, afterCoordinatesReset.coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H12, afterCoordinatesReset.clockFormat);
        assertFalse(afterCoordinatesReset.clockSeconds);
        assertTrue(settings.content(custom()));
        HudContentSettings resetClock = settings.getConfig().active().content()
            .withClockFormat(HudContentSettings.ClockFormat.H24).withClockSeconds(true);
        assertTrue(settings.content(resetClock));
        HudContentSettings afterClockReset = HudConfig.load(file()).active().content();
        assertEquals(2, afterClockReset.coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H24, afterClockReset.clockFormat);
        assertTrue(afterClockReset.clockSeconds);
    }
    @Test public void schemaSevenMigrationPreservesAppearanceAndMakesVerifiedBackup() throws Exception {
        HudConfig cfg = new HudConfig();
        cfg.profile(0).setAppearance(HudModule.FPS, new HudAppearance(HudAppearance.Color.AQUA, HudAppearance.Backdrop.SOFT, false));
        cfg.save(file());
        Properties old = read(file()); old.setProperty("schemaVersion", "7");
        removeContent(old, "profile.0."); removeContent(old, "profile.1."); write(file(), old);
        byte[] original = Files.readAllBytes(file());
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.isReadOnly());
        assertArrayEquals(original, Files.readAllBytes(file()));
        try (Stream<Path> paths = Files.list(dir.getRoot().toPath())) {
            Path backup = paths.filter(p -> p.getFileName().toString().startsWith("hud.properties.v7-")).findFirst().get();
            assertArrayEquals(original, Files.readAllBytes(backup));
        }
        assertDefault(settings.getConfig().profile(0).content());
        assertDefault(settings.getConfig().profile(1).content());
        assertEquals(HudAppearance.Color.AQUA, settings.getConfig().profile(0).appearance(HudModule.FPS).color);
        assertTrue(settings.content(custom()));
        assertEquals("8", read(file()).getProperty("schemaVersion"));
        assertCustom(HudConfig.load(file()).profile(0).content());
    }
    @Test public void versionTwoProfileImportKeepsAppearanceAndDefaultsNewSettings() throws Exception {
        HudConfig cfg = new HudConfig();
        cfg.active().setAppearance(HudModule.FPS, new HudAppearance(HudAppearance.Color.GOLD, HudAppearance.Backdrop.OFF, false));
        Path exported = dir.getRoot().toPath().resolve("profile.properties");
        HudConfig.exportProfile(exported, cfg.active());
        Properties old = read(exported); old.setProperty("schemaVersion", "2"); removeContent(old, ""); write(exported, old);
        HudProfile imported = HudConfig.importProfile(exported);
        assertDefault(imported.content());
        assertEquals(HudAppearance.Color.GOLD, imported.appearance(HudModule.FPS).color);
        old.remove("appearance.fps.shadow"); write(exported, old);
        try { HudConfig.importProfile(exported); fail("Partial v2 must fail"); }
        catch (HudConfig.InvalidConfigException expected) { }
    }
    @Test public void invalidOrMissingFieldsAreRejectedAndFutureSchemaIsReadOnly() throws Exception {
        new HudConfig().save(file());
        Properties complete = read(file());
        for (String key : new String[] {"content.coordinates.precision", "content.clock.format", "content.clock.seconds", "content.fps.label", "content.cps.mode"}) {
            Properties missing = new Properties(); missing.putAll(complete); missing.remove("profile.0." + key); write(file(), missing);
            try { HudConfig.load(file()); fail("Missing " + key); } catch (HudConfig.InvalidConfigException expected) { }
        }
        for (String[] entry : new String[][] {{"content.coordinates.precision", "-1"}, {"content.coordinates.precision", "3"},
                {"content.coordinates.precision", "1.5"}, {"content.clock.format", "H25"}, {"content.clock.seconds", "yes"},
                {"content.fps.label", "yes"}, {"content.cps.mode", "MIDDLE"}}) {
            Properties bad = new Properties(); bad.putAll(complete); bad.setProperty("profile.0." + entry[0], entry[1]); write(file(), bad);
            try { HudConfig.load(file()); fail("Invalid " + entry[0]); } catch (HudConfig.InvalidConfigException expected) { }
        }
        byte[] future = "schemaVersion=9\nkeep=true\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        Files.write(file(), future); HudSettings readOnly = HudSettings.open(file());
        assertTrue(readOnly.isReadOnly()); assertFalse(readOnly.content(custom())); assertArrayEquals(future, Files.readAllBytes(file()));
    }
    @Test public void brokenVersionThreeImportLeavesBothProfilesAndSavedBytesUntouched() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.content(custom()));
        assertTrue(settings.selectProfile(1));
        assertTrue(settings.content(HudContentSettings.defaults().withCoordinatesPrecision(0)));
        byte[] before = Files.readAllBytes(file());
        Path imported = file().resolveSibling("spaceclient-hud-import.properties");
        HudConfig.exportProfile(imported, settings.getConfig().profile(0));
        Properties complete = read(imported);
        for (String bad : new String[] {"missing", "range", "clock"}) {
            Properties broken = new Properties(); broken.putAll(complete);
            if (bad.equals("missing")) broken.remove("content.clock.seconds");
            else if (bad.equals("range")) broken.setProperty("content.coordinates.precision", "3");
            else broken.setProperty("content.clock.format", "invalid");
            write(imported, broken);
            assertFalse(settings.importProfile());
            assertArrayEquals(before, Files.readAllBytes(file()));
            assertCustom(settings.getConfig().profile(0).content());
            assertEquals(0, settings.getConfig().profile(1).content().coordinatesPrecision);
        }
    }
    @Test public void failedContentWriteRestoresBothProfiles() throws Exception {
        HudSettings s = HudSettings.open(file());
        Files.createDirectory(file()); Files.write(file().resolve("obstacle"), new byte[]{1});
        assertFalse(s.content(custom()));
        assertDefault(s.getConfig().profile(0).content()); assertDefault(s.getConfig().profile(1).content());
    }
    @Test public void constructorRejectsOutOfRangeAndNull() {
        for (int precision : new int[] {-1, 3}) {
            try { HudContentSettings.defaults().withCoordinatesPrecision(precision); fail(); }
            catch (IllegalArgumentException expected) { }
        }
        try { HudContentSettings.defaults().withClockFormat(null); fail(); }
        catch (IllegalArgumentException expected) { }
        try { HudContentSettings.defaults().withCpsMode(null); fail(); }
        catch (IllegalArgumentException expected) { }
    }
}
