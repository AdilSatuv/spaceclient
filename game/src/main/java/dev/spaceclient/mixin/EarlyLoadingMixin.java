package dev.spaceclient.mixin;
import dev.spaceclient.menu.LoadingTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class EarlyLoadingMixin {
    @Shadow private Identifier mojang;
    @Inject(method="loadLogo(Lnet/minecraft/client/texture/TextureManager;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/gl/Framebuffer;unbind()V"))
    private void space$early(TextureManager textures,CallbackInfo ci){LoadingTheme.early((MinecraftClient)(Object)this,textures,mojang);}
}
