package dev.spaceclient.mixin;
import dev.spaceclient.mods.SoundRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayerInteractionManager.class)
public abstract class SoundAttackMixin {
 @Inject(method="attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V",at=@At("HEAD"))
 private void spaceclient$remember(PlayerEntity p,Entity target,CallbackInfo ci){MinecraftClient c=MinecraftClient.getInstance();if(c.player==p)SoundRuntime.attack(c,target);}
}
