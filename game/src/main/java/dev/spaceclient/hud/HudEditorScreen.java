package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Minecraft input/drawing adapter. Only complete gestures are committed through HudSettings. */
public final class HudEditorScreen extends Screen {
    private static final int TEXT = 0xF2EEE7, MUTED = 0xB7B3AD, ACCENT = 0xD7AF6A;
    private static final int BORDER = 0xFF303238, PANEL = 0xF2191B20, SURFACE = 0xFF222429;
    private final Screen parent;
    private HudModule selected = HudModule.FPS;
    private HudPlacement draft;
    private boolean dragging, capturedRepeat, previousRepeat;
    private boolean profilesTab;
    private double grabX, grabY;
    private TextFieldWidget name;
    private int lastMouseX, lastMouseY;
    private String status = "";
    private final HudEditorConfirmation confirmation = new HudEditorConfirmation();

    public HudEditorScreen(Screen parent) { this(parent, HudModule.FPS); }
    public HudEditorScreen(Screen parent, HudModule selected) {
        if (selected == null) throw new IllegalArgumentException("Missing HUD module");
        this.parent = parent;
        this.selected = selected;
    }
    private HudSettings settings() { return SpaceClient.hud; }
    private HudProfile profile() { return settings().getConfig().active(); }
    private HudPlacement placement() { return draft == null ? profile().placement(selected) : draft; }
    private String label(HudModule module) { return HudRenderer.text(client, module); }
    private HudPlacement.Bounds bounds(HudModule module) {
        return HudRenderer.bounds(client, label(module), module == selected ? placement() : profile().placement(module), width, height);
    }
    private boolean sidebar() { return HudEditorLayout.useSidebar(width, height, bounds(selected)); }
    private int barY() { return sidebar() ? HudEditorLayout.sidebarY(height) : HudEditorLayout.toolbarY(height, bounds(selected)); }
    private boolean roomy() { return width >= 320 && height >= 240; }
    private int panelWidth() { return sidebar() ? HudEditorLayout.SIDEBAR_WIDTH : HudEditorLayout.panelWidth(width); }
    private int panelHeight() { return sidebar() ? HudEditorLayout.SIDEBAR_HEIGHT : HudEditorLayout.HEIGHT; }
    private int panelX() { return sidebar() ? HudEditorLayout.sidebarX(width, bounds(selected)) : HudEditorLayout.panelX(width); }
    private int nameBoxX() { return panelX() + (sidebar() ? 42 : 40); }
    private int nameBoxY() { return barY() + (sidebar() ? 108 : 54); }

