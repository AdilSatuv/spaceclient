package dev.spaceclient.menu;

import dev.spaceclient.hud.SpaceUiFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;

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
        fill(x, y, x + width, y + height, 0x7041434A);
        fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xE0101114);
        fill(x + 16, y, x + width - 16, y + 1, GOLD);
    }
    public static void button(MinecraftClient client, ButtonWidget button, int height,
                              boolean hovered, boolean focused) {
        int x = button.x, y = button.y, width = button.getWidth();
        boolean highlight = button.active && (hovered || focused);
        boolean primary = client.currentScreen instanceof TitleScreen && button.active
            && (button.id == 1 || button.id == 11);
        fill(x, y, x + width, y + height, highlight ? GOLD : 0xFF51535A);
        fill(x + 1, y + 1, x + width - 1, y + height - 1,
            !button.active ? 0xFF18191C : primary ? (highlight ? 0xFFF0D5A6 : GOLD)
                : highlight ? 0xFF302B23 : 0xFF222429);
        if (focused && button.active) fill(x + 3, y + 3, x + 5, y + height - 3, primary ? 0xFF211C15 : GOLD);
        String label = SpaceUiFont.trimToWidth(button.message, Math.max(1, width - 16));
        SpaceUiFont.draw(client, label, x + (width - SpaceUiFont.width(label)) / 2f,
            y + (height - SpaceUiFont.height()) / 2f, !button.active ? 0xFF888A91 : primary ? 0xFF211C15 : TEXT);
    }
}
