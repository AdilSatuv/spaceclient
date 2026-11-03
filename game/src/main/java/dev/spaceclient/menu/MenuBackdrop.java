package dev.spaceclient.menu;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.util.Identifier;
import net.minecraft.client.texture.NativeImageBackedTexture;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** One bundled, static landscape. Cover fitting keeps its proportions on resize. */
public final class MenuBackdrop extends DrawableHelper {
    private static Identifier image;
    private static boolean attempted;
    private static Identifier listImage;
    private static final MenuBackdrop DRAW = new MenuBackdrop();
    private MenuBackdrop() { }
    public static void render(MinecraftClient client, int width, int height) {
        if (!attempted) {
            attempted = true;
            try (InputStream in = MenuBackdrop.class.getResourceAsStream("/assets/spaceclient/menu/cosmic-night.jpg")) {
                BufferedImage pixels = in == null ? null : ImageIO.read(in);
                if (pixels != null) image = client.getTextureManager().registerDynamicTexture("space-menu", new NativeImageBackedTexture(pixels));
            } catch (Exception | LinkageError failure) { image = null; }
        }
        MenuTheme.background(width, height, false);
        if (image != null) {
            client.getTextureManager().bindTexture(image);
            GlStateManager.color(1, 1, 1, 1);
            float scale = Math.max(width / 1672f, height / 941f);
            int w = (int) Math.ceil(1672 * scale), h = (int) Math.ceil(941 * scale);
            drawTexture((width - w) / 2, (height - h) / 2, 0, 0, w, h, (float) w, (float) h);
        }
        DRAW.fillGradient(0, 0, width, height, 0xD5091221, 0xEE080B12);
        stars(width, height, false);
        orbit(width, height);
        DRAW.fillGradient(0, 0, width, height, 0x00050910, 0xA0050910);
    }
    /** Layout editing keeps the live world and real HUD coordinates visible. */
    public static void editorOverlay(int width, int height) {
        DRAW.fillGradient(0, 0, width, height, 0x600A1424, 0xA0060A12);
        stars(width, height, true);
        fill(0, 0, width, 1, 0xFF71644F);
    }
    private static void stars(int width, int height, boolean edgesOnly) {
        for (int i = 0; i < 54; i++) {
            int x = (i * 127 + 31) % Math.max(1, width);
            int y = (i * 73 + 17) % Math.max(1, height);
            if (edgesOnly && x > 24 && x < width - 24 && y > 24 && y < height - 24) continue;
            fill(x, y, x + 1, y + 1, i % 4 == 0 ? 0xA6DAC49E : 0x605B7897);
        }
    }
    private static void orbit(int width, int height) {
        float x = width * .80f, y = height * .45f, r = Math.min(height * .39f, width * .26f);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST); GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            disk(x, y, r + 3, .42f, .36f, .26f, .30f);
            disk(x, y, r, .08f, .13f, .21f, 1);
            disk(x - r * .12f, y + r * .06f, r * .96f, .035f, .055f, .09f, 1);
            GL11.glTranslatef(x, y, 0); GL11.glRotatef(-22, 0, 0, 1);
            GL11.glLineWidth(1);
            for (int ring = 0; ring < 2; ring++) {
                GL11.glColor4f(.76f, .66f, .47f, ring == 0 ? .35f : .12f);
                GL11.glBegin(GL11.GL_LINE_LOOP);
                for (int i = 0; i < 96; i++) {
                    double angle = Math.PI * 2 * i / 96;
                    GL11.glVertex2d(Math.cos(angle) * r * (1.45 + ring * .10), Math.sin(angle) * r * (.42 + ring * .03));
                }
                GL11.glEnd();
            }
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    private static void disk(float x, float y, float radius, float red, float green, float blue, float alpha) {
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2f(x, y);
        for (int i = 0; i <= 96; i++) {
            double angle = Math.PI * 2 * (i % 96) / 96;
            GL11.glVertex2d(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }
    /** Native scrolling lists retain clipping and selection, with a quiet blue surface instead of dirt. */
    public static Identifier listTexture(MinecraftClient client) {
        if (listImage == null) {
            BufferedImage pixel = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            pixel.setRGB(0, 0, 0xFF65758F);
            listImage = client.getTextureManager().registerDynamicTexture("space-menu-list", new NativeImageBackedTexture(pixel));
        }
        return listImage;
    }
}
