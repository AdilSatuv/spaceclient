package dev.spaceclient.perf.mixin;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MinecraftClient.class)
public abstract class PerfFocus {
    @Inject(method="closeScreen()V",at=@At("HEAD"),cancellable=true)
    private void perf$noMouseGrab(CallbackInfo info){info.cancel();}
    @Inject(method="getMaxFramerate()I",at=@At("HEAD"),cancellable=true)
    private void perf$constantBudget(CallbackInfoReturnable<Integer> info){info.setReturnValue(260);}
}
