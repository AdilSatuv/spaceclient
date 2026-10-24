package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.HudConfig;
import dev.spaceclient.hud.HudModule;
import dev.spaceclient.hud.HudModulesScreen;
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
        if (!config.enabled || client.player == null || client.options.hudHidden || client.options.debugEnabled
                || client.currentScreen instanceof HudModulesScreen) return;
        int y = 8;
        client.textRenderer.drawWithShadow("SPACE CLIENT", 8, y, 0xF2B56B);
        y += 12;
        if (HudModule.FPS.isEnabled(config)) {
            client.textRenderer.drawWithShadow(MinecraftClient.getCurrentFps() + " FPS", 8, y, 0xFFFFFF);
            y += 12;
        }
        if (HudModule.COORDINATES.isEnabled(config)) {
            String position = String.format(java.util.Locale.ROOT, "XYZ %.1f / %.1f / %.1f", client.player.x, client.player.y, client.player.z);
            client.textRenderer.drawWithShadow(position, 8, y, 0xFFFFFF);
            y += 12;
        }
        if (HudModule.KEYSTROKES.isEnabled(config)) {
            String keys = (client.options.forwardKey.isPressed() ? "[W] " : " W  ")
                + (client.options.leftKey.isPressed() ? "[A] " : " A  ")
                + (client.options.backKey.isPressed() ? "[S] " : " S  ")
                + (client.options.rightKey.isPressed() ? "[D]" : " D ");
            client.textRenderer.drawWithShadow(keys, 8, y, 0xF2B56B);
            y += 12;
        }
        if (HudModule.CPS.isEnabled(config)) client.textRenderer.drawWithShadow(
            "CPS " + SpaceClient.clicks.getLeftCps() + " L / " + SpaceClient.clicks.getRightCps() + " R", 8, y, 0xFFFFFF);
    }
}
