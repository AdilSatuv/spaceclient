package dev.spaceclient.mixin;
import dev.spaceclient.mods.HitboxRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityRenderDispatcher.class)
public abstract class HitboxesMixin {
 @Inject(method="render(Lnet/minecraft/entity/Entity;DDDFF)Z",at=@At("RETURN"))
 private void spaceclient$hitbox(Entity e,double x,double y,double z,float yaw,float delta,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue())HitboxRenderer.draw(e,x,y,z);}
}
