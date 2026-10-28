package dev.spaceclient.menu;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.util.Identifier;
import net.minecraft.client.texture.NativeImageBackedTexture;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** One bundled, static landscape. Cover fitting keeps its proportions on resize. */
public final class MenuBackdrop extends DrawableHelper {
    private static Identifier image;
    private static boolean attempted;
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
        if (image == null) { MenuTheme.background(width, height, false); return; }
        client.getTextureManager().bindTexture(image);
        GlStateManager.color(1, 1, 1, 1);
        float scale = Math.max(width / 1672f, height / 941f);
        int w = (int) Math.ceil(1672 * scale), h = (int) Math.ceil(941 * scale);
        drawTexture((width - w) / 2, (height - h) / 2, 0, 0, w, h, (float) w, (float) h);
        DRAW.fillGradient(0, 0, width, height, 0x18070B12, 0x8005090F);
        // A local scrim isolates the controls while leaving the landscape visible.
        int edge = Math.min(width, width >= 560 ? width * 3 / 40 + 290 : width);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.rotate(-90, 0, 0, 1);
            DRAW.fillGradient(-height, 0, 0, edge, 0xAA080B11, 0x00080B11);
        } finally { GlStateManager.popMatrix(); }
    }
}
