package dev.spaceclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

/** Read-only player information; arrows are counted only in the main inventory/hotbar. */
public final class PlayerStatsHud {
    private PlayerStatsHud() { }
    public static ExperienceProgress experience(MinecraftClient client) {
        return client == null || client.player == null ? ExperienceProgress.unavailable()
            : ExperienceProgress.of(client.player.experienceLevel, client.player.experienceProgress);
    }
    public static String arrows(MinecraftClient client) {
        return client == null || client.player == null ? "Arrows: --"
            : "Arrows: " + count(client.player.inventory.main, Items.ARROW);
    }
    static long count(ItemStack[] slots, Item arrow) {
        if (slots == null || arrow == null) return 0;
        long count = 0;
        for (ItemStack stack : slots)
            if (stack != null && stack.count > 0 && stack.getItem() == arrow) count += stack.count;
        return count;
    }
}
