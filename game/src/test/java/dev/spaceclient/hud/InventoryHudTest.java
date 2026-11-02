package dev.spaceclient.hud;

import java.util.HashSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryHudTest {
    @Test public void storageRowsKeepNativeOrderAndOptionalHotbarIsLastWithoutDuplicateSlots() {
        HashSet<Integer> ids=new HashSet<Integer>();
        for(int i=0;i<36;i++){int slot=InventoryHud.slotIndex(i,true);assertTrue(ids.add(slot));assertEquals(i<27?i+9:i-27,slot);}
        assertEquals(36,ids.size());assertEquals(27,InventoryHud.cells(false));assertEquals(36,InventoryHud.cells(true));
        for(int i:new int[]{-1,27,36})try{InventoryHud.slotIndex(i,false);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void occupiedSlotCountSeparatesStorageHotbarAndInvalidStacksWithoutMutatingItems() {
        Item item=new Item();ItemStack[] slots=new ItemStack[40];slots[0]=new ItemStack(item,64,0);slots[8]=new ItemStack(item,3,0);
        slots[9]=new ItemStack(item,1,0);slots[35]=new ItemStack(item,17,0);slots[36]=new ItemStack(item,64,0);
        slots[12]=new ItemStack(item,0,0);slots[13]=new ItemStack(item,-1,0);
        assertEquals(2,InventoryHud.used(slots,false));assertEquals(4,InventoryHud.used(slots,true));assertEquals(17,slots[35].count);
        assertEquals(0,InventoryHud.used(null,true));assertEquals(0,InventoryHud.used(new ItemStack[9],false));
        assertEquals("Inventory --",InventoryHud.text(null,false));
    }
    @Test public void completeGridFitsSmallViewportsAtEveryAnchorWithoutChangingPreferredScale() {
        for(boolean hotbar:new boolean[]{false,true})for(HudPlacement.Horizontal h:HudPlacement.Horizontal.values())for(HudPlacement.Vertical v:HudPlacement.Vertical.values()) {
            HudPlacement p=new HudPlacement(h,v,16000,16000,3);
            HudPlacement.Bounds b=p.resolve(320,240,InventoryHud.WIDTH,InventoryHud.height(hotbar));
            assertTrue(b.x>=0&&b.y>=0&&b.x+b.width<=320.001&&b.y+b.height<=240.001);assertEquals(3,p.scale,0);
            HudPlacement moved=p.at(-100,-100,320,240,InventoryHud.WIDTH,InventoryHud.height(hotbar),false);
            b=moved.resolve(320,240,InventoryHud.WIDTH,InventoryHud.height(hotbar));assertEquals(0,b.x,0);assertEquals(0,b.y,0);
        }
        assertEquals(70,InventoryHud.height(false));assertEquals(88,InventoryHud.height(true));
    }
    @Test public void changingAnyExistingContentFormatPreservesInventoryOptions() {
        HudContentSettings c=HudContentSettings.defaults().withInventoryHotbar(true).withInventoryOverlays(false);
        HudContentSettings[] changes={c.withCoordinatesPrecision(2),c.withClockFormat(HudContentSettings.ClockFormat.H12),c.withClockSeconds(false),
            c.withFpsLabel(false),c.withCpsMode(HudContentSettings.CpsMode.RIGHT),c.withKeystrokesJump(false),c.withKeystrokesMouse(false),
            c.withPingLabel(false),c.withBiomeLabel(false),c.withPackLabel(false),c.withPackNames(3),c.withKeyLayout(HudContentSettings.KeyLayout.BLOCK),
            c.withSprintDisplay(HudContentSettings.SprintDisplay.COMPACT),c.withEquipment(EquipmentContent.defaults().hand(false))};
        for(HudContentSettings result:changes){assertTrue(result.inventoryHotbar);assertFalse(result.inventoryOverlays);}
        assertFalse(HudContentSettings.defaults().inventoryHotbar);assertTrue(HudContentSettings.defaults().inventoryOverlays);
        assertTrue(c.withInventoryHotbar(false).inventoryOverlays==c.inventoryOverlays);
    }
}
