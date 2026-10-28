package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Settings for one HUD module; layout and enabled state belong to the active profile. */
public final class HudModuleSettingsScreen extends Screen {
    private static final int TEXT = 0xF2EEE7, MUTED = 0xB3B0AA, ACCENT = 0xD7AF6A;
    private static final int BORDER = 0xFF3A3B3E, PANEL = 0xFF101114, SURFACE = 0xFF191B20;
    private final Screen parent;
    private final HudModule module;
    private int left, top, panelWidth, focus;
    private int tab; // 0: layout, 1: appearance, 2: module content
    private String status = "";

    public HudModuleSettingsScreen(Screen parent, HudModule module) {
        if (module == null) throw new IllegalArgumentException("Module is required");
        this.parent = parent;
        this.module = module;
    }
    private HudSettings settings() { return SpaceClient.hud; }
    private HudPlacement placement() { return settings().getConfig().active().placement(module); }
    private HudAppearance appearance() { return settings().getConfig().active().appearance(module); }
    private HudContentSettings content() { return settings().getConfig().active().content(); }
    private boolean hasContentSettings() {
        return module == HudModule.COORDINATES || module == HudModule.CLOCK || module == HudModule.FPS || module == HudModule.CPS;
    }
    private boolean roomy() { return width >= 320 && height >= 240; }

    @Override public void init() {
        panelWidth = Math.max(0, Math.min(380, width - 24));
        left = (width - panelWidth) / 2;
        top = (height - 224) / 2;
        SpaceClient.clicks.clear();
    }
    @Override public void removed() {
        SpaceUiFont.release();
        SpaceClient.clicks.clear();
    }
    @Override public boolean shouldPauseGame() { return true; }

