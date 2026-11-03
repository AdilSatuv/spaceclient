package dev.spaceclient.qa.mixin;
import dev.spaceclient.qa.InputNetworkCheck;
import dev.spaceclient.mods.SoundRuntime;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=SoundRuntime.class,remap=false)
public abstract class ChimeObserve {
 @Inject(method="playChime",at=@At("HEAD"),remap=false)
 private static void qa$chime(MinecraftClient c,CallbackInfo ci){InputNetworkCheck.chimes++;}
}
