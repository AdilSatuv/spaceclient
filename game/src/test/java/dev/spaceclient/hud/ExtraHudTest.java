package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExtraHudTest {
    @Test public void schemaEightMigratesWithoutEnablingNewWidgetsAndKeepsBothPresets() throws Exception {
        Path file=Files.createTempDirectory("space-v8").resolve("hud.properties");
        HudConfig original=new HudConfig(); original.profile(0).fps=false; original.profile(1).armor=true;
        original.profile(0).place(HudModule.COORDINATES,new HudPlacement(HudPlacement.Horizontal.RIGHT,HudPlacement.Vertical.BOTTOM,37,21,1.8));
        original.save(file);
        Properties old=new Properties();try(java.io.InputStream in=Files.newInputStream(file)){old.load(in);}
        old.setProperty("schemaVersion","8");HudTestFixtures.removeNewModules(old,"profile.0.");HudTestFixtures.removeNewModules(old,"profile.1.");
        try(java.io.OutputStream out=Files.newOutputStream(file)){old.store(out,"Old schema 8 shape");}
        byte[] before=Files.readAllBytes(file);HudSettings s=HudSettings.open(file);
        assertFalse(s.isReadOnly());assertArrayEquals(before,Files.readAllBytes(file));
        assertFalse(s.getConfig().profile(0).memory);assertFalse(s.getConfig().profile(1).playtime);
        assertTrue(s.toggle(HudModule.ITEM_COUNTER));assertTrue(s.toggle(HudModule.MEMORY));
        HudConfig r=HudConfig.load(file);assertTrue(r.profile(0).itemCounter);assertTrue(r.profile(0).memory);
        assertFalse(r.profile(1).itemCounter);assertFalse(r.profile(0).fps);assertTrue(r.profile(1).armor);
        assertEquals(37,r.profile(0).placement(HudModule.COORDINATES).x,0);
        Path exported=file.resolveSibling("profile.properties");HudConfig.exportProfile(exported,r.profile(0));
        HudProfile imported=HudConfig.importProfile(exported);assertTrue(imported.itemCounter);assertTrue(imported.memory);
        Properties incomplete=new Properties();try(java.io.InputStream in=Files.newInputStream(file)){incomplete.load(in);}
        incomplete.remove("profile.0.appearance.memory.color");
        try(java.io.OutputStream out=Files.newOutputStream(file)){incomplete.store(out,"Broken v9");}
        try{HudConfig.load(file);fail("Missing new fields must fail");}catch(HudConfig.InvalidConfigException expected){}
    }
    @Test public void sessionCountsActiveIntervalsAndResetsOnDisconnect() {
        SessionTimer t=new SessionTimer();t.update(1_000_000_000L,true,false);
        t.update(2_000_000_000L,true,false);assertEquals(1,t.seconds());
        t.update(3_000_000_000L,true,true);t.update(20_000_000_000L,true,true);assertEquals(1,t.seconds());
        t.update(21_000_000_000L,true,false);t.update(22_000_000_000L,true,false);assertEquals(2,t.seconds());
        t.update(23_000_000_000L,false,false);assertEquals(0,t.seconds());
        assertEquals("Session 1:01:01",HudMetrics.playtime(3661));
        assertEquals("RAM 512 / 2048 MB",HudMetrics.memory(512L*1048576,2048L*1048576));
        assertEquals("RAM --",HudMetrics.memory(-1,0));
    }
}
