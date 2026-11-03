package dev.spaceclient.mixin;
import dev.spaceclient.mods.DynamicFpsRuntime;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class DynamicFpsMixin {
 @Inject(method="getMaxFramerate()I",at=@At("RETURN"),cancellable=true)
 private void spaceclient$cap(CallbackInfoReturnable<Integer> ci){ci.setReturnValue(DynamicFpsRuntime.limit(ci.getReturnValue()));}
 @Inject(method="stop()V",at=@At("HEAD"))
 private void spaceclient$release(CallbackInfo ci){dev.spaceclient.mods.InventoryBlurRenderer.release();}
}
