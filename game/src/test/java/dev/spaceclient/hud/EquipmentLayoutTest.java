package dev.spaceclient.hud;
import org.junit.Test;
import static org.junit.Assert.*;
public class EquipmentLayoutTest {
    @Test public void effectsCollapseUnusedRowsAndReserveAnOverflowLine() {
        for(int limit=1;limit<=5;limit++) {
            assertEquals(24,EquipmentLayout.effectHeight(0,limit,true));
            int full=EquipmentLayout.effectHeight(limit,limit,true);
            assertEquals(full+12,EquipmentLayout.effectHeight(limit+20,limit,true));
            assertEquals(full-limit*2,EquipmentLayout.effectHeight(limit,limit,false));
            for(int count=1;count<=10;count++) {
                int height=EquipmentLayout.effectHeight(count,limit,true);
                assertTrue((Math.min(count,limit)-1)*26+24<=height);
                HudPlacement.Bounds b=new HudPlacement(HudPlacement.Horizontal.RIGHT,HudPlacement.Vertical.BOTTOM,8,8,3)
                    .resolve(320,240,EquipmentLayout.EFFECT_WIDTH,height);
                assertTrue(b.x>=0&&b.y>=0);assertTrue(b.x+b.width<=320&&b.y+b.height<=240);
            }
        }
    }
    @Test public void armorWidthReservesMaximumValuesAndFitsBothSlotCounts() {
        int cell=EquipmentLayout.armorCell(new String[]{"100%","n/a"},s->s.length()*6);
        assertTrue(cell>=28);
        int large=EquipmentLayout.armorCell(new String[]{"2147483647"},s->s.length()*6);assertTrue(large<=68);
        for(int count:new int[]{4,5}) {
            int width=EquipmentLayout.armorWidth(count,large);
            HudPlacement p=new HudPlacement(HudPlacement.Horizontal.RIGHT,HudPlacement.Vertical.BOTTOM,5,9,3);
            HudPlacement.Bounds b=p.resolve(320,240,width,EquipmentLayout.ARMOR_HEIGHT);
            assertTrue(b.x+b.width<=320&&b.y+b.height<=240);
            HudPlacement.Bounds anchored=p.anchored(HudPlacement.Horizontal.LEFT,HudPlacement.Vertical.TOP,320,240,width,EquipmentLayout.ARMOR_HEIGHT)
                .resolve(320,240,width,EquipmentLayout.ARMOR_HEIGHT);
            assertEquals(b.x,anchored.x,.0001);assertEquals(b.y,anchored.y,.0001);
        }
    }
}
