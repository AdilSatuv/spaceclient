package dev.spaceclient.mixin;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.*;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.minecraft.entity.TntEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TntEntityRenderer.class)
public abstract class TntCountdownMixin {
 @Inject(method="render(Lnet/minecraft/entity/TntEntity;DDDFF)V",at=@At("RETURN"))
 private void spaceclient$fuse(TntEntity e,double x,double y,double z,float yaw,float delta,CallbackInfo ci){
  ModSettings s=SpaceClient.mods;MinecraftClient c=MinecraftClient.getInstance();if(s==null||!s.tntCountdown()||e.removed||c.options.hudHidden||x*x+y*y+z*z>s.tntDistance()*s.tntDistance())return;
  WorldLabel.draw(String.format(Locale.ROOT,"TNT %.1fs",Math.max(0,e.fuseTimer-delta)/20f),x,y+1.35,z,s.tntColor().rgb(),s.tntBackground());
 }
}
