package dev.spaceclient.qa.mixin;
import dev.spaceclient.qa.InputNetworkCheck;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=MinecraftClient.class,priority=500)
public abstract class QaClient {
 @Inject(method="tick()V",at=@At("HEAD"))
 private void qa$input(CallbackInfo ci){InputNetworkCheck.apply(MinecraftClient.getInstance());}
}
