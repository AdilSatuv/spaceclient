package dev.spaceclient;

import dev.spaceclient.hud.HudModule;
import dev.spaceclient.hud.HudSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class HudSettingsTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    private Path file() { return directory.getRoot().toPath().resolve("hud.properties"); }
    private void write(String text) throws Exception { Files.write(file(), text.getBytes(StandardCharsets.ISO_8859_1)); }

    @Test public void eachModuleCanBeDisabledIndependentlyAndSurvivesRestart() throws Exception {
        for (HudModule selected : HudModule.values()) {
            new HudConfig().save(file());
            HudSettings settings = HudSettings.open(file());
            assertTrue(settings.toggle(selected));
            HudSettings restarted = HudSettings.open(file());
            for (HudModule module : HudModule.values()) {
                boolean initiallyEnabled = module.isEnabled(new HudConfig());
                assertEquals(initiallyEnabled ^ (module == selected), module.isEnabled(restarted.getConfig()));
            }
            assertTrue(restarted.getConfig().active().enabled);
        }
    }

    @Test public void masterVisibilityPreservesModuleChoices() throws Exception {
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.toggle(HudModule.CPS));
        assertTrue(settings.toggleHud());
        HudSettings hidden = HudSettings.open(file());
        assertFalse(hidden.getConfig().active().enabled);
        assertFalse(hidden.getConfig().active().cps);
        assertTrue(hidden.toggleHud());
        assertFalse(HudSettings.open(file()).getConfig().active().cps);
    }

    @Test public void invalidConfigIsPreservedByteForByteAndCanRecover() throws Exception {
        write("enabled=perhaps\nfps=false\n");
        byte[] original = Files.readAllBytes(file());
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.isReadOnly());
        assertFalse(settings.getNotice().isEmpty());
        assertFalse(Files.exists(file()));
        List<Path> backups;
        try (Stream<Path> paths = Files.list(directory.getRoot().toPath())) {
            backups = paths.filter(path -> path.toString().endsWith(".bak")).collect(Collectors.toList());
        }
        assertEquals(1, backups.size());
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
        assertTrue(settings.toggle(HudModule.FPS));
        assertFalse(HudConfig.load(file()).active().fps);
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
    }

    @Test public void malformedUnicodeCannotCrashStartup() throws Exception {
        write("enabled=\\uZZZZ");
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.isReadOnly());
        assertTrue(settings.getConfig().active().enabled);
        assertTrue(settings.getNotice().contains("backed up"));
    }

    @Test public void oversizedConfigIsBackedUpWithoutParsing() throws Exception {
        byte[] bytes = new byte[HudConfig.MAX_BYTES + 1];
        Files.write(file(), bytes);
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.getNotice().contains("backed up"));
        assertTrue(settings.toggle(HudModule.CPS));
    }

    @Test public void futureSchemaIsNeverOverwrittenOrRenamed() throws Exception {
        write("schemaVersion=8\ncustomFutureField=keep-me\n");
        byte[] original = Files.readAllBytes(file());
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.isReadOnly());
        assertFalse(settings.toggle(HudModule.FPS));
        assertFalse(settings.toggleHud());
        assertArrayEquals(original, Files.readAllBytes(file()));
        assertTrue(settings.getNotice().contains("Newer config"));
    }

    @Test public void unreadableConfigDoesNotBecomeWritableDefaults() throws Exception {
        Files.createDirectory(file());
        HudSettings settings = HudSettings.open(file());
        assertTrue(settings.isReadOnly());
        assertFalse(settings.toggleHud());
        assertTrue(Files.isDirectory(file()));
    }

    @Test public void failedSaveRollsBackBothCommandsAndRetryClearsError() throws Exception {
        HudSettings settings = HudSettings.open(file());
        Files.createDirectory(file());
        // A nonempty directory cannot be atomically replaced with a config file.
        Path obstacle = file().resolve("keep");
        Files.write(obstacle, new byte[] { 1 });
        assertFalse(settings.toggle(HudModule.CPS));
        assertTrue(settings.getConfig().active().cps);
        assertFalse(settings.getError().isEmpty());
        assertFalse(settings.toggleHud());
        assertTrue(settings.getConfig().active().enabled);
        Files.delete(obstacle);
        Files.delete(file());
        assertTrue(settings.toggle(HudModule.CPS));
        assertEquals("", settings.getError());
        assertFalse(HudConfig.load(file()).active().cps);
        try (Stream<Path> paths = Files.list(directory.getRoot().toPath())) {
            assertFalse(paths.anyMatch(path -> path.toString().endsWith(".tmp")));
        }
    }

    @Test public void moduleSearchSupportsAliasesWhitespaceAndEmptyResults() {
        assertEquals(11, HudModule.search("  ").size());
        assertEquals(HudModule.CPS, HudModule.search(" МЫШЬ ").get(0));
        assertEquals(HudModule.COORDINATES, HudModule.search("xyz").get(0));
        assertEquals(HudModule.KEYSTROKES, HudModule.search("WASD").get(0));
        assertEquals(HudModule.COMPASS, HudModule.search("КОМПАС").get(0));
        assertEquals(HudModule.CLOCK, HudModule.search("ЧАСЫ").get(0));
        assertEquals(HudModule.SPEED, HudModule.search("скорость").get(0));
        assertEquals(HudModule.HEALTH, HudModule.search("ЗДОРОВЬЕ").get(0));
        assertEquals(HudModule.HUNGER, HudModule.search("сытость").get(0));
        assertEquals(HudModule.ARMOR, HudModule.search("броня").get(0));
        assertEquals(HudModule.POTION_EFFECTS, HudModule.search("эффекты").get(0));
        assertTrue(HudModule.search("not-a-module").isEmpty());
    }

    @Test public void searchDoesNotDependOnOperatingSystemLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals(HudModule.COORDINATES, HudModule.search("COORDINATES").get(0));
        } finally { Locale.setDefault(previous); }
    }
}
