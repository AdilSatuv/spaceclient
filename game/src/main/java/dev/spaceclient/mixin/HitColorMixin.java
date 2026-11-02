package dev.spaceclient.mixin;

import dev.spaceclient.mods.HitColorRuntime;
import java.nio.FloatBuffer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class HitColorMixin {
    @Shadow protected FloatBuffer buffer;
    @Inject(method="method_10252(Lnet/minecraft/entity/LivingEntity;FZ)Z",at=@At(value="INVOKE",target="Ljava/nio/FloatBuffer;flip()Ljava/nio/Buffer;"))
    private void spaceclient$hurtTint(LivingEntity entity,float delta,boolean features,CallbackInfoReturnable<Boolean> callback){HitColorRuntime.apply(entity,buffer);}
}
