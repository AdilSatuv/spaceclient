package dev.spaceclient.hud;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerStatsHudTest {
    @Test public void levelProgressShowsZeroPartialAndFullWithoutUsingTotalXp() {
        assertEquals("XP Lv. 0 | 0%", ExperienceProgress.of(0,0).text());
        assertEquals("XP Lv. 12 | 50%", ExperienceProgress.of(12,.5f).text());
        assertEquals(99, ExperienceProgress.of(30,.999f).percent());
        assertEquals("XP Lv. 31 | 100%", ExperienceProgress.of(31,1).text());
        assertTrue(ExperienceProgress.of(Integer.MAX_VALUE,.25f).available);
        assertEquals(25,ExperienceProgress.of(Integer.MAX_VALUE,.25f).percent());
    }
    @Test public void missingAndInvalidExperienceNeverLooksLikeRealZero() {
        for(float value:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-.01f,1.01f}) {
            ExperienceProgress xp=ExperienceProgress.of(4,value);
            assertFalse(xp.available);assertEquals("XP --",xp.text());assertEquals(0,xp.progress,0);
        }
        assertFalse(ExperienceProgress.of(-1,.5f).available);
        assertEquals("XP --",PlayerStatsHud.experience(null).text());
        assertEquals("Arrows: --",PlayerStatsHud.arrows(null));
    }
    @Test public void arrowCountIncludesEveryPositiveArrowStackAndKeepsInventoryUnchanged() {
        Item arrow=new Item();ItemStack a=new ItemStack(arrow,64,0),b=new ItemStack(arrow,7,0);
        NbtCompound tag=new NbtCompound();tag.putString("display","named arrow");b.setNbt(tag);
        ItemStack other=new ItemStack(new Item(),32,0),empty=new ItemStack(arrow,0,0),invalid=new ItemStack(arrow,-3,0);
        ItemStack[] main=new ItemStack[36];main[0]=a;main[35]=b;main[5]=other;main[8]=empty;main[9]=invalid;
        assertEquals(71,PlayerStatsHud.count(main,arrow));assertEquals(64,a.count);assertEquals(7,b.count);
        assertSame(b,main[35]);assertEquals("named arrow",b.getNbt().getString("display"));
        assertEquals(0,PlayerStatsHud.count(new ItemStack[36],arrow));assertEquals(0,PlayerStatsHud.count(null,arrow));
        ItemStack huge=new ItemStack(arrow,Integer.MAX_VALUE,0);
        assertEquals(2L*Integer.MAX_VALUE,PlayerStatsHud.count(new ItemStack[]{huge,huge},arrow));
    }
}
