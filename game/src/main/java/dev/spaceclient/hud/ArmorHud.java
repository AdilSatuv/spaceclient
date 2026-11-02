package dev.spaceclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/** Text-only status of this player's own equipped items and selected hand. */
public final class ArmorHud {
    private ArmorHud() { }

    public static String text(MinecraftClient client) {
        return text(client,EquipmentContent.defaults());
    }
    public static String text(MinecraftClient client,EquipmentContent content) {
        if (client == null || client.player == null) return "Armor --";
        // Vanilla 1.8.9 armor indices are boots, leggings, chestplate, helmet.
        ItemStack[] slots=slots(client,content.hand);
        return "Armor H:" + value(slots[0],content) + " C:" + value(slots[1],content)
            + " L:" + value(slots[2],content) + " B:" + value(slots[3],content)
            + (content.hand?" Hand:"+value(slots[4],content):"");
    }
    public static ItemStack[] slots(MinecraftClient c,boolean hand) {
        ItemStack[] slots=new ItemStack[hand?5:4];
        if(c!=null&&c.player!=null){for(int i=0;i<4;i++)slots[i]=c.player.getArmorSlot(3-i);if(hand)slots[4]=c.player.getMainHandStack();}return slots;
    }
    public static String value(ItemStack stack,EquipmentContent content) {
        if (stack == null || stack.count <= 0) return ArmorDurability.value(false, false, 0, 0);
        if (stack.getItem() == null) return "?";
        return content.durability==EquipmentContent.Durability.PERCENT
            ? ArmorDurability.value(true, stack.isDamageable(), stack.getDamage(), stack.getMaxDamage())
            : ArmorDurability.remaining(true, stack.isDamageable(), stack.getDamage(), stack.getMaxDamage());
    }
    public static double fraction(ItemStack stack) {
        return stack==null||stack.count<=0||stack.getItem()==null?Double.NaN:ArmorDurability.fraction(true,stack.isDamageable(),stack.getDamage(),stack.getMaxDamage());
    }
}
