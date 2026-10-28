package dev.spaceclient.mixin;


import dev.spaceclient.freelook.FreeLookState;
import dev.spaceclient.freelook.FreeLookRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Independent third-person camera while V is held; actual aim is unchanged. */
@Mixin(GameRenderer.class)
public abstract class FreeLookRendererMixin {
    @Unique private final FreeLookState spaceclient$freeLook = FreeLookRuntime.state;
    @Unique private ClientPlayerEntity spaceclient$player;



    @Inject(method = "render(FJ)V", at = @At("HEAD"))
    private void spaceclient$updateCamera(float delta, long time, CallbackInfo callback) {
        MinecraftClient c = MinecraftClient.getInstance();
        FreeLookRuntime.update(c, FreeLookRuntime.eligible(c));
        spaceclient$player = FreeLookRuntime.owner;
    }

    @Redirect(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;increaseTransforms(FF)V"))
    private void spaceclient$turnCamera(ClientPlayerEntity player, float dx, float dy) {
        if (spaceclient$freeLook.active()) spaceclient$freeLook.turn(dx, dy);
        else player.increaseTransforms(dx, dy);
    }

    @Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;yaw:F"))
    private float spaceclient$cameraYaw(Entity entity) { return spaceclient$freeLook.active() && entity == spaceclient$player ? spaceclient$freeLook.yaw() : entity.yaw; }
    @Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevYaw:F"))
    private float spaceclient$cameraPrevYaw(Entity entity) { return spaceclient$freeLook.active() && entity == spaceclient$player ? spaceclient$freeLook.yaw() : entity.prevYaw; }
    @Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;pitch:F"))
    private float spaceclient$cameraPitch(Entity entity) { return spaceclient$freeLook.active() && entity == spaceclient$player ? spaceclient$freeLook.pitch() : entity.pitch; }
    @Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevPitch:F"))
    private float spaceclient$cameraPrevPitch(Entity entity) { return spaceclient$freeLook.active() && entity == spaceclient$player ? spaceclient$freeLook.pitch() : entity.prevPitch; }
}
