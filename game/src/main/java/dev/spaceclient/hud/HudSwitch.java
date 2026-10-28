package dev.spaceclient.hud;

import org.lwjgl.opengl.GL11;

/** Antialiased pill and circular thumb. Input hitboxes are managed by the containing card. */
final class HudSwitch {
    private HudSwitch() { }
    static void draw(int x, int y, boolean enabled) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_POLYGON_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (enabled) GL11.glColor4f(.82f, .67f, .44f, 1);
            else GL11.glColor4f(.23f, .24f, .26f, 1);
            pill(x + 2, y + 1, 24, 12);
            if (enabled) GL11.glColor4f(.96f, .95f, .92f, 1);
            else GL11.glColor4f(.59f, .60f, .62f, 1);
            pill(enabled ? x + 15 : x + 4, y + 2, 10, 10);
        } finally { GL11.glPopAttrib(); }
    }
    private static void pill(float x, float y, float width, float height) {
        float radius = height / 2f;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x + width / 2f, y + radius);
        for (int i = 0; i <= 48; i++) {
            double angle = Math.PI * 2 * i / 48;
            float centerX = Math.cos(angle) >= 0 ? x + width - radius : x + radius;
            GL11.glVertex2d(centerX + radius * Math.cos(angle), y + radius + radius * Math.sin(angle));
        }
        GL11.glEnd();
    }
}
