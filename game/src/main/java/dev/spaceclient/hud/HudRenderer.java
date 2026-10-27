package dev.spaceclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.HudConfig;
import dev.spaceclient.SpaceClient;
import java.util.Locale;
import java.time.LocalTime;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

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
            case CLOCK: return "Time " + HudMetrics.clock(LocalTime.now());
            case SPEED: return client.player == null ? "Speed --" : HudMetrics.speed(
                client.player.x, client.player.z, client.player.prevX, client.player.prevZ, client.isPaused());
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
            backdrop(0, 0, textWidth(client, text), 10);
            client.textRenderer.drawWithShadow(text, 1, 0, color);
        } finally { GlStateManager.popMatrix(); }
    }
    public static void render(MinecraftClient client, HudConfig config, int width, int height) {
        if (width < 1 || height < 1) return;
        backdrop(8, 8, 8 + textWidth(client, "SPACE CLIENT"), 18);
        client.textRenderer.drawWithShadow("SPACE CLIENT", 9, 8, 0xF2B56B);
        for (HudModule module : HudModule.values()) if (module.isEnabled(config)) {
            String text = text(client, module);
            draw(client, text, bounds(client, text, config.active().placement(module), width, height),
                module == HudModule.KEYSTROKES ? 0xF2B56B : 0xFFFFFF);
        }
    }

    /** Raw GL with saved attributes leaves GlStateManager's cached state untouched. */
    private static void backdrop(int left, int top, int right, int bottom) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glColor4f(16 / 255f, 20 / 255f, 29 / 255f, 176 / 255f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2i(right, top);
            GL11.glVertex2i(left, top);
            GL11.glVertex2i(left, bottom);
            GL11.glVertex2i(right, bottom);
            GL11.glEnd();
        } finally { GL11.glPopAttrib(); }
    }
}
