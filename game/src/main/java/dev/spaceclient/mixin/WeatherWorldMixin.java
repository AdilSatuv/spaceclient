package dev.spaceclient.mixin;

import dev.spaceclient.mods.ModSettings;
import dev.spaceclient.mods.WeatherRuntime;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WeatherWorldMixin {
    @Inject(method="getRainGradient(F)F",at=@At("HEAD"),cancellable=true)
    private void spaceclient$rain(float delta,CallbackInfoReturnable<Float> callback) {
        ModSettings.WeatherMode mode=WeatherRuntime.mode((World)(Object)this);
        if(mode!=null)callback.setReturnValue(mode==ModSettings.WeatherMode.CLEAR?0f:1f);
    }
    @Inject(method="getThunderGradient(F)F",at=@At("HEAD"),cancellable=true)
    private void spaceclient$thunder(float delta,CallbackInfoReturnable<Float> callback) {
        if(WeatherRuntime.mode((World)(Object)this)!=null)callback.setReturnValue(0f);
    }
}
