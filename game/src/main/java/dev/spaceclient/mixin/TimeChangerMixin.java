package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client sky only: no writes to synchronized time or the integrated server's world. */
@Mixin(World.class)
public abstract class TimeChangerMixin {
    @Inject(method = "getSkyAngle(F)F", at = @At("HEAD"), cancellable = true)
    private void spaceclient$skyTime(float delta, CallbackInfoReturnable<Float> callback) {
        if (!((Object)this instanceof ClientWorld) || SpaceClient.mods == null || !SpaceClient.mods.timeChanger()) return;
        World world = (World)(Object)this;
        if (world.dimension.getType() != 0) return;
        callback.setReturnValue(world.dimension.getSkyAngle(SpaceClient.mods.timePreset().ticks, 0));
    }
}
