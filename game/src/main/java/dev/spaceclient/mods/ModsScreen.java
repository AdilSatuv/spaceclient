package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.freelook.FreeLookRuntime;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Controls that affect play are kept separate from the HUD layout editor. */
public final class ModsScreen extends Screen {
    private final Screen parent;
    private int focus;
    private int left, top, panelWidth;
    public ModsScreen(Screen parent) { this.parent = parent; }
    @Override public void init() {
        panelWidth = Math.min(400, width - 24);
        left = (width - panelWidth) / 2;
        top = Math.max(6, (height - 178) / 2);
        SpaceClient.clicks.clear();
    }
    @Override public boolean shouldPauseGame() { return true; }
    @Override public void removed() { SpaceClient.clicks.clear(); }
    @Override public void render(int mouseX, int mouseY, float delta) {
        fillGradient(0, 0, width, height, 0xEC03060D, 0xF5090D16);
        fill(left, top, left + panelWidth, top + 178, 0xFF10141D);
        fill(left, top, left + panelWidth, top + 2, 0xFFF2B56B);
        textRenderer.drawWithShadow(textRenderer.trimToWidth("SPACE CLIENT / MODS", panelWidth - 24), left + 12, top + 12, 0xF4EDE3);
        textRenderer.drawWithShadow(textRenderer.trimToWidth("Optional controls - changes save automatically", panelWidth - 24), left + 12, top + 27, 0xB6B7C4);
        card(0, "AutoSprint", "Sprint while moving forward", SpaceClient.mods.autoSprint());
        card(1, "FreeLook", FreeLookRuntime.keyConflicts(client) ? "V unavailable: already bound" : "Hold V to look around", SpaceClient.mods.freeLook());
        String error = SpaceClient.mods.error();
        textRenderer.drawWithShadow(textRenderer.trimToWidth(error.isEmpty() ? "Tab: select  |  Space: toggle  |  Esc: back" : error,
            panelWidth - 24), left + 12, top + 145, error.isEmpty() ? 0xB6B7C4 : 0xF2B56B);
        textRenderer.drawWithShadow("Done", left + 12, top + 163, focus == 2 ? 0xF2B56B : 0xF4EDE3);
    }
    private void card(int index, String title, String description, boolean enabled) {
        int y = top + 44 + index * 47;
        fill(left + 12, y, left + panelWidth - 12, y + 39, focus == index ? 0xFF343039 : 0xFF191E28);
        textRenderer.drawWithShadow(textRenderer.trimToWidth(title, panelWidth - 80), left + 20, y + 6, 0xF4EDE3);
        String state = enabled ? "ON" : "OFF";
        textRenderer.drawWithShadow(state, left + panelWidth - 20 - textRenderer.getStringWidth(state), y + 6, enabled ? 0xF2B56B : 0xB6B7C4);
        textRenderer.drawWithShadow(textRenderer.trimToWidth(description, panelWidth - 40), left + 20, y + 21, 0xB6B7C4);
    }
    private void activate() {
        if (focus == 0) SpaceClient.mods.toggleAutoSprint();
        else if (focus == 1) SpaceClient.mods.toggleFreeLook();
        else client.setScreen(parent);
    }
    @Override protected void keyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) { client.setScreen(parent); return; }
        if (key == Keyboard.KEY_TAB || key == Keyboard.KEY_DOWN || key == Keyboard.KEY_UP) {
            int direction = key == Keyboard.KEY_UP || (key == Keyboard.KEY_TAB && hasShiftDown()) ? -1 : 1;
            focus = Math.floorMod(focus + direction, 3);
        } else if ((key == Keyboard.KEY_SPACE || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)
                && !Keyboard.isRepeatEvent()) activate();
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        if (button != 0 || x < left + 12 || x >= left + panelWidth - 12) return;
        for (int i = 0; i < 2; i++) {
            int cardY = top + 44 + i * 47;
            if (y >= cardY && y < cardY + 39) { focus = i; activate(); return; }
        }
        if (y >= top + 158 && y < top + 178) { focus = 2; activate(); }
    }
}
