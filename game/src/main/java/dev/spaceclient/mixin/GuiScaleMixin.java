package dev.spaceclient.mixin;
import dev.spaceclient.mods.GuiScaleRuntime;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Window.class)
public abstract class GuiScaleMixin {
 @Redirect(method="<init>",at=@At(value="FIELD",target="Lnet/minecraft/client/option/GameOptions;guiScale:I"))
 private int spaceclient$scale(GameOptions options){return GuiScaleRuntime.requested(options.guiScale);}
}
