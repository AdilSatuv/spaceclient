package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;

/** Minecraft adapter: drawing and input only; persistence lives in HudSettings. */
public final class HudModulesScreen extends Screen {
    private static final int TEXT = 0xEDF2FF;
    private static final int MUTED = 0xA6B4CD;
    private static final int ACCENT = 0x6DDFF6;
    private static final int BORDER = 0xFF34435F;
    private static final int CARD = 0xFF151D30;
    private static final int CARD_ENABLED = 0xFF1A2C40;
    private static final int CARD_FOCUS = 0xFF263650;
    private TextFieldWidget search;
    private String query = "";
    private List<HudModule> modules = HudModule.search("");
    private int focus;
    private int page;
    private int left, top, panelWidth, panelHeight, cardWidth, cardHeight;
    private boolean previousRepeatEvents;
    private boolean capturedRepeatEvents;

    @Override public void init() {
        if (!capturedRepeatEvents) {
            previousRepeatEvents = Keyboard.areRepeatEventsEnabled();
            capturedRepeatEvents = true;
        }
        Keyboard.enableRepeatEvents(true);
        panelWidth = Math.min(460, width - 24);
        panelHeight = Math.min(266, height - 12);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        cardWidth = (panelWidth - 32) / 2;
        cardHeight = panelHeight < 250 ? 36 : 46;
        search = new TextFieldWidget(0, textRenderer, left + 60, top + 49, panelWidth - 76, 16);
        search.setHasBorder(false);
        search.setMaxLength(64);
        search.setEditableColor(TEXT);
        search.setText(query);
        updateFocus();
        SpaceClient.clicks.clear();
    }

    @Override public void tick() { search.tick(); }

    @Override public void removed() {
        if (capturedRepeatEvents) Keyboard.enableRepeatEvents(previousRepeatEvents);
        SpaceClient.clicks.clear();
    }

    @Override public boolean shouldPauseGame() { return true; }

