package dev.spaceclient.qa.mixin;
import dev.spaceclient.qa.InputNetworkCheck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GameRenderer.class,priority=500)
public abstract class QaRenderer {
 @Inject(method="render(FJ)V",at=@At("HEAD"))
 private void qa$input(float f,long n,CallbackInfo ci){InputNetworkCheck.apply(MinecraftClient.getInstance());}
 @Inject(method="render(FJ)V",at=@At("RETURN"))
 private void qa$frame(float f,long n,CallbackInfo ci){InputNetworkCheck.frame(MinecraftClient.getInstance());}
}
