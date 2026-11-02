package dev.spaceclient.mods;
import dev.spaceclient.SpaceClient;
import java.lang.ref.WeakReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.entity.*;
import net.minecraft.util.Identifier;
public final class SoundRuntime {
 public static final RecentTarget recent=new RecentTarget();private static WeakReference<Object> world=new WeakReference<Object>(null);
 private SoundRuntime() { }
 public static void context(MinecraftClient c){Object w=c==null?null:c.world;if(world.get()!=w){recent.clear();world=new WeakReference<Object>(w);}if(SpaceClient.mods==null||!SpaceClient.mods.sounds())recent.clear();}
 public static ModSettings.SoundTheme theme(SoundInstance sound){ModSettings s=SpaceClient.mods;if(s==null||!s.sounds()||sound==null||sound.isRepeatable()||sound instanceof TickableSoundInstance)return ModSettings.SoundTheme.VANILLA;int g=SoundPolicy.group(sound.getIdentifier().toString());return g==1?s.blockSounds():g==2?s.combatSounds():ModSettings.SoundTheme.VANILLA;}
 public static Identifier identifier(SoundInstance sound){return new Identifier(SoundPolicy.map(sound.getIdentifier().toString(),theme(sound)));}
 public static SoundCategory category(SoundInstance sound,WeightedSoundSet chosen){if(theme(sound)==ModSettings.SoundTheme.VANILLA)return chosen.getSoundCategory();WeightedSoundSet original=MinecraftClient.getInstance().getSoundManager().get(sound.getIdentifier());return original==null?chosen.getSoundCategory():original.getSoundCategory();}
 public static float volume(SoundInstance sound){return sound.getVolume()*(theme(sound)==ModSettings.SoundTheme.VANILLA?1:SpaceClient.mods.soundsVolume()/100f);}
 public static float pitch(SoundInstance sound){return sound.getPitch()*SoundPolicy.pitch(theme(sound));}
 public static void attack(MinecraftClient c,Entity target){context(c);if(SpaceClient.mods!=null&&SpaceClient.mods.sounds()&&c!=null&&target instanceof LivingEntity&&((LivingEntity)target).getHealth()>0)recent.mark(target,System.nanoTime());}
 public static boolean death(MinecraftClient c,Entity target,long nanos){context(c);if(c==null||c.player==null||SpaceClient.mods==null||!SpaceClient.mods.sounds()||SpaceClient.mods.killChime()==ModSettings.KillChime.OFF||!recent.consume(target,nanos))return false;playChime(c);return true;}
 public static void preview(MinecraftClient c){if(c!=null&&SpaceClient.mods!=null&&SpaceClient.mods.killChime()!=ModSettings.KillChime.OFF)playChime(c);}
 private static void playChime(MinecraftClient c){ModSettings s=SpaceClient.mods;float x=c.player==null?0:(float)c.player.x,y=c.player==null?0:(float)c.player.y,z=c.player==null?0:(float)c.player.z;c.getSoundManager().play(new PositionedSoundInstance(new Identifier(s.killChime().id),s.soundsVolume()/100f,1,x,y,z));}
}
