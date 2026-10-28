package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.AutoSprintPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Ask vanilla to sprint when the feature is enabled, keeping all vanilla restrictions. */
@Mixin(ClientPlayerEntity.class)
public abstract class AutoSprintMixin {
    @Redirect(method = "tickMovement()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean spaceclient$sprintKey(KeyBinding key) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localPlayer = client != null && client.player == (Object) this;
        return AutoSprintPolicy.sprintKey(key.isPressed(),
            SpaceClient.mods != null && SpaceClient.mods.autoSprint(), localPlayer,
            client != null && client.focused, client == null || client.currentScreen != null);
    }
}
