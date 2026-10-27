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
    private static final int TEXT = 0xF4EDE3, MUTED = 0xB6B7C4, ACCENT = 0xF2B56B, BORDER = 0xFF414652;
    private final Screen parent;
    private HudModule selected = HudModule.FPS;
    private HudPlacement draft;
    private boolean dragging, capturedRepeat, previousRepeat;
    private double grabX, grabY;
    private TextFieldWidget name;
    private int lastMouseX, lastMouseY;
    private String status = "";

    public HudEditorScreen(Screen parent) { this.parent = parent; }
    private HudSettings settings() { return SpaceClient.hud; }
    private HudProfile profile() { return settings().getConfig().active(); }
    private HudPlacement placement() { return draft == null ? profile().placement(selected) : draft; }
    private String label(HudModule module) { return HudRenderer.text(client, module); }
    private HudPlacement.Bounds bounds(HudModule module) {
        return HudRenderer.bounds(client, label(module), module == selected ? placement() : profile().placement(module), width, height);
    }
    private int barY() { return HudEditorLayout.toolbarY(height, bounds(selected)); }
    private boolean roomy() { return width >= 320 && height >= 240; }

    @Override public void init() {
        String pendingName = name == null ? profile().name : name.getText();
        boolean focused = name != null && name.isFocused();
        commitDraft();
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
        finishName();
        commitDraft();
        if (capturedRepeat) Keyboard.enableRepeatEvents(previousRepeat);
        SpaceClient.clicks.clear();
    }
    @Override public void render(int mouseX, int mouseY, float delta) {
        fillGradient(0, 0, width, height, 0xC403060D, 0xE0090D16);
        if (client.player == null || !roomy()) {
            drawCenteredString(textRenderer, "Resize the window to edit HUD", width / 2, height / 2 - 12, TEXT);
            drawCenteredString(textRenderer, "Esc - back", width / 2, height / 2 + 4, MUTED);
            return;
        }
        lastMouseX = mouseX; lastMouseY = mouseY;
        client.textRenderer.drawWithShadow("SPACE CLIENT", 8, 8, ACCENT);
        for (HudModule module : HudModule.values()) if (module != selected) drawModule(module);
        drawModule(selected);
        int y = barY(), w = width - 16;
        fill(8, y, width - 8, y + HudEditorLayout.HEIGHT, 0xFF10141D);
        fill(8, y, width - 8, y + 2, 0xFFF2B56B);
        String hoverHint = "";
        for (Action action : controls()) {
            boolean hovered = action.contains(mouseX, mouseY);
            boolean activeProfile = action.id == 10 + settings().getConfig().activeIndex();
            box(action.x, action.y, action.width, 20, activeProfile,
                hovered && action.enabled, action.enabled);
            drawCenteredString(textRenderer, textRenderer.trimToWidth(action.label, action.width - 6),
                action.x + action.width / 2, action.y + 6, action.enabled ? TEXT : 0x777B85);
            if (hovered) hoverHint = action.hint;
        }
        textRenderer.drawWithShadow("Name", 14, y + 36, MUTED);
        box(40, y + 30, 90, 20, name.isFocused(), false, !settings().isReadOnly());
        name.x = 42; name.y = y + 36; name.render();
        textRenderer.drawWithShadow(String.format(Locale.ROOT, "%.1fx", placement().scale), 206, y + 36, ACCENT);
        String message = settings().getError();
        if (message.isEmpty() && settings().isReadOnly()) message = settings().getNotice();
        if (message.isEmpty()) message = hoverHint.isEmpty() ? status : hoverHint;
        if (message.isEmpty()) message = "Tab module | Arrows move | +/- scale | N name | Esc back";
        if (!profile().enabled && !settings().isReadOnly() && settings().getError().isEmpty() && hoverHint.isEmpty()) message = "HUD hidden; this is a layout preview. Esc - modules";
        textRenderer.drawWithShadow(textRenderer.trimToWidth(message, w - 12), 14, y + 79,
            settings().getError().isEmpty() ? MUTED : ACCENT);
    }
    private void drawModule(HudModule module) {
        HudPlacement.Bounds b = bounds(module);
        int left = Math.max(0, (int) Math.floor(b.x) - 2), top = Math.max(0, (int) Math.floor(b.y) - 2);
        int right = Math.min(width, (int) Math.ceil(b.x + b.width) + 2), bottom = Math.min(height, (int) Math.ceil(b.y + b.height) + 2);
        int border = module == selected ? 0xFFF2B56B : BORDER;
        fill(left, top, right, top + 1, border); fill(left, bottom - 1, right, bottom, border);
        fill(left, top, left + 1, bottom, border); fill(right - 1, top, right, bottom, border);
        boolean visible = profile().enabled && module.isEnabled(settings().getConfig());
        HudRenderer.draw(client, label(module), b, visible ? (module == HudModule.KEYSTROKES ? ACCENT : TEXT) : MUTED);
    }
    private List<Action> controls() {
        List<Action> actions = new ArrayList<>();
        int y = barY(), x = 8, w = width - 16;
        int profileWidth = Math.min(96, 60 + Math.max(0, w - 304) / 2);
        boolean editable = !settings().isReadOnly();
        actions.add(new Action(10, "1: " + settings().getConfig().profile(0).name, x + 6, y + 6, profileWidth, editable, "Select HUD profile 1 (key 1)"));
        actions.add(new Action(11, "2: " + settings().getConfig().profile(1).name, x + 10 + profileWidth, y + 6, profileWidth, editable, "Select HUD profile 2 (key 2)"));
        actions.add(new Action(12, selected.getTitle(), x + 14 + profileWidth * 2, y + 6, 86, true, "Select next module (Tab / Shift+Tab)"));
        actions.add(new Action(13, "Done", x + w - 60, y + 6, 54, true, "Return to modules (Esc); changes save automatically"));
        actions.add(new Action(30, "Save", x + 124, y + 30, 44, editable, "Save profile name (N, then Enter)"));
        actions.add(new Action(20, "-", x + 172, y + 30, 22, editable, "Smaller widget (- or mouse wheel over widget)"));
        actions.add(new Action(21, "+", x + 232, y + 30, 22, editable, "Larger widget (+ or mouse wheel over widget)"));
        String anchor = (placement().horizontal == HudPlacement.Horizontal.LEFT ? "L" : "R")
            + "/" + (placement().vertical == HudPlacement.Vertical.TOP ? "T" : "B");
        actions.add(new Action(22, anchor, x + w - 48, y + 30, 42, editable, "Change edge anchors, keep current position (A)"));
        actions.add(new Action(40, "<", x + 6, y + 54, 20, editable, "Move left one GUI unit (Left arrow)"));
        actions.add(new Action(41, ">", x + 28, y + 54, 20, editable, "Move right one GUI unit (Right arrow)"));
        actions.add(new Action(42, "^", x + 50, y + 54, 20, editable, "Move up one GUI unit (Up arrow)"));
        actions.add(new Action(43, "v", x + 72, y + 54, 20, editable, "Move down one GUI unit (Down arrow)"));
        actions.add(new Action(44, "Reset", x + 96, y + 54, 36, editable, "Reset selected placement and scale (R)"));
        actions.add(new Action(45, "All", x + 136, y + 54, 30, editable, "Reset this profile layout; keep names and toggles (Home)"));
        actions.add(new Action(46, selected.isEnabled(settings().getConfig()) ? "ON" : "OFF", x + 170, y + 54, 30, editable, "Toggle selected module (Space)"));
        actions.add(new Action(47, "Import", x + 204, y + 54, 44, editable, "Reads config/spaceclient-hud-import.properties (I)"));
        actions.add(new Action(48, "Export", x + 252, y + 54, 44, editable, "Writes config/spaceclient-hud-export.properties (O)"));
        return actions;
    }
    private void box(int x, int y, int w, int h, boolean focused, boolean hovered, boolean enabled) {
        fill(x, y, x + w, y + h, focused ? 0xFFF2B56B : BORDER);
        fill(x + 1, y + 1, x + w - 1, y + h - 1, enabled && (focused || hovered) ? 0xFF272B35 : 0xFF191E28);
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        if (button != 0 || !roomy() || client.player == null) return;
        int yBar = barY();
        if (x >= 40 && x < 130 && y >= yBar + 30 && y < yBar + 50) {
            if (name.isFocused()) name.mouseClicked(x, y, button);
            else focusName();
            return;
        }
        if (!finishName()) return;
        for (Action action : controls()) if (action.contains(x, y)) {
            if (action.enabled) activate(action.id);
            return;
        }
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
            if (name.isFocused()) { name.setText(profile().name); name.setFocused(false); return; }
            commitDraft(); client.setScreen(parent); return;
        }
        if (!roomy() || client.player == null) return;
        if (name.isFocused()) {
            if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) && !Keyboard.isRepeatEvent()) finishName();
            else name.keyPressed(character, key);
            return;
        }
        if (key == Keyboard.KEY_TAB && !Keyboard.isRepeatEvent()) {
            if (commitDraft()) selected = HudModule.values()[Math.floorMod(selected.ordinal() + (hasShiftDown() ? -1 : 1), HudModule.values().length)];
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
        name.setText(profile().name); name.setFocused(true);
        name.setCursorToStart(); name.setSelectionEnd(name.getText().length());
    }
    private boolean finishName() {
        if (name == null || !name.isFocused()) return true;
        if (!name.getText().equals(profile().name) && !settings().renameProfile(name.getText())) return false;
        name.setFocused(false); status = "Profile name saved"; return true;
    }
    private void activate(int id) {
        if (!commitDraft() || !finishName()) return;
        if (id == 13) { client.setScreen(parent); return; }
        if (id == 12) { selected = HudModule.values()[(selected.ordinal() + 1) % HudModule.values().length]; return; }
        if (settings().isReadOnly()) return;
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
