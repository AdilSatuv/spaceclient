package dev.spaceclient.mixin;

import dev.spaceclient.mods.ModSettings;
import dev.spaceclient.mods.WeatherRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.world.LayeredBiomeSource;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class WeatherRendererMixin {
    @Shadow private MinecraftClient client;
    @Redirect(method="renderWeather(F)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/biome/Biome;method_3830()Z"))
    private boolean spaceclient$precipitationBiome(Biome biome) {
        return WeatherRuntime.mode(client.world)!=null||biome.method_3830();
    }
    @Redirect(method="renderWeather(F)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/LayeredBiomeSource;method_3852(FI)F"))
    private float spaceclient$precipitationType(LayeredBiomeSource source,float temperature,int height) {
        ModSettings.WeatherMode mode=WeatherRuntime.mode(client.world);
        return mode==ModSettings.WeatherMode.SNOW?0f:mode==ModSettings.WeatherMode.RAIN?1f:source.method_3852(temperature,height);
    }
    @Inject(method="tickRainSplashing()V",at=@At("HEAD"),cancellable=true)
    private void spaceclient$rainSplashes(CallbackInfo callback) {
        ModSettings.WeatherMode mode=WeatherRuntime.mode(client.world);
        if(mode==ModSettings.WeatherMode.CLEAR||mode==ModSettings.WeatherMode.SNOW)callback.cancel();
    }
}
