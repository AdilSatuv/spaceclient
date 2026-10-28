package dev.spaceclient.mixin;

import dev.spaceclient.freelook.FreeLookRuntime;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A detached view must not interact with blocks/entities behind the apparent crosshair. */
@Mixin(MinecraftClient.class)
public abstract class FreeLookInputMixin {
    // Restore even if rendering is suspended while focus, world, key, or settings change.
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void spaceclient$releaseCamera(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (FreeLookRuntime.active && (!FreeLookRuntime.eligible(client) || FreeLookRuntime.owner != client.player)) {
            FreeLookRuntime.update(client, false);
        }
    }
    @Inject(method = {"doAttack()V", "doUse()V"}, at = @At("HEAD"), cancellable = true)
    private void spaceclient$blockInteraction(CallbackInfo callback) {
        if (FreeLookRuntime.active) callback.cancel();
    }

    @ModifyVariable(method = "handleBlockBreaking(Z)V", at = @At("HEAD"), argsOnly = true)
    private boolean spaceclient$stopBreaking(boolean breaking) {
        return !FreeLookRuntime.active && breaking;
    }
}
