package dev.spaceclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/** Text-only status of this player's own equipped items and selected hand. */
public final class ArmorHud {
    private ArmorHud() { }

    public static String text(MinecraftClient client) {
        if (client == null || client.player == null) return "Armor --";
        // Vanilla 1.8.9 armor indices are boots, leggings, chestplate, helmet.
        return "Armor H:" + status(client.player.getArmorSlot(3))
            + " C:" + status(client.player.getArmorSlot(2))
            + " L:" + status(client.player.getArmorSlot(1))
            + " B:" + status(client.player.getArmorSlot(0))
            + " Hand:" + status(client.player.getMainHandStack());
    }

    private static String status(ItemStack stack) {
        if (stack == null || stack.count <= 0) return ArmorDurability.value(false, false, 0, 0);
        if (stack.getItem() == null) return "?";
        return ArmorDurability.value(true, stack.isDamageable(), stack.getDamage(), stack.getMaxDamage());
    }
}
