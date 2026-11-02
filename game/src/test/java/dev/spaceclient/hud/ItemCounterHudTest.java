package dev.spaceclient.hud;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemCounterHudTest {
    @Test public void stackCountExcludesInvalidEmptyAndOtherVariants(){
        Item type=new Item();ItemStack held=new ItemStack(type,16,0),same=new ItemStack(type,8,0),empty=new ItemStack(type,0,0),variant=new ItemStack(type,4,1),invalid=new ItemStack((Item)null,2,0);
        assertEquals(2,ItemCounterHud.stacks(held,new ItemStack[]{held,same,empty,variant,invalid,null}));assertEquals(0,ItemCounterHud.stacks(invalid,new ItemStack[]{invalid}));assertEquals(0,ItemCounterHud.count(invalid,new ItemStack[]{invalid}));assertEquals(0,ItemCounterHud.stacks(held,null));assertEquals(16,held.count);
    }
    @Test public void countsOnlyMatchingItemsMetadataAndTagsWithoutChangingStacks() {
        Item type=new Item();ItemStack held=new ItemStack(type,3,1),same=new ItemStack(type,12,1);
        ItemStack variant=new ItemStack(type,7,2),other=new ItemStack(new Item(),4,1),tagged=new ItemStack(type,8,1);
        NbtCompound tag=new NbtCompound();tag.putString("custom","keep");tagged.setNbt(tag);
        assertEquals(15,ItemCounterHud.count(held,new ItemStack[]{held,same,variant,other,tagged,null}));
        assertEquals(3,held.count);assertEquals(12,same.count);assertEquals("keep",tagged.getNbt().getString("custom"));
        assertEquals(0,ItemCounterHud.count(null,new ItemStack[]{same}));
        ItemStack huge=new ItemStack(type,Integer.MAX_VALUE,1);
        assertEquals(2L*Integer.MAX_VALUE,ItemCounterHud.count(held,new ItemStack[]{huge,huge}));
    }
}
