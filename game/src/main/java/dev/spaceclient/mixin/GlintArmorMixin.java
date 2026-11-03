package dev.spaceclient.mixin;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.GlintRuntime;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ArmorFeatureRenderer.class)
public abstract class GlintArmorMixin {
 private static final String METHOD="renderGlint(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/model/EntityModel;FFFFFFF)V";
 @Redirect(method=METHOD,at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/GlStateManager;color(FFFF)V",ordinal=1))
 private void spaceclient$color(float r,float g,float b,float a){if(SpaceClient.mods!=null&&SpaceClient.mods.enchantmentGlint()){int c=GlintRuntime.color(0xFF8040CC);r=(c>>16&255)/255f;g=(c>>8&255)/255f;b=(c&255)/255f;}GlStateManager.color(r,g,b,a);}
 @Redirect(method=METHOD,at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V"))
 private void spaceclient$speed(float x,float y,float z){float speed=SpaceClient.mods!=null&&SpaceClient.mods.enchantmentGlint()?SpaceClient.mods.glintSpeed()/100f:1;GlStateManager.translate(x*speed,y*speed,z);}
}
