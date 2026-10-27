package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;

/** Minecraft adapter: drawing and input only; persistence lives in HudSettings. */
public final class HudModulesScreen extends Screen {
    private static final int TEXT = 0xF4EDE3;
    private static final int MUTED = 0xB6B7C4;
    private static final int ACCENT = 0xF2B56B;
    private static final int BORDER = 0xFF414652;
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
        fillGradient(0, 0, width, height, 0xEC03060D, 0xF5090D16);
        fill(left, top, left + panelWidth, top + panelHeight, 0xFF10141D);
        fill(left, top, left + panelWidth, top + 2, 0xFFF2B56B);
        textRenderer.drawWithShadow("SPACE CLIENT / HUD", left + 12, top + 13, TEXT);
        textRenderer.drawWithShadow("Right Shift / Esc - close", left + 12, top + 27, MUTED);
        if (hasPages()) {
            int previousX = left + panelWidth - 116, nextX = left + panelWidth - 36;
            box(previousX, top + 10, 24, 20, focus == previousControl(), hovered(mouseX, mouseY, previousX, top + 10, 24, 20));
            drawCenteredString(textRenderer, "<", previousX + 12, top + 16, TEXT);
            drawCenteredString(textRenderer, (page + 1) + "/" + HudModulePages.count(modules.size()), left + panelWidth - 64, top + 16, MUTED);
            box(nextX, top + 10, 24, 20, focus == nextControl(), hovered(mouseX, mouseY, nextX, top + 10, 24, 20));
            drawCenteredString(textRenderer, ">", nextX + 12, top + 16, TEXT);
        }

        box(left + 12, top + 44, panelWidth - 24, 23, focus == 0, false);
        textRenderer.drawWithShadow("Search", left + 20, top + 49, MUTED);
        search.render();
        if (query.isEmpty() && !search.isFocused()) {
            textRenderer.drawWithShadow("Modules...", left + 60, top + 49, MUTED);
        }
        boolean readOnly = SpaceClient.hud.isReadOnly();
        box(left + 12, top + 74, panelWidth - 24, 20, focus == 1,
            hovered(mouseX, mouseY, left + 12, top + 74, panelWidth - 24, 20));
        textRenderer.drawWithShadow("HUD visibility", left + 20, top + 80, TEXT);
        rightLabel(SpaceClient.hud.getConfig().active().enabled ? "ON" : "OFF", left + panelWidth - 20, top + 80, ACCENT);

        for (int i = HudModulePages.first(page, modules.size()); i < HudModulePages.end(page, modules.size()); i++) {
            HudModule module = modules.get(i);
            int x = cardX(i), y = cardY(i);
            box(x, y, cardWidth, cardHeight, focus == i + 2, hovered(mouseX, mouseY, x, y, cardWidth, cardHeight));
            textRenderer.drawWithShadow(module.getTitle(), x + 8, y + 8, TEXT);
            boolean enabled = module.isEnabled(SpaceClient.hud.getConfig());
            rightLabel(enabled ? "ON" : "OFF", x + cardWidth - 8, y + 8, enabled ? ACCENT : MUTED);
            textRenderer.drawWithShadow(textRenderer.trimToWidth(module.getDescription(), cardWidth - 16), x + 8, y + 23, MUTED);
        }
        if (modules.isEmpty()) {
            drawCenteredString(textRenderer, "No modules found", width / 2, top + 116, TEXT);
            drawCenteredString(textRenderer, "Clear the search to show modules.", width / 2, top + 131, MUTED);
        }

        String status = SpaceClient.hud.getError();
        if (status.isEmpty()) status = SpaceClient.hud.getNotice();
        if (status.isEmpty()) status = "Changes save automatically";
        textRenderer.drawWithShadow(textRenderer.trimToWidth(status, panelWidth - 24), left + 12, top + panelHeight - 42,
            readOnly || !SpaceClient.hud.getError().isEmpty() ? ACCENT : MUTED);
        textRenderer.drawWithShadow(textRenderer.trimToWidth("Tab / Shift+Tab  -  Space", panelWidth - 194), left + 12, top + panelHeight - 19, MUTED);
        int editorX = left + panelWidth - 170, editorY = top + panelHeight - 28;
        box(editorX, editorY, 82, 20, focus == modules.size() + 2, hovered(mouseX, mouseY, editorX, editorY, 82, 20));
        drawCenteredString(textRenderer, "Edit layout", editorX + 41, editorY + 6, TEXT);
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

    private void rightLabel(String text, int right, int y, int color) {
        textRenderer.drawWithShadow(text, right - textRenderer.getStringWidth(text), y, color);
    }

    private void box(int x, int y, int w, int h, boolean focused, boolean hovered) {
        fill(x, y, x + w, y + h, focused ? 0xFFF2B56B : BORDER);
        fill(x + 1, y + 1, x + w - 1, y + h - 1, focused || hovered ? 0xFF272B35 : 0xFF191E28);
    }

    private boolean hovered(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
