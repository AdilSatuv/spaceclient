package dev.spaceclient.mixin;
import dev.spaceclient.mods.ParticleRuntime;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ParticleManager.class)
public abstract class ParticlesMixin {
 @Inject(method="addParticle(Lnet/minecraft/client/particle/Particle;)V",at=@At("HEAD"),cancellable=true)
 private void spaceclient$style(Particle p,CallbackInfo ci){if(!ParticleRuntime.prepare(p))ci.cancel();}
 @Inject(method="addParticle(IDDDDDD[I)Lnet/minecraft/client/particle/Particle;",at=@At("RETURN"))
 private void spaceclient$extra(int id,double x,double y,double z,double vx,double vy,double vz,int[] args,CallbackInfoReturnable<Particle> ci){if(ci.getReturnValue()==null)return;int n=ParticleRuntime.extras();if(n==0)return;ParticleRuntime.extra(true);try{for(int i=0;i<n;i++)((ParticleManager)(Object)this).addParticle(id,x,y,z,vx,vy,vz,args);}finally{ParticleRuntime.extra(false);}}
}
