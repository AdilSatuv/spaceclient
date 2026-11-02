package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class NoHurtCamMixin {
    @Inject(method = "bobViewWhenHurt(F)V", at = @At("HEAD"), cancellable = true)
    private void spaceclient$steadyHurtView(float delta, CallbackInfo callback) {
        if (SpaceClient.mods == null || !SpaceClient.mods.noHurtCam()) return;
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null && c.getCameraEntity() == c.player && c.player.getHealth() > 0 && c.player.deathTime == 0)
            callback.cancel();
    }
}
