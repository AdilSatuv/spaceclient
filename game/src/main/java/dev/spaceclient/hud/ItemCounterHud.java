package dev.spaceclient.hud;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/** Counts exact matches in the 36 main inventory/hotbar slots; excludes equipped armor and containers. */
public final class ItemCounterHud {
    private ItemCounterHud() { }
    public static String text(MinecraftClient client) {
        if (client == null || client.player == null) return "Items --";
        ItemStack held = client.player.getMainHandStack();
        if (held == null || held.count <= 0) return "Items: hold an item";
        long count = count(held, client.player.inventory.main);
        String name = client.textRenderer.trimToWidth(held.getCustomName(), 120);
        return name + ": " + count;
    }
    static long count(ItemStack held, ItemStack[] slots) {
        if (held == null || held.count <= 0 || slots == null) return 0;
        long count = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.count > 0 && stack.getItem() == held.getItem()
                    && stack.getDamage() == held.getDamage() && Objects.equals(stack.getNbt(), held.getNbt())) count += stack.count;
        }
        return count;
    }
}