    @Override public void render(int mouseX, int mouseY, float tickDelta) {
        fillGradient(0, 0, width, height, 0xED030711, 0xF5060B19);
        fill(left, top, left + panelWidth, top + panelHeight, 0xFF0B1020);
        fill(left, top, left + panelWidth, top + 2, 0xFF927CFF);
        fill(left + 12, top + 12, left + 15, top + 36, 0xFF6DDFF6);
        textRenderer.drawWithShadow("HUD MODULES", left + 21, top + 12, TEXT);
        textRenderer.drawWithShadow("PERSONALIZE YOUR HUD", left + 21, top + 27, MUTED);
        if (hasPages()) {
            int previousX = left + panelWidth - 116, nextX = left + panelWidth - 36;
            box(previousX, top + 10, 24, 20, focus == previousControl(), hovered(mouseX, mouseY, previousX, top + 10, 24, 20));
            drawCenteredString(textRenderer, "<", previousX + 12, top + 16, TEXT);
            drawCenteredString(textRenderer, (page + 1) + "/" + HudModulePages.count(modules.size()), left + panelWidth - 64, top + 16, MUTED);
            box(nextX, top + 10, 24, 20, focus == nextControl(), hovered(mouseX, mouseY, nextX, top + 10, 24, 20));
            drawCenteredString(textRenderer, ">", nextX + 12, top + 16, TEXT);
        }

        box(left + 12, top + 44, panelWidth - 24, 23, focus == 0, false);
        textRenderer.drawWithShadow("FIND", left + 20, top + 49, ACCENT);
        search.render();
        if (query.isEmpty() && !search.isFocused()) {
            textRenderer.drawWithShadow("Search modules...", left + 60, top + 49, MUTED);
        }
        boolean readOnly = SpaceClient.hud.isReadOnly();
        box(left + 12, top + 74, panelWidth - 24, 20, focus == 1,
            hovered(mouseX, mouseY, left + 12, top + 74, panelWidth - 24, 20));
        String profile = "HUD visibility / " + SpaceClient.hud.getConfig().active().name;
        textRenderer.drawWithShadow(textRenderer.trimToWidth(profile, panelWidth - 72), left + 20, top + 80, TEXT);
        stateBadge(left + panelWidth - 47, top + 77, SpaceClient.hud.getConfig().active().enabled);

        for (int i = HudModulePages.first(page, modules.size()); i < HudModulePages.end(page, modules.size()); i++) {
            HudModule module = modules.get(i);
            int x = cardX(i), y = cardY(i);
            boolean enabled = module.isEnabled(SpaceClient.hud.getConfig());
            card(x, y, cardWidth, cardHeight, focus == i + 2,
                hovered(mouseX, mouseY, x, y, cardWidth, cardHeight), enabled);
            textRenderer.drawWithShadow(textRenderer.trimToWidth(module.getTitle(), cardWidth - 52), x + 8, y + 8, TEXT);
            stateBadge(x + cardWidth - 39, y + 6, enabled);
            textRenderer.drawWithShadow(textRenderer.trimToWidth(module.getDescription(), cardWidth - 16), x + 8, y + 23, MUTED);
        }
        if (modules.isEmpty()) {
            drawCenteredString(textRenderer, "No modules found", width / 2, top + 116, TEXT);
            drawCenteredString(textRenderer, "Clear the search to show modules.", width / 2, top + 131, MUTED);
        }

        String status = SpaceClient.hud.getError();
        if (status.isEmpty()) status = SpaceClient.hud.getNotice();
        if (status.isEmpty() && !SpaceClient.hud.getConfig().active().enabled) status = "HUD hidden; turn it on above";
        if (status.isEmpty()) status = hasPages() ? "PgUp / PgDn: pages  |  Changes saved automatically" : "Changes save automatically";
        textRenderer.drawWithShadow(textRenderer.trimToWidth(status, panelWidth - 24), left + 12, top + panelHeight - 42,
            readOnly || !SpaceClient.hud.getError().isEmpty() ? ACCENT : MUTED);
        textRenderer.drawWithShadow(textRenderer.trimToWidth("Tab / Shift+Tab  -  Space", panelWidth - 194), left + 12, top + panelHeight - 19, MUTED);
        int editorX = left + panelWidth - 170, editorY = top + panelHeight - 28;
        primaryButton(editorX, editorY, 82, 20, focus == modules.size() + 2, hovered(mouseX, mouseY, editorX, editorY, 82, 20));
        drawCenteredString(textRenderer, "EDIT HUD", editorX + 41, editorY + 6, 0x0B1020);
        int doneX = left + panelWidth - 80, doneY = top + panelHeight - 28;
        box(doneX, doneY, 68, 20, focus == modules.size() + 3, hovered(mouseX, mouseY, doneX, doneY, 68, 20));
        drawCenteredString(textRenderer, "Done / Esc", doneX + 34, doneY + 6, TEXT);
    }

