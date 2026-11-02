package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class PlayerStatsMigrationTest {
    @Rule public TemporaryFolder directory=new TemporaryFolder();
    private byte[] fixture(String name)throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)) {
            assertNotNull(name,in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();
        }
    }
    private Properties read(Path file)throws Exception {
        Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}return p;
    }
    private void write(Path file,Properties p)throws Exception {
        try(OutputStream out=Files.newOutputStream(file)){p.store(out,"test");}
    }
    @Test public void realSchemaFifteenGetsVerifiedBackupAndIndependentOptInModules()throws Exception {
        Path file=directory.getRoot().toPath().resolve("hud");byte[] old=fixture("schema15-81c467f.properties");Files.write(file,old);
        HudSettings s=HudSettings.open(file);assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));
        assertEquals(1,s.getConfig().activeIndex());HudProfile first=s.getConfig().profile(0);
        assertTrue(first.dayCounter);assertTrue(first.lightLevel);assertEquals(29,first.placement(HudModule.LIGHT_LEVEL).x,0);
        assertEquals(HudPlacement.Vertical.BOTTOM,first.placement(HudModule.LIGHT_LEVEL).vertical);
        assertEquals(HudAppearance.Color.AQUA,first.appearance(HudModule.DAY_COUNTER).color);
        assertEquals(HudContentSettings.KeyLayout.BLOCK,first.content().keyLayout);
        assertEquals(EquipmentContent.Style.ICONS,first.content().equipment.armorStyle);
        for(int i=0;i<2;i++) {
            HudProfile p=s.getConfig().profile(i);assertFalse(p.experience);assertFalse(p.arrows);
            assertEquals(HudPlacement.Horizontal.RIGHT,p.placement(HudModule.EXPERIENCE).horizontal);
            assertEquals(80,p.placement(HudModule.EXPERIENCE).y,0);assertEquals(104,p.placement(HudModule.ARROW_COUNTER).y,0);
        }
        try(java.util.stream.Stream<Path> paths=Files.list(file.getParent())) {
            assertArrayEquals(old,Files.readAllBytes(paths.filter(p->p.getFileName().toString().startsWith("hud.v15-")).findFirst().get()));
        }
        assertTrue(s.toggle(HudModule.EXPERIENCE));assertTrue(s.toggle(HudModule.ARROW_COUNTER));
        HudConfig restored=HudConfig.load(file);assertEquals(0,restored.migrationSourceVersion());
        assertTrue(restored.profile(1).experience);assertTrue(restored.profile(1).arrows);assertFalse(restored.profile(0).experience);
        assertTrue(restored.copy().active().experience);assertTrue(restored.copy().active().arrows);
    }
    @Test public void exportElevenRoundTripsAndRealExportTenKeepsWorldSettings()throws Exception {
        Path file=directory.getRoot().toPath().resolve("export");HudProfile p=new HudProfile("Stats");p.experience=true;p.arrows=true;
        p.place(HudModule.EXPERIENCE,new HudPlacement(HudPlacement.Horizontal.RIGHT,HudPlacement.Vertical.BOTTOM,21,33,1.5));
        p.setAppearance(HudModule.EXPERIENCE,new HudAppearance(HudAppearance.Color.GOLD,HudAppearance.Backdrop.OFF,false));
        HudConfig.exportProfile(file,p);assertEquals("12",read(file).getProperty("schemaVersion"));
        HudProfile imported=HudConfig.importProfile(file);assertTrue(imported.experience);assertTrue(imported.arrows);
        assertEquals(21,imported.placement(HudModule.EXPERIENCE).x,0);assertEquals(1.5,imported.placement(HudModule.EXPERIENCE).scale,0);
        assertEquals(HudAppearance.Backdrop.OFF,imported.appearance(HudModule.EXPERIENCE).backdrop);
        Files.write(file,fixture("profile-v10-81c467f.properties"));imported=HudConfig.importProfile(file);
        assertFalse(imported.experience);assertFalse(imported.arrows);assertTrue(imported.dayCounter);assertTrue(imported.lightLevel);
        assertEquals(HudPlacement.Vertical.BOTTOM,imported.placement(HudModule.LIGHT_LEVEL).vertical);
        assertEquals(29,imported.placement(HudModule.LIGHT_LEVEL).x,0);
    }
    @Test public void incompleteModernFilesAreRejectedAndFailedSavesRollBackNewFlags()throws Exception {
        Path file=directory.getRoot().toPath().resolve("hud");new HudConfig().save(file);Properties original=read(file);
        for(String key:new String[]{"experience","arrows","layout.experience.y","appearance.arrows.shadow"}) {
            Properties broken=new Properties();broken.putAll(original);broken.remove("profile.0."+key);write(file,broken);
            try{HudConfig.load(file);fail(key);}catch(HudConfig.InvalidConfigException expected){}
        }
        Path blocked=directory.newFile("blocked").toPath();HudSettings s=HudSettings.open(blocked.resolve("hud"));
        assertFalse(s.toggle(HudModule.EXPERIENCE));assertFalse(s.getConfig().active().experience);
        assertFalse(s.toggle(HudModule.ARROW_COUNTER));assertFalse(s.getConfig().active().arrows);
        assertEquals(HudModule.EXPERIENCE,HudSection.PLAYER.search("опыт").get(0));
        assertEquals(HudModule.ARROW_COUNTER,HudSection.COMBAT.search("стрелы").get(0));
    }
}
