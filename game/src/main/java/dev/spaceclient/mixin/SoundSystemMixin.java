package dev.spaceclient.mixin;
import dev.spaceclient.mods.SoundRuntime;
import net.minecraft.client.sound.*;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
 @Redirect(method="play(Lnet/minecraft/client/sound/SoundInstance;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/sound/SoundInstance;getIdentifier()Lnet/minecraft/util/Identifier;"))
 private Identifier spaceclient$event(SoundInstance sound){return SoundRuntime.identifier(sound);}
 @Redirect(method="play(Lnet/minecraft/client/sound/SoundInstance;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/sound/WeightedSoundSet;getSoundCategory()Lnet/minecraft/client/sound/SoundCategory;"))
 private SoundCategory spaceclient$category(WeightedSoundSet chosen,SoundInstance sound){return SoundRuntime.category(sound,chosen);}
 @Redirect(method="method_7099",at=@At(value="INVOKE",target="Lnet/minecraft/client/sound/SoundInstance;getPitch()F"))
 private float spaceclient$pitch(SoundInstance sound){return SoundRuntime.pitch(sound);}
 @Redirect(method="method_7100",at=@At(value="INVOKE",target="Lnet/minecraft/client/sound/SoundInstance;getVolume()F"))
 private float spaceclient$volume(SoundInstance sound){return SoundRuntime.volume(sound);}
}
