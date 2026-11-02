package dev.spaceclient.mods;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Original geometry; saves raw GL state without disturbing GlStateManager's cache. */
public final class CrosshairRenderer {
    private CrosshairRenderer() { }
    public static void draw(float x, float y, CrosshairStyle s) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            if (s.outline) shape(x, y, s, 1, 0x101114);
            shape(x, y, s, 0, s.color.rgb);
        } finally { GL11.glPopAttrib(); }
    }
    private static void shape(float x, float y, CrosshairStyle s, float border, int color) {
        GL11.glColor4f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, 1);
        float t = s.thickness / 2f, size = s.size, gap = s.gap;
        if (s.shape == CrosshairStyle.Shape.DOT) disk(x, y, t + border);
        else if (s.shape == CrosshairStyle.Shape.RING) {
            ring(x, y, Math.max(0, size - s.thickness - border), size + border);
        } else {
            rect(x - gap - size - border, y - t - border, x - gap + border, y + t + border);
            rect(x + gap - border, y - t - border, x + gap + size + border, y + t + border);
            rect(x - t - border, y - gap - size - border, x + t + border, y - gap + border);
            rect(x - t - border, y + gap - border, x + t + border, y + gap + size + border);
        }
        if (s.dot && s.shape != CrosshairStyle.Shape.DOT) disk(x, y, t + border);
    }
    private static void rect(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1,y1); GL11.glVertex2f(x2,y1); GL11.glVertex2f(x2,y2); GL11.glVertex2f(x1,y2);
        GL11.glEnd();
    }
    private static void disk(float x, float y, float radius) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2f(x,y);
        for (int i = 0; i <= 64; i++) { double a = Math.PI * 2 * i / 64; GL11.glVertex2d(x + Math.cos(a)*radius,y + Math.sin(a)*radius); }
        GL11.glEnd();
    }
    private static void ring(float x, float y, float inner, float outer) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= 64; i++) { double a = Math.PI * 2 * i / 64;
            GL11.glVertex2d(x + Math.cos(a)*inner,y + Math.sin(a)*inner);
            GL11.glVertex2d(x + Math.cos(a)*outer,y + Math.sin(a)*outer);
        }
        GL11.glEnd();
    }
}
