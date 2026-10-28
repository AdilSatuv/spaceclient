package dev.spaceclient.hud;

/** Geometry for the HUD browser, shared by drawing, hit testing and layout tests. */
final class HudModulesLayout {
    final int left, top, panelWidth, panelHeight, contentLeft, contentWidth, cardWidth, cardHeight;
    final boolean sidebar;

    HudModulesLayout(int width, int height) {
        panelWidth = Math.max(0, Math.min(600, width - 24));
        panelHeight = Math.max(0, Math.min(300, height - 12));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        sidebar = panelWidth >= 440 && panelHeight >= 250;
        contentLeft = left + (sidebar ? 82 : 12);
        contentWidth = Math.max(0, left + panelWidth - 12 - contentLeft);
        cardWidth = Math.max(0, (contentWidth - 8) / 2);
        cardHeight = Math.max(36, Math.min(72, (panelHeight - 156) / 2));
    }

    int cardX(int index) { return contentLeft + ((index % HudModulePages.PAGE_SIZE) % 2) * (cardWidth + 8); }
    int cardY(int index) { return top + 98 + ((index % HudModulePages.PAGE_SIZE) / 2) * (cardHeight + 6); }
    int footerY() { return top + panelHeight - 27; }
}
