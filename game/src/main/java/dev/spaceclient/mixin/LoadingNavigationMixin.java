package dev.spaceclient.mixin;
import dev.spaceclient.menu.MenuFocus;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These native button-only screens override rather than delegate Screen.keyPressed. */
@Mixin({ConnectScreen.class,DisconnectedScreen.class})
public abstract class LoadingNavigationMixin {
    @Inject(method="keyPressed(CI)V",at=@At("HEAD"),cancellable=true)
    private void space$navigate(char c,int key,CallbackInfo ci){
        if(Keyboard.isRepeatEvent()&&(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER||key==Keyboard.KEY_SPACE)){ci.cancel();return;}
        if(((MenuFocus)this).spaceclient$navigate(c,key))ci.cancel();
    }
}
