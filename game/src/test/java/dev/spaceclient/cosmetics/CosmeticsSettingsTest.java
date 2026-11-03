package dev.spaceclient.cosmetics;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class CosmeticsSettingsTest {
    @Test public void choicesPersistAndSnapshotsDoNotMutateTheStore() throws Exception {
        Path file=Files.createTempDirectory("space-cosmetic").resolve("prefs.properties");
        CosmeticsSettings settings=CosmeticsSettings.open(file);assertEquals(CosmeticsSettings.Wings.NONE,settings.view().wings);
        assertTrue(settings.change(value->{value.wings=CosmeticsSettings.Wings.EMBER;value.halo=CosmeticsSettings.Halo.ORBIT;value.scale=130;value.cape=false;}));
        CosmeticsSettings.State reloaded=CosmeticsSettings.open(file).view();assertEquals(CosmeticsSettings.Wings.EMBER,reloaded.wings);assertEquals(130,reloaded.scale);assertFalse(reloaded.cape);
        reloaded.scale=999;assertEquals(130,settings.view().scale);
        assertFalse(settings.change(value->value.scale=10000));assertEquals(130,settings.view().scale);
        assertTrue(settings.reset());assertEquals(CosmeticsSettings.Wings.NONE,CosmeticsSettings.open(file).view().wings);
    }
    @Test public void futureFilesAreNotRewrittenAndWriteFailureRollsBack() throws Exception {
        Path file=Files.createTempDirectory("space-cosmetic-future").resolve("prefs.properties");
        byte[] bytes="schemaVersion=99\nwings=FUTURE\n".getBytes(StandardCharsets.UTF_8);Files.write(file,bytes);
        CosmeticsSettings future=CosmeticsSettings.open(file);assertTrue(future.readOnly());assertFalse(future.reset());assertArrayEquals(bytes,Files.readAllBytes(file));
        Path broken=Files.createTempFile("space-cosmetic-file-parent","tmp").resolve("prefs.properties");
        CosmeticsSettings failed=CosmeticsSettings.open(broken);assertFalse(failed.change(value->value.wings=CosmeticsSettings.Wings.CRYSTAL));
        assertEquals(CosmeticsSettings.Wings.NONE,failed.view().wings);assertFalse(failed.error().isEmpty());
    }
}
