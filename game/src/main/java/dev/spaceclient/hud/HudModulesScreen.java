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
        page = HudModulePages.clamp(page, modules.size(), layout.pageSize);
        panelWidth = layout.panelWidth; panelHeight = layout.panelHeight;
        left = layout.left; top = layout.top;
        sidebar = layout.sidebar;
        contentLeft = layout.contentLeft; contentWidth = layout.contentWidth;
        cardWidth = layout.cardWidth; cardHeight = layout.cardHeight;
        search = new TextFieldWidget(0, textRenderer, contentLeft + 30, top + 50,
            Math.max(1, layout.searchWidth - 38), 16);
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
        fillGradient(0, 0, width, height, 0x85070709, 0xA70A0B0E);
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
            GlStateManager.scale(1.5f, 1.5f, 1);
            SpaceUiFont.draw(client, "HUD", contentLeft / 1.5f, (top + 9) / 1.5f, TEXT);
        } finally { GlStateManager.popMatrix(); }
        int editorX = contentLeft + contentWidth - 82, editorY = top + 10;
        primaryButton(editorX, editorY, 82, 20, focus == modules.size() + 2,
            hovered(mouseX, mouseY, editorX, editorY, 82, 20));
        centerText("Edit layout", editorX + 41, editorY + 6, 0x17130D);

        box(contentLeft, top + 44, layout.searchWidth, 23, focus == 0,
            hovered(mouseX, mouseY, contentLeft, top + 44, layout.searchWidth, 23));
        searchIcon(contentLeft + 10, top + 51);
        search.render();
        if (query.isEmpty() && !search.isFocused())
            SpaceUiFont.draw(client, "Search modules...", contentLeft + 30, top + 50, MUTED);

        boolean readOnly = SpaceClient.hud.isReadOnly();
        box(layout.profileLeft, layout.profileTop, layout.profileWidth, 23, focus == 1,
            hovered(mouseX, mouseY, layout.profileLeft, layout.profileTop, layout.profileWidth, 23));
        String profile = SpaceClient.hud.getConfig().active().name;
        if (!sidebar) SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(profile, layout.profileWidth - 48),
            layout.profileLeft + 7, layout.profileTop + 6, TEXT);
        int masterSwitchX = sidebar ? layout.profileLeft + (layout.profileWidth - 28) / 2 : layout.profileLeft + layout.profileWidth - 34;
        stateBadge(masterSwitchX, layout.profileTop + 4, SpaceClient.hud.getConfig().active().enabled);

        for (int i = HudModulePages.first(page, modules.size(), layout.pageSize); i < HudModulePages.end(page, modules.size(), layout.pageSize); i++) {
            HudModule module = modules.get(i);
            int x = cardX(i), y = cardY(i);
            boolean enabled = module.isEnabled(SpaceClient.hud.getConfig());
            card(x, y, cardWidth, cardHeight, focus == i + 2,
                hovered(mouseX, mouseY, x, y, cardWidth, cardHeight), enabled);
            int insetY = (cardHeight - 36) / 2;
            int iconSize = cardWidth >= 145 ? 22 : 16;
            int textInset = iconSize + 17;
            HudModuleIcons.draw(module, x + 8, y + (cardHeight - iconSize) / 2, iconSize);
            int titleWidth = Math.max(1, cardWidth - textInset - 38);
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getTitle(), titleWidth), x + textInset, y + 8 + insetY, TEXT);
            stateBadge(x + cardWidth - 36, y + 6 + insetY, enabled);
            drawDescription(module, x + textInset, y + 23 + insetY, Math.max(1, cardWidth - textInset - 8), cardHeight);
            if (cardHeight >= 44 && cardWidth >= 145)
                SpaceUiFont.draw(client, ">", x + cardWidth - 13, y + cardHeight - 16, MUTED);
        }
        if (modules.isEmpty()) {
            centerText("No modules found", contentLeft + contentWidth / 2, top + 110, TEXT);
            centerText("Clear the search to show modules.", contentLeft + contentWidth / 2, top + 125, MUTED);
        }

        String status = SpaceClient.hud.getError();
        if (status.isEmpty()) status = SpaceClient.hud.getNotice();
        if (status.isEmpty() && !SpaceClient.hud.getConfig().active().enabled) status = "HUD hidden; enable MASTER";
        if (status.isEmpty() && readOnly) status = "Settings are read-only";
        if (status.isEmpty()) {
            for (int i = HudModulePages.first(page, modules.size(), layout.pageSize); i < HudModulePages.end(page, modules.size(), layout.pageSize); i++) {
                if (hovered(mouseX, mouseY, cardX(i), cardY(i), cardWidth, cardHeight)) {
                    status = "Click: settings  |  ON/OFF: toggle";
                    break;
                }
            }
        }
        if (status.isEmpty()) status = "Customize your in-game overlay";
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(status, contentWidth - 90), contentLeft + 2, top + 27,
            readOnly || !SpaceClient.hud.getError().isEmpty() ? ACCENT : MUTED);

        int footerY = layout.footerY();
        if (hasPages()) {
            int prevX = contentLeft, nextX = contentLeft + 64;
            box(prevX, footerY, 20, 20, focus == previousControl(), hovered(mouseX, mouseY, prevX, footerY, 20, 20));
            centerText("<", prevX + 10, footerY + 6, TEXT);
            centerText((page + 1) + "/" + HudModulePages.count(modules.size(), layout.pageSize), contentLeft + 42, footerY + 6, MUTED);
            box(nextX, footerY, 20, 20, focus == nextControl(), hovered(mouseX, mouseY, nextX, footerY, 20, 20));
            centerText(">", nextX + 10, footerY + 6, TEXT);
        }
        int doneX = contentLeft + contentWidth - 68;
        box(doneX, footerY, 68, 20, focus == modules.size() + 3, hovered(mouseX, mouseY, doneX, footerY, 68, 20));
        centerText("Done / Esc", doneX + 34, footerY + 6, TEXT);
        if (contentWidth >= 360) SpaceUiFont.draw(client, "Enter: settings  |  Space: toggle", contentLeft + 96, footerY + 6, MUTED);
    }

    private void drawSidebar() {
        fill(left + 68, top + 1, left + 69, top + panelHeight - 1, BORDER);
        SpaceUiFont.draw(client, "S P A C E", left + 12, top + 15, TEXT);
        rounded(left + 7, top + 46, 54, 25, 0xFF252321);
        fill(left + 7, top + 49, left + 9, top + 68, 0xFFD7AF6A);
        SpaceUiFont.draw(client, "HUD", left + 16, top + 54, TEXT);
        SpaceUiFont.draw(client, "MODULES", left + 12, top + 82, MUTED);
        SpaceUiFont.draw(client, "MASTER", left + 9, layout.profileTop - 13, MUTED);
        fill(left + 10, top + panelHeight - 54, left + 58, top + panelHeight - 53, BORDER);
        SpaceUiFont.draw(client, "PROFILE", left + 12, top + panelHeight - 45, MUTED);
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(SpaceClient.hud.getConfig().active().name, 46),
            left + 12, top + panelHeight - 30, TEXT);
    }

    private boolean roomy() { return width >= 320 && height >= 240; }

    @Override protected void keyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            if (!Keyboard.isRepeatEvent()) super.keyPressed(character, key);
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
                focus = HudModulePages.first(page, modules.size(), layout.pageSize) + 2;
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
                page = HudModulePages.clamp(page, modules.size(), layout.pageSize);
            }
            return;
        }
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            focus = Math.floorMod(focus + (key == Keyboard.KEY_UP ? -1 : 1), controlCount());
            updateFocus();
        } else if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER || key == Keyboard.KEY_SPACE)
                && !Keyboard.isRepeatEvent()) {
            activate(focus, key == Keyboard.KEY_SPACE);
        }
    }

    @Override protected void mouseClicked(int x, int y, int button) {
        if (button != 0 || !roomy()) return;
        if (hovered(x, y, contentLeft, top + 44, layout.searchWidth, 23)) {
            focus = 0;
            updateFocus();
            search.mouseClicked(x, y, button);
            search.setFocused(true);
        } else if (hovered(x, y, layout.profileLeft, layout.profileTop, layout.profileWidth, 23)) {
            focus = 1;
            updateFocus();
            activate(focus, true);
        } else if (hovered(x, y, contentLeft + contentWidth - 82, top + 10, 82, 20)) {
            focus = modules.size() + 2;
            updateFocus();
            activate(focus, false);
        } else {
            int footerY = layout.footerY();
            if (hasPages() && hovered(x, y, contentLeft, footerY, 20, 20)) {
                focus = previousControl(); changePage(-1); updateFocus(); return;
            }
            if (hasPages() && hovered(x, y, contentLeft + 64, footerY, 20, 20)) {
                focus = nextControl(); changePage(1); updateFocus(); return;
            }
            for (int i = HudModulePages.first(page, modules.size(), layout.pageSize); i < HudModulePages.end(page, modules.size(), layout.pageSize); i++) {
                if (hovered(x, y, cardX(i), cardY(i), cardWidth, cardHeight)) {
                    focus = i + 2; updateFocus();
                    activate(focus, isToggleHit(x, y, cardX(i), cardY(i), cardWidth, cardHeight)); return;
                }
            }
            if (hovered(x, y, contentLeft + contentWidth - 68, footerY, 68, 20)) {
                focus = modules.size() + 3; updateFocus(); activate(focus, false);
            }
        }
    }

    private void activate(int control, boolean toggleModule) {
        if (control == 1 && !SpaceClient.hud.isReadOnly()) SpaceClient.toggleHud();
        else if (control == modules.size() + 2) client.setScreen(new HudEditorScreen(this));
        else if (control == modules.size() + 3) client.setScreen(null);
        else if (hasPages() && control == previousControl()) changePage(-1);
        else if (hasPages() && control == nextControl()) changePage(1);
        else if (control >= 2 && control < modules.size() + 2) {
            HudModule module = modules.get(control - 2);
            if (toggleModule) {
                if (!SpaceClient.hud.isReadOnly()) SpaceClient.hud.toggle(module);
            } else client.setScreen(new HudModuleSettingsScreen(this, module));
        }
    }

    private boolean hasPages() { return HudModulePages.count(modules.size(), layout.pageSize) > 1; }
    private int previousControl() { return modules.size() + 4; }
    private int nextControl() { return modules.size() + 5; }
    private int controlCount() { return modules.size() + 4 + (hasPages() ? 2 : 0); }
    private void changePage(int delta) { page = HudModulePages.move(page, delta, modules.size(), layout.pageSize); }
    private void updateFocus() {
        search.setFocused(focus == 0);
        if (focus >= 2 && focus < modules.size() + 2) page = HudModulePages.forIndex(focus - 2, modules.size(), layout.pageSize);
    }
    private int cardX(int index) { return layout.cardX(index); }
    private int cardY(int index) { return layout.cardY(index); }

    private void stateBadge(int x, int y, boolean enabled) {
        capsule(x + 2, y + 1, 24, 12, enabled ? 0xFFD2AC70 : 0xFF3A3C41);
        int knob = enabled ? x + 15 : x + 4;
        capsule(knob, y + 2, 10, 10, enabled ? 0xFFF5F2EB : 0xFF96989E);
    }

    private void capsule(int x, int y, int w, int h, int color) {
        for (int row = 0; row < h; row++) {
            double dy = row + .5 - h / 2.0;
            int inset = Math.max(0, (int) Math.ceil(h / 2.0 - Math.sqrt(h * h / 4.0 - dy * dy)));
            fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private void primaryButton(int x, int y, int w, int h, boolean focused, boolean hovered) {
        rounded(x, y, w, h, focused ? 0xFFF8E5C3 : 0xFFD7AF6A);
        rounded(x + 1, y + 1, w - 2, h - 2, hovered || focused ? 0xFFEACB92 : 0xFFD7AF6A);
    }
    private void drawDescription(HudModule module, int x, int y, int width, int height) {
        if (height < 48) {
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getDescription(), width), x, y, MUTED);
            return;
        }
        String full = module.getDisplayDescription();
        int limit = (int) (width / .8f);
        String first = SpaceUiFont.trimToWidth(full, limit);
        if (first.length() < full.length()) {
            int lastSpace = first.lastIndexOf(' ');
            if (lastSpace > 0) first = first.substring(0, lastSpace);
        }
        String rest = full.substring(first.length()).trim();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(.8f, .8f, 1);
            SpaceUiFont.draw(client, first, x / .8f, y / .8f, MUTED);
            if (!rest.isEmpty()) SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(rest, limit), x / .8f, (y + 9) / .8f, MUTED);
        } finally { GlStateManager.popMatrix(); }
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
    private void centerText(String text, int centerX, int y, int color) {
        SpaceUiFont.draw(client, text, centerX - SpaceUiFont.width(text) / 2f, y, color);
    }
    private void rounded(int x, int y, int w, int h, int color) {
        fill(x + 2, y, x + w - 2, y + h, color);
        fill(x, y + 2, x + w, y + h - 2, color);
        fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    /** Four GUI units around the visible badge are reserved for toggling. */
    static boolean isToggleHit(int px, int py, int x, int y, int width, int height) {
        int badgeY = y + 6 + (height - 36) / 2;
        return px >= x + width - 40 && px < x + width - 4
            && py >= badgeY - 4 && py < badgeY + 18;
    }

    private boolean hovered(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