    @Override public void init() {
        String pendingName = name == null ? profile().name : name.getText();
        boolean focused = name != null && name.isFocused();
        commitDraft();
        confirmation.cancel();
        dragging = false;
        if (!capturedRepeat) {
            previousRepeat = Keyboard.areRepeatEventsEnabled();
            capturedRepeat = true;
        }
        Keyboard.enableRepeatEvents(true);
        name = new TextFieldWidget(0, textRenderer, 42, 36, 86, 16);
        name.setHasBorder(false);
        name.setMaxLength(32);
        name.setEditableColor(TEXT);
        name.setEditable(!settings().isReadOnly());
        name.setText(pendingName);
        name.setFocused(focused);
        SpaceClient.clicks.clear();
    }
    @Override public void tick() { name.tick(); }
    @Override public boolean shouldPauseGame() { return true; }
    @Override public void removed() {
        SpaceUiFont.release();
        finishName();
        commitDraft();
        if (capturedRepeat) Keyboard.enableRepeatEvents(previousRepeat);
        SpaceClient.clicks.clear();
    }
    @Override public void render(int mouseX, int mouseY, float delta) {
        // Keep the game visible while lifting the inspector above it.
        fillGradient(0, 0, width, height, 0x5006080A, 0x8006080A);
        if (client.player == null || !roomy()) {
            centerText("Resize the window to edit HUD", width / 2, height / 2 - 12, TEXT);
            centerText("Esc - back", width / 2, height / 2 + 4, MUTED);
            return;
        }
        lastMouseX = mouseX; lastMouseY = mouseY;
        for (HudModule module : HudModule.values()) if (module != selected) drawModule(module);
        drawModule(selected);
        int y = barY(), x = panelX(), w = panelWidth(), h = panelHeight();
        rounded(x + 2, y + 3, w, h, 0x88000000);
        rounded(x, y, w, h, BORDER);
        rounded(x + 1, y + 1, w - 2, h - 2, PANEL);
        if (sidebar()) {
            SpaceUiFont.draw(client, "HUD EDITOR", x + 8, y + 10, TEXT);
            fill(x + 8, y + 27, x + w - 8, y + 28, BORDER);
            fill(x + 8, y + 76, x + w - 8, y + 77, BORDER);
            fill(x + 8, y + 194, x + w - 8, y + 195, BORDER);
        } else {
            fill(x + 6, y + 28, x + w - 6, y + 29, BORDER);
            fill(x + 6, y + 76, x + w - 6, y + 77, BORDER);
        }
        String hoverHint = "";
        for (Action action : controls()) {
            boolean hovered = action.contains(mouseX, mouseY);
            boolean activeProfile = action.id == 10 + settings().getConfig().activeIndex();
            boolean confirmed = confirmation.isPending(action.id);
            boolean chosen = activeProfile || confirmed || action.id == 12
                || action.id == (profilesTab ? 51 : 50);
            boolean primary = action.id == 13;
            box(action.x, action.y, action.width, 20, chosen, hovered && action.enabled, action.enabled, primary);
            int ink = !action.enabled ? 0x817E78 : primary ? 0x15120E : confirmed ? ACCENT : TEXT;
            centerText(SpaceUiFont.trimToWidth(action.label, action.width - 6),
                action.x + action.width / 2, action.y + 6, ink);
            if (hovered) hoverHint = action.hint;
        }
        if (sidebar()) {
            SpaceUiFont.draw(client, "L/P: tabs", x + 8, y + 182, MUTED);
            if (profilesTab) {
                SpaceUiFont.draw(client, "Name", x + 8, y + 114, MUTED);
                box(nameBoxX(), nameBoxY(), 90, 20, name.isFocused(), false, !settings().isReadOnly(), false);
                name.x = nameBoxX() + 2; name.y = nameBoxY() + 6; name.render();
            } else {
                SpaceUiFont.draw(client, "MODULE", x + 8, y + 88, MUTED);
                SpaceUiFont.draw(client, "POSITION", x + 8, y + 108, MUTED);
                SpaceUiFont.draw(client, "SCALE", x + 8, y + 134, MUTED);
                SpaceUiFont.draw(client, String.format(Locale.ROOT, "%.1fx", placement().scale), x + 88, y + 134, TEXT);
                SpaceUiFont.draw(client, "ANCHOR", x + 8, y + 160, MUTED);
            }
        } else if (profilesTab) {
            SpaceUiFont.draw(client, "Name", x + 6, y + 60, MUTED);
            box(nameBoxX(), nameBoxY(), 90, 20, name.isFocused(), false, !settings().isReadOnly(), false);
            name.x = nameBoxX() + 2; name.y = nameBoxY() + 6; name.render();
            SpaceUiFont.draw(client, "L/P: tabs", x + 180, y + 60, MUTED);
        } else {
            SpaceUiFont.draw(client, "MOVE", x + 6, y + 36, MUTED);
            SpaceUiFont.draw(client, "SCALE", x + 132, y + 36, MUTED);
            SpaceUiFont.draw(client, String.format(Locale.ROOT, "%.1fx", placement().scale), x + 190, y + 36, ACCENT);
            SpaceUiFont.draw(client, "EDGE", x + 6, y + 60, MUTED);
            SpaceUiFont.draw(client, "L/P: tabs", x + 207, y + 60, MUTED);
        }
        String message = confirmation.message();
        if (message.isEmpty()) message = settings().getError();
        if (message.isEmpty() && settings().isReadOnly()) message = settings().getNotice();
        if (message.isEmpty()) message = hoverHint.isEmpty() ? status : hoverHint;
        if (message.isEmpty()) message = sidebar() ? "Arrows: move | +/-: scale" : profilesTab ? "1/2: profile  |  N: rename" : "Tab: module  |  Arrows: move  |  +/-: scale";
        if (confirmation.message().isEmpty() && !profile().enabled && !settings().isReadOnly() && settings().getError().isEmpty() && hoverHint.isEmpty()) message = "HUD hidden; this is a layout preview. Esc - modules";
        int messageY = y + (sidebar() ? 196 : 81);
        if (sidebar()) {
            // Long notices must stay understandable in the narrower inspector.
            String first = SpaceUiFont.trimToWidth(message, w - 16);
            SpaceUiFont.draw(client, first, x + 8, messageY, settings().getError().isEmpty() ? MUTED : ACCENT);
            if (first.length() < message.length()) {
                String rest = message.substring(first.length()).trim();
                SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(rest, w - 16), x + 8, messageY + 11,
                    settings().getError().isEmpty() ? MUTED : ACCENT);
            }
        } else SpaceUiFont.draw(client, SpaceUiFont.trimToWidth(message, w - 12), x + 6, messageY,
            settings().getError().isEmpty() ? MUTED : ACCENT);
    }
    private void drawModule(HudModule module) {
        HudPlacement.Bounds b = bounds(module);
        int left = Math.max(0, (int) Math.floor(b.x) - 2), top = Math.max(0, (int) Math.floor(b.y) - 2);
        int right = Math.min(width, (int) Math.ceil(b.x + b.width) + 2), bottom = Math.min(height, (int) Math.ceil(b.y + b.height) + 2);
        int border = module == selected ? 0xFFD7AF6A : BORDER;
        fill(left, top, right, top + 1, border); fill(left, bottom - 1, right, bottom, border);
        fill(left, top, left + 1, bottom, border); fill(right - 1, top, right, bottom, border);
        if (module == selected) {
            // Corner handles distinguish selection by shape as well as colour.
            fill(left, top, Math.min(right, left + 3), Math.min(bottom, top + 3), 0xFFF2EEE7);
            fill(Math.max(left, right - 3), Math.max(top, bottom - 3), right, bottom, 0xFFF2EEE7);
        }
        boolean visible = profile().enabled && module.isEnabled(settings().getConfig());
        HudAppearance appearance = profile().appearance(module);
        HudRenderer.draw(client, label(module), b, appearance, visible ? HudRenderer.color(module, appearance) : MUTED);
    }
    private List<Action> controls() {
        List<Action> actions = new ArrayList<>();
        int y = barY(), x = panelX(), w = panelWidth();
        boolean editable = !settings().isReadOnly();
        String anchor = (placement().horizontal == HudPlacement.Horizontal.LEFT ? "L" : "R")
            + "/" + (placement().vertical == HudPlacement.Vertical.TOP ? "T" : "B");
        if (sidebar()) {
            actions.add(new Action(13, "Done", x + w - 54, y + 5, 46, true, "Return to modules (Esc); changes save automatically"));
            actions.add(new Action(14, "<", x + 8, y + 31, 20, true, "Previous module (Shift+Tab / PgUp)"));
            actions.add(new Action(12, selected.getTitle(), x + 30, y + 31, 88, true, "Next module (Tab / PgDn)"));
            actions.add(new Action(15, ">", x + 120, y + 31, 20, true, "Next module (Tab / PgDn)"));
            actions.add(new Action(50, "LAYOUT", x + 8, y + 54, 65, true, "Layout controls (L)"));
            actions.add(new Action(51, "PROFILES", x + 77, y + 54, 70, true, "Profile and import/export controls (P)"));
            if (profilesTab) {
                actions.add(new Action(10, "1: " + settings().getConfig().profile(0).name, x + 8, y + 82, 65, editable, "Select HUD profile 1 (key 1)"));
                actions.add(new Action(11, "2: " + settings().getConfig().profile(1).name, x + 77, y + 82, 70, editable, "Select HUD profile 2 (key 2)"));
                actions.add(new Action(47, confirmation.isPending(47) ? "Yes" : "Import", x + 8, y + 134, 44, editable, "Import and replace this profile (I)"));
                actions.add(new Action(48, "Export", x + 56, y + 134, 44, editable, "Export this profile (O)"));
                actions.add(new Action(30, "Save", x + 104, y + 134, 44, editable, "Save profile name (N, then Enter)"));
                actions.add(new Action(45, confirmation.isPending(45) ? "Yes" : "Reset all", x + 8, y + 160, 70, editable, "Reset this profile layout (Home)"));
            } else {
                actions.add(new Action(46, selected.isEnabled(settings().getConfig()) ? "ON" : "OFF", x + 105, y + 82, 42, editable, "Toggle selected module (Space)"));
                actions.add(new Action(40, "<", x + 58, y + 102, 20, editable, "Move left one GUI unit (Left arrow)"));
                actions.add(new Action(41, ">", x + 80, y + 102, 20, editable, "Move right one GUI unit (Right arrow)"));
                actions.add(new Action(42, "^", x + 102, y + 102, 20, editable, "Move up one GUI unit (Up arrow)"));
                actions.add(new Action(43, "v", x + 124, y + 102, 20, editable, "Move down one GUI unit (Down arrow)"));
                actions.add(new Action(20, "-", x + 62, y + 128, 20, editable, "Smaller widget (- or mouse wheel over widget)"));
                actions.add(new Action(21, "+", x + 120, y + 128, 20, editable, "Larger widget (+ or mouse wheel over widget)"));
                actions.add(new Action(22, anchor, x + 54, y + 154, 40, editable, "Change edge anchors, keep current position (A)"));
                actions.add(new Action(44, "Reset", x + 98, y + 154, 48, editable, "Reset selected placement and scale (R)"));
            }
            return actions;
        }
        actions.add(new Action(50, "EDIT", x + 6, y + 6, 42, true, "Layout controls (L)"));
        actions.add(new Action(51, "PROFILES", x + 51, y + 6, 56, true, "Profile and import/export controls (P)"));
        actions.add(new Action(14, "<", x + 110, y + 6, 18, true, "Previous module (Shift+Tab / PgUp)"));
        actions.add(new Action(12, selected.getTitle(), x + 130, y + 6, 68, true, "Next module (Tab / PgDn)"));
        actions.add(new Action(15, ">", x + 200, y + 6, 18, true, "Next module (Tab / PgDn)"));
        actions.add(new Action(13, "Done", x + w - 60, y + 6, 54, true, "Return to modules (Esc); changes save automatically"));
        if (profilesTab) {
            actions.add(new Action(10, "1: " + settings().getConfig().profile(0).name, x + 6, y + 30, 70, editable, "Select HUD profile 1 (key 1)"));
            actions.add(new Action(11, "2: " + settings().getConfig().profile(1).name, x + 80, y + 30, 70, editable, "Select HUD profile 2 (key 2)"));
            actions.add(new Action(45, confirmation.isPending(45) ? "Yes" : "Reset all", x + 154, y + 30, 54, editable, "Reset this profile layout (Home)"));
            actions.add(new Action(47, confirmation.isPending(47) ? "Yes" : "Import", x + 212, y + 30, 42, editable, "Import and replace this profile (I)"));
            actions.add(new Action(48, "Export", x + 258, y + 30, 42, editable, "Export this profile (O)"));
            actions.add(new Action(30, "Save", x + 134, y + 54, 44, editable, "Save profile name (N, then Enter)"));
        } else {
            actions.add(new Action(40, "<", x + 36, y + 30, 20, editable, "Move left one GUI unit (Left arrow)"));
            actions.add(new Action(41, ">", x + 58, y + 30, 20, editable, "Move right one GUI unit (Right arrow)"));
            actions.add(new Action(42, "^", x + 80, y + 30, 20, editable, "Move up one GUI unit (Up arrow)"));
            actions.add(new Action(43, "v", x + 102, y + 30, 20, editable, "Move down one GUI unit (Down arrow)"));
            actions.add(new Action(20, "-", x + 164, y + 30, 22, editable, "Smaller widget (- or mouse wheel over widget)"));
            actions.add(new Action(21, "+", x + 220, y + 30, 22, editable, "Larger widget (+ or mouse wheel over widget)"));
            actions.add(new Action(22, anchor, x + 48, y + 54, 42, editable, "Change edge anchors, keep current position (A)"));
            actions.add(new Action(44, "Reset", x + 96, y + 54, 46, editable, "Reset selected placement and scale (R)"));
            actions.add(new Action(46, selected.isEnabled(settings().getConfig()) ? "ON" : "OFF", x + 148, y + 54, 42, editable, "Toggle selected module (Space)"));
        }
        return actions;
    }
    private void box(int x, int y, int w, int h, boolean focused, boolean hovered, boolean enabled, boolean primary) {
        int edge = focused ? 0xFFD7AF6A : hovered ? 0xFF71695C : BORDER;
        int inside = !enabled ? 0xFF1A1B1E : primary ? (hovered ? 0xFFE6C78F : 0xFFD7AF6A)
            : focused ? 0xFF373027 : hovered ? 0xFF30302F : SURFACE;
        rounded(x, y, w, h, primary ? inside : edge);
        if (!primary) rounded(x + 1, y + 1, w - 2, h - 2, inside);
        if (focused && !primary) fill(x + 4, y + h - 2, x + w - 4, y + h - 1, 0xFFD7AF6A);
    }
    private void centerText(String text, int centerX, int y, int color) {
        SpaceUiFont.draw(client, text, centerX - SpaceUiFont.width(text) / 2f, y, color);
    }
    private void rounded(int x, int y, int w, int h, int color) {
        fill(x + 2, y, x + w - 2, y + h, color);
        fill(x, y + 2, x + w, y + h - 2, color);
        fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        if (button != 0 || !roomy() || client.player == null || dragging) return;
        int yBar = barY();
        int xBar = panelX();
        if (profilesTab && x >= nameBoxX() && x < nameBoxX() + 90 && y >= nameBoxY() && y < nameBoxY() + 20) {
            cancelConfirmation();
            if (name.isFocused()) name.mouseClicked(x, y, button);
            else focusName();
            return;
        }
        if (!finishName()) return;
        for (Action action : controls()) if (action.contains(x, y)) {
            if (action.enabled) activate(action.id);
            else cancelConfirmation();
            return;
        }
        cancelConfirmation();
        // Empty inspector space is not part of the draggable canvas.
        if (x >= xBar && x < xBar + panelWidth() && y >= yBar && y < yBar + panelHeight()) return;
        if (settings().isReadOnly()) return;
        if (bounds(selected).contains(x, y)) { beginDrag(selected, x, y); return; }
        HudModule[] modules = HudModule.values();
        for (int i = modules.length - 1; i >= 0; i--) if (bounds(modules[i]).contains(x, y)) {
            beginDrag(modules[i], x, y); return;
        }
    }
    private void beginDrag(HudModule module, int x, int y) {
        if (!commitDraft()) return;
        selected = module;
        HudPlacement.Bounds b = bounds(module);
        grabX = x - b.x; grabY = y - b.y;
        draft = profile().placement(module);
        dragging = true;
        status = "Drag to move; edges snap within 6 GUI units";
    }
    @Override protected void mouseDragged(int x, int y, int button, long elapsed) {
        if (button == 0 && dragging) draft = placement().at(x - grabX, y - grabY,
            width, height, HudRenderer.textWidth(client, label(selected)), 10);
    }
    @Override protected void mouseReleased(int x, int y, int button) {
        if (button == 0 && dragging) {
            mouseDragged(x, y, button, 0);
            dragging = false;
            commitDraft();
        }
    }
    @Override public void handleMouse() {
        super.handleMouse();
        int wheel = Mouse.getEventDWheel();
        if (client.currentScreen == this && roomy() && wheel != 0 && !dragging && !name.isFocused()
                && !settings().isReadOnly() && bounds(selected).contains(lastMouseX, lastMouseY)) {
            cancelConfirmation();
            scale(wheel > 0 ? 0.1 : -0.1);
            commitDraft();
        }
    }
    @Override public void handleKeyboard() {
        super.handleKeyboard();
        int key = Keyboard.getEventKey();
        if (client.currentScreen == this && !Keyboard.getEventKeyState() && !dragging && isAdjustment(key)) commitDraft();
    }
    private boolean isAdjustment(int key) {
        return key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN
            || key == Keyboard.KEY_EQUALS || key == Keyboard.KEY_ADD || key == Keyboard.KEY_MINUS || key == Keyboard.KEY_SUBTRACT;
    }
    @Override protected void keyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            if (Keyboard.isRepeatEvent()) return;
            if (cancelConfirmation()) return;
            if (name.isFocused()) { name.setText(profile().name); name.setFocused(false); return; }
            commitDraft(); client.setScreen(parent); return;
        }
        if (!roomy() || client.player == null) return;
        if (key != Keyboard.KEY_HOME && key != Keyboard.KEY_I) cancelConfirmation();
        if (name.isFocused()) {
            if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER || key == Keyboard.KEY_TAB)
                    && !Keyboard.isRepeatEvent()) finishName();
            else name.keyPressed(character, key);
            return;
        }
        if ((key == Keyboard.KEY_TAB || key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_NEXT) && !Keyboard.isRepeatEvent()) {
            if (!dragging && commitDraft()) selectModule(key == Keyboard.KEY_PRIOR || (key == Keyboard.KEY_TAB && hasShiftDown()) ? -1 : 1);
            return;
        }
        if ((key == Keyboard.KEY_L || key == Keyboard.KEY_P) && !Keyboard.isRepeatEvent()) {
            if (!dragging) activate(key == Keyboard.KEY_L ? 50 : 51);
            return;
        }
        if (settings().isReadOnly() || dragging) return;
        if (key == Keyboard.KEY_LEFT) nudge(-1, 0);
        else if (key == Keyboard.KEY_RIGHT) nudge(1, 0);
        else if (key == Keyboard.KEY_UP) nudge(0, -1);
        else if (key == Keyboard.KEY_DOWN) nudge(0, 1);
        else if (key == Keyboard.KEY_EQUALS || key == Keyboard.KEY_ADD) scale(0.1);
        else if (key == Keyboard.KEY_MINUS || key == Keyboard.KEY_SUBTRACT) scale(-0.1);
        else if (!Keyboard.isRepeatEvent()) {
            if (key == Keyboard.KEY_1) activate(10);
            else if (key == Keyboard.KEY_2) activate(11);
            else if (key == Keyboard.KEY_N) focusName();
            else if (key == Keyboard.KEY_A) activate(22);
            else if (key == Keyboard.KEY_R) activate(44);
            else if (key == Keyboard.KEY_HOME) activate(45);
            else if (key == Keyboard.KEY_SPACE) activate(46);
            else if (key == Keyboard.KEY_I) activate(47);
            else if (key == Keyboard.KEY_O) activate(48);
        }
    }
    private void nudge(int dx, int dy) {
        HudPlacement.Bounds b = bounds(selected);
        draft = placement().at(b.x + dx, b.y + dy, width, height, HudRenderer.textWidth(client, label(selected)), 10, false);
    }
    private void scale(double delta) {
        draft = placement().withScale(Math.max(0.5, Math.min(3, Math.round((placement().scale + delta) * 10) / 10.0)));
    }
    private boolean commitDraft() {
        if (draft == null) return true;
        HudPlacement pending = draft;
        draft = null;
        boolean saved = settings().place(selected, pending);
        status = saved ? "Position saved. Arrows move; +/- scale" : settings().getError();
        return saved;
    }
    private void focusName() {
        if (settings().isReadOnly() || !commitDraft()) return;
        profilesTab = true;
        name.setText(profile().name); name.setFocused(true);
        name.setCursorToStart(); name.setSelectionEnd(name.getText().length());
    }
    private boolean finishName() {
        if (name == null || !name.isFocused()) return true;
        if (!name.getText().equals(profile().name) && !settings().renameProfile(name.getText())) return false;
        name.setFocused(false); status = "Profile name saved"; return true;
    }
    private void activate(int id) {
        if (dragging) return;
        if (!commitDraft() || !finishName()) return;
        if (id != 45 && id != 47) cancelConfirmation();
        if (id == 13) { client.setScreen(parent); return; }
        if (id == 50 || id == 51) { profilesTab = id == 51; status = ""; return; }
        if (id == 12 || id == 14 || id == 15) { selectModule(id == 14 ? -1 : 1); return; }
        if (settings().isReadOnly()) return;
        if (id == 45 || id == 47) {
            profilesTab = true;
            if (!confirmation.confirm(id)) return;
        }
        boolean saved = true;
        if (id == 10 || id == 11) { saved = settings().selectProfile(id - 10); name.setText(profile().name); }
        else if (id == 30) saved = settings().renameProfile(name.getText());
        else if (id == 20 || id == 21) { scale(id == 20 ? -0.1 : 0.1); commitDraft(); return; }
        else if (id == 22) {
            int anchor = (placement().horizontal.ordinal() * 2 + placement().vertical.ordinal() + 1) % 4;
            draft = placement().anchored(HudPlacement.Horizontal.values()[anchor / 2], HudPlacement.Vertical.values()[anchor % 2],
                width, height, HudRenderer.textWidth(client, label(selected)), 10);
            commitDraft(); return;
        } else if (id >= 40 && id <= 43) {
            nudge(id == 40 ? -1 : id == 41 ? 1 : 0, id == 42 ? -1 : id == 43 ? 1 : 0);
            commitDraft(); return;
        } else if (id == 44) saved = settings().reset(selected);
        else if (id == 45) saved = settings().resetLayout();
        else if (id == 46) saved = settings().toggle(selected);
        else if (id == 47) { saved = settings().importProfile(); name.setText(profile().name); }
        else if (id == 48) saved = settings().exportProfile();
        status = saved ? (id == 47 || id == 48 ? settings().getNotice() : "HUD profile saved") : settings().getError();
    }
    private boolean cancelConfirmation() {
        if (!confirmation.cancel()) return false;
        status = "Action cancelled";
        return true;
    }
    private void selectModule(int delta) {
        HudModule[] modules = HudModule.values();
        selected = modules[HudEditorLayout.nextModuleIndex(selected.ordinal(), delta, modules.length)];
        status = "Module " + (selected.ordinal() + 1) + "/" + modules.length + ": " + selected.getTitle();
    }
    private static final class Action {
        final int id, x, y, width;
        final String label, hint;
        final boolean enabled;
        Action(int id, String label, int x, int y, int width, boolean enabled, String hint) {
            this.id = id; this.label = label; this.x = x; this.y = y; this.width = width; this.enabled = enabled; this.hint = hint;
        }
        boolean contains(int px, int py) { return px >= x && px < x + width && py >= y && py < y + 20; }
    }
}
