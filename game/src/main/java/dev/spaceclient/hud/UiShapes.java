package dev.spaceclient.hud;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Rounded geometry with an alpha fringe, independent of driver polygon smoothing support. */
public final class UiShapes {
    private UiShapes() { }
    public static void rounded(float x, float y, float w, float h, float r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE); GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            color(color, 1);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2f(x + w/2, y + h/2);
            for (int i = 0; i <= 64; i++) point(x,y,w,h,r,Math.max(0,r-.3f),i);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i <= 64; i++) {
                color(color, 1); point(x,y,w,h,r,Math.max(0,r-.3f),i);
                color(color, 0); point(x,y,w,h,r,r+.7f,i);
            }
            GL11.glEnd();
        } finally { GL11.glPopAttrib(); }
    }
    private static void point(float x, float y, float w, float h, float radius, float edge, int i) {
        double a = Math.PI * 2 * i / 64, c = Math.cos(a), s = Math.sin(a);
        GL11.glVertex2d(x + (c >= 0 ? w-radius : radius) + edge*c, y + (s >= 0 ? h-radius : radius) + edge*s);
    }
    private static void color(int color, float coverage) {
        GL11.glColor4f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,((color>>>24)&255)/255f*coverage);
    }
}
