package dev.spaceclient.menu;

/** Pure layout in Minecraft scaled coordinates. Small windows use a compact stack. */
public final class MenuLayout {
    public final int left, top, width, step, panelTop, panelBottom, buttonHeight;
    public MenuLayout(int screenWidth, int screenHeight, int count) {
        width = Math.min(260, Math.max(1, screenWidth - 32));
        left = screenWidth >= 560 ? Math.max(32, screenWidth * 3 / 40) : (screenWidth - width) / 2;
        buttonHeight = screenHeight >= 300 ? 28 : 20;
        step = buttonHeight + (screenHeight >= 300 ? 6 : 2);
        int block = Math.max(1, count) * step - (step - buttonHeight);
        top = Math.max(48, (screenHeight - block + 36) / 2);
        panelTop = Math.max(4, top - 58);
        panelBottom = Math.min(screenHeight - 16, top + block + 12);
    }
}
