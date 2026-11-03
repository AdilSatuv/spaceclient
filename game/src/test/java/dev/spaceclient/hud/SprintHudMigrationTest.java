package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

public class SprintHudMigrationTest {
    private byte[] fixture(String name)throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)){
            assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toByteArray();
        }
    }
    @Test public void realSchemaNineAddsVisibleStatusAndPreservesBothExistingProfiles()throws Exception {
        Path file=Files.createTempDirectory("space-sprint-v9").resolve("hud.properties");
        byte[] old=fixture("schema9-3a945b5.properties");Files.write(file,old);
        HudSettings s=HudSettings.open(file);assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));
        assertEquals(1,s.getConfig().activeIndex());
        for(int i=0;i<2;i++)assertTrue(s.getConfig().profile(i).sprintStatus);
        try(Stream<Path> paths=Files.list(file.getParent())){
            Path backup=paths.filter(p->p.getFileName().toString().startsWith("hud.properties.v9-")).findFirst().get();
            assertArrayEquals(old,Files.readAllBytes(backup));
        }
        assertTrue(s.toggle(HudModule.SPRINT_STATUS));
        HudPlacement position=new HudPlacement(HudPlacement.Horizontal.RIGHT,HudPlacement.Vertical.BOTTOM,17,23,1.6);
        assertTrue(s.place(HudModule.SPRINT_STATUS,position));
        HudConfig r=HudConfig.load(file);assertTrue(r.profile(0).sprintStatus);assertFalse(r.profile(1).sprintStatus);
        assertEquals(17,r.profile(1).placement(HudModule.SPRINT_STATUS).x,0);
        assertFalse(r.profile(0).fps);assertTrue(r.profile(0).memory);assertTrue(r.profile(0).itemCounter);
        assertFalse(r.profile(1).enabled);assertTrue(r.profile(1).playtime);assertTrue(r.profile(1).armor);
        assertEquals(31,r.profile(0).placement(HudModule.COORDINATES).x,0);
        assertEquals(HudAppearance.Color.AQUA,r.profile(0).appearance(HudModule.MEMORY).color);
        Path export=file.resolveSibling("profile.properties");HudConfig.exportProfile(export,r.profile(1));
        HudProfile imported=HudConfig.importProfile(export);assertFalse(imported.sprintStatus);
        assertEquals(23,imported.placement(HudModule.SPRINT_STATUS).y,0);
        Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}
        assertEquals("18",p.getProperty("schemaVersion"));p.remove("profile.1.sprintstatus");
        try(java.io.OutputStream out=Files.newOutputStream(file)){p.store(out,"Incomplete v10");}
        try{HudConfig.load(file);fail("Missing status flag must fail");}catch(HudConfig.InvalidConfigException expected){}
    }
    @Test public void realVersionFourExportKeepsExistingDataAndDefaultsStatusOn()throws Exception {
        Path file=Files.createTempFile("space-sprint-export4",".properties");
        Files.write(file,fixture("profile-v4-3a945b5.properties"));
        HudProfile p=HudConfig.importProfile(file);assertTrue(p.sprintStatus);assertTrue(p.memory);assertTrue(p.itemCounter);
        assertFalse(p.fps);assertEquals("Sprint legacy A",p.name);
        assertEquals(31,p.placement(HudModule.COORDINATES).x,0);
    }
}
