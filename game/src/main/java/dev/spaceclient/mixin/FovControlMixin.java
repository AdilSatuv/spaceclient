package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.FovRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class FovControlMixin {
    @Redirect(method="getFov(FZ)F",at=@At(value="FIELD",target="Lnet/minecraft/client/option/GameOptions;fov:F",opcode=180))
    private float spaceclient$baseFov(GameOptions options){return FovRuntime.active(MinecraftClient.getInstance())?SpaceClient.mods.cameraFov():options.fov;}
    @Redirect(method="updateMovementFovMultiplier()V",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSpeed()F"))
    private float spaceclient$dynamicFov(AbstractClientPlayerEntity player){
        float vanilla=player.getSpeed();return FovRuntime.active(MinecraftClient.getInstance())?FovRuntime.dynamic(vanilla,SpaceClient.mods.dynamicFov()):vanilla;
    }
}
