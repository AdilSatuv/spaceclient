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
    private String status = "";

    public HudModuleSettingsScreen(Screen parent, HudModule module) {
        if (module == null) throw new IllegalArgumentException("Module is required");
        this.parent = parent;
        this.module = module;
    }
    private HudSettings settings() { return SpaceClient.hud; }
    private HudPlacement placement() { return settings().getConfig().active().placement(module); }
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
        SpaceUiFont.draw(client, "MODULE SETTINGS", left + 14, top + 12, ACCENT);
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getTitle(), panelWidth - 28), left + 14, top + 29, TEXT);
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getDescription(), panelWidth - 28), left + 14, top + 44, MUTED);
        fill(left + 14, top + 61, left + panelWidth - 14, top + 62, BORDER);
        SpaceUiFont.draw(client, "Enabled", left + 16, top + 73, TEXT);
        SpaceUiFont.draw(client, "Scale", left + 16, top + 101, TEXT);
        SpaceUiFont.draw(client, String.format(Locale.ROOT, "%.1fx", placement().scale), left + panelWidth - 114, top + 101, TEXT);
        SpaceUiFont.draw(client, "Anchor", left + 16, top + 129, TEXT);
        SpaceUiFont.draw(client, "Reset this module's placement", left + 16, top + 157, MUTED);
        for (Action action : controls()) {
            boolean hovered = action.contains(mouseX, mouseY);
            boolean selected = focus == action.id;
            boolean primary = action.id == 5;
            box(action.x, action.y, action.w, 22, selected, hovered, action.enabled, primary);
            int color = !action.enabled ? 0x77756F : primary ? 0x17130D : TEXT;
            center(SpaceUiFont.trimToWidth(action.label, action.w - 6), action.x + action.w / 2, action.y + 5, color);
        }
        String notice = settings().getError();
        if (notice.isEmpty() && settings().isReadOnly()) notice = settings().getNotice();
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
        actions.add(new Action(0, module.isEnabled(settings().getConfig()) ? "ON" : "OFF", right - 54, top + 67, 54, editable));
        actions.add(new Action(1, "-", right - 142, top + 95, 26, editable && p.scale > 0.5));
        actions.add(new Action(2, "+", right - 54, top + 95, 26, editable && p.scale < 3.0));
        actions.add(new Action(3, anchor, right - 54, top + 123, 54, editable && client.player != null));
        actions.add(new Action(4, "Reset", right - 54, top + 151, 54, editable));
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
            for (Action action : controls()) if (action.id == focus && action.enabled) { activate(action.id); return; }
        }
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        if (button != 0 || !roomy()) return;
        for (Action action : controls()) if (action.contains(x, y)) {
            focus = action.id;
            if (action.enabled) activate(action.id);
            return;
        }
    }
    private void activate(int id) {
        if (id == 6) { client.setScreen(parent); return; }
        if (id == 5) { client.setScreen(new HudEditorScreen(this, module)); return; }
        if (settings().isReadOnly()) return;
        boolean saved;
        if (id == 0) saved = settings().toggle(module);
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
    private void center(String text, int x, int y, int color) {
        SpaceUiFont.draw(client, text, x - SpaceUiFont.width(text) / 2f, y, color);
    }
    private void box(int x, int y, int w, int h, boolean selected, boolean hovered, boolean enabled, boolean primary) {
        int edge = selected ? 0xFFD7AF6A : hovered && enabled ? 0xFF71695C : BORDER;
        int inside = !enabled ? 0xFF1A1B1E : primary ? (hovered || selected ? 0xFFE6C78F : 0xFFD7AF6A)
            : selected || hovered ? 0xFF302C26 : SURFACE;
        rounded(x, y, w, h, primary && !selected ? inside : edge);
        rounded(x + 1, y + 1, w - 2, h - 2, inside);
        if (selected) fill(x + 4, y + h - 2, x + w - 4, y + h - 1, 0xFFD7AF6A);
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
