package dev.spaceclient.mixin;
import dev.spaceclient.menu.LoadingTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LoadingScreenRenderer;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingScreenRenderer.class)
public abstract class LoadingRendererMixin {
    @Shadow private MinecraftClient client;
    @Shadow private String title;
    @Shadow private String field_1028;
    @Inject(method="setProgressPercentage(I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/gl/Framebuffer;unbind()V"))
    private void space$loading(int progress,CallbackInfo ci){Window w=new Window(client);LoadingTheme.render(client,w.getWidth(),w.getHeight(),title,field_1028,progress);}
}
