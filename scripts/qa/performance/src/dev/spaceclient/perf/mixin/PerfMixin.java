package dev.spaceclient.perf.mixin;
import dev.spaceclient.perf.PerfCapture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class PerfMixin {
    @Inject(method="render(FJ)V",at=@At("HEAD"))
    private void perf$begin(float delta,long time,CallbackInfo info){PerfCapture.begin();}
    @Inject(method="render(FJ)V",at=@At("RETURN"))
    private void perf$end(float delta,long time,CallbackInfo info){PerfCapture.frame(MinecraftClient.getInstance());}
}
