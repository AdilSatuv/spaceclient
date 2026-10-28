package dev.spaceclient.menu;

import dev.spaceclient.hud.SpaceUiFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.resource.language.I18n;
import com.mojang.blaze3d.platform.GlStateManager;

/** Shared Space surfaces; custom interactive widgets keep their own renderer and behavior. */
public final class MenuTheme extends DrawableHelper {
    public static final int TEXT = 0xFFF1F0EC, MUTED = 0xFFB6B7BD, GOLD = 0xFFE3C38A;
    private static final MenuTheme DRAW = new MenuTheme();
    private MenuTheme() { }
    public static boolean applies(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return screen instanceof TitleScreen || screen instanceof GameMenuScreen
            || name.startsWith("net.minecraft.client.gui.screen.options.")
            || name.startsWith("net.minecraft.client.gui.screen.world.")
            || name.startsWith("net.minecraft.client.gui.screen.multiplayer.")
            || name.equals("net.minecraft.client.gui.screen.SettingsScreen")
            || name.equals("net.minecraft.client.gui.screen.VideoOptionsScreen")
            || name.equals("net.minecraft.client.gui.screen.ResourcePackScreen")
            || name.equals("net.minecraft.client.gui.screen.SoundsScreen")
            || name.equals("net.minecraft.client.gui.screen.AddServerScreen")
            || name.equals("net.minecraft.client.gui.screen.DirectConnectScreen")
            || name.equals("net.minecraft.client.gui.screen.OpenToLanScreen");
    }
    public static boolean standard(ButtonWidget button) {
        return button.getClass() == ButtonWidget.class || button.getClass() == OptionButtonWidget.class;
    }
    public static void background(int width, int height, boolean inWorld) {
        DRAW.fillGradient(0, 0, width, height, inWorld ? 0xD5101114 : 0xFF191B20,
            inWorld ? 0xEE090A0D : 0xFF090A0D);
        fill(0, 0, width, 1, 0xFF6F6250);
    }
    public static void panel(int x, int y, int width, int height) {
        rounded(x, y, width, height, 4, 0x7041434A);
        rounded(x + 1, y + 1, width - 2, height - 2, 3, 0xED101114);
        fill(x + 16, y, x + width - 16, y + 1, GOLD);
    }
    private static void rounded(int x, int y, int w, int h, int r, int color) {
        fill(x + r, y, x + w - r, y + h, color);
        fill(x, y + r, x + r, y + h - r, color);
        fill(x + w - r, y + r, x + w, y + h - r, color);
        fill(x + 1, y + 1, x + r, y + r, color);
        fill(x + w - r, y + 1, x + w - 1, y + r, color);
        fill(x + 1, y + h - r, x + r, y + h - 1, color);
        fill(x + w - r, y + h - r, x + w - 1, y + h - 1, color);
    }
    public static void sectionSurface(int width, int height) {
        if (width < 240 || height < 220) return;
        rounded(12, 28, width - 24, height - 40, 4, 0xE8101114);
        fill(24, 28, width - 24, 29, 0xFF6F6250);
    }
    public static void sectionHeader(MinecraftClient client, Screen screen, int width, int height) {
        String name = screen.getClass().getName();
        String key = name.equals("net.minecraft.client.gui.screen.SettingsScreen") ? "options.title"
            : name.equals("net.minecraft.client.gui.screen.world.SelectWorldScreen") ? "selectWorld.title"
            : name.equals("net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen") ? "multiplayer.title" : null;
        if (key == null || width < 320 || height < 240) return;
        fill(0, 0, width, 28, 0xFF101114);
        String title = SpaceUiFont.trimToWidth(I18n.translate(key), width - 160);
        SpaceUiFont.draw(client, "S P A C E", 14, 10, GOLD);
        SpaceUiFont.draw(client, title, (width - SpaceUiFont.width(title)) / 2f, 10, TEXT);
        fill(12, 27, width - 12, 28, 0xFF6F6250);
    }
    public static void titleDetails(MinecraftClient client, MenuLayout layout, int width, int height) {
        if (width >= 560) {
            String version = "SPACE / 1.8.9";
            SpaceUiFont.draw(client, version, width - SpaceUiFont.width(version) - 18, 14, MUTED);
            // Decorative orbit marker, no simulated status or service indicator.
            icon(14, width - 34, 32, GOLD);
        }
        if (layout.panelBottom + 25 < height - 13) {
            String hint = I18n.translate("space.menu.keyboard");
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(hint, layout.width),
                layout.left, layout.panelBottom + 8, MUTED);
        }
    }
    private static String description(int id) {
        switch (id) {
            case 1: case 11: return I18n.translate("space.menu.worlds");
            case 2: return I18n.translate("space.menu.servers");
            case 14: return I18n.translate("space.menu.realms");
            case 0: return I18n.translate("space.menu.settings");
            case 5: return I18n.translate("space.menu.language");
            case 4: return I18n.translate("space.menu.quit");
            default: return "";
        }
    }
    private static void smallText(MinecraftClient client, String text, int x, int y, int maxWidth, int color) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(.75f, .75f, 1);
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(text, (int) (maxWidth / .75f)), x / .75f, y / .75f, color);
        } finally { GlStateManager.popMatrix(); }
    }
    private static void icon(int id, int x, int y, int color) {
        if (id == 1 || id == 11) {
            fill(x, y + 2, x + 12, y + 3, color); fill(x, y + 10, x + 12, y + 11, color);
            fill(x, y + 3, x + 1, y + 10, color); fill(x + 11, y + 3, x + 12, y + 10, color);
            fill(x + 5, y + 3, x + 6, y + 10, color); fill(x + 1, y + 6, x + 11, y + 7, color);
        } else if (id == 2) {
            for (int i = 0; i < 3; i++) {
                fill(x + 1, y + i * 5, x + 11, y + i * 5 + 3, color);
                fill(x + 3, y + i * 5 + 1, x + 4, y + i * 5 + 2, 0xFF222429);
            }
        } else if (id == 0) {
            fill(x + 5, y, x + 7, y + 12, color); fill(x, y + 5, x + 12, y + 7, color);
            rounded(x + 2, y + 2, 8, 8, 2, color); fill(x + 5, y + 5, x + 7, y + 7, 0xFF222429);
        } else if (id == 4) {
            fill(x, y + 1, x + 1, y + 12, color); fill(x, y + 1, x + 7, y + 2, color);
            fill(x, y + 11, x + 7, y + 12, color); fill(x + 4, y + 6, x + 12, y + 7, color);
            fill(x + 9, y + 4, x + 10, y + 9, color);
        } else {
            rounded(x + 1, y + 1, 10, 10, 3, color);
            rounded(x + 2, y + 2, 8, 8, 2, 0xFF222429);
            fill(x + 5, y, x + 6, y + 12, color); fill(x, y + 5, x + 12, y + 6, color);
        }
    }
    public static void button(MinecraftClient client, ButtonWidget button, int height,
                              boolean hovered, boolean focused) {
        int x = button.x, y = button.y, width = button.getWidth();
        boolean highlight = button.active && (hovered || focused);
        boolean primary = client.currentScreen instanceof TitleScreen && button.active
            && (button.id == 1 || button.id == 11);
        rounded(x, y, width, height, 3, highlight ? GOLD : 0xFF484A51);
        rounded(x + 1, y + 1, width - 2, height - 2, 2,
            !button.active ? 0xFF18191C : primary ? (highlight ? 0xFFF0D5A6 : GOLD)
                : highlight ? 0xFF302B23 : 0xFF222429);
        if (focused && button.active) fill(x + 3, y + 3, x + 5, y + height - 3, primary ? 0xFF211C15 : GOLD);
        if (client.currentScreen instanceof TitleScreen && width >= 180) {
            int color = !button.active ? 0xFF888A91 : primary ? 0xFF211C15 : TEXT;
            rounded(x + 9, y + (height - 18) / 2, 22, 18, 3, primary ? 0x30211C15 : 0xFF17191E);
            icon(button.id, x + 14, y + (height - 12) / 2, color);
            String title = SpaceUiFont.trimToWidth(button.message, width - 68);
            SpaceUiFont.draw(client, title, x + 40, y + (height >= 28 ? 3 : (height - SpaceUiFont.height()) / 2f), color);
            if (height >= 28) smallText(client, description(button.id), x + 40, y + 17, width - 68,
                primary ? 0xFF453A28 : MUTED);
            fill(x + width - 14, y + height / 2 - 2, x + width - 13, y + height / 2 + 3, color);
            fill(x + width - 13, y + height / 2 - 1, x + width - 12, y + height / 2 + 2, color);
            return;
        }
        String label = SpaceUiFont.trimToWidth(button.message, Math.max(1, width - 16));
        SpaceUiFont.draw(client, label, x + (width - SpaceUiFont.width(label)) / 2f,
            y + (height - SpaceUiFont.height()) / 2f, !button.active ? 0xFF888A91 : primary ? 0xFF211C15 : TEXT);
    }
}
