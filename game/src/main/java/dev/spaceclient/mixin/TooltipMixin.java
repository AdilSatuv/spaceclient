package dev.spaceclient.mixin;
import dev.spaceclient.mods.*;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
public abstract class TooltipMixin {
 @Inject(method="renderTooltip(Ljava/util/List;II)V",at=@At("HEAD"),cancellable=true)
 private void spaceclient$tip(List<String> lines,int x,int y,CallbackInfo ci){if(TooltipRuntime.draw((Screen)(Object)this,lines,x,y))ci.cancel();}
 @Redirect(method="renderTooltip(Lnet/minecraft/item/ItemStack;II)V",at=@At(value="INVOKE",target="Lnet/minecraft/item/ItemStack;getTooltip(Lnet/minecraft/entity/player/PlayerEntity;Z)Ljava/util/List;"))
 private List<String> spaceclient$food(ItemStack stack,PlayerEntity p,boolean advanced){return NutritionRuntime.tooltip(stack.getTooltip(p,advanced),stack);}
 @Inject(method="handleMouse()V",at=@At("HEAD"))
 private void spaceclient$wheel(CallbackInfo ci){TooltipRuntime.wheel((Screen)(Object)this);DynamicFpsRuntime.activity();}
 @Inject(method="handleKeyboard()V",at=@At("HEAD"))
 private void spaceclient$keyActivity(CallbackInfo ci){DynamicFpsRuntime.activity();}
 @Inject(method="renderBackground(I)V",at=@At("HEAD"))
 private void spaceclient$blur(int tint,CallbackInfo ci){InventoryBlurRenderer.draw();}
}
