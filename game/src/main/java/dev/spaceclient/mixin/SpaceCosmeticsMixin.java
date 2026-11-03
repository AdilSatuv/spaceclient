package dev.spaceclient.mixin;

import dev.spaceclient.cosmetics.CosmeticFeature;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class SpaceCosmeticsMixin extends LivingEntityRenderer<AbstractClientPlayerEntity> {
    protected SpaceCosmeticsMixin(EntityRenderDispatcher dispatcher,EntityModel model,float shadow) { super(dispatcher,model,shadow); }
    @Inject(method="<init>(Lnet/minecraft/client/render/entity/EntityRenderDispatcher;Z)V",at=@At("RETURN"))
    private void space$features(EntityRenderDispatcher dispatcher,boolean slim,CallbackInfo info) {
        features.add(new CosmeticFeature((PlayerEntityRenderer)(Object)this));
    }
}
