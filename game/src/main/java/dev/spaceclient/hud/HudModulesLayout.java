package dev.spaceclient.hud;

/** Geometry for the HUD browser, shared by drawing, hit testing and layout tests. */
final class HudModulesLayout {
    final int left, top, panelWidth, panelHeight, contentLeft, contentWidth, cardWidth, cardHeight;
    final boolean sidebar;
    final boolean list;
    final int pageSize, searchWidth, searchTop, cardsTop, sidebarWidth, profileLeft, profileTop, profileWidth;
    final int filterLeft, filterWidth = 51;

    HudModulesLayout(int width, int height) {
        this(width,height,false);
    }
    HudModulesLayout(int width, int height,boolean list) {
        this.list=list;
        panelWidth = Math.max(0, Math.min(600, width - 24));
        panelHeight = Math.max(0, Math.min(list?430:330, height - 12));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        sidebar = panelWidth >= 390 && panelHeight >= 220;
        sidebarWidth = sidebar ? (panelWidth >= 500 ? 108 : 68) : 0;
        contentLeft = left + (sidebar ? sidebarWidth + 18 : 12);
        contentWidth = Math.max(0, left + panelWidth - 12 - contentLeft);
        cardWidth = Math.max(0,list?contentWidth:(contentWidth - 8) / 2);
        profileWidth = 74;
        searchWidth = Math.max(0, contentWidth - 166);
        filterLeft = contentLeft + searchWidth + 7;
        profileLeft = sidebar ? contentLeft + contentWidth - 164 : contentLeft + searchWidth + 8;
        profileTop = sidebar ? top + 20 : top + 44;
        searchTop = panelHeight >= 300 ? 67 : 44;
        cardsTop = panelHeight >= 300 ? 106 : 74;
        pageSize = list?Math.max(1,(panelHeight-cardsTop-31)/31):panelHeight >= 220 ? 6 : 4;
        int rows = pageSize / 2;
        cardHeight = list?28:Math.max(36, Math.min(72, (panelHeight - cardsTop - 27 - 4 - (rows - 1) * 6) / rows));
    }

    int cardX(int index) { return contentLeft + (list?0:((index % pageSize) % 2) * (cardWidth + 8)); }
    int cardY(int index) { return top + cardsTop + (list?(index%pageSize)*31:((index % pageSize) / 2) * (cardHeight + 6)); }
    int sectionTop(HudSection section) {
        return section == HudSection.SETTINGS ? top + panelHeight - 57 : section==HudSection.COSMETICS?top+panelHeight-29:top + 43 + section.ordinal() * 28;
    }
    int footerY() { return top + panelHeight - 27; }
}
