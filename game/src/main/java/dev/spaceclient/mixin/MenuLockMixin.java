package dev.spaceclient.mixin;

import dev.spaceclient.menu.MenuTheme;
import dev.spaceclient.menu.MenuFocus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.LockButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LockButtonWidget.class)
public abstract class MenuLockMixin extends ButtonWidget {
    protected MenuLockMixin() { super(0, 0, 0, ""); }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spaceclient$lock(MinecraftClient client, int mx, int my, CallbackInfo ci) {
        if (!MenuTheme.applies(client.currentScreen)) return;
        LockButtonWidget button = (LockButtonWidget) (Object) this;
        if (button.visible) {
            hovered = mx >= button.x && mx < button.x + button.getWidth() && my >= button.y && my < button.y + height;
            boolean focused = client.currentScreen instanceof MenuFocus && ((MenuFocus) client.currentScreen).spaceclient$isFocused(button);
            MenuTheme.control(button.x, button.y, button.getWidth(), height, focused, hovered, button.active, false, false);
            MenuTheme.lock(button.x, button.y, button.isLocked(), button.active);
        }
        ci.cancel();
    }
}
