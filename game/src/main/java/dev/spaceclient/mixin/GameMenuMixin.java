package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.HudModulesScreen;
import dev.spaceclient.mods.ModsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Discoverable mouse access to the same HUD screen as the right-Shift shortcut. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuMixin extends Screen {
    @Unique private ButtonWidget spaceclient$hudButton;
    @Unique private ButtonWidget spaceclient$modsButton;

    @Inject(method = "init()V", at = @At("RETURN"))
    private void spaceclient$addHudButton(CallbackInfo callback) {
        // Vanilla leaves this row between achievements/statistics and options/LAN.
        spaceclient$hudButton = new ButtonWidget(0x5343, width / 2 - 100, height / 4 + 56, 98, 20, "Space HUD");
        spaceclient$hudButton.active = client.player != null && SpaceClient.hud != null;
        buttons.add(spaceclient$hudButton);
        spaceclient$modsButton = new ButtonWidget(0x5344, width / 2 + 2, height / 4 + 56, 98, 20, "Space Mods");
        spaceclient$modsButton.active = client.player != null && SpaceClient.mods != null;
        buttons.add(spaceclient$modsButton);
    }

    @Inject(method = "buttonClicked(Lnet/minecraft/client/gui/widget/ButtonWidget;)V",
        at = @At("HEAD"), cancellable = true)
    private void spaceclient$openHud(ButtonWidget button, CallbackInfo callback) {
        if ((button != spaceclient$hudButton && button != spaceclient$modsButton) || !button.active || client.player == null) return;
        client.setScreen(button == spaceclient$hudButton ? new HudModulesScreen() : new ModsScreen(this));
        callback.cancel();
    }
}
