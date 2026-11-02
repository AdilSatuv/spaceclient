package dev.spaceclient.mixin;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.*;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemEntityRenderer.class)
public abstract class DroppedItemMixin {
 @Redirect(method="method_10221",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V",ordinal=0))
 private void spaceclient$restHeight(float x,float y,float z,ItemEntity e,double px,double py,double pz,float delta,BakedModel model){
  boolean physics=SpaceClient.mods!=null&&SpaceClient.mods.itemPhysics();float size=ItemPresentation.droppedScale();
  if(physics||size!=1){float bob=net.minecraft.util.math.MathHelper.sin((e.getAge()+delta)/10f+e.hoverHeight)*.1f+.1f;float base=y-(float)py-bob;y=(float)py+(physics?(e.onGround&&!model.hasDepth()&&SpaceClient.mods.physicsFlat()?.025f:base*size):bob+base*size);}GlStateManager.translate(x,y,z);
 }
 @Redirect(method="method_10221",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/GlStateManager;rotate(FFFF)V"))
 private void spaceclient$tumble(float angle,float x,float y,float z,ItemEntity e,double px,double py,double pz,float delta,BakedModel model){
  if(SpaceClient.mods==null||!SpaceClient.mods.itemPhysics()){GlStateManager.rotate(angle,x,y,z);return;}
  if(e.onGround){GlStateManager.rotate(Math.floorMod(e.getEntityId()*31,360),0,1,0);if(!model.hasDepth()&&SpaceClient.mods.physicsFlat())GlStateManager.rotate(90,1,0,0);}
  else{float spin=ItemPresentation.spin(e.getAge()+delta,SpaceClient.mods.physicsSpeed(),e.getEntityId());GlStateManager.rotate(spin,0,1,0);GlStateManager.rotate(spin*.7f,1,0,0);}
 }
 @Inject(method="method_10221",at=@At("RETURN"))
 private void spaceclient$droppedSize(ItemEntity e,double x,double y,double z,float delta,BakedModel model,CallbackInfoReturnable<Integer> ci){float s=ItemPresentation.droppedScale();GlStateManager.scale(s,s,s);}
}
