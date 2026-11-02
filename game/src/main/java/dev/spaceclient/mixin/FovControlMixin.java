package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.FovRuntime;
import dev.spaceclient.mods.ZoomRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class FovControlMixin {
    @Unique private float spaceclient$lastProjection=Float.NaN;
    @Unique private boolean spaceclient$fovWasActive;
    @Unique private double spaceclient$lastZoom=1;
    /** A stationary or paused camera still needs a fresh frustum after a FOV change. */
    @Inject(method="getFov(FZ)F",at=@At("RETURN"))
    private void spaceclient$refreshFrustum(float delta,boolean worldProjection,CallbackInfoReturnable<Float> result){
        if(!worldProjection)return;
        MinecraftClient c=MinecraftClient.getInstance();boolean active=FovRuntime.active(c);
        float projection=result.getReturnValue();double zoom=ZoomRuntime.state.divisor();
        if(c.world!=null&&c.worldRenderer!=null&&(active||spaceclient$fovWasActive)
                &&(active!=spaceclient$fovWasActive||Float.isNaN(spaceclient$lastProjection)||Math.abs(projection-spaceclient$lastProjection)>.01f||Math.abs(zoom-spaceclient$lastZoom)>.001))
            c.worldRenderer.scheduleTerrainUpdate();
        spaceclient$lastProjection=projection;spaceclient$fovWasActive=active;spaceclient$lastZoom=zoom;
    }
    @Redirect(method="getFov(FZ)F",at=@At(value="FIELD",target="Lnet/minecraft/client/option/GameOptions;fov:F",opcode=180))
    private float spaceclient$baseFov(GameOptions options){return FovRuntime.active(MinecraftClient.getInstance())?SpaceClient.mods.cameraFov():options.fov;}
    @Redirect(method="updateMovementFovMultiplier()V",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSpeed()F"))
    private float spaceclient$dynamicFov(AbstractClientPlayerEntity player){
        float vanilla=player.getSpeed();return FovRuntime.active(MinecraftClient.getInstance())?FovRuntime.dynamic(vanilla,SpaceClient.mods.dynamicFov()):vanilla;
    }
}
