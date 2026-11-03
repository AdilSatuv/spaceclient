package dev.spaceclient.mods;

import java.lang.ref.WeakReference;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class CombatMetricsRuntime {
    private static final CombatChain chain=new CombatChain();
    private static WeakReference<Object> owner=new WeakReference<Object>(null);
    private static WeakReference<Object> world=new WeakReference<Object>(null);
    private static double distance=Double.NaN;
    private static long attackTime;
    private CombatMetricsRuntime(){}
    public static void context(MinecraftClient c){if(owner.get()!=c.player||world.get()!=c.world){chain.reset();distance=Double.NaN;owner=new WeakReference<Object>(c.player);world=new WeakReference<Object>(c.world);}}
    public static void attack(MinecraftClient c,PlayerEntity player,Entity target){
        context(c);if(c.player!=player||target==null||c.currentScreen!=null)return;
        long now=System.nanoTime();chain.attack(target.getEntityId(),now);attackTime=now;
        distance=Double.NaN;
        if(c.result!=null&&c.result.entity==target&&c.result.pos!=null){
            Vec3d p=c.result.pos;double x=p.x-c.player.x,y=p.y-(c.player.y+c.player.getEyeHeight()),z=p.z-c.player.z;
            distance=Math.sqrt(x*x+y*y+z*z);
        }
    }
    public static void hurt(MinecraftClient c,Entity e){context(c);if(e!=null)chain.hurt(e.getEntityId(),e==c.player,System.nanoTime());}
    public static int combo(MinecraftClient c){context(c);return c.player==null?0:chain.count(System.nanoTime());}
    public static String reach(MinecraftClient c){context(c);return !Double.isFinite(distance)||System.nanoTime()-attackTime>5_000_000_000L?"Reach: --":String.format(Locale.ROOT,"Reach: %.2f blocks",distance);}
}
