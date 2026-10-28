package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Fixtures were written by the unmodified HudConfig serializer from commit 5527acd.
 * Only the Properties.store timestamp comment was normalized. */
public class HudLegacyFixtureTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    private byte[] fixture(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/hud-legacy/" + name)) {
            assertNotNull("Missing legacy fixture: " + name, in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int count;
            while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
            return out.toByteArray();
        }
    }

    private void assertFirstProfile(HudProfile profile) {
        assertEquals("Орбита A", profile.name);
        assertFalse(profile.fps);
        assertFalse(profile.cps);
        assertTrue(profile.armor);
        HudPlacement position = profile.placement(HudModule.COORDINATES);
        assertEquals(HudPlacement.Horizontal.RIGHT, position.horizontal);
        assertEquals(HudPlacement.Vertical.BOTTOM, position.vertical);
        assertEquals(23.5, position.x, 0);
        assertEquals(17.25, position.y, 0);
        assertEquals(1.7, position.scale, 0);
        HudAppearance appearance = profile.appearance(HudModule.COORDINATES);
        assertEquals(HudAppearance.Color.AQUA, appearance.color);
        assertEquals(HudAppearance.Backdrop.SOFT, appearance.backdrop);
        assertFalse(appearance.shadow);
        assertEquals(1, profile.content().coordinatesPrecision);
        assertEquals(HudContentSettings.ClockFormat.H24, profile.content().clockFormat);
        assertTrue(profile.content().clockSeconds);
        assertTrue(profile.content().fpsLabel);
        assertEquals(HudContentSettings.CpsMode.BOTH, profile.content().cpsMode);
    }

    @Test public void realSchemaSevenMigrationBacksUpAndPreservesBothProfiles() throws Exception {
        Path file = directory.getRoot().toPath().resolve("hud.properties");
        byte[] original = fixture("schema7-5527acd.properties");
        Files.write(file, original);
        HudSettings settings = HudSettings.open(file);
        assertFalse(settings.isReadOnly());
        assertArrayEquals(original, Files.readAllBytes(file));
        assertEquals(1, settings.getConfig().activeIndex());
        assertFirstProfile(settings.getConfig().profile(0));
        HudProfile second = settings.getConfig().profile(1);
        assertEquals("Night", second.name);
        assertFalse(second.enabled);
        assertTrue(second.clock);
        assertEquals(HudPlacement.Vertical.BOTTOM, second.placement(HudModule.CLOCK).vertical);
        assertEquals(0.8, second.placement(HudModule.CLOCK).scale, 0);
        assertEquals(HudAppearance.Color.GOLD, second.appearance(HudModule.CLOCK).color);
        assertEquals(HudAppearance.Backdrop.OFF, second.appearance(HudModule.CLOCK).backdrop);
        assertFalse(second.appearance(HudModule.CLOCK).shadow);
        Path backup;
        try (Stream<Path> files = Files.list(directory.getRoot().toPath())) {
            backup = files.filter(path -> path.getFileName().toString().startsWith("hud.properties.v7-"))
                .findFirst().orElseThrow(() -> new AssertionError("Expected verified schema 7 backup"));
        }
        assertArrayEquals(original, Files.readAllBytes(backup));
        assertTrue(settings.content(settings.getConfig().active().content().withClockSeconds(false)));
        HudConfig restored = HudConfig.load(file);
        assertEquals(1, restored.activeIndex());
        assertFirstProfile(restored.profile(0));
        assertFalse(restored.profile(1).content().clockSeconds);
        assertEquals(HudAppearance.Backdrop.OFF, restored.profile(1).appearance(HudModule.CLOCK).backdrop);
        assertArrayEquals(original, Files.readAllBytes(backup));
    }

    @Test public void realVersionTwoExportImportsOnlyIntoSelectedProfile() throws Exception {
        Path file = directory.getRoot().toPath().resolve("hud.properties");
        HudSettings settings = HudSettings.open(file);
        assertTrue(settings.selectProfile(1));
        Path imported = file.resolveSibling("spaceclient-hud-import.properties");
        Files.write(imported, fixture("profile-v2-5527acd.properties"));
        assertTrue(settings.importProfile());
        HudConfig restored = HudConfig.load(file);
        assertEquals(1, restored.activeIndex());
        assertFirstProfile(restored.profile(1));
        assertEquals("Profile 1", restored.profile(0).name);
        assertTrue(restored.profile(0).fps);
        assertEquals(HudAppearance.Color.AUTO, restored.profile(0).appearance(HudModule.COORDINATES).color);
    }
}
