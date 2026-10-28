package dev.spaceclient.mixin;

import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.menu.MenuFocus;
import dev.spaceclient.menu.MenuLayout;
import dev.spaceclient.menu.MenuNavigation;
import dev.spaceclient.menu.MenuTheme;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.LanguageButton;
import net.minecraft.client.resource.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retains vanilla init/actions/demo checks and Realms notification lifecycle. */
@Mixin(TitleScreen.class)
public abstract class TitleMenuMixin extends Screen {
    @Shadow private String oldGl1;
    @Shadow private Screen realmsNotificationScreen;
    @Shadow private boolean areRealmsNotificationsEnabled() { return false; }
    @Unique private MenuLayout spaceclient$layout;

    @Inject(method = "init()V", at = @At("RETURN"))
    private void spaceclient$layout(CallbackInfo ci) {
        // Keep the native warning, its link hitbox and layout on unsupported hardware.
        spaceclient$layout = null;
        if (!oldGl1.isEmpty() || width < 240 || height < 220) return;
        for (int i = 0; i < buttons.size(); i++) {
            ButtonWidget button = buttons.get(i);
            if (button instanceof LanguageButton)
                buttons.set(i, new ButtonWidget(button.id, button.x, button.y, I18n.translate("options.language")));
        }
        MenuNavigation.sortTitle(buttons);
        spaceclient$layout = new MenuLayout(width, height, buttons.size());
        int row = 0;
        for (ButtonWidget button : buttons) {
            button.x = spaceclient$layout.left;
            button.y = spaceclient$layout.top + row++ * spaceclient$layout.step;
            button.setWidth(spaceclient$layout.width);
        }
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spaceclient$render(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (spaceclient$layout == null) return;
        MenuLayout layout = spaceclient$layout;
        MenuTheme.background(width, height, false);
        MenuTheme.panel(layout.left - 12, layout.panelTop, layout.width + 24,
            layout.panelBottom - layout.panelTop);
        spaceclient$center("S  P  A  C  E", layout.panelTop + 12, MenuTheme.GOLD);
        spaceclient$center("Minecraft 1.8.9", layout.panelTop + 31, MenuTheme.MUTED);
        super.render(mouseX, mouseY, delta);
        // The native Realms service still owns notification rendering and mouse behavior.
        if (areRealmsNotificationsEnabled() && realmsNotificationScreen != null)
            realmsNotificationScreen.render(mouseX, mouseY, delta);
        String copyright = "Copyright Mojang AB. Do not distribute!";
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(copyright, width - 16), 8, height - 13, MenuTheme.MUTED);
        ci.cancel();
    }
    @Unique private void spaceclient$center(String text, int y, int color) {
        SpaceUiFont.draw(client, text, (width - SpaceUiFont.width(text)) / 2f, y, color);
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void spaceclient$keys(char character, int key, CallbackInfo ci) {
        if (spaceclient$layout != null && ((MenuFocus) this).spaceclient$navigate(character, key)) ci.cancel();
    }
    @Inject(method = "removed", at = @At("RETURN"))
    private void spaceclient$release(CallbackInfo ci) { SpaceUiFont.release(); }
}
