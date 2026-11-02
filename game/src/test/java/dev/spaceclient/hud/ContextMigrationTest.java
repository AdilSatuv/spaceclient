package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContextMigrationTest {
    private byte[] fixture(String name)throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)){
            assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toByteArray();
        }
    }
    @Test public void realSchemaTenKeepsPlacementsAndAddsIndependentContextWidgets()throws Exception {
        Path file=Files.createTempDirectory("space-context-v10").resolve("hud.properties");
        byte[] old=fixture("schema10-170f32c.properties");Files.write(file,old);HudSettings s=HudSettings.open(file);
        assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));assertEquals(1,s.getConfig().activeIndex());
        assertFalse(s.getConfig().profile(0).sprintStatus);assertTrue(s.getConfig().profile(1).memory);
        for(int i=0;i<2;i++){
            HudProfile p=s.getConfig().profile(i);assertFalse(p.ping);assertFalse(p.biome);assertFalse(p.packs);
            assertTrue(p.content().keystrokesJump);assertTrue(p.content().keystrokesMouse);
        }
        try(Stream<Path> paths=Files.list(file.getParent())){
            Path backup=paths.filter(p->p.getFileName().toString().startsWith("hud.properties.v10-")).findFirst().get();
            assertArrayEquals(old,Files.readAllBytes(backup));
        }
        assertTrue(s.toggle(HudModule.PING));assertTrue(s.toggle(HudModule.BIOME));assertTrue(s.toggle(HudModule.PACK_DISPLAY));
        assertTrue(s.content(s.getConfig().active().content().withKeystrokesJump(false).withKeystrokesMouse(false)));
        HudConfig r=HudConfig.load(file);assertTrue(r.profile(1).ping);assertTrue(r.profile(1).biome);assertTrue(r.profile(1).packs);
        assertFalse(r.profile(0).ping);assertFalse(r.profile(1).content().keystrokesJump);assertTrue(r.profile(0).content().keystrokesJump);
        assertEquals(41,r.profile(0).placement(HudModule.KEYSTROKES).x,0);assertEquals(1.3,r.profile(0).placement(HudModule.KEYSTROKES).scale,0);
        assertEquals(HudAppearance.Backdrop.SOFT,r.profile(0).appearance(HudModule.KEYSTROKES).backdrop);
        assertEquals(HudContentSettings.ClockFormat.H12,r.profile(0).content().clockFormat);
        Path export=file.resolveSibling("profile.properties");HudConfig.exportProfile(export,r.profile(1));HudProfile imported=HudConfig.importProfile(export);
        assertTrue(imported.packs);assertFalse(imported.content().keystrokesMouse);
        Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}
        assertEquals("15",p.getProperty("schemaVersion"));p.remove("profile.1.content.keystrokes.jump");
        try(OutputStream out=Files.newOutputStream(file)){p.store(out,"Broken v11");}
        try{HudConfig.load(file);fail("Missing keystrokes setting must fail");}catch(HudConfig.InvalidConfigException expected){}
    }
    @Test public void realProfileV5AndEditsToOtherModulesPreserveKeystrokesOptions()throws Exception {
        Path file=Files.createTempFile("space-context-v5",".properties");Files.write(file,fixture("profile-v5-170f32c.properties"));
        HudProfile p=HudConfig.importProfile(file);assertFalse(p.sprintStatus);assertFalse(p.fps);assertTrue(p.content().keystrokesMouse);
        assertFalse(p.ping);assertEquals("Context A",p.name);
        HudContentSettings c=p.content().withKeystrokesMouse(false).withKeystrokesJump(false)
            .withClockSeconds(true).withClockFormat(HudContentSettings.ClockFormat.H24).withCoordinatesPrecision(0)
            .withFpsLabel(true).withCpsMode(HudContentSettings.CpsMode.LEFT);
        assertFalse(c.keystrokesJump);assertFalse(c.keystrokesMouse);
    }
}
