package dev.spaceclient.hud;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.BreakIterator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloadListener;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GLContext;

/** Antialiased Space UI text. Minecraft HUD text and editable fields keep their native renderer. */
public final class SpaceUiFont {
    private static final int RASTER_SCALE = 4;
    private static final int FONT_SIZE = 10;
    private static final int PAD = 2;
    private static final int MAX_TEXT_LENGTH = 256;
    private static final int MAX_CACHE_ENTRIES = 128;
    private static final long MAX_CACHE_BYTES = 16L * 1024 * 1024;
    private static final String FONT_PATH = "/assets/spaceclient/font/DejaVuSans.ttf";
    private static final Map<String, Texture> CACHE = new LinkedHashMap<String, Texture>(32, .75f, true);
    private static final Font FONT = loadFont();
    private static final FontMetrics METRICS = makeMetrics();
    private static ResourceManager registeredManager;
    private static volatile boolean needsClear;
    private static boolean gpuFailed;
    private static int cachedScale;
    private static long cacheBytes;
    private SpaceUiFont() { }

    private static Font loadFont() {
        try (InputStream in = SpaceUiFont.class.getResourceAsStream(FONT_PATH)) {
            if (in == null) return null;
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont((float) (FONT_SIZE * RASTER_SCALE));
        } catch (Exception | LinkageError failure) { return null; }
    }
    private static FontMetrics makeMetrics() {
        if (FONT == null) return null;
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try { configure(g); return g.getFontMetrics(FONT); } finally { g.dispose(); }
        } catch (RuntimeException | LinkageError failure) { return null; }
    }
    private static void configure(Graphics2D g) {
        g.setFont(FONT);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }
    private static boolean supported(String text) {
        if (FONT == null || METRICS == null || gpuFailed || text == null || text.length() > MAX_TEXT_LENGTH || text.indexOf('\u00a7') >= 0) return false;
        try { return FONT.canDisplayUpTo(text) < 0; } catch (RuntimeException | LinkageError failure) { return false; }
    }
    private static MinecraftClient client() {
        try { return MinecraftClient.getInstance(); } catch (RuntimeException | LinkageError failure) { return null; }
    }
    private static int vanillaWidth(String text) {
        MinecraftClient c = client();
        return c == null || c.textRenderer == null || text == null ? 0 : c.textRenderer.getStringWidth(text);
    }
    public static int width(String text) {
        if (text == null || text.isEmpty()) return 0;
        if (!supported(text)) return vanillaWidth(text);
        try { return (METRICS.stringWidth(text) + RASTER_SCALE - 1) / RASTER_SCALE; }
        catch (RuntimeException | LinkageError failure) { return vanillaWidth(text); }
    }
    public static int height() {
        if (FONT != null && METRICS != null && !gpuFailed) return (METRICS.getHeight() + RASTER_SCALE - 1) / RASTER_SCALE;
        MinecraftClient c = client();
        return c == null || c.textRenderer == null ? 9 : c.textRenderer.fontHeight;
    }
    public static String trimToWidth(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (!supported(text)) {
            MinecraftClient c = client();
            if (c == null || c.textRenderer == null) return "";
            String vanilla = c.textRenderer.trimToWidth(text, maxWidth);
            BreakIterator boundary = BreakIterator.getCharacterInstance(Locale.ROOT);
            boundary.setText(text);
            int end = boundary.isBoundary(vanilla.length()) ? vanilla.length() : boundary.preceding(vanilla.length());
            if (end == BreakIterator.DONE) end = 0;
            if (end > 0 && text.charAt(end - 1) == '\u00a7') end--;
            return text.substring(0, end);
        }
        BreakIterator it = BreakIterator.getCharacterInstance(Locale.ROOT);
        it.setText(text);
        int last = 0;
        for (int end = it.next(); end != BreakIterator.DONE; end = it.next()) {
            if (width(text.substring(0, end)) > maxWidth) break;
            last = end;
        }
        return text.substring(0, last);
    }

    /** Must be called on Minecraft's render thread with an active GL context. */
    public static void draw(MinecraftClient client, String text, float x, float y, int color) {
        if (client == null || text == null || text.isEmpty()) return;
        if (!supported(text)) { vanilla(client, text, x, y, color); return; }
        try {
            attachReloadListener(client);
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            boolean pushed = false;
            try {
                GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
                pushed = true;
                int scale = Math.max(2, Math.min(8, new Window(client).getScaleFactor()));
                if (needsClear || cachedScale != scale) { clearTextures(); needsClear = false; cachedScale = scale; }
                Texture texture = CACHE.get(text);
                if (texture == null) {
                    texture = createTexture(text, scale);
                    CACHE.put(text, texture);
                    cacheBytes += texture.bytes();
                    while (CACHE.size() > MAX_CACHE_ENTRIES || cacheBytes > MAX_CACHE_BYTES) {
                        Iterator<Texture> it = CACHE.values().iterator();
                        Texture stale = it.next(); it.remove(); cacheBytes -= stale.bytes(); GL11.glDeleteTextures(stale.id);
                    }
                }
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id);
                int a = (color >>> 24) & 255;
                if (a == 0) a = 255; // Minecraft's 0xRRGGBB convention.
                GL11.glColor4f(((color >>> 16) & 255) / 255f, ((color >>> 8) & 255) / 255f, (color & 255) / 255f, a / 255f);
                float w = texture.width / (float) scale, h = texture.height / (float) scale;
                float left = x - PAD / (float) RASTER_SCALE, top = y - PAD / (float) RASTER_SCALE;
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0, 0); GL11.glVertex2f(left, top);
                GL11.glTexCoord2f(0, 1); GL11.glVertex2f(left, top + h);
                GL11.glTexCoord2f(1, 1); GL11.glVertex2f(left + w, top + h);
                GL11.glTexCoord2f(1, 0); GL11.glVertex2f(left + w, top);
                GL11.glEnd();
            } finally {
                if (pushed) GL11.glPopAttrib();
                GL13.glActiveTexture(active);
            }
        } catch (RuntimeException | LinkageError failure) {
            gpuFailed = true;
            try { clearTextures(); } catch (RuntimeException | LinkageError ignored) { CACHE.clear(); cacheBytes = 0; }
            vanilla(client, text, x, y, color);
        }
    }
    private static void vanilla(MinecraftClient c, String text, float x, float y, int color) {
        if (c.textRenderer != null) c.textRenderer.drawWithShadow(text, x, y, color);
    }
    private static void attachReloadListener(MinecraftClient c) {
        ResourceManager manager = c.getResourceManager();
        if (manager != registeredManager && manager instanceof ReloadableResourceManager) {
            registeredManager = manager;
            ((ReloadableResourceManager) manager).registerListener(new ResourceReloadListener() {
                @Override public void reload(ResourceManager ignored) { needsClear = true; }
            });
        }
    }
    private static Texture createTexture(String text, int scale) {
        int pixelWidth = Math.max(1, (int) Math.ceil((METRICS.stringWidth(text) + 2 * PAD) * scale / (double) RASTER_SCALE));
        int pixelHeight = Math.max(1, (int) Math.ceil((METRICS.getHeight() + 2 * PAD) * scale / (double) RASTER_SCALE));
        if (pixelWidth > GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE) || (long) pixelWidth * pixelHeight * 4 > MAX_CACHE_BYTES)
            throw new IllegalArgumentException("Text is too large for a font texture");
        if ((GLContext.getCapabilities().OpenGL21 || GLContext.getCapabilities().GL_ARB_pixel_buffer_object)
                && GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING) != 0)
            throw new IllegalStateException("Pixel unpack buffer is bound");
        BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            configure(g); g.scale(scale / (double) RASTER_SCALE, scale / (double) RASTER_SCALE);
            g.setComposite(AlphaComposite.Src); g.setColor(java.awt.Color.WHITE);
            g.drawString(text, PAD, PAD + METRICS.getAscent());
        } finally { g.dispose(); }
        ByteBuffer pixels = BufferUtils.createByteBuffer(pixelWidth * pixelHeight * 4);
        for (int yy = 0; yy < pixelHeight; yy++) for (int xx = 0; xx < pixelWidth; xx++) {
            int argb = image.getRGB(xx, yy);
            pixels.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) (argb >>> 24));
        }
        pixels.flip();
        int id = GL11.glGenTextures();
        if (id == 0) throw new IllegalStateException("Unable to allocate font texture");
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            boolean clientStatePushed = false;
            try {
                GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
                clientStatePushed = true;
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, pixelWidth, pixelHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            } finally {
                if (clientStatePushed) GL11.glPopClientAttrib();
            }
            int actualWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int actualHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (actualWidth != pixelWidth || actualHeight != pixelHeight)
                throw new IllegalStateException("Font texture allocation failed");
            return new Texture(id, pixelWidth, pixelHeight);
        } catch (RuntimeException | LinkageError failure) { GL11.glDeleteTextures(id); throw failure; }
    }
    private static void clearTextures() {
        for (Texture texture : CACHE.values()) GL11.glDeleteTextures(texture.id);
        CACHE.clear();
        cacheBytes = 0;
    }
    /** Release cached GPU textures on screen removal, on the render thread. */
    public static void release() {
        try { clearTextures(); needsClear = false; }
        catch (RuntimeException | LinkageError failure) { CACHE.clear(); cacheBytes = 0; gpuFailed = true; }
    }
    private static final class Texture {
        final int id, width, height;
        Texture(int id, int width, int height) { this.id = id; this.width = width; this.height = height; }
        long bytes() { return (long) width * height * 4; }
    }
}
