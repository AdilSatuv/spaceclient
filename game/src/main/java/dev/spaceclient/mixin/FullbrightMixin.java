package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.ModSettings;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class FullbrightMixin {
    @Shadow private boolean lightmapDirty;
    @Unique private int spaceclient$visualState = -1;

    /** A settings change also refreshes a paused world's lightmap. */
    @Inject(method = "render(FJ)V", at = @At("HEAD"))
    private void spaceclient$refreshLightmap(float delta, long time, CallbackInfo callback) {
        ModSettings s = SpaceClient.mods;
        if (s == null) return;
        int state = (s.fullbright() ? s.brightness().ordinal() + 1 : 0)
            + 4 * (s.timeChanger() ? s.timePreset().ordinal() + 1 : 0);
        if (state != spaceclient$visualState) {
            lightmapDirty = true;
            spaceclient$visualState = state;
        }
    }
    @Redirect(method = "updateLightmap(F)V", at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/option/GameOptions;gamma:F"))
    private float spaceclient$renderGamma(GameOptions options) {
        return SpaceClient.mods == null ? options.gamma : SpaceClient.mods.renderGamma(options.gamma);
    }
}
