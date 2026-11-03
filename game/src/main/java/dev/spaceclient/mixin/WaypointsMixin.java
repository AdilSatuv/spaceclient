package dev.spaceclient.mixin;
import dev.spaceclient.mods.WaypointRuntime;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class WaypointsMixin {
 @Inject(method="renderWorld(IFJ)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/GameRenderer;renderWeather(F)V",shift=At.Shift.AFTER))
 private void spaceclient$markers(int pass,float delta,long deadline,CallbackInfo ci){WaypointRuntime.draw(delta);}
}
