package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class HudMigrationTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();
    private Path file() { return directory.getRoot().toPath().resolve("hud.properties"); }
    private byte[] version2() {
        return "schemaVersion=2\nactiveProfile=1\nprofile.0.fps=false\nprofile.1.cps=false\n".getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test public void schemaTwoBackupMatchesBeforeFirstWriteAndSchemaFourPersistsCompass() throws Exception {
        byte[] original = version2();
        Files.write(file(), original);
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.isReadOnly());
        assertArrayEquals(original, Files.readAllBytes(file()));
        List<Path> backups;
        try (Stream<Path> files = Files.list(directory.getRoot().toPath())) {
            backups = files.filter(path -> path.getFileName().toString().contains(".v2-")).collect(Collectors.toList());
        }
        assertEquals(1, backups.size());
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
        assertFalse(settings.getConfig().profile(1).compass);
        assertTrue(settings.toggle(HudModule.COMPASS));
        assertTrue(new String(Files.readAllBytes(file()), StandardCharsets.ISO_8859_1).contains("schemaVersion=4"));
        assertTrue(HudConfig.load(file()).profile(1).compass);
        assertFalse(HudConfig.load(file()).profile(0).fps);
        assertFalse(HudConfig.load(file()).profile(1).cps);
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
    }

    @Test public void schemaThreeMigrationPreservesCompassAndBacksUpBeforeChanges() throws Exception {
        byte[] original = "schemaVersion=3\nprofile.0.compass=true\nprofile.1.fps=false\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file(), original);
        HudSettings settings = HudSettings.open(file());
        assertFalse(settings.isReadOnly());
        assertTrue(settings.getConfig().profile(0).compass);
        assertFalse(settings.getConfig().profile(0).clock);
        assertFalse(settings.getConfig().profile(0).speed);
        assertArrayEquals(original, Files.readAllBytes(file()));
        List<Path> backups;
        try (Stream<Path> files = Files.list(directory.getRoot().toPath())) {
            backups = files.filter(path -> path.getFileName().toString().contains(".v3-")).collect(Collectors.toList());
        }
        assertEquals(1, backups.size());
        assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
        assertTrue(settings.toggle(HudModule.CLOCK));
        HudConfig updated = HudConfig.load(file());
        assertTrue(updated.profile(0).clock);
        assertTrue(updated.profile(0).compass);
        assertFalse(updated.profile(1).fps);
    }

    @Test public void failedOrMismatchedMigrationBackupLeavesOriginalReadOnly() throws Exception {
        byte[] original = version2();
        Files.write(file(), original);
        HudSettings failed = HudSettings.open(file(), (source, target) -> { throw new IOException("blocked"); });
        assertTrue(failed.isReadOnly());
        assertFalse(failed.toggle(HudModule.COMPASS));
        assertArrayEquals(original, Files.readAllBytes(file()));
        HudSettings mismatch = HudSettings.open(file(), (source, target) -> Files.write(target, new byte[] {1}));
        assertTrue(mismatch.isReadOnly());
        assertFalse(mismatch.toggle(HudModule.COMPASS));
        assertArrayEquals(original, Files.readAllBytes(file()));
    }
}
