package dev.spaceclient;

import dev.spaceclient.hud.HudModule;
import dev.spaceclient.hud.HudPlacement;
import dev.spaceclient.hud.HudProfile;
import dev.spaceclient.hud.HudSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class HudProfilesTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();
    private Path file() { return directory.getRoot().toPath().resolve("hud.properties"); }
    private HudPlacement corner() {
        return new HudPlacement(HudPlacement.Horizontal.RIGHT, HudPlacement.Vertical.BOTTOM, 12, 18, 1.8);
    }

    @Test public void invalidNamesDoNotReplaceTheSavedName() throws Exception {
        HudSettings settings = HudSettings.open(file());
        for (String name : new String[] {"", " ", " padded", "a\nb", "\u00a7red", "\u200b", "123456789012345678901234567890123"}) {
            assertFalse(settings.renameProfile(name));
            assertEquals("Profile 1", settings.getConfig().active().name);
        }
        assertTrue(settings.renameProfile("Орбита 1"));
        assertEquals("Орбита 1", HudConfig.load(file()).active().name);
    }

    @Test public void legacyMigrationKeepsBothPresetsIndependentAndBacksUpBeforeAnyUpgrade() throws Exception {
        byte[] original = "schemaVersion=1\nenabled=false\ncps=false\nfps=false\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file(), original);
        HudSettings settings = HudSettings.open(file());
        assertArrayEquals(original, Files.readAllBytes(file()));
        List<Path> backups;
        try (Stream<Path> files = Files.list(directory.getRoot().toPath())) {
            backups = files.filter(p -> p.toString().endsWith(".bak")).collect(Collectors.toList());
        }
        assertEquals(1, backups.size());
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
        assertFalse(settings.getConfig().profile(0).cps);
        assertFalse(settings.getConfig().profile(1).cps);
        assertFalse(settings.getConfig().profile(0).compass);
        assertFalse(settings.getConfig().profile(1).compass);
        assertTrue(settings.place(HudModule.CPS, corner()));
        assertTrue(settings.toggle(HudModule.CPS));
        assertFalse(settings.getConfig().profile(1).cps);
        assertEquals(1, settings.getConfig().profile(1).placement(HudModule.CPS).scale, 0);
        Properties properties = new Properties();
        try (java.io.InputStream input = Files.newInputStream(file())) { properties.load(input); }
        assertEquals("8", properties.getProperty("schemaVersion"));
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
    }
    @Test public void compassDefaultsOffInExistingProfilesAndPersistsIndependently() throws Exception {
        Files.write(file(), "schemaVersion=2\nprofile.0.fps=false\nprofile.1.cps=false\n".getBytes(StandardCharsets.ISO_8859_1));
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.getConfig().profile(0).compass);
        assertFalse(settings.getConfig().profile(1).compass);
        assertTrue(settings.toggle(HudModule.COMPASS));
        assertTrue(settings.place(HudModule.COMPASS, corner()));
        HudConfig reloaded = HudConfig.load(file());
        assertTrue(reloaded.profile(0).compass);
        assertFalse(reloaded.profile(1).compass);
        assertFalse(reloaded.profile(0).fps);
        assertFalse(reloaded.profile(1).cps);
        assertEquals(1.8, reloaded.profile(0).placement(HudModule.COMPASS).scale, 0);
        assertEquals(1.0, reloaded.profile(1).placement(HudModule.COMPASS).scale, 0);
        assertTrue(reloaded.copy().profile(0).compass);
    }
    @Test public void compassExportAndLegacyImportRemainStrict() throws Exception {
        HudConfig config = new HudConfig();
        config.active().compass = true;
        config.active().place(HudModule.COMPASS, corner());
        Path exported = directory.getRoot().toPath().resolve("export.properties");
        HudConfig.exportProfile(exported, config.active());
        HudProfile reloaded = HudConfig.importProfile(exported);
        assertTrue(reloaded.compass);
        assertEquals(1.8, reloaded.placement(HudModule.COMPASS).scale, 0);

        Properties legacy = new Properties();
        try (java.io.InputStream in = Files.newInputStream(exported)) { legacy.load(in); }
        // Reconstruct the last version-1 export before stepping back through older shapes.
        legacy.setProperty("schemaVersion", "1");
        legacy.remove("content.coordinates.precision");
        legacy.remove("content.clock.format");
        legacy.remove("content.clock.seconds");
        legacy.remove("content.fps.label");
        legacy.remove("content.cps.mode");
        for (HudModule module : HudModule.values()) for (String field : new String[] {"color", "backdrop", "shadow"})
            legacy.remove("appearance." + module.getId() + "." + field);
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { legacy.store(out, "Schema 1 export"); }
        assertTrue(HudConfig.importProfile(exported).compass);
        for (String module : new String[] {"armor", "effects"}) {
            legacy.remove(module);
            for (String field : new String[] {"horizontal", "vertical", "x", "y", "scale"})
                legacy.remove("layout." + module + "." + field);
        }
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { legacy.store(out, "Vitals-era export"); }
        HudProfile vitals = HudConfig.importProfile(exported);
        assertTrue(vitals.compass);
        assertFalse(vitals.armor);
        assertFalse(vitals.effects);
        for (String module : new String[] {"health", "hunger"}) {
            legacy.remove(module);
            for (String field : new String[] {"horizontal", "vertical", "x", "y", "scale"})
                legacy.remove("layout." + module + "." + field);
        }
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { legacy.store(out, "Metrics-era export"); }
        HudProfile metrics = HudConfig.importProfile(exported);
        assertTrue(metrics.compass);
        assertFalse(metrics.health);
        assertFalse(metrics.hunger);
        for (String module : new String[] {"clock", "speed"}) {
            legacy.remove(module);
            for (String field : new String[] {"horizontal", "vertical", "x", "y", "scale"})
                legacy.remove("layout." + module + "." + field);
        }
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { legacy.store(out, "Compass-era export"); }
        assertTrue(HudConfig.importProfile(exported).compass);
        assertFalse(HudConfig.importProfile(exported).clock);
        assertFalse(HudConfig.importProfile(exported).speed);
        legacy.remove("compass");
        for (String field : new String[] {"horizontal", "vertical", "x", "y", "scale"})
            legacy.remove("layout.compass." + field);
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { legacy.store(out, "Legacy export"); }
        assertFalse(HudConfig.importProfile(exported).compass);

        legacy.setProperty("compass", "true"); // A partial new extension is not a valid legacy or new export.
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { legacy.store(out, "Partial export"); }
        try { HudConfig.importProfile(exported); fail("Partial compass fields must be rejected"); }
        catch (HudConfig.InvalidConfigException expected) { }
        legacy.remove("compass");
        legacy.setProperty("unknown", "value");
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { legacy.store(out, "Unknown field"); }
        try { HudConfig.importProfile(exported); fail("Unknown fields must be rejected"); }
        catch (HudConfig.InvalidConfigException expected) { }
    }
    @Test public void clockAndSpeedPersistPerProfileAndRejectPartialExports() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.getConfig().profile(0).clock);
        assertFalse(settings.getConfig().profile(0).speed);
        assertTrue(settings.toggle(HudModule.CLOCK));
        assertTrue(settings.place(HudModule.CLOCK, corner()));
        assertTrue(settings.selectProfile(1));
        assertTrue(settings.toggle(HudModule.SPEED));
        HudConfig loaded = HudConfig.load(file());
        assertTrue(loaded.profile(0).clock);
        assertFalse(loaded.profile(0).speed);
        assertFalse(loaded.profile(1).clock);
        assertTrue(loaded.profile(1).speed);
        assertEquals(1.8, loaded.profile(0).placement(HudModule.CLOCK).scale, 0);
        Path exported = directory.getRoot().toPath().resolve("new-export.properties");
        HudConfig.exportProfile(exported, loaded.profile(1));
        assertTrue(HudConfig.importProfile(exported).speed);
        Properties partial = new Properties();
        try (java.io.InputStream in = Files.newInputStream(exported)) { partial.load(in); }
        partial.remove("layout.speed.x");
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { partial.store(out, "Partial"); }
        try { HudConfig.importProfile(exported); fail("Partial speed fields must be rejected"); }
        catch (HudConfig.InvalidConfigException expected) { }
    }
    @Test public void healthAndHungerPersistIndependentlyAndPartialImportsAreRejected() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.getConfig().profile(0).health);
        assertFalse(settings.getConfig().profile(1).hunger);
        assertTrue(settings.toggle(HudModule.HEALTH));
        assertTrue(settings.place(HudModule.HEALTH, corner()));
        assertTrue(settings.selectProfile(1));
        assertTrue(settings.toggle(HudModule.HUNGER));
        HudConfig loaded = HudConfig.load(file());
        assertTrue(loaded.profile(0).health);
        assertFalse(loaded.profile(0).hunger);
        assertFalse(loaded.profile(1).health);
        assertTrue(loaded.profile(1).hunger);
        assertEquals(1.8, loaded.profile(0).placement(HudModule.HEALTH).scale, 0);
        assertTrue(loaded.copy().profile(1).hunger);
        Path exported = directory.getRoot().toPath().resolve("status-export.properties");
        HudConfig.exportProfile(exported, loaded.profile(1));
        assertTrue(HudConfig.importProfile(exported).hunger);
        Properties partial = new Properties();
        try (java.io.InputStream in = Files.newInputStream(exported)) { partial.load(in); }
        partial.remove("layout.hunger.scale");
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { partial.store(out, "Partial"); }
        try { HudConfig.importProfile(exported); fail("Partial hunger fields must be rejected"); }
        catch (HudConfig.InvalidConfigException expected) { }
    }
    @Test public void armorAndEffectsAreIndependentAndPartialExportsAreRejected() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.getConfig().active().armor);
        assertFalse(settings.getConfig().active().effects);
        assertTrue(settings.toggle(HudModule.ARMOR));
        assertTrue(settings.place(HudModule.ARMOR, corner()));
        assertTrue(settings.selectProfile(1));
        assertTrue(settings.toggle(HudModule.POTION_EFFECTS));
        HudConfig loaded = HudConfig.load(file());
        assertTrue(loaded.profile(0).armor);
        assertFalse(loaded.profile(0).effects);
        assertFalse(loaded.profile(1).armor);
        assertTrue(loaded.profile(1).effects);
        assertEquals(1.8, loaded.profile(0).placement(HudModule.ARMOR).scale, 0);
        Path exported = directory.getRoot().toPath().resolve("effects-export.properties");
        HudConfig.exportProfile(exported, loaded.profile(1));
        assertTrue(HudConfig.importProfile(exported).effects);
        Properties partial = new Properties();
        try (java.io.InputStream in = Files.newInputStream(exported)) { partial.load(in); }
        partial.remove("layout.effects.x");
        try (java.io.OutputStream out = Files.newOutputStream(exported)) { partial.store(out, "Partial"); }
        try { HudConfig.importProfile(exported); fail("Partial effects fields must be rejected"); }
        catch (HudConfig.InvalidConfigException expected) { }
    }
    @Test public void profilesFlagsNamesLayoutsAndSelectionSurviveRestartIndependently() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.renameProfile("Орбита A"));
        assertTrue(settings.place(HudModule.COORDINATES, corner()));
        assertTrue(settings.toggle(HudModule.FPS));
        assertTrue(settings.selectProfile(1));
        assertTrue(settings.renameProfile("Орбита B"));
        assertTrue(settings.toggleHud());
        HudConfig loaded = HudConfig.load(file());
        assertEquals(1, loaded.activeIndex());
        assertFalse(loaded.active().enabled);
        assertTrue(loaded.active().fps);
        assertEquals("Орбита B", loaded.active().name);
        loaded.select(0);
        assertTrue(loaded.active().enabled);
        assertFalse(loaded.active().fps);
        assertEquals("Орбита A", loaded.active().name);
        assertEquals(1.8, loaded.active().placement(HudModule.COORDINATES).scale, 0);
    }
    @Test public void failedSelectionRenameAndPlacementRestoreTheWholeSnapshot() throws Exception {
        HudSettings settings = HudSettings.open(file());
        Files.createDirectory(file());
        Files.write(file().resolve("obstacle"), new byte[] {1});
        assertFalse(settings.selectProfile(1));
        assertEquals(0, settings.getConfig().activeIndex());
        assertFalse(settings.renameProfile("Changed"));
        assertFalse(settings.place(HudModule.FPS, corner()));
        assertEquals("Profile 1", settings.getConfig().active().name);
        assertEquals(1, settings.getConfig().active().placement(HudModule.FPS).scale, 0);
        assertEquals("Profile 2", settings.getConfig().profile(1).name);
    }
    @Test public void selectedProfileImportAndExportDoNotTouchTheOtherPreset() throws Exception {
        HudSettings settings = HudSettings.open(file());
        settings.renameProfile("Exported");
        settings.place(HudModule.FPS, corner());
        settings.toggle(HudModule.CPS);
        assertTrue(settings.exportProfile());
        Path exported = file().resolveSibling("spaceclient-hud-export.properties");
        Files.copy(exported, file().resolveSibling("spaceclient-hud-import.properties"));
        settings.selectProfile(1);
        assertTrue(settings.importProfile());
        assertEquals("Exported", settings.getConfig().active().name);
        assertFalse(settings.getConfig().active().cps);
        settings.renameProfile("Imported copy");
        settings.resetLayout();
        HudConfig loaded = HudConfig.load(file());
        assertEquals(1, loaded.active().placement(HudModule.FPS).scale, 0);
        assertEquals(1.8, loaded.profile(0).placement(HudModule.FPS).scale, 0);
        assertEquals("Exported", loaded.profile(0).name);
    }
    @Test public void malformedMissingUnknownAndOversizedImportsPreserveBothProfilesAndConfigBytes() throws Exception {
        HudSettings settings = HudSettings.open(file());
        settings.renameProfile("Keep me");
        byte[] original = Files.readAllBytes(file());
        Path imported = file().resolveSibling("spaceclient-hud-import.properties");
        assertFalse(settings.importProfile());
        for (String data : new String[] {
            "format=spaceclient-hud-profile\nschemaVersion=2\n",
            "format=spaceclient-hud-profile\nschemaVersion=1\n",
            "name=\\uZZZZ\n"
        }) {
            Files.write(imported, data.getBytes(StandardCharsets.ISO_8859_1));
            assertFalse(settings.importProfile());
            assertArrayEquals(original, Files.readAllBytes(file()));
        }
        HudConfig.exportProfile(imported, settings.getConfig().active());
        Files.write(imported, "unknown=value\n".getBytes(StandardCharsets.ISO_8859_1), java.nio.file.StandardOpenOption.APPEND);
        assertFalse(settings.importProfile());
        Files.write(imported, new byte[HudConfig.MAX_BYTES + 1]);
        assertFalse(settings.importProfile());
        assertEquals("Keep me", settings.getConfig().active().name);
        assertEquals("Profile 2", settings.getConfig().profile(1).name);
        assertArrayEquals(original, Files.readAllBytes(file()));
    }
    @Test public void invalidPlacementsAndNamesRecoverWhileFutureSchemaRemainsReadOnly() throws Exception {
        for (String data : new String[] {
            "schemaVersion=2\nprofile.0.layout.fps.scale=NaN\n",
            "schemaVersion=2\nprofile.1.layout.cps.x=-1\n",
            "schemaVersion=2\nprofile.0.compass=perhaps\n",
            "schemaVersion=2\nprofile.0.name=\\u00a7red\n",
            "schemaVersion=2\nactiveProfile=8\n"
        }) {
            Files.write(file(), data.getBytes(StandardCharsets.ISO_8859_1));
            HudSettings recovered = HudSettings.open(file());
            assertFalse(recovered.isReadOnly());
            assertTrue(recovered.getNotice().contains("backed up"));
        }
        byte[] future = "schemaVersion=9\nkeep=true\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file(), future);
        HudSettings readOnly = HudSettings.open(file());
        assertTrue(readOnly.isReadOnly());
        assertFalse(readOnly.selectProfile(1));
        assertFalse(readOnly.place(HudModule.FPS, corner()));
        assertFalse(readOnly.importProfile());
        assertFalse(readOnly.exportProfile());
        assertArrayEquals(future, Files.readAllBytes(file()));
    }
}
