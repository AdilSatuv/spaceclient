package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;

/** Minecraft adapter: drawing and input only; persistence lives in HudSettings. */
public final class HudModulesScreen extends Screen {
    private static final int TEXT = 0xF2EEE7;
    private static final int MUTED = 0xB3B0AA;
    private static final int ACCENT = 0xD7AF6A;
    private static final int BORDER = 0xFF3A3B3E;
    private static final int CARD = 0xFF191B20;
    private static final int CARD_ENABLED = 0xFF1D1D1D;
    private static final int CARD_FOCUS = 0xFF2A2824;
    private TextFieldWidget search;
    private String query = "";
    private List<HudModule> modules = HudModule.search("");
    private int focus;
    private int page;
    private int left, top, panelWidth, panelHeight, contentLeft, contentWidth, cardWidth, cardHeight;
    private boolean sidebar;
    private HudModulesLayout layout;
    private boolean previousRepeatEvents;
    private boolean capturedRepeatEvents;

    @Override public void init() {
        if (!capturedRepeatEvents) {
            previousRepeatEvents = Keyboard.areRepeatEventsEnabled();
            capturedRepeatEvents = true;
        }
        Keyboard.enableRepeatEvents(true);
        layout = new HudModulesLayout(width, height);
        panelWidth = layout.panelWidth; panelHeight = layout.panelHeight;
        left = layout.left; top = layout.top;
        sidebar = layout.sidebar;
        contentLeft = layout.contentLeft; contentWidth = layout.contentWidth;
        cardWidth = layout.cardWidth; cardHeight = layout.cardHeight;
        search = new TextFieldWidget(0, textRenderer, contentLeft + 30, top + 50,
            Math.max(1, contentWidth - 38), 16);
        search.setHasBorder(false);
        search.setMaxLength(64);
        search.setEditableColor(TEXT);
        search.setText(query);
        updateFocus();
        SpaceClient.clicks.clear();
    }

    @Override public void tick() { search.tick(); }

    @Override public void removed() {
        SpaceUiFont.release();
        if (capturedRepeatEvents) Keyboard.enableRepeatEvents(previousRepeatEvents);
        SpaceClient.clicks.clear();
    }

    @Override public boolean shouldPauseGame() { return true; }

    @Override public void render(int mouseX, int mouseY, float tickDelta) {
        fillGradient(0, 0, width, height, 0xC9070709, 0xE30A0B0E);
        if (!roomy()) {
            centerText("Resize the window to open HUD", width / 2, height / 2 - 12, TEXT);
            centerText("Esc - close", width / 2, height / 2 + 4, MUTED);
            return;
        }
        // A restrained shell leaves the world visible around the edges.
        rounded(left + 2, top + 3, panelWidth, panelHeight, 0x90000000);
        rounded(left, top, panelWidth, panelHeight, BORDER);
        rounded(left + 1, top + 1, panelWidth - 2, panelHeight - 2, 0xFF101114);
        if (sidebar) drawSidebar();

        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(1.3f, 1.3f, 1);
            SpaceUiFont.draw(client, "HUD", contentLeft / 1.3f, (top + 9) / 1.3f, TEXT);
        } finally { GlStateManager.popMatrix(); }
        SpaceUiFont.draw(client, "Customize your overlay", contentLeft, top + 27, MUTED);
        int editorX = contentLeft + contentWidth - 82, editorY = top + 10;
        primaryButton(editorX, editorY, 82, 20, focus == modules.size() + 2,
            hovered(mouseX, mouseY, editorX, editorY, 82, 20));
        centerText("Edit HUD", editorX + 41, editorY + 6, 0x17130D);

        box(contentLeft, top + 44, contentWidth, 23, focus == 0,
            hovered(mouseX, mouseY, contentLeft, top + 44, contentWidth, 23));
        searchIcon(contentLeft + 10, top + 51);
        search.render();
        if (query.isEmpty() && !search.isFocused())
            SpaceUiFont.draw(client, "Search modules...", contentLeft + 30, top + 50, MUTED);

