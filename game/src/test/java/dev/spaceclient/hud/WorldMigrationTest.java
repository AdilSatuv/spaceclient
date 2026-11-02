package dev.spaceclient.hud;
import dev.spaceclient.HudConfig;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;
public class WorldMigrationTest {
    private byte[] fixture(String name)throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();
        }
    }
    @Test public void realSchemaFourteenPreservesEquipmentAndAddsOptInWorldWidgetsWithRightAnchors()throws Exception {
        Path file=Files.createTempDirectory("space-world-v14").resolve("hud");byte[] old=fixture("schema14-1683729.properties");Files.write(file,old);
        HudSettings s=HudSettings.open(file);assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));assertEquals(1,s.getConfig().activeIndex());
        assertEquals(EquipmentContent.Style.ICONS,s.getConfig().profile(0).content().equipment.effectStyle);
        assertEquals(HudContentSettings.KeyLayout.BLOCK,s.getConfig().profile(0).content().keyLayout);assertEquals(40,s.getConfig().profile(0).placement(HudModule.ARMOR).x,0);
        for(int i=0;i<2;i++) {
            assertFalse(s.getConfig().profile(i).dayCounter);assertFalse(s.getConfig().profile(i).lightLevel);
            assertEquals(HudPlacement.Horizontal.RIGHT,s.getConfig().profile(i).placement(HudModule.DAY_COUNTER).horizontal);
            assertEquals(56,s.getConfig().profile(i).placement(HudModule.DAY_COUNTER).y,0);
        }
        try(java.util.stream.Stream<Path> paths=Files.list(file.getParent())) {
            assertArrayEquals(old,Files.readAllBytes(paths.filter(p->p.getFileName().toString().startsWith("hud.v14-")).findFirst().get()));
        }
        assertTrue(s.toggle(HudModule.DAY_COUNTER));assertTrue(s.toggle(HudModule.LIGHT_LEVEL));HudConfig r=HudConfig.load(file);
        assertTrue(r.profile(1).dayCounter);assertTrue(r.profile(1).lightLevel);assertFalse(r.profile(0).dayCounter);
        assertTrue(r.copy().active().dayCounter);assertTrue(r.copy().active().lightLevel);assertEquals(0,r.migrationSourceVersion());
    }
    @Test public void exportTenRoundTripsAndRealVersionNineStillImports()throws Exception {
        Path file=Files.createTempFile("space-world-export",".properties");HudProfile p=new HudProfile("World export");p.dayCounter=true;p.lightLevel=true;
        p.place(HudModule.LIGHT_LEVEL,new HudPlacement(HudPlacement.Horizontal.RIGHT,HudPlacement.Vertical.BOTTOM,31,22,1.3));
        p.setAppearance(HudModule.DAY_COUNTER,new HudAppearance(HudAppearance.Color.AQUA,HudAppearance.Backdrop.SOFT,false));
        HudConfig.exportProfile(file,p);HudProfile imported=HudConfig.importProfile(file);assertTrue(imported.dayCounter);assertTrue(imported.lightLevel);
        assertEquals(31,imported.placement(HudModule.LIGHT_LEVEL).x,0);assertEquals(HudAppearance.Color.AQUA,imported.appearance(HudModule.DAY_COUNTER).color);
        Files.write(file,fixture("profile-v9-1683729.properties"));imported=HudConfig.importProfile(file);
        assertFalse(imported.dayCounter);assertFalse(imported.lightLevel);assertEquals(5,imported.content().equipment.maxEffects);
        assertEquals(HudPlacement.Horizontal.RIGHT,imported.placement(HudModule.LIGHT_LEVEL).horizontal);
    }
    @Test public void incompleteWorldSnapshotsAndFailedSavesCannotResetOrEnableWidgets()throws Exception {
        Path file=Files.createTempDirectory("space-world-invalid").resolve("hud");new HudConfig().save(file);
        Properties original=new Properties();try(InputStream in=Files.newInputStream(file)){original.load(in);}
        for(String key:new String[]{"daycounter","layout.lightlevel.y","appearance.daycounter.color"}) {
            Properties broken=new Properties();broken.putAll(original);broken.remove("profile.0."+key);
            try(OutputStream out=Files.newOutputStream(file)){broken.store(out,"Missing");}
            try{HudConfig.load(file);fail(key);}catch(HudConfig.InvalidConfigException expected){}
        }
        Path blocked=Files.createTempFile("space-world-blocked",".file");HudSettings s=HudSettings.open(blocked.resolve("hud"));
        assertFalse(s.toggle(HudModule.DAY_COUNTER));assertFalse(s.getConfig().active().dayCounter);assertFalse(s.getConfig().active().lightLevel);
        assertEquals(HudModule.DAY_COUNTER,HudSection.WORLD.search("день").get(0));assertEquals(HudModule.LIGHT_LEVEL,HudSection.WORLD.search("освещение").get(0));
    }
}
