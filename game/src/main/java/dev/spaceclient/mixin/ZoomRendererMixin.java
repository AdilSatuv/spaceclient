package dev.spaceclient.mixin;

import dev.spaceclient.mods.ZoomRuntime;
import dev.spaceclient.mods.FovRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class ZoomRendererMixin {
    @Inject(method = "render(FJ)V", at = @At("HEAD"))
    private void spaceclient$zoomFrame(float delta, long time, CallbackInfo callback) {
        ZoomRuntime.update(MinecraftClient.getInstance());
    }
    @Inject(method = "getFov(FZ)F", at = @At("RETURN"), cancellable = true)
    private void spaceclient$zoomFov(float delta, boolean worldProjection, CallbackInfoReturnable<Float> callback) {
        if (worldProjection) {
            float projection=(float)(callback.getReturnValue()/ZoomRuntime.state.divisor());
            // One final projection owns both Zoom and FOV Control culling updates.
            FovRuntime.projection(MinecraftClient.getInstance(),projection);
            callback.setReturnValue(projection);
        }
    }
}
