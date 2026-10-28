package dev.spaceclient.mixin;

import dev.spaceclient.menu.MenuFocus;
import dev.spaceclient.menu.MenuTheme;
import dev.spaceclient.menu.MenuNavigation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(Screen.class)
public abstract class MenuScreenMixin implements MenuFocus {
    @Shadow protected List<ButtonWidget> buttons;
    @Shadow protected net.minecraft.client.MinecraftClient client;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow protected abstract void buttonClicked(ButtonWidget button);
    @Unique private ButtonWidget spaceclient$focus;

    @Override public boolean spaceclient$isFocused(ButtonWidget button) {
        return button == spaceclient$focus;
    }
    @Inject(method = "renderBackground(I)V", at = @At("HEAD"), cancellable = true)
    private void spaceclient$background(int offset, CallbackInfo ci) {
        if (MenuTheme.applies((Screen) (Object) this)) {
            MenuTheme.background(width, height, client.world != null);
            ci.cancel();
        }
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void spaceclient$mouse(int x, int y, int button, CallbackInfo ci) {
        spaceclient$focus = null;
    }
    // Only button-only pause menu: forms, lists, sliders and their keyboard handling are untouched.
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void spaceclient$keys(char character, int key, CallbackInfo ci) {
        if (!((Object) this instanceof GameMenuScreen)) return;
        if (spaceclient$navigate(character, key)) ci.cancel();
    }
    @Override public boolean spaceclient$navigate(char character, int key) {
        if (key == Keyboard.KEY_TAB || key == Keyboard.KEY_DOWN || key == Keyboard.KEY_UP) {
            int direction = key == Keyboard.KEY_UP || (key == Keyboard.KEY_TAB && Screen.hasShiftDown()) ? -1 : 1;
            spaceclient$focus = MenuNavigation.next(buttons, spaceclient$focus, direction);
            return true;
        }
        if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER || key == Keyboard.KEY_SPACE)
            && spaceclient$focus != null && buttons.contains(spaceclient$focus)
            && spaceclient$focus.visible && spaceclient$focus.active) {
            spaceclient$focus.playDownSound(client.getSoundManager());
            buttonClicked(spaceclient$focus);
            return true;
        }
        return false;
    }
}
