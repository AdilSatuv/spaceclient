package dev.spaceclient.settings;

import dev.spaceclient.hud.HudViewport;
import dev.spaceclient.hud.HudModule;
import dev.spaceclient.hud.HudPlacement;
import dev.spaceclient.hud.HudSettings;
import dev.spaceclient.mods.GameplayModule;
import dev.spaceclient.mods.ModSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsControlsTest {
    @Test public void numbersClampWithoutWrappingOrDiscardingOffGridValues() {
        SettingNumber opacity=new SettingNumber(5,60,5);
        assertEquals(60,opacity.adjust(60,1,false));
        assertEquals(5,opacity.adjust(5,-1,false));
        assertEquals(28,opacity.adjust(23,1,false));
        assertEquals(18,opacity.adjust(23,-1,false));
        assertEquals(48,opacity.adjust(23,1,true));
        assertEquals(5,opacity.adjust(23,-1,true));
    }
    @Test public void draggingClampsAndSelectsLegalSteps() {
        SettingNumber size=new SettingNumber(25,200,25);
        assertEquals(25,size.at(-10));
        assertEquals(200,size.at(10));
        assertEquals(125,size.at(.5));
        assertEquals(25,size.at(Double.NaN));
        for(int i=0;i<=100;i++) {
            int value=size.at(i/100.0);
            assertTrue(value>=25&&value<=200);
            assertEquals(0,(value-25)%25);
        }
        assertEquals(0,size.fraction(25),0);
        assertEquals(1,size.fraction(200),0);
    }
    @Test public void focusedRowsStayBetweenHeaderAndFooterAtSupportedSizes() {
        int[][] sizes={{320,240},{640,360},{960,540},{1280,720},{1920,1080}};
        for(int[] size:sizes)for(int header:new int[]{66,112,124,178}) {
            HudViewport v=new HudViewport(size[0],size[1]);
            SettingsLayout l=new SettingsLayout(v.width,v.height,header);
            assertTrue(l.left>=12);assertTrue(l.top>=12);
            assertTrue(l.controlX()>l.left+200);
            for(int count:new int[]{1,4,9,40}) {
                int offset=0;
                for(int index=0;index<count;index++) {
                    offset=l.reveal(offset,index,count);
                    int y=l.rowY(index,offset);
                    assertTrue(y>=l.bodyTop);
                    assertTrue(y+40<=l.top+l.height-80);
                }
                assertEquals(Math.max(0,count-l.slots),l.offset(Integer.MAX_VALUE,count));
                assertEquals(0,l.reveal(offset,0,count));
            }
        }
    }
    @Test public void adjustedValuesSurviveReloadAndModuleResetKeepsEnabledState() throws Exception {
        Path path=Files.createTempDirectory("space-settings-controls").resolve("mods.properties");
        ModSettings s=ModSettings.open(path);
        assertTrue(s.toggleItemSize());assertTrue(s.heldItemScale(83));
        SettingNumber range=new SettingNumber(50,150,10);
        assertTrue(s.heldItemScale(range.adjust(s.heldItemScale(),1,false)));
        assertEquals(93,ModSettings.open(path).heldItemScale());
        assertTrue(s.reset(GameplayModule.ITEM_SIZE));
        ModSettings restored=ModSettings.open(path);
        assertTrue(restored.itemSize());assertEquals(80,restored.heldItemScale());
    }
    @Test public void hudScaleWritesOnlyActiveProfileAndPlacementResetRestoresScale() throws Exception {
        Path path=Files.createTempDirectory("space-settings-hud").resolve("hud.properties");
        HudSettings s=HudSettings.open(path);
        double other=s.getConfig().profile(1).placement(HudModule.FPS).scale;
        SettingNumber range=new SettingNumber(5,30,1);
        assertTrue(s.place(HudModule.FPS,s.getConfig().active().placement(HudModule.FPS).withScale(range.at(.6)/10.0)));
        HudSettings restored=HudSettings.open(path);
        assertEquals(2.0,restored.getConfig().active().placement(HudModule.FPS).scale,0);
        assertEquals(other,restored.getConfig().profile(1).placement(HudModule.FPS).scale,0);
        assertTrue(restored.reset(HudModule.FPS));
        assertEquals(HudPlacement.defaults(HudModule.FPS).scale,HudSettings.open(path).getConfig().active().placement(HudModule.FPS).scale,0);
    }
    @Test public void readOnlyFilesAndFailedWritesRetainTheirOriginalState() throws Exception {
        Path path=Files.createTempFile("space-settings-readonly",".properties");
        byte[] bytes="schemaVersion=999\n".getBytes(StandardCharsets.UTF_8);Files.write(path,bytes);
        ModSettings readOnly=ModSettings.open(path);
        assertTrue(readOnly.isReadOnly());assertFalse(readOnly.heldItemScale(150));
        assertArrayEquals(bytes,Files.readAllBytes(path));
        Path parent=Files.createTempFile("space-settings-failure",".file");
        ModSettings failed=ModSettings.open(parent.resolve("mods.properties"));
        int before=failed.heldItemScale();
        assertFalse(failed.heldItemScale(new SettingNumber(50,150,10).adjust(before,1,false)));
        assertEquals(before,failed.heldItemScale());assertFalse(failed.error().isEmpty());
    }
}
