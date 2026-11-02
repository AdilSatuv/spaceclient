package dev.spaceclient.hud;
import dev.spaceclient.HudConfig;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class EquipmentContentTest {
    private byte[] fixture(String name)throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();
        }
    }
    @Test public void realSchemaThirteenMigratesWithoutChangingWidgetsAndExportsBothFormats()throws Exception {
        Path file=Files.createTempDirectory("space-equipment-v13").resolve("hud");byte[] old=fixture("schema13-96825f9.properties");Files.write(file,old);
        HudSettings s=HudSettings.open(file);assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));
        assertEquals(HudContentSettings.KeyLayout.BLOCK,s.getConfig().profile(0).content().keyLayout);
        assertEquals(HudContentSettings.SprintDisplay.COMPACT,s.getConfig().profile(0).content().sprintDisplay);
        assertEquals(EquipmentContent.Style.TEXT,s.getConfig().profile(0).content().equipment.armorStyle);
        assertEquals(40,s.getConfig().profile(0).placement(HudModule.ARMOR).x,0);
        try(java.util.stream.Stream<Path> paths=Files.list(file.getParent())) {
            assertArrayEquals(old,Files.readAllBytes(paths.filter(p->p.getFileName().toString().startsWith("hud.v13-")).findFirst().get()));
        }
        EquipmentContent e=EquipmentContent.defaults().armorStyle(EquipmentContent.Style.ICONS).effectStyle(EquipmentContent.Style.ICONS)
            .durability(EquipmentContent.Durability.REMAINING).hand(false).maxEffects(5).effectTime(false);
        assertTrue(s.content(s.getConfig().active().content().withEquipment(e)));HudConfig r=HudConfig.load(file);
        assertEquals(EquipmentContent.Style.ICONS,r.profile(1).content().equipment.armorStyle);assertFalse(r.profile(1).content().equipment.hand);
        assertEquals(EquipmentContent.Style.TEXT,r.profile(0).content().equipment.armorStyle);
        Path export=file.resolveSibling("export");HudConfig.exportProfile(export,r.profile(1));
        assertEquals(5,HudConfig.importProfile(export).content().equipment.maxEffects);
        Files.write(export,fixture("profile-v8-96825f9.properties"));HudProfile imported=HudConfig.importProfile(export);
        assertEquals("Equipment A",imported.name);assertEquals(HudContentSettings.KeyLayout.BLOCK,imported.content().keyLayout);
        assertEquals(EquipmentContent.Style.TEXT,imported.content().equipment.effectStyle);assertEquals(2,imported.content().equipment.maxEffects);
    }
    @Test public void contentEditsPreserveOtherModulesAndEquipmentFormats() {
        EquipmentContent e=EquipmentContent.defaults().armorStyle(EquipmentContent.Style.ICONS).effectStyle(EquipmentContent.Style.ICONS).hand(false).maxEffects(5).effectTime(false);
        HudContentSettings c=HudContentSettings.defaults().withEquipment(e).withCoordinatesPrecision(2).withClockSeconds(false)
            .withClockFormat(HudContentSettings.ClockFormat.H12).withFpsLabel(false).withCpsMode(HudContentSettings.CpsMode.RIGHT)
            .withKeystrokesJump(false).withKeystrokesMouse(false).withPingLabel(false).withBiomeLabel(false).withPackLabel(false).withPackNames(3)
            .withKeyLayout(HudContentSettings.KeyLayout.BLOCK).withSprintDisplay(HudContentSettings.SprintDisplay.COMPACT);
        assertSame(e,c.equipment);assertEquals(HudContentSettings.KeyLayout.BLOCK,c.keyLayout);
        c=c.withEquipment(c.equipment.durability(EquipmentContent.Durability.REMAINING));
        assertEquals(HudContentSettings.SprintDisplay.COMPACT,c.sprintDisplay);assertFalse(c.pingLabel);assertEquals(3,c.packNames);assertFalse(c.equipment.effectTime);
    }
    @Test public void incompleteAndInvalidNewFormatsCannotSilentlyResetAProfile()throws Exception {
        Path file=Files.createTempFile("space-equipment-format",".properties");new HudConfig().save(file);
        Properties original=new Properties();try(InputStream in=Files.newInputStream(file)){original.load(in);}
        for(String key:new String[]{"armor.style","armor.durability","armor.hand","effects.style","effects.limit","effects.time"}) {
            Properties broken=new Properties();broken.putAll(original);broken.remove("profile.0.content."+key);
            try(OutputStream out=Files.newOutputStream(file)){broken.store(out,"Missing");}
            try{HudConfig.load(file);fail(key);}catch(HudConfig.InvalidConfigException expected){}
        }
        for(String value:new String[]{"0","6","no"}) {
            Properties broken=new Properties();broken.putAll(original);broken.setProperty("profile.0.content.effects.limit",value);
            try(OutputStream out=Files.newOutputStream(file)){broken.store(out,"Invalid");}
            try{HudConfig.load(file);fail(value);}catch(HudConfig.InvalidConfigException expected){}
        }
    }
}
