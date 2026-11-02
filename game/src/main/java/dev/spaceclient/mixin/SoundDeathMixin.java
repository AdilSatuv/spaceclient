package dev.spaceclient.mixin;
import dev.spaceclient.mods.SoundRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class SoundDeathMixin {
 @Inject(method="onEntityStatus(Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;)V",at=@At("TAIL"))
 private void spaceclient$down(EntityStatusS2CPacket p,CallbackInfo ci){MinecraftClient c=MinecraftClient.getInstance();if(p.getStatus()==3&&c.world!=null)SoundRuntime.death(c,p.getEntity(c.world),System.nanoTime());}
}