        boolean readOnly = SpaceClient.hud.isReadOnly();
        box(contentLeft, top + 73, contentWidth, 19, focus == 1,
            hovered(mouseX, mouseY, contentLeft, top + 73, contentWidth, 19));
        String profile = "HUD / " + SpaceClient.hud.getConfig().active().name;
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(profile, contentWidth - 55), contentLeft + 9, top + 78, TEXT);
        stateBadge(contentLeft + contentWidth - 38, top + 75, SpaceClient.hud.getConfig().active().enabled);

        for (int i = HudModulePages.first(page, modules.size()); i < HudModulePages.end(page, modules.size()); i++) {
            HudModule module = modules.get(i);
            int x = cardX(i), y = cardY(i);
            boolean enabled = module.isEnabled(SpaceClient.hud.getConfig());
            card(x, y, cardWidth, cardHeight, focus == i + 2,
                hovered(mouseX, mouseY, x, y, cardWidth, cardHeight), enabled);
            int insetY = (cardHeight - 36) / 2;
            moduleIcon(module, x + 9, y + 10 + insetY, enabled);
            int titleWidth = Math.max(1, cardWidth - 65);
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getTitle(), titleWidth), x + 28, y + 8 + insetY, TEXT);
            stateBadge(x + cardWidth - 36, y + 6 + insetY, enabled);
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getDescription(), Math.max(1, cardWidth - 36)),
                x + 28, y + 23 + insetY, MUTED);
        }
        if (modules.isEmpty()) {
            centerText("No modules found", contentLeft + contentWidth / 2, top + 110, TEXT);
            centerText("Clear the search to show modules.", contentLeft + contentWidth / 2, top + 125, MUTED);
        }

        String status = SpaceClient.hud.getError();
        if (status.isEmpty()) status = SpaceClient.hud.getNotice();
        if (status.isEmpty() && !SpaceClient.hud.getConfig().active().enabled) status = "HUD hidden; turn it on above";
        if (status.isEmpty()) status = readOnly ? "Settings are read-only" : hasPages() ? "PgUp / PgDn: pages" : "Changes save automatically";
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(status, contentWidth - 4), contentLeft + 2, top + panelHeight - 44,
            readOnly || !SpaceClient.hud.getError().isEmpty() ? ACCENT : MUTED);

        int footerY = layout.footerY();
        if (hasPages()) {
            int prevX = contentLeft, nextX = contentLeft + 64;
            box(prevX, footerY, 20, 20, focus == previousControl(), hovered(mouseX, mouseY, prevX, footerY, 20, 20));
            centerText("<", prevX + 10, footerY + 6, TEXT);
            centerText((page + 1) + "/" + HudModulePages.count(modules.size()), contentLeft + 42, footerY + 6, MUTED);
            box(nextX, footerY, 20, 20, focus == nextControl(), hovered(mouseX, mouseY, nextX, footerY, 20, 20));
            centerText(">", nextX + 10, footerY + 6, TEXT);
        }
        int doneX = contentLeft + contentWidth - 68;
        box(doneX, footerY, 68, 20, focus == modules.size() + 3, hovered(mouseX, mouseY, doneX, footerY, 68, 20));
        centerText("Done / Esc", doneX + 34, footerY + 6, TEXT);
        if (contentWidth >= 360) SpaceUiFont.draw(client, "Tab: select  |  Space: toggle", contentLeft + 96, footerY + 6, MUTED);
    }

    private void drawSidebar() {
        fill(left + 68, top + 1, left + 69, top + panelHeight - 1, BORDER);
        SpaceUiFont.draw(client, "S P A C E", left + 12, top + 15, TEXT);
        rounded(left + 7, top + 46, 54, 25, 0xFF252321);
        fill(left + 7, top + 49, left + 9, top + 68, 0xFFD7AF6A);
        SpaceUiFont.draw(client, "HUD", left + 16, top + 54, TEXT);
        SpaceUiFont.draw(client, "MODULES", left + 12, top + 82, MUTED);
        fill(left + 10, top + panelHeight - 54, left + 58, top + panelHeight - 53, BORDER);
        SpaceUiFont.draw(client, "PROFILE", left + 12, top + panelHeight - 45, MUTED);
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(SpaceClient.hud.getConfig().active().name, 46),
            left + 12, top + panelHeight - 30, TEXT);
    }

    private boolean roomy() { return width >= 320 && height >= 240; }

    @Override protected void keyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            super.keyPressed(character, key);
            return;
        }
        if (!roomy()) return;
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
        if (button != 0 || !roomy()) return;
        if (hovered(x, y, contentLeft, top + 44, contentWidth, 23)) {
            focus = 0;
            updateFocus();
            search.mouseClicked(x, y, button);
            search.setFocused(true);
        } else if (hovered(x, y, contentLeft, top + 73, contentWidth, 19)) {
            focus = 1;
            updateFocus();
            activate(focus);
        } else if (hovered(x, y, contentLeft + contentWidth - 82, top + 10, 82, 20)) {
            focus = modules.size() + 2;
            updateFocus();
            activate(focus);
        } else {
            int footerY = layout.footerY();
            if (hasPages() && hovered(x, y, contentLeft, footerY, 20, 20)) {
                focus = previousControl(); changePage(-1); updateFocus(); return;
            }
            if (hasPages() && hovered(x, y, contentLeft + 64, footerY, 20, 20)) {
                focus = nextControl(); changePage(1); updateFocus(); return;
            }
            for (int i = HudModulePages.first(page, modules.size()); i < HudModulePages.end(page, modules.size()); i++) {
                if (hovered(x, y, cardX(i), cardY(i), cardWidth, cardHeight)) {
                    focus = i + 2; updateFocus(); activate(focus); return;
                }
            }
            if (hovered(x, y, contentLeft + contentWidth - 68, footerY, 68, 20)) {
                focus = modules.size() + 3; updateFocus(); activate(focus);
            }
        }
    }

    private void activate(int control) {
        if (control == 1 && !SpaceClient.hud.isReadOnly()) SpaceClient.toggleHud();
        else if (control == modules.size() + 2) client.setScreen(new HudEditorScreen(this));
        else if (control == modules.size() + 3) client.setScreen(null);
        else if (hasPages() && control == previousControl()) changePage(-1);
        else if (hasPages() && control == nextControl()) changePage(1);
        else if (control >= 2 && control < modules.size() + 2 && !SpaceClient.hud.isReadOnly()) SpaceClient.hud.toggle(modules.get(control - 2));
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
    private int cardX(int index) { return layout.cardX(index); }
    private int cardY(int index) { return layout.cardY(index); }

    private void stateBadge(int x, int y, boolean enabled) {
        rounded(x, y, 28, 14, enabled ? 0xFF5C4A31 : 0xFF292B2F);
        centerText(enabled ? "ON" : "OFF", x + 14, y + 3, enabled ? 0xFFF2D49C : MUTED);
    }

    private void primaryButton(int x, int y, int w, int h, boolean focused, boolean hovered) {
        rounded(x, y, w, h, focused ? 0xFFF8E5C3 : 0xFFD7AF6A);
        rounded(x + 1, y + 1, w - 2, h - 2, hovered || focused ? 0xFFEACB92 : 0xFFD7AF6A);
    }
    private void box(int x, int y, int w, int h, boolean focused, boolean hovered) {
        rounded(x, y, w, h, focused ? 0xFFD7AF6A : BORDER);
        rounded(x + 1, y + 1, w - 2, h - 2, focused || hovered ? CARD_FOCUS : CARD);
    }
    private void card(int x, int y, int w, int h, boolean focused, boolean hovered, boolean enabled) {
        rounded(x, y, w, h, focused ? 0xFFD7AF6A : BORDER);
        rounded(x + 1, y + 1, w - 2, h - 2, focused || hovered ? CARD_FOCUS : enabled ? CARD_ENABLED : CARD);
        if (focused) fill(x + 4, y + h - 2, x + w - 4, y + h - 1, 0xFFD7AF6A);
    }
    private void searchIcon(int x, int y) {
        int c = 0xFFB3B0AA;
        fill(x + 1, y, x + 6, y + 1, c); fill(x, y + 1, x + 1, y + 6, c);
        fill(x + 6, y + 1, x + 7, y + 6, c); fill(x + 1, y + 6, x + 6, y + 7, c);
        fill(x + 6, y + 6, x + 9, y + 9, c);
    }
    private void moduleIcon(HudModule module, int x, int y, boolean enabled) {
        int c = enabled ? 0xFFF2EEE7 : 0xFFB3B0AA;
        // Small distinct line symbols, drawn without external branded assets.
        if (module == HudModule.FPS || module == HudModule.SPEED) {
            fill(x, y + 8, x + 3, y + 13, c); fill(x + 5, y + 4, x + 8, y + 13, c); fill(x + 10, y, x + 13, y + 13, c);
        } else if (module == HudModule.KEYSTROKES) {
            fill(x + 5, y, x + 9, y + 4, c); fill(x, y + 6, x + 4, y + 10, c);
            fill(x + 5, y + 6, x + 9, y + 10, c); fill(x + 10, y + 6, x + 14, y + 10, c);
        } else if (module == HudModule.COORDINATES || module == HudModule.COMPASS) {
            fill(x + 6, y, x + 7, y + 13, c); fill(x, y + 6, x + 13, y + 7, c);
            fill(x + 2, y + 2, x + 4, y + 4, c); fill(x + 9, y + 9, x + 11, y + 11, c);
        } else {
            fill(x + 2, y + 1, x + 11, y + 2, c); fill(x + 1, y + 2, x + 2, y + 11, c);
            fill(x + 11, y + 2, x + 12, y + 11, c); fill(x + 2, y + 11, x + 11, y + 12, c);
            fill(x + 5, y + 4, x + 8, y + 9, c);
        }
    }
    private void centerText(String text, int centerX, int y, int color) {
        SpaceUiFont.draw(client, text, centerX - SpaceUiFont.width(text) / 2f, y, color);
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
