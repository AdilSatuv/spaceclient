package dev.spaceclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.HudConfig;
import dev.spaceclient.SpaceClient;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;

/** Shared live data and drawing for the in-game HUD and the editor preview. */
public final class HudRenderer {
    private HudRenderer() { }
    public static String text(MinecraftClient client, HudModule module) {
        switch (module) {
            case FPS: return MinecraftClient.getCurrentFps() + " FPS";
            case COORDINATES: return String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f",
                client.player.x, client.player.y, client.player.z);
            case KEYSTROKES: return (client.options.forwardKey.isPressed() ? "[W] " : " W  ")
                + (client.options.leftKey.isPressed() ? "[A] " : " A  ")
                + (client.options.backKey.isPressed() ? "[S] " : " S  ")
                + (client.options.rightKey.isPressed() ? "[D]" : " D ");
            case CPS: return "CPS " + SpaceClient.clicks.getLeftCps() + " L / " + SpaceClient.clicks.getRightCps() + " R";
            case COMPASS: return "Compass " + Compass.direction(client.player.yaw);
            default: throw new AssertionError(module);
        }
    }
    public static int textWidth(MinecraftClient client, String text) {
        return Math.max(1, client.textRenderer.getStringWidth(text) + 2);
    }
    public static HudPlacement.Bounds bounds(MinecraftClient client, String text, HudPlacement placement, int width, int height) {
        return placement.resolve(width, height, textWidth(client, text), 10);
    }
    public static void draw(MinecraftClient client, String text, HudPlacement.Bounds bounds, int color) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(bounds.x, bounds.y, 0);
            GlStateManager.scale(bounds.scale, bounds.scale, 1);
            client.textRenderer.drawWithShadow(text, 0, 0, color);
        } finally { GlStateManager.popMatrix(); }
    }
    public static void render(MinecraftClient client, HudConfig config, int width, int height) {
        if (width < 1 || height < 1) return;
        client.textRenderer.drawWithShadow("SPACE CLIENT", 8, 8, 0xF2B56B);
        for (HudModule module : HudModule.values()) if (module.isEnabled(config)) {
            String text = text(client, module);
            draw(client, text, bounds(client, text, config.active().placement(module), width, height),
                module == HudModule.KEYSTROKES ? 0xF2B56B : 0xFFFFFF);
        }
    }
}
