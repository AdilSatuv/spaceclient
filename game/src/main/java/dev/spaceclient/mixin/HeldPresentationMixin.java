package dev.spaceclient.mixin;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.*;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HeldItemRenderer.class)
public abstract class HeldPresentationMixin {
 @Shadow @Final private MinecraftClient client;
 @Shadow private void applyEquipAndSwingOffset(float equip,float swing) { }
 @Redirect(method="renderArmHoldingItem(F)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V"))
 private void spaceclient$legacySwing(HeldItemRenderer owner,float equip,float swing,float delta){applyEquipAndSwingOffset(equip,AnimationsRuntime.swing(client,swing,delta));}
 @Inject(method="applyEatOrDrinkTransformation(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)V",at=@At("RETURN"))
 private void spaceclient$legacyEat(AbstractClientPlayerEntity player,float delta,CallbackInfo ci){if(SpaceClient.mods!=null&&SpaceClient.mods.animations()&&SpaceClient.mods.animationEat()){GlStateManager.translate(-.08f,.04f,0);GlStateManager.rotate(-8,0,0,1);}}
 @Inject(method="applyBowTransformation(FLnet/minecraft/client/network/AbstractClientPlayerEntity;)V",at=@At("RETURN"))
 private void spaceclient$legacyBow(float delta,AbstractClientPlayerEntity player,CallbackInfo ci){if(SpaceClient.mods!=null&&SpaceClient.mods.animations()&&SpaceClient.mods.animationBow()){GlStateManager.translate(-.12f,-.03f,.03f);GlStateManager.rotate(-8,0,0,1);}}
 @Inject(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/GlStateManager;pushMatrix()V",shift=At.Shift.AFTER))
 private void spaceclient$itemSize(LivingEntity e,ItemStack item,ModelTransformation.Mode mode,CallbackInfo ci){if(mode==ModelTransformation.Mode.FIRST_PERSON){float scale=ItemPresentation.heldScale();GlStateManager.scale(scale,scale,scale);}}
}