    @Override public void render(int mouseX, int mouseY, float delta) {
        fillGradient(0, 0, width, height, 0xD0070709, 0xE50A0B0E);
        if (!roomy()) {
            center("Resize the window to configure HUD", width / 2, height / 2 - 12, TEXT);
            center("Esc - back", width / 2, height / 2 + 4, MUTED);
            return;
        }
        rounded(left + 2, top + 3, panelWidth, 224, 0x90000000);
        rounded(left, top, panelWidth, 224, BORDER);
        rounded(left + 1, top + 1, panelWidth - 2, 222, PANEL);
        SpaceUiFont.draw(client, "MODULE", left + 14, top + 12, ACCENT);
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getTitle(), panelWidth - 28), left + 14, top + 29, TEXT);
        drawPreview();
        fill(left + 14, top + 61, left + panelWidth - 14, top + 62, BORDER);
        if (tab == 2) {
            if (module == HudModule.COORDINATES) {
                SpaceUiFont.draw(client, "Decimal places", left + 16, top + 73, TEXT);
                SpaceUiFont.draw(client, "0 rounds to a whole number", left + 16, top + 101, MUTED);
            } else if (module == HudModule.FPS) {
                SpaceUiFont.draw(client, "Show FPS label", left + 16, top + 73, TEXT);
                SpaceUiFont.draw(client, "Off shows only the number", left + 16, top + 101, MUTED);
            } else if (module == HudModule.CPS) {
                SpaceUiFont.draw(client, "Mouse buttons", left + 16, top + 73, TEXT);
                SpaceUiFont.draw(client, "Choose both, left or right", left + 16, top + 101, MUTED);
            } else {
                SpaceUiFont.draw(client, "Time format", left + 16, top + 73, TEXT);
                SpaceUiFont.draw(client, "Show seconds", left + 16, top + 101, TEXT);
                SpaceUiFont.draw(client, "Uses your local time", left + 16, top + 129, MUTED);
            }
            SpaceUiFont.draw(client, "Reset content", left + 16, top + 157, MUTED);
        } else if (tab == 1) {
            SpaceUiFont.draw(client, "Text color", left + 16, top + 73, TEXT);
            SpaceUiFont.draw(client, "Background", left + 16, top + 101, TEXT);
            SpaceUiFont.draw(client, "Text shadow", left + 16, top + 129, TEXT);
            SpaceUiFont.draw(client, "Reset appearance", left + 16, top + 157, MUTED);
        } else {
            SpaceUiFont.draw(client, "Enabled", left + 16, top + 73, TEXT);
            SpaceUiFont.draw(client, "Scale", left + 16, top + 101, TEXT);
            SpaceUiFont.draw(client, String.format(Locale.ROOT, "%.1fx", placement().scale), left + panelWidth - 114, top + 101, TEXT);
            SpaceUiFont.draw(client, "Anchor", left + 16, top + 129, TEXT);
            SpaceUiFont.draw(client, "Reset this module's placement", left + 16, top + 157, MUTED);
        }
        List<Action> visible = controls();
        for (int i = 0; i < visible.size(); i++) {
            Action action = visible.get(i);
            boolean hovered = action.contains(mouseX, mouseY);
            boolean selected = focus == i;
            boolean primary = action.id == 5;
            boolean activeTab = action.id == (tab == 0 ? 7 : tab == 1 ? 12 : 13);
            box(action.x, action.y, action.w, 22, selected, hovered, action.enabled, primary, activeTab);
            int color = !action.enabled ? 0x77756F : primary ? 0x17130D : activeTab ? ACCENT : TEXT;
            center(SpaceUiFont.trimToWidth(action.label, action.w - 6), action.x + action.w / 2, action.y + 5, color);
        }
        String notice = settings().getError();
        if (notice.isEmpty() && settings().isReadOnly()) notice = settings().getNotice();
        if (notice.isEmpty() && tab == 1 && appearance().backdrop != HudAppearance.Backdrop.STRONG)
            notice = "Lighter background may reduce readability";
        if (notice.isEmpty() && placement().scale > 1.4) notice = "Preview fitted to the available space";
        if (notice.isEmpty()) notice = status.isEmpty() ? "Tab: select  |  Enter / Space: activate" : status;
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(notice, panelWidth - 28), left + 14, top + 181,
            settings().getError().isEmpty() ? MUTED : ACCENT);
    }

    private List<Action> controls() {
        List<Action> actions = new ArrayList<>();
        boolean editable = !settings().isReadOnly();
        int right = left + panelWidth - 14;
        HudPlacement p = placement();
        String anchor = (p.horizontal == HudPlacement.Horizontal.LEFT ? "L" : "R") + "/"
            + (p.vertical == HudPlacement.Vertical.TOP ? "T" : "B");
        if (hasContentSettings()) {
            actions.add(new Action(7, "Layout", right - 203, top + 7, 58, true));
            actions.add(new Action(12, "Appearance", right - 141, top + 7, 78, true));
            actions.add(new Action(13, "Content", right - 59, top + 7, 59, true));
        } else {
            actions.add(new Action(7, "Layout", right - 174, top + 7, 84, true));
            actions.add(new Action(12, "Appearance", right - 86, top + 7, 86, true));
        }
        if (tab == 2 && hasContentSettings()) {
            HudContentSettings c = content();
            if (module == HudModule.COORDINATES) {
                actions.add(new Action(14, Integer.toString(c.coordinatesPrecision) + "  >", right - 98, top + 67, 98, editable));
            } else if (module == HudModule.FPS) {
                actions.add(new Action(18, c.fpsLabel ? "ON" : "OFF", right - 98, top + 67, 98, editable));
            } else if (module == HudModule.CPS) {
                actions.add(new Action(19, label(c.cpsMode.name()) + "  >", right - 98, top + 67, 98, editable));
            } else {
                actions.add(new Action(15, (c.clockFormat == HudContentSettings.ClockFormat.H24 ? "24 hour" : "12 hour") + "  >", right - 98, top + 67, 98, editable));
                actions.add(new Action(16, c.clockSeconds ? "ON" : "OFF", right - 98, top + 95, 98, editable));
            }
            actions.add(new Action(17, "Reset", right - 98, top + 151, 98, editable));
        } else if (tab == 1) {
            HudAppearance a = appearance();
            actions.add(new Action(8, label(a.color.name()) + "  >", right - 98, top + 67, 98, editable));
            actions.add(new Action(9, label(a.backdrop.name()) + "  >", right - 98, top + 95, 98, editable));
            actions.add(new Action(10, a.shadow ? "ON" : "OFF", right - 98, top + 123, 98, editable));
            actions.add(new Action(11, "Reset", right - 98, top + 151, 98, editable));
        } else {
            actions.add(new Action(0, module.isEnabled(settings().getConfig()) ? "ON" : "OFF", right - 54, top + 67, 54, editable));
            actions.add(new Action(1, "-", right - 142, top + 95, 26, editable && p.scale > 0.5));
            actions.add(new Action(2, "+", right - 54, top + 95, 26, editable && p.scale < 3.0));
            actions.add(new Action(3, anchor + "  >", right - 54, top + 123, 54, editable && client.player != null));
            actions.add(new Action(4, "Reset", right - 54, top + 151, 54, editable));
        }
        actions.add(new Action(5, "Edit layout", right - 154, top + 196, 90, true));
        actions.add(new Action(6, "Back", right - 58, top + 196, 58, true));
        return actions;
    }

    @Override protected void keyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) { if (!Keyboard.isRepeatEvent()) client.setScreen(parent); return; }
        if (!roomy()) return;
        if (key == Keyboard.KEY_TAB && !Keyboard.isRepeatEvent()) {
            focus = Math.floorMod(focus + (hasShiftDown() ? -1 : 1), controls().size());
            return;
        }
        if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER || key == Keyboard.KEY_SPACE)
                && !Keyboard.isRepeatEvent()) {
            List<Action> visible = controls();
            if (focus < visible.size() && visible.get(focus).enabled) activate(visible.get(focus).id);
        }
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        if (button != 0 || !roomy()) return;
        List<Action> visible = controls();
        for (int i = 0; i < visible.size(); i++) if (visible.get(i).contains(x, y)) {
            focus = i;
            if (visible.get(i).enabled) activate(visible.get(i).id);
            return;
        }
    }
    private void activate(int id) {
        if (id == 7 || id == 12 || id == 13) {
            tab = id == 7 ? 0 : id == 12 ? 1 : 2;
            focus = tab;
            status = "";
            return;
        }
        if (id == 6) { client.setScreen(parent); return; }
        if (id == 5) { client.setScreen(new HudEditorScreen(this, module)); return; }
        if (settings().isReadOnly()) return;
        boolean saved;
        if (id == 18) saved = settings().content(content().withFpsLabel(!content().fpsLabel));
        else if (id == 19) {
            HudContentSettings c = content();
            HudContentSettings.CpsMode[] values = HudContentSettings.CpsMode.values();
            saved = settings().content(c.withCpsMode(values[(c.cpsMode.ordinal() + 1) % values.length]));
        } else if (id == 14) {
            HudContentSettings c = content();
            saved = settings().content(c.withCoordinatesPrecision((c.coordinatesPrecision + 1) % 3));
        } else if (id == 15) {
            HudContentSettings c = content();
            saved = settings().content(c.withClockFormat(c.clockFormat == HudContentSettings.ClockFormat.H24
                ? HudContentSettings.ClockFormat.H12 : HudContentSettings.ClockFormat.H24));
        } else if (id == 16) saved = settings().content(content().withClockSeconds(!content().clockSeconds));
        else if (id == 17) {
            HudContentSettings c = content();
            if (module == HudModule.COORDINATES) c = c.withCoordinatesPrecision(1);
            else if (module == HudModule.CLOCK) c = c.withClockFormat(HudContentSettings.ClockFormat.H24).withClockSeconds(true);
            else if (module == HudModule.FPS) c = c.withFpsLabel(true);
            else if (module == HudModule.CPS) c = c.withCpsMode(HudContentSettings.CpsMode.BOTH);
            saved = settings().content(c);
        } else if (id == 8) {
            HudAppearance a = appearance();
            HudAppearance.Color[] values = HudAppearance.Color.values();
            saved = settings().appearance(module, a.withColor(values[(a.color.ordinal() + 1) % values.length]));
        } else if (id == 9) {
            HudAppearance a = appearance();
            HudAppearance.Backdrop[] values = HudAppearance.Backdrop.values();
            saved = settings().appearance(module, a.withBackdrop(values[(a.backdrop.ordinal() + 1) % values.length]));
        } else if (id == 10) saved = settings().appearance(module, appearance().withShadow(!appearance().shadow));
        else if (id == 11) saved = settings().appearance(module, HudAppearance.defaults());
        else if (id == 0) saved = settings().toggle(module);
        else if (id == 1 || id == 2) {
            double scale = Math.max(0.5, Math.min(3.0, Math.round((placement().scale + (id == 1 ? -0.1 : 0.1)) * 10) / 10.0));
            saved = settings().place(module, placement().withScale(scale));
        } else if (id == 3) {
            if (client.player == null) return;
            HudPlacement p = placement();
            int next = (p.horizontal.ordinal() * 2 + p.vertical.ordinal() + 1) % 4;
            String text = HudRenderer.text(client, module);
            saved = settings().place(module, p.anchored(HudPlacement.Horizontal.values()[next / 2],
                HudPlacement.Vertical.values()[next % 2], width, height, HudRenderer.textWidth(client, text), 10));
        } else if (id == 4) saved = settings().reset(module);
        else return;
        status = saved ? "Saved to the active profile" : settings().getError();
    }
    /** A compact sample uses the same font, color and backdrop as the actual HUD. */
    private void drawPreview() {
        double scale = previewScale(placement().scale);
        SpaceUiFont.draw(client, placement().scale > scale ? "Preview*" : "Preview", left + 14, top + 44, MUTED);
        int x = left + 78;
        int available = panelWidth - 94;
        fill(x - 2, top + 41, left + panelWidth - 14, top + 58, 0xFF555359);
        if (client.player == null) {
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth("Unavailable", available), x + 2, top + 44, TEXT);
            return;
        }
        String text = HudRenderer.text(client, module);
        int maxTextWidth = Math.max(1, (int) Math.floor((available - 6) / scale));
        String sample = client.textRenderer.trimToWidth(text, maxTextWidth);
        if (client.textRenderer.getStringWidth(text) > maxTextWidth) {
            String ellipsis = "...";
            int prefixWidth = Math.max(0, maxTextWidth - client.textRenderer.getStringWidth(ellipsis));
            sample = client.textRenderer.trimToWidth(text, prefixWidth) + ellipsis;
        }
        if (sample.isEmpty()) return;
        HudPlacement inPanel = new HudPlacement(HudPlacement.Horizontal.LEFT, HudPlacement.Vertical.TOP, x + 2, top + 43, scale);
        HudPlacement.Bounds b = HudRenderer.bounds(client, sample, inPanel, width, height);
        HudRenderer.draw(client, sample, b, appearance(), HudRenderer.color(module, appearance()));
    }
    /** The short preview row fits up to 1.4x; larger preferred scales stay saved unchanged. */
    static double previewScale(double preferred) { return Math.min(1.4, preferred); }
    private String label(String value) {
        return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
    }
    private void center(String text, int x, int y, int color) {
        SpaceUiFont.draw(client, text, x - SpaceUiFont.width(text) / 2f, y, color);
    }
    private void box(int x, int y, int w, int h, boolean selected, boolean hovered, boolean enabled, boolean primary, boolean activeTab) {
        int edge = selected ? 0xFFD7AF6A : hovered && enabled ? 0xFF71695C : BORDER;
        int inside = !enabled ? 0xFF1A1B1E : primary ? (hovered || selected ? 0xFFE6C78F : 0xFFD7AF6A)
            : activeTab ? 0xFF3A3122 : selected || hovered ? 0xFF302C26 : SURFACE;
        rounded(x, y, w, h, primary && !selected ? inside : edge);
        rounded(x + 1, y + 1, w - 2, h - 2, inside);
        if (activeTab) fill(x + 4, y + h - 2, x + w - 4, y + h - 1, 0xFFD7AF6A);
    }
    private void rounded(int x, int y, int w, int h, int color) {
        fill(x + 2, y, x + w - 2, y + h, color);
        fill(x, y + 2, x + w, y + h - 2, color);
        fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }
    private static final class Action {
        final int id, x, y, w;
        final String label;
        final boolean enabled;
        Action(int id, String label, int x, int y, int w, boolean enabled) {
            this.id = id; this.label = label; this.x = x; this.y = y; this.w = w; this.enabled = enabled;
        }
        boolean contains(int px, int py) { return px >= x && px < x + w && py >= y && py < y + 22; }
    }
}
