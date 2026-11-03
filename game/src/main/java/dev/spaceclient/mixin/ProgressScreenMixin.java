package dev.spaceclient.mixin;
import dev.spaceclient.menu.LoadingTheme;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ProgressScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProgressScreen.class)
public abstract class ProgressScreenMixin extends Screen {
    @Shadow private String title;
    @Shadow private String task;
    @Shadow private int progress;
    @Shadow private boolean done;
    @Inject(method="render(IIF)V",at=@At("HEAD"),cancellable=true)
    private void space$progress(int x,int y,float delta,CallbackInfo ci){if(!done){LoadingTheme.render(client,width,height,title,task,progress);ci.cancel();}}
}
