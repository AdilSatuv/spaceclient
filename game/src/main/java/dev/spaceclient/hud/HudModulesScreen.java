package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import dev.spaceclient.mods.GameModuleSettingsScreen;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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
    private HudSection section = HudSection.HOME;
    private ModuleFilter filter = ModuleFilter.ALL;
    private List<CatalogModule> modules = section.catalog("");
    private final Map<String, HudSwitch.Motion> switches = new HashMap<String, HudSwitch.Motion>();
    private int focus = -1;
    private int page;
    private int left, top, panelWidth, panelHeight, contentLeft, contentWidth, cardWidth, cardHeight;
    private boolean sidebar;
    private HudModulesLayout layout;
    private HudViewport viewport;
    private boolean previousRepeatEvents;
    private boolean capturedRepeatEvents;

    @Override public void init() {
        int previousCard = focus >= 2 && focus < modules.size() + 2 ? focus - 2 : -1;
        refresh();
        if (previousCard >= 0) focus = modules.isEmpty() ? 0 : Math.min(previousCard, modules.size() - 1) + 2;
        if (!capturedRepeatEvents) {
            previousRepeatEvents = Keyboard.areRepeatEventsEnabled();
            capturedRepeatEvents = true;
        }
        Keyboard.enableRepeatEvents(true);
        viewport = new HudViewport(width, height);
        layout = new HudModulesLayout(viewport.width, viewport.height);
        page = HudModulePages.clamp(page, modules.size(), layout.pageSize);
        panelWidth = layout.panelWidth; panelHeight = layout.panelHeight;
        left = layout.left; top = layout.top;
        sidebar = layout.sidebar;
        contentLeft = layout.contentLeft; contentWidth = layout.contentWidth;
        cardWidth = layout.cardWidth; cardHeight = layout.cardHeight;
        search = new TextFieldWidget(0, textRenderer, contentLeft + 30, top + layout.searchTop + 6,
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
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(viewport.scale, viewport.scale, 1);
            renderPanel(viewport.toUi(mouseX), viewport.toUi(mouseY));
        } finally { GlStateManager.popMatrix(); }
    }
    private void renderPanel(int mouseX, int mouseY) {
        // A restrained shell leaves the world visible around the edges.
        rounded(left + 2, top + 3, panelWidth, panelHeight, 0x90000000);
        rounded(left, top, panelWidth, panelHeight, BORDER);
        rounded(left + 1, top + 1, panelWidth - 2, panelHeight - 2, 0xFF101114);
        if (sidebar) drawSidebar(mouseX, mouseY);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(2f, 2f, 1);
            SpaceUiFont.draw(client, section == HudSection.HOME ? "Modules" : section.label, contentLeft / 2f, (top + 19) / 2f, TEXT);
        } finally { GlStateManager.popMatrix(); }
        int editorX = contentLeft + contentWidth - 82, editorY = top + 20;
        primaryButton(editorX, editorY, 82, 20, focus == modules.size() + 2,
            hovered(mouseX, mouseY, editorX, editorY, 82, 20));
        centerText("Edit layout", editorX + 41, editorY + 6, 0x17130D);

        box(contentLeft, top + layout.searchTop, layout.searchWidth, 23, focus == 0,
            hovered(mouseX, mouseY, contentLeft, top + layout.searchTop, layout.searchWidth, 23));
        searchIcon(contentLeft + 10, top + layout.searchTop + 7);
        search.render();
        if (query.isEmpty() && !search.isFocused())
            SpaceUiFont.draw(client, "Search modules...", contentLeft + 30, top + layout.searchTop + 6, MUTED);
        for (ModuleFilter item : ModuleFilter.values()) {
            int x = layout.filterLeft + item.ordinal() * 53, y = top + layout.searchTop;
            box(x, y, layout.filterWidth, 23, focus == filterControl(item), hovered(mouseX, mouseY, x, y, layout.filterWidth, 23));
            if (item == filter) fill(x + 5, y + 21, x + layout.filterWidth - 5, y + 22, 0xFFD7AF6A);
            centerText(item.label, x + layout.filterWidth / 2, y + 6, item == filter ? ACCENT : MUTED);
        }

        boolean readOnly = SpaceClient.hud.isReadOnly();
        box(layout.profileLeft, layout.profileTop, layout.profileWidth, 23, focus == 1,
            hovered(mouseX, mouseY, layout.profileLeft, layout.profileTop, layout.profileWidth, 23));
        SpaceUiFont.draw(client, "HUD", layout.profileLeft + 6, layout.profileTop + 6, TEXT);
        int masterSwitchX = layout.profileLeft + layout.profileWidth - 34;
        stateBadge("master", masterSwitchX, layout.profileTop + 4, SpaceClient.hud.getConfig().active().enabled);

        for (int i = HudModulePages.first(page, modules.size(), layout.pageSize); i < HudModulePages.end(page, modules.size(), layout.pageSize); i++) {
            CatalogModule module = modules.get(i);
            int x = cardX(i), y = cardY(i);
            boolean enabled = module.isEnabled(SpaceClient.hud.getConfig(), SpaceClient.mods);
            card(x, y, cardWidth, cardHeight, focus == i + 2,
                hovered(mouseX, mouseY, x, y, cardWidth, cardHeight), enabled);
            int insetY = (cardHeight - 36) / 2;
            int iconSize = cardWidth >= 190 ? 24 : cardWidth >= 145 ? 22 : 16;
            int textInset = iconSize + (cardWidth >= 190 ? 26 : 17);
            HudModuleIcons.draw(module, x + (cardWidth >= 190 ? 15 : 8), y + (cardHeight - iconSize) / 2, iconSize);
            int titleWidth = Math.max(1, cardWidth - textInset - 38);
            SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(module.getTitle(), titleWidth), x + textInset, y + 8 + insetY, TEXT);
            stateBadge(module.id(), x + cardWidth - 36, y + 6 + insetY, enabled);
            drawDescription(module, x + textInset, y + 23 + insetY, Math.max(1, cardWidth - textInset - 8), cardHeight);
            if (cardHeight >= 44 && cardWidth >= 145) {
                HudModuleIcons.star(x + cardWidth - 34, y + cardHeight - 17, SpaceClient.browser.favorite(module.id()));
                SpaceUiFont.draw(client, ">", x + cardWidth - 13, y + cardHeight - 16, MUTED);
            }
        }
        if (modules.isEmpty()) {
            centerText("No modules found", contentLeft + contentWidth / 2, top + 110, TEXT);
            centerText(filter == ModuleFilter.FAVORITES ? "Star a card or press F to add favorites." : "Try All or clear the search.", contentLeft + contentWidth / 2, top + 125, MUTED);
        }

        String status = SpaceClient.hud.getError();
        if (status.isEmpty()) status = SpaceClient.mods.error();
        if (status.isEmpty()) status = SpaceClient.browser.error();
        if (status.isEmpty()) status = SpaceClient.hud.getNotice();
        if (status.isEmpty() && !SpaceClient.hud.getConfig().active().enabled) status = "HUD hidden; enable HUD";
        if (status.isEmpty() && readOnly) status = "Settings are read-only";
        if (status.isEmpty()) {
            for (int i = HudModulePages.first(page, modules.size(), layout.pageSize); i < HudModulePages.end(page, modules.size(), layout.pageSize); i++) {
                if (hovered(mouseX, mouseY, cardX(i), cardY(i), cardWidth, cardHeight)) {
                    status = "Click: settings  |  Switch: toggle  |  F: favorite";
                    break;
                }
            }
        }
        if (status.isEmpty()) status = modules.size() + " modules  /  " + section.description;
        SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(status, contentWidth - 90), contentLeft + 2, top + 46,
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
        if (contentWidth >= 360) SpaceUiFont.draw(client, "Enter: settings  |  Space: toggle  |  F: star", contentLeft + 96, footerY + 6, MUTED);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        fill(left + layout.sidebarWidth, top + 1, left + layout.sidebarWidth + 1, top + panelHeight - 1, BORDER);
        SpaceUiFont.draw(client, "S P A C E", left + 15, top + 19, TEXT);
        for (HudSection item : HudSection.values()) {
            int y = sectionY(item), x = left + 7, w = layout.sidebarWidth - 14;
            boolean selected = item == section;
            boolean focused = focus == sectionControlStart() + item.ordinal();
            boolean over = hovered(mouseX, mouseY, x, y, w, 25);
            if (selected || focused || over) rounded(x, y, w, 25, focused ? 0xFF3C352B : 0xFF252321);
            if (selected || focused) fill(x, y + 3, x + 2, y + 22, 0xFFD7AF6A);
            HudModuleIcons.drawSection(item, left + 17, y + 6, 13, selected);
            SpaceUiFont.draw(client, item.label, left + 39, y + 7, selected ? TEXT : MUTED);
        }
        fill(left + 10, sectionY(HudSection.SETTINGS) - 7, left + layout.sidebarWidth - 10, sectionY(HudSection.SETTINGS) - 6, BORDER);
    }

    private int sectionY(HudSection item) {
        return layout.sectionTop(item);
    }
    private int sectionControlStart() { return modules.size() + 4 + (hasPages() ? 2 : 0); }
    private void selectSection(HudSection next) {
        if (next == HudSection.SETTINGS) { client.setScreen(new SettingsScreen(this, client.options)); return; }
        section = next;
        query = "";
        search.setText(query);
        refresh();
        page = 0;
        focus = sectionControlStart() + next.ordinal();
        updateFocus();
    }

    private boolean roomy() { return width >= 320 && height >= 240; }

    @Override protected void keyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            if (!Keyboard.isRepeatEvent()) super.keyPressed(character, key);
            return;
        }
        if (!roomy()) return;
        if (key == Keyboard.KEY_TAB) {
            int direction = hasShiftDown() ? -1 : 1;
            focus = focus < 0 ? (direction < 0 ? controlCount() - 1 : 0)
                : Math.floorMod(focus + direction, controlCount());
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
                refresh();
                page = HudModulePages.clamp(page, modules.size(), layout.pageSize);
            }
            return;
        }
        if (key == Keyboard.KEY_F && focus >= 2 && focus < modules.size() + 2 && !Keyboard.isRepeatEvent()) {
            favorite(focus - 2); return;
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
        x = viewport.toUi(x);
        y = viewport.toUi(y);
        for (ModuleFilter item : ModuleFilter.values()) {
            if (hovered(x, y, layout.filterLeft + item.ordinal() * 53, top + layout.searchTop, layout.filterWidth, 23)) {
                selectFilter(item); return;
            }
        }
        if (sidebar) {
            for (HudSection item : HudSection.values()) {
                if (hovered(x, y, left + 7, sectionY(item), layout.sidebarWidth - 14, 25)) {
                    selectSection(item); return;
                }
            }
        }
        if (hovered(x, y, contentLeft, top + layout.searchTop, layout.searchWidth, 23)) {
            focus = 0;
            updateFocus();
            search.mouseClicked(x, y, button);
            search.setFocused(true);
        } else if (hovered(x, y, layout.profileLeft, layout.profileTop, layout.profileWidth, 23)) {
            focus = 1;
            updateFocus();
            activate(focus, true);
        } else if (hovered(x, y, contentLeft + contentWidth - 82, top + 20, 82, 20)) {
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
                    if (cardHeight >= 44 && cardWidth >= 145 && hovered(x, y, cardX(i) + cardWidth - 39, cardY(i) + cardHeight - 24, 22, 22)) {
                        favorite(i); return;
                    }
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
        else if (control >= filterControl(ModuleFilter.ALL) && control <= filterControl(ModuleFilter.FAVORITES))
            selectFilter(ModuleFilter.values()[control - filterControl(ModuleFilter.ALL)]);
        else if (sidebar && control >= sectionControlStart() && control < sectionControlStart() + HudSection.values().length)
            selectSection(HudSection.values()[control - sectionControlStart()]);
        else if (control >= 2 && control < modules.size() + 2) {
            CatalogModule module = modules.get(control - 2);
            if (toggleModule) {
                if (module.hud != null) { if (!SpaceClient.hud.isReadOnly()) SpaceClient.hud.toggle(module.hud); }
                else module.game.toggle(SpaceClient.mods);
                int previousCard = control - 2;
                refresh(); focus = modules.isEmpty() ? filterControl(filter) : Math.min(previousCard, modules.size() - 1) + 2;
                updateFocus();
            } else if (module.hud != null) client.setScreen(new HudModuleSettingsScreen(this, module.hud));
            else client.setScreen(new GameModuleSettingsScreen(this, module.game));
        }
    }

    private boolean hasPages() { return HudModulePages.count(modules.size(), layout.pageSize) > 1; }
    private int previousControl() { return modules.size() + 4; }
    private int nextControl() { return modules.size() + 5; }
    private int filterControl(ModuleFilter item) { return sectionControlStart() + (sidebar ? HudSection.values().length : 0) + item.ordinal(); }
    private int controlCount() { return filterControl(ModuleFilter.FAVORITES) + 1; }
    private void refresh() {
        modules = filter.apply(section.catalog(query), SpaceClient.hud.getConfig(), SpaceClient.mods, SpaceClient.browser);
        if (layout != null) page = HudModulePages.clamp(page, modules.size(), layout.pageSize);
    }
    private void selectFilter(ModuleFilter next) {
        filter = next; refresh(); page = 0; focus = filterControl(next); updateFocus();
    }
    private void favorite(int index) {
        SpaceClient.browser.toggleFavorite(modules.get(index).id());
        refresh(); focus = modules.isEmpty() ? filterControl(filter) : Math.min(index, modules.size() - 1) + 2; updateFocus();
    }
    @Override public void handleMouse() {
        super.handleMouse();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && roomy() && hasPages()) {
            changePage(wheel > 0 ? -1 : 1);
            focus = HudModulePages.first(page, modules.size(), layout.pageSize) + 2; updateFocus();
        }
    }
    private void changePage(int delta) { page = HudModulePages.move(page, delta, modules.size(), layout.pageSize); }
    private void updateFocus() {
        search.setFocused(focus == 0);
        if (focus >= 2 && focus < modules.size() + 2) page = HudModulePages.forIndex(focus - 2, modules.size(), layout.pageSize);
    }
    private int cardX(int index) { return layout.cardX(index); }
    private int cardY(int index) { return layout.cardY(index); }

    private void stateBadge(String id, int x, int y, boolean enabled) {
        HudSwitch.Motion motion = switches.get(id);
        if (motion == null) { motion = new HudSwitch.Motion(enabled); switches.put(id, motion); }
        HudSwitch.draw(x, y, motion.sample(enabled, System.nanoTime()));
    }

    private void primaryButton(int x, int y, int w, int h, boolean focused, boolean hovered) {
        rounded(x, y, w, h, focused ? 0xFFF8E5C3 : 0xFFD7AF6A);
        rounded(x + 1, y + 1, w - 2, h - 2, hovered || focused ? 0xFFEACB92 : 0xFFD7AF6A);
    }
    private void drawDescription(CatalogModule module, int x, int y, int width, int height) {
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
            if (!rest.isEmpty()) SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(rest, Math.max(1,(int)((width-32)/.8f))), x / .8f, (y + 9) / .8f, MUTED);
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
        UiShapes.rounded(x, y, w, h, 5, color);
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
