package dev.spaceclient.menu;

import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import dev.spaceclient.BuildIdentity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.screen.ResourcePackScreen;
import net.minecraft.client.gui.screen.SoundsScreen;
import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.gui.screen.DirectConnectScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.options.ChatOptionsScreen;
import net.minecraft.client.gui.screen.options.SkinOptionsScreen;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
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
        return screen instanceof TitleScreen || screen instanceof GameMenuScreen
            || screen instanceof SettingsScreen || screen instanceof VideoOptionsScreen
            || screen instanceof ResourcePackScreen || screen instanceof SoundsScreen
            || screen instanceof AddServerScreen || screen instanceof DirectConnectScreen
            || screen instanceof OpenToLanScreen || screen instanceof SelectWorldScreen
            || screen instanceof CreateWorldScreen || screen instanceof EditWorldScreen
            || screen instanceof MultiplayerScreen || screen instanceof ChatOptionsScreen
            || screen instanceof SkinOptionsScreen || screen instanceof ControlsOptionsScreen
            || screen instanceof LanguageOptionsScreen
            || screen instanceof net.minecraft.client.gui.screen.ConnectScreen
            || screen instanceof net.minecraft.client.gui.screen.DisconnectedScreen
            || screen instanceof net.minecraft.client.gui.screen.SnooperScreen
            || screen instanceof net.minecraft.client.gui.screen.CustomizeFlatLevelScreen
            || screen instanceof net.minecraft.client.gui.screen.SuperflatPresetScreen
            || screen instanceof net.minecraft.client.gui.screen.CustomizeWorldScreen
            || screen instanceof net.minecraft.client.gui.screen.CustomizedWorldPresetsScreen
            || screen instanceof net.minecraft.client.gui.screen.ConfirmScreen
            || screen instanceof net.minecraft.client.gui.screen.AchievementsScreen
            || screen instanceof net.minecraft.client.gui.screen.StatsScreen
            || screen instanceof net.minecraft.client.gui.screen.DemoScreen
            || screen instanceof net.minecraft.client.gui.screen.FatalErrorScreen
            || screen instanceof net.minecraft.client.gui.screen.StreamScreen
            || screen instanceof net.minecraft.client.gui.screen.StreamIngestScreen
            || screen instanceof net.minecraft.client.gui.screen.StreamUtilitiesScreen
            || screen instanceof net.minecraft.client.gui.screen.TwitchErrorScreen;
    }
    public static boolean standard(ButtonWidget button) {
        return button.getClass() == ButtonWidget.class || button.getClass() == OptionButtonWidget.class;
    }
    public static boolean themedButton(ButtonWidget button, Screen screen) {
        // Subclass drag callbacks are still invoked by MenuButtonMixin before painting.
        return standard(button) || applies(screen);
    }
    public static void background(int width, int height, boolean inWorld) {
        DRAW.fillGradient(0, 0, width, height, inWorld ? 0xC5091423 : 0xFF111D2E,
            inWorld ? 0xE8070C15 : 0xFF070C15);
        fill(0, 0, width, 1, 0xFF6F6250);
    }
    public static void panel(int x, int y, int width, int height) {
        UiShapes.rounded(x + 2, y + 4, width, height, 8, 0x70000000);
        UiShapes.rounded(x, y, width, height, 8, 0xFF455164);
        UiShapes.rounded(x + 1, y + 1, width - 2, height - 2, 7, 0xEC111A28);
        fill(x + 12, y + 1, x + Math.min(width - 12, 62), y + 2, GOLD);
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
        rounded(12, 28, width - 24, height - 40, 4, 0xB8111A28);
        fill(24, 28, width - 24, 29, 0xFF6F6250);
    }
    public static void sectionHeader(MinecraftClient client, Screen screen, int width, int height) {
        if(screen instanceof net.minecraft.client.gui.screen.ConnectScreen){LoadingTheme.header(client,width,height);LoadingTheme.credits(client,width,height);return;}
        String key = screen instanceof SettingsScreen ? "options.title"
            : screen instanceof SelectWorldScreen ? "selectWorld.title"
            : screen instanceof MultiplayerScreen ? "multiplayer.title" : null;
        if (key == null || width < 320 || height < 240) return;
        fill(0, 0, width, 28, 0xFF101114);
        String title = SpaceUiFont.trimToWidth(I18n.translate(key), width - 160);
        SpaceUiFont.draw(client, "S P A C E", 14, 10, GOLD);
        SpaceUiFont.draw(client, title, (width - SpaceUiFont.width(title)) / 2f, 10, TEXT);
        fill(12, 27, width - 12, 28, 0xFF6F6250);
    }
    public static void titleDetails(MinecraftClient client, MenuLayout layout, int width, int height) {
        String build = BuildIdentity.label();
        if (width >= 560) {
            String version = "SPACE / 1.8.9 / " + build;
            SpaceUiFont.draw(client, version, width - SpaceUiFont.width(version) - 18, 14, MUTED);
            // Decorative orbit marker, no simulated status or service indicator.
            icon(14, width - 34, 32, GOLD);
        } else if (width >= 320) {
            SpaceUiFont.draw(client, build, width - SpaceUiFont.width(build) - 12, 4, MUTED);
        } else if (width >= 240) {
            // The compact brand starts at y=13 at the smallest themed layout.
            int buildX=width-8-(int)Math.ceil(SpaceUiFont.width(build)*.75);
            fill(buildX-2,0,width-6,12,0xFF101114);
            smallText(client,build,buildX,1,width-16,MUTED);
        }
        if (layout.panelBottom + 25 < height - 13) {
            String hint = MenuStrings.get(client, "keyboard");
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(hint, layout.width),
                layout.left, layout.panelBottom + 8, MUTED);
        }
    }
    private static String description(MinecraftClient client, int id) {
        switch (id) {
            case 1: case 11: return MenuStrings.get(client, "worlds");
            case 2: return MenuStrings.get(client, "servers");
            case 14: return MenuStrings.get(client, "realms");
            case 0: return MenuStrings.get(client, "settings");
            case 5: return MenuStrings.get(client, "language");
            case 4: return MenuStrings.get(client, "quit");
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
        boolean primary = client.currentScreen instanceof TitleScreen && button.active
            && (button.id == 1 || button.id == 11);
        control(x, y, width, height, focused, hovered, button.active, primary, false);
        if (client.currentScreen instanceof TitleScreen && width >= 180) {
            int color = !button.active ? 0xFF888A91 : primary ? 0xFF211C15 : TEXT;
            rounded(x + 9, y + (height - 18) / 2, 22, 18, 3, primary ? 0x30211C15 : 0xFF17191E);
            icon(button.id, x + 14, y + (height - 12) / 2, color);
            String title = SpaceUiFont.trimToWidth(button.message, width - 68);
            SpaceUiFont.draw(client, title, x + 40, y + (height >= 28 ? 3 : (height - SpaceUiFont.height()) / 2f), color);
            if (height >= 28) smallText(client, description(client, button.id), x + 40, y + 17, width - 68,
                primary ? 0xFF453A28 : MUTED);
            fill(x + width - 14, y + height / 2 - 2, x + width - 13, y + height / 2 + 3, color);
            fill(x + width - 13, y + height / 2 - 1, x + width - 12, y + height / 2 + 2, color);
            return;
        }
        // Native world-list actions have narrow hitboxes. Fit translated labels before truncating.
        int available = Math.max(1, width - 8);
        float scale = Math.min(1f, Math.max(.75f, available / (float) Math.max(1, SpaceUiFont.width(button.message))));
        int limit = (int) (available / scale);
        String label = button.message;
        if (SpaceUiFont.width(label) > limit)
            label = SpaceUiFont.trimToWidth(label, Math.max(1, limit - SpaceUiFont.width("…"))) + "…";
        float progress = button instanceof MenuSlider ? ((MenuSlider) button).spaceclient$progress()
            : button instanceof net.minecraft.client.gui.widget.SliderWidget ? ((net.minecraft.client.gui.widget.SliderWidget) button).getProgress() : -1;
        if (progress >= 0) slider(x + 8, y + height - 5, width - 16, progress, button.active);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(scale, scale, 1);
            SpaceUiFont.draw(client, label, (x + (width - SpaceUiFont.width(label) * scale) / 2f) / scale,
                (progress >= 0 ? y + 2 : y + (height - SpaceUiFont.height() * scale) / 2f) / scale,
                !button.active ? 0xFF888A91 : primary ? 0xFF211C15 : TEXT);
        } finally { GlStateManager.popMatrix(); }
    }
    public static void control(int x, int y, int width, int height, boolean focused, boolean hovered,
                               boolean enabled, boolean primary, boolean selected) {
        int edge = !enabled ? 0xFF354151 : focused ? GOLD : hovered ? 0xFF9D8D70 : 0xFF4A586C;
        UiShapes.rounded(x, y + 2, width, height, 5, 0x50000000);
        UiShapes.rounded(x, y, width, height, 5, edge);
        UiShapes.rounded(x + 1, y + 1, width - 2, height - 2, 4,
            !enabled ? 0xFF18202B : primary ? (hovered || focused ? 0xFFF0D5A6 : GOLD)
                : selected || focused ? 0xFF354052 : hovered ? 0xFF2B394C : 0xFF202D40);
        if (enabled && height > 8 && width > 12)
            fill(x + 5, y + 1, x + width - 5, y + 2, primary ? 0x80FFFFFF : 0x305D789B);
        if (selected && enabled) fill(x + 1, y + 5, x + 3, y + height - 5, GOLD);
        if (focused && enabled) {
            fill(x + 4, y - 2, x + width - 4, y - 1, TEXT);
            fill(x + 4, y + height + 1, x + width - 4, y + height + 2, TEXT);
        }
    }
    public static void card(int x, int y, int width, int height, boolean focused, boolean hovered, boolean selected) {
        control(x, y, width, height, focused, hovered, true, false, selected);
    }
    public static void slider(int x, int y, int width, float progress, boolean enabled) {
        int offset = Math.round(Math.max(0, Math.min(1, progress)) * Math.max(0, width));
        UiShapes.rounded(x, y, width, 2, 1, 0xFF5B6B81);
        if (offset > 0) UiShapes.rounded(x, y, offset, 2, 1, enabled ? GOLD : MUTED);
        UiShapes.rounded(x + offset - 2, y - 2, 5, 6, 2, enabled ? TEXT : MUTED);
    }
    public static void lock(int x, int y, boolean locked, boolean active) {
        int ink = active ? GOLD : MUTED;
        rounded(x + 6, y + 9, 8, 6, 1, ink);
        fill(x + 6, y + 5, x + 7, y + 10, ink);
        fill(x + 7, y + 4, x + 12, y + 5, ink);
        fill(x + 12, y + 5, x + 13, y + (locked ? 10 : 7), ink);
        fill(x + 9, y + 11, x + 10, y + 13, 0xFF111A28);
    }
}
