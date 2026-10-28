package dev.spaceclient.hud;

/** Geometry for the HUD browser, shared by drawing, hit testing and layout tests. */
final class HudModulesLayout {
    final int left, top, panelWidth, panelHeight, contentLeft, contentWidth, cardWidth, cardHeight;
    final boolean sidebar;
    final int pageSize, searchWidth, profileLeft, profileTop, profileWidth;

    HudModulesLayout(int width, int height) {
        panelWidth = Math.max(0, Math.min(600, width - 24));
        panelHeight = Math.max(0, Math.min(350, height - 12));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        sidebar = panelWidth >= 390 && panelHeight >= 220;
        contentLeft = left + (sidebar ? 82 : 12);
        contentWidth = Math.max(0, left + panelWidth - 12 - contentLeft);
        cardWidth = Math.max(0, (contentWidth - 8) / 2);
        profileWidth = sidebar ? 54 : 92;
        searchWidth = Math.max(0, sidebar ? contentWidth : contentWidth - profileWidth - 8);
        profileLeft = sidebar ? left + 7 : contentLeft + searchWidth + 8;
        profileTop = sidebar ? top + panelHeight - 79 : top + 44;
        pageSize = panelHeight >= 220 ? 6 : 4;
        int rows = pageSize / 2;
        cardHeight = Math.max(36, Math.min(72, (panelHeight - 74 - 27 - 4 - (rows - 1) * 6) / rows));
    }

    int cardX(int index) { return contentLeft + ((index % pageSize) % 2) * (cardWidth + 8); }
    int cardY(int index) { return top + 74 + ((index % pageSize) / 2) * (cardHeight + 6); }
    int footerY() { return top + panelHeight - 27; }
}
