package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class InventoryMigrationTest {
    @Rule public TemporaryFolder dir=new TemporaryFolder();
    private byte[] fixture(String name)throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)) {
            assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();
        }
    }
    @Test public void realSchemaSixteenPreservesPlayerWidgetsAndBacksUpBeforeInventoryOptIn()throws Exception {
        Path file=dir.getRoot().toPath().resolve("hud");byte[] old=fixture("schema16-952b258.properties");Files.write(file,old);HudSettings s=HudSettings.open(file);
        assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));assertEquals(1,s.getConfig().activeIndex());
        assertTrue(s.getConfig().profile(0).experience);assertTrue(s.getConfig().profile(0).arrows);
        assertEquals(25,s.getConfig().profile(0).placement(HudModule.EXPERIENCE).x,0);
        assertEquals(HudAppearance.Color.GOLD,s.getConfig().profile(0).appearance(HudModule.EXPERIENCE).color);
        for(int i=0;i<2;i++) {
            HudProfile p=s.getConfig().profile(i);assertFalse(p.inventory);assertFalse(p.content().inventoryHotbar);assertTrue(p.content().inventoryOverlays);
            assertEquals(HudPlacement.Vertical.BOTTOM,p.placement(HudModule.INVENTORY).vertical);
        }
        try(java.util.stream.Stream<Path> files=Files.list(file.getParent())) {
            assertArrayEquals(old,Files.readAllBytes(files.filter(p->p.getFileName().toString().startsWith("hud.v16-")).findFirst().get()));
        }
        assertTrue(s.toggle(HudModule.INVENTORY));assertTrue(s.content(s.getConfig().active().content().withInventoryHotbar(true).withInventoryOverlays(false)));
        HudConfig r=HudConfig.load(file);assertTrue(r.active().inventory);assertTrue(r.active().content().inventoryHotbar);assertFalse(r.active().content().inventoryOverlays);
        assertFalse(r.profile(0).inventory);assertTrue(r.profile(0).experience);assertTrue(r.copy().active().inventory);
    }
    @Test public void inventoryExportRoundTripsAndRealExportElevenKeepsOldData()throws Exception {
        Path file=dir.getRoot().toPath().resolve("export");HudProfile p=new HudProfile("Inventory");p.inventory=true;
        p.setContent(p.content().withInventoryHotbar(true).withInventoryOverlays(false));
        p.place(HudModule.INVENTORY,new HudPlacement(HudPlacement.Horizontal.RIGHT,HudPlacement.Vertical.BOTTOM,31,22,1.4));
        HudConfig.exportProfile(file,p);HudProfile r=HudConfig.importProfile(file);assertTrue(r.inventory);assertTrue(r.content().inventoryHotbar);assertFalse(r.content().inventoryOverlays);
        assertEquals(31,r.placement(HudModule.INVENTORY).x,0);assertEquals(1.4,r.placement(HudModule.INVENTORY).scale,0);
        Files.write(file,fixture("profile-v11-952b258.properties"));r=HudConfig.importProfile(file);
        assertFalse(r.inventory);assertFalse(r.content().inventoryHotbar);assertTrue(r.content().inventoryOverlays);assertTrue(r.experience);assertTrue(r.arrows);
        assertEquals(HudContentSettings.KeyLayout.BLOCK,r.content().keyLayout);
    }
    @Test public void missingInventoryFieldsAndFailedSaveCannotSilentlyResetNewSettings()throws Exception {
        Path file=dir.getRoot().toPath().resolve("hud");new HudConfig().save(file);Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}
        for(String key:new String[]{"inventory","content.inventory.hotbar","content.inventory.overlays","layout.inventory.y","appearance.inventory.color"}) {
            Properties bad=new Properties();bad.putAll(p);bad.remove("profile.1."+key);try(OutputStream out=Files.newOutputStream(file)){bad.store(out,"missing");}
            try{HudConfig.load(file);fail(key);}catch(HudConfig.InvalidConfigException expected){}
        }
        Path blocked=dir.newFile("blocked").toPath();HudSettings s=HudSettings.open(blocked.resolve("hud"));
        assertFalse(s.toggle(HudModule.INVENTORY));assertFalse(s.getConfig().active().inventory);
        assertFalse(s.content(s.getConfig().active().content().withInventoryHotbar(true)));assertFalse(s.getConfig().active().content().inventoryHotbar);
        assertEquals(HudModule.INVENTORY,HudSection.PLAYER.search("инвентарь").get(0));
    }
}
