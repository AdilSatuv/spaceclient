package dev.spaceclient.menu;

import net.minecraft.client.gui.widget.ButtonWidget;

public interface MenuFocus {
    boolean spaceclient$isFocused(ButtonWidget button);
    boolean spaceclient$navigate(char character, int key);
}
