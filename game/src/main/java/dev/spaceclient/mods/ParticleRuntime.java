package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.particle.Particle;

public final class ParticleRuntime {
    private static int thinning,extra;
    private static boolean spawningExtra;
    private ParticleRuntime(){}
    public static boolean prepare(Particle p){
        ModSettings s=SpaceClient.mods;if(s==null||!s.particles()||p==null)return true;
        int amount=Math.min(s.particleAmount(),100);thinning+=amount;
        if(thinning<100)return false;thinning-=100;
        p.scale(s.particleScale()/100f);
        if(s.particleTint()){int rgb=s.particleColor().rgb();p.setColor((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f);}
        return true;
    }
    public static int extras(){ModSettings s=SpaceClient.mods;if(spawningExtra||s==null||!s.particles())return 0;extra+=Math.max(0,s.particleAmount()-100);int n=extra/100;extra%=100;return n;}
    public static void extra(boolean v){spawningExtra=v;}
}
