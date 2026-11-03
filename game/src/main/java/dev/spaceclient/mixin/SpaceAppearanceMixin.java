package dev.spaceclient.mixin;

import dev.spaceclient.cosmetics.CosmeticsRuntime;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class SpaceAppearanceMixin {
    private AbstractClientPlayerEntity space$self() { return (AbstractClientPlayerEntity)(Object)this; }
    // Legacy mappings are reversed: PlayerEntityRenderer binds getCapeId() as the player's SKIN.
    @Inject(method="getCapeId()Lnet/minecraft/util/Identifier;",at=@At("HEAD"),cancellable=true)
    private void space$skin(CallbackInfoReturnable<Identifier> result) {
        Identifier skin=CosmeticsRuntime.skin(space$self());if(skin!=null)result.setReturnValue(skin);
    }
    // CapeFeatureRenderer binds getSkinId() as the CAPE. Keep its native motion and visibility checks.
    @Inject(method="getSkinId()Lnet/minecraft/util/Identifier;",at=@At("HEAD"),cancellable=true)
    private void space$cape(CallbackInfoReturnable<Identifier> result) {
        Identifier cape=CosmeticsRuntime.cape(space$self());if(cape!=null)result.setReturnValue(cape);
    }
    @Inject(method="hasSkinTexture",at=@At("HEAD"),cancellable=true)
    private void space$hasSkin(CallbackInfoReturnable<Boolean> result) {
        if(CosmeticsRuntime.skin(space$self())!=null)result.setReturnValue(true);
    }
    @Inject(method="canRenderCapeTexture",at=@At("HEAD"),cancellable=true)
    private void space$hasCape(CallbackInfoReturnable<Boolean> result) {
        if(CosmeticsRuntime.cape(space$self())!=null)result.setReturnValue(true);
    }
    @Inject(method="getModel",at=@At("HEAD"),cancellable=true)
    private void space$model(CallbackInfoReturnable<String> result) {
        if(CosmeticsRuntime.skin(space$self())!=null)result.setReturnValue("slim".equals(CosmeticsRuntime.appearance().skinModel)?"slim":"default");
    }
}
