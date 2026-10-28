package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.HudConfig;
import dev.spaceclient.hud.HudModulesScreen;
import dev.spaceclient.hud.HudEditorScreen;
import dev.spaceclient.hud.HudModuleSettingsScreen;
import dev.spaceclient.hud.HudRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class HudMixin {
    @Inject(method = "render(F)V", at = @At("RETURN"))
    private void spaceclient$render(float tickDelta, CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        HudConfig config = SpaceClient.hud.getConfig();
        if (!config.active().enabled || client.player == null || client.options.hudHidden || client.options.debugEnabled
                || client.currentScreen instanceof HudModulesScreen || client.currentScreen instanceof HudEditorScreen
                || client.currentScreen instanceof HudModuleSettingsScreen) return;
        Window window = new Window(client);
        HudRenderer.render(client, config, window.getWidth(), window.getHeight());
    }
}
