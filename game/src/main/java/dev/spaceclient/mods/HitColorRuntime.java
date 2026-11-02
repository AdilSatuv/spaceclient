package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import java.nio.FloatBuffer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
/** Change only the native hurt-tint buffer, preserving the rest of its render pass. */
public final class HitColorRuntime {
    private HitColorRuntime() { }
    public static boolean apply(LivingEntity entity,FloatBuffer buffer){
        ModSettings s=SpaceClient.mods;
        if(s==null||!s.hitColor()||entity==null||entity.hurtTime<=0||entity.deathTime>0
                ||(s.hitPlayersOnly()&&!(entity instanceof PlayerEntity)))return false;
        int rgb=s.damageColor().rgb();buffer.put(0,((rgb>>16)&255)/255f);buffer.put(1,((rgb>>8)&255)/255f);buffer.put(2,(rgb&255)/255f);buffer.put(3,s.hitOpacity()/100f);return true;
    }
}
