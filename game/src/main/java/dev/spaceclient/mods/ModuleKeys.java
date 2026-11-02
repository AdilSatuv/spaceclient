package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class ModuleKeys {
    private ModuleKeys() { }
    public static boolean conflicts(MinecraftClient c, int key) {
        if (key == 0 || key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RSHIFT
                || (key >= Keyboard.KEY_F1 && key <= Keyboard.KEY_F10) || key == Keyboard.KEY_F11 || key == Keyboard.KEY_F12) return true;
        if (c == null || c.options == null || c.options.allKeys == null) return true;
        for (KeyBinding binding : c.options.allKeys) if (binding != null && binding.getCode() == key) return true;
        return SpaceClient.mods != null && key == SpaceClient.mods.freeLookKey() && key == SpaceClient.mods.zoomKey();
    }
    public static boolean held(MinecraftClient c, int key) {
        return key > 0 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isCreated() && !conflicts(c, key) && Keyboard.isKeyDown(key);
    }
    public static String label(int key) {
        if (key == 0) return "Unbound";
        String name = Keyboard.getKeyName(key);
        return name == null ? "Key " + key : name;
    }
}
