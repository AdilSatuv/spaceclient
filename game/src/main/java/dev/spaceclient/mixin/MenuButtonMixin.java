package dev.spaceclient.mixin;

import dev.spaceclient.menu.MenuFocus;
import dev.spaceclient.menu.MenuTheme;
import dev.spaceclient.menu.MenuButtonSize;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonWidget.class)
public abstract class MenuButtonMixin implements MenuButtonSize {
    @Shadow protected int height;
    @Shadow protected boolean hovered;
    @Shadow protected abstract void mouseDragged(MinecraftClient client, int mouseX, int mouseY);
    @Override public void spaceclient$setHeight(int value) { height = value; }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spaceclient$render(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
        ButtonWidget button = (ButtonWidget) (Object) this;
        if (!MenuTheme.applies(client.currentScreen) || !MenuTheme.themedButton(button, client.currentScreen)) return;
        if (button.visible) {
            hovered = mouseX >= button.x && mouseY >= button.y
                && mouseX < button.x + button.getWidth() && mouseY < button.y + height;
            boolean focused = client.currentScreen instanceof MenuFocus
                && ((MenuFocus) client.currentScreen).spaceclient$isFocused(button);
            // Native sliders update their option/volume and label in this callback.
            // Paint over its legacy handle only after those side effects have run.
            mouseDragged(client, mouseX, mouseY);
            MenuTheme.button(client, button, height, hovered, focused);
        }
        ci.cancel();
    }
}
