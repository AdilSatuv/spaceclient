package dev.spaceclient.hud;

import org.lwjgl.opengl.GL11;

/** Original line icons in a 22-unit grid; state and the current GUI transform are restored. */
final class HudModuleIcons {
    private HudModuleIcons() { }
    static void drawSection(HudSection section, int x, int y, int size, boolean selected) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(size / 22f, size / 22f, 1);
            GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND); GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(1.3f);
            if (selected) GL11.glColor4f(.88f, .76f, .55f, 1);
            else GL11.glColor4f(.65f, .67f, .70f, 1);
            switch (section) {
                case HOME:
                    path(false, 1,10, 11,2, 21,10); path(false, 4,9, 4,20, 9,20, 9,14, 13,14, 13,20, 18,20, 18,9); break;
                case COMBAT:
                    circle(11,11,8); path(false, 11,0, 11,5); path(false, 11,17, 11,22);
                    path(false, 0,11, 5,11); path(false, 17,11, 22,11); break;
                case VISUALS:
                    path(true, 1,11, 5,6, 9,4, 13,4, 17,6, 21,11, 17,16, 13,18, 9,18, 5,16);
                    circle(11,11,3); break;
                case HUD:
                    rect(2,3,20,16); path(false, 11,16, 11,20); path(false, 7,20, 15,20); break;
                case PLAYER: case SETTINGS:
                    circle(11,11,8); circle(11,11,3);
                    path(false, 11,0, 11,3); path(false, 11,19, 11,22);
                    path(false, 0,11, 3,11); path(false, 19,11, 22,11); break;
                case WORLD:
                    path(false, 16,2, 19,4, 17,7, 14,8, 7,19, 4,20, 2,18, 3,15, 12,8, 13,4, 16,2); break;
                case MISC:
                    rect(2,2,9,9); rect(13,2,20,9); rect(2,13,9,20); rect(13,13,20,20); break;
            }
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    static void draw(HudModule module, int x, int y, int size) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(size / 22f, size / 22f, 1);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(1.3f);
            GL11.glColor4f(.94f, .94f, .94f, 1);
            switch (module) {
                case FPS: case SPEED:
                    rect(1, 13, 5, 21); rect(8, 8, 12, 21); rect(15, 2, 19, 21); break;
                case COORDINATES:
                    path(false, 11,21, 4,11, 4,8, 5,5, 8,3, 11,2, 14,3, 17,5, 18,8, 18,11, 11,21);
                    circle(11, 8, 3); break;
                case KEYSTROKES:
                    rect(7, 0, 14, 7); rect(0, 10, 6, 17); rect(8, 10, 14, 17); rect(16, 10, 22, 17); break;
                case CPS:
                    path(true, 5,7, 6,4, 9,2, 13,2, 16,4, 17,7, 17,16, 15,19, 12,20, 9,20, 6,19, 5,16);
                    path(false, 5,9, 17,9); path(false, 11,0, 11,8); break;
                case COMPASS:
                    circle(11, 11, 10); path(true, 6,16, 9,8, 16,5, 13,13); path(false, 9,8, 13,13); break;
                case ARMOR:
                    path(true, 2,5, 6,3, 8,5, 14,5, 16,3, 20,5, 18,10, 16,9, 16,19, 14,20, 8,20, 6,19, 6,9, 4,10); break;
                case CLOCK:
                    circle(11, 11, 9); path(false, 11,5, 11,11, 15,14); break;
                case HEALTH:
                    path(true, 11,20, 2,11, 2,7, 4,4, 7,4, 11,7, 15,4, 18,4, 20,7, 20,11); break;
                case HUNGER:
                    circle(9, 8, 5); path(false, 12,12, 19,19, 17,21, 10,14); break;
                default:
                    path(true, 8,2, 14,2, 14,6, 17,9, 17,19, 5,19, 5,9, 8,6); path(false, 5,13, 17,13);
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
    private static void rect(float x1, float y1, float x2, float y2) {
        path(true, x1,y1, x2,y1, x2,y2, x1,y2);
    }
    private static void circle(float x, float y, float radius) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            GL11.glVertex2d(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }
    private static void path(boolean closed, float... points) {
        GL11.glBegin(closed ? GL11.GL_LINE_LOOP : GL11.GL_LINE_STRIP);
        for (int i = 0; i < points.length; i += 2) GL11.glVertex2f(points[i], points[i + 1]);
        GL11.glEnd();
    }
}