    @Override protected void keyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            super.keyPressed(character, key);
            return;
        }
        if (key == Keyboard.KEY_TAB) {
            focus = Math.floorMod(focus + (hasShiftDown() ? -1 : 1), controlCount());
            updateFocus();
            return;
        }
        if (hasPages() && (key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_NEXT)) {
            if (!Keyboard.isRepeatEvent()) {
                changePage(key == Keyboard.KEY_PRIOR ? -1 : 1);
                focus = HudModulePages.first(page, modules.size()) + 2;
                updateFocus();
            }
            return;
        }
        if (focus == 0) {
            if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                focus = modules.isEmpty() ? 1 : 2;
                updateFocus();
            } else {
                search.keyPressed(character, key);
                query = search.getText();
                modules = HudModule.search(query);
                page = HudModulePages.clamp(page, modules.size());
            }
            return;
        }
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            focus = Math.floorMod(focus + (key == Keyboard.KEY_UP ? -1 : 1), controlCount());
            updateFocus();
        } else if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER || key == Keyboard.KEY_SPACE)
                && !Keyboard.isRepeatEvent()) {
            activate(focus);
        }
    }

    @Override protected void mouseClicked(int x, int y, int button) {
        if (button != 0) return;
        if (hovered(x, y, left + 12, top + 44, panelWidth - 24, 23)) {
            focus = 0;
            updateFocus();
            search.mouseClicked(x, y, button);
            search.setFocused(true);
        } else if (hovered(x, y, left + 12, top + 74, panelWidth - 24, 20)) {
            focus = 1;
            updateFocus();
            activate(focus);
        } else {
            if (hasPages() && hovered(x, y, left + panelWidth - 116, top + 10, 24, 20)) {
                focus = previousControl();
                changePage(-1);
                updateFocus();
                return;
            }
            if (hasPages() && hovered(x, y, left + panelWidth - 36, top + 10, 24, 20)) {
                focus = nextControl();
                changePage(1);
                updateFocus();
                return;
            }
            for (int i = HudModulePages.first(page, modules.size()); i < HudModulePages.end(page, modules.size()); i++) {
                if (hovered(x, y, cardX(i), cardY(i), cardWidth, cardHeight)) {
                    focus = i + 2;
                    updateFocus();
                    activate(focus);
                    return;
                }
            }
            if (hovered(x, y, left + panelWidth - 170, top + panelHeight - 28, 82, 20)) {
                focus = modules.size() + 2;
                updateFocus();
                client.setScreen(new HudEditorScreen(this));
            }
            else if (hovered(x, y, left + panelWidth - 80, top + panelHeight - 28, 68, 20)) client.setScreen(null);
        }
    }

    private void activate(int control) {
        if (control == 1) SpaceClient.toggleHud();
        else if (control == modules.size() + 2) client.setScreen(new HudEditorScreen(this));
        else if (control == modules.size() + 3) client.setScreen(null);
        else if (hasPages() && control == previousControl()) changePage(-1);
        else if (hasPages() && control == nextControl()) changePage(1);
        else if (control >= 2 && control < modules.size() + 2) SpaceClient.hud.toggle(modules.get(control - 2));
    }

    private boolean hasPages() { return HudModulePages.count(modules.size()) > 1; }
    private int previousControl() { return modules.size() + 4; }
    private int nextControl() { return modules.size() + 5; }
    private int controlCount() { return modules.size() + 4 + (hasPages() ? 2 : 0); }
    private void changePage(int delta) { page = HudModulePages.move(page, delta, modules.size()); }
    private void updateFocus() {
        search.setFocused(focus == 0);
        if (focus >= 2 && focus < modules.size() + 2) page = HudModulePages.forIndex(focus - 2, modules.size());
    }
    private int cardX(int index) { return left + 12 + ((index % HudModulePages.PAGE_SIZE) % 2) * (cardWidth + 8); }
    private int cardY(int index) { return top + 102 + ((index % HudModulePages.PAGE_SIZE) / 2) * (cardHeight + 6); }

    private void stateBadge(int x, int y, boolean enabled) {
        rounded(x, y, 27, 14, enabled ? 0xFF244C5B : 0xFF28344A);
        drawCenteredString(textRenderer, enabled ? "ON" : "OFF", x + 13, y + 3, enabled ? ACCENT : MUTED);
    }

    private void primaryButton(int x, int y, int w, int h, boolean focused, boolean hovered) {
        rounded(x, y, w, h, focused ? 0xFFEDF2FF : 0xFF6DDFF6);
        rounded(x + 1, y + 1, w - 2, h - 2, hovered || focused ? 0xFFB6F2FF : 0xFF6DDFF6);
    }
    private void box(int x, int y, int w, int h, boolean focused, boolean hovered) {
        rounded(x, y, w, h, focused ? 0xFF6DDFF6 : BORDER);
        rounded(x + 1, y + 1, w - 2, h - 2, focused || hovered ? 0xFF263650 : CARD);
    }

    private void card(int x, int y, int w, int h, boolean focused, boolean hovered, boolean enabled) {
        rounded(x, y, w, h, focused ? 0xFF6DDFF6 : BORDER);
        rounded(x + 1, y + 1, w - 2, h - 2,
            focused || hovered ? CARD_FOCUS : enabled ? CARD_ENABLED : CARD);
        if (enabled) fill(x + 1, y + 3, x + 3, y + h - 3, 0xFF6DDFF6);
    }

    private void rounded(int x, int y, int w, int h, int color) {
        fill(x + 2, y, x + w - 2, y + h, color);
        fill(x, y + 2, x + w, y + h - 2, color);
        fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    private boolean hovered(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
