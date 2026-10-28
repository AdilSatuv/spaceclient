package dev.spaceclient.menu;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Traversal is explicit and never activates hidden or disabled actions. */
public final class MenuNavigation {
    private MenuNavigation() { }
    public static void sortTitle(List<ButtonWidget> buttons) {
        Collections.sort(buttons, new Comparator<ButtonWidget>() {
            @Override public int compare(ButtonWidget a, ButtonWidget b) {
                return Integer.compare(titleOrder(a.id), titleOrder(b.id));
            }
        });
    }
    private static int titleOrder(int id) {
        switch (id) {
            case 1: case 11: return 0; // Play / play demo
            case 2: case 12: return 1; // Multiplayer / reset demo
            case 14: return 2; // Realms
            case 0: return 3; // Options
            case 5: return 4; // Language
            case 4: return 5; // Quit
            default: return 6;
        }
    }
    public static ButtonWidget next(List<ButtonWidget> buttons, ButtonWidget current, int direction) {
        List<ButtonWidget> ordered = new ArrayList<ButtonWidget>(buttons);
        Collections.sort(ordered, new Comparator<ButtonWidget>() {
            @Override public int compare(ButtonWidget a, ButtonWidget b) {
                int row = Integer.compare(a.y, b.y);
                return row != 0 ? row : Integer.compare(a.x, b.x);
            }
        });
        int index = ordered.indexOf(current);
        if (index < 0) index = direction > 0 ? -1 : 0;
        for (int i = 0; i < ordered.size(); i++) {
            index = (index + direction + ordered.size()) % ordered.size();
            ButtonWidget next = ordered.get(index);
            if (next.visible && next.active && MenuTheme.standard(next)) return next;
        }
        return null;
    }
}
