package dev.spaceclient.mixin;
import dev.spaceclient.mods.GlintRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ItemRenderer.class)
public abstract class GlintItemMixin {
 @ModifyConstant(method="renderGlint(Lnet/minecraft/client/render/model/BakedModel;)V",constant=@Constant(intValue=-8372020))
 private int spaceclient$color(int vanilla){return GlintRuntime.color(vanilla);}
 @Redirect(method="renderGlint(Lnet/minecraft/client/render/model/BakedModel;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/MinecraftClient;getTime()J"))
 private long spaceclient$time(){return GlintRuntime.time(MinecraftClient.getTime());}
}
