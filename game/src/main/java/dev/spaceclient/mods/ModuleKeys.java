package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class ModuleKeys {
    private static final GameplayModule[] HOLD_MODULES={GameplayModule.FREE_LOOK,GameplayModule.ZOOM,GameplayModule.SNAPLOOK};
    private ModuleKeys() { }
    public static boolean toggleConflicts(MinecraftClient c, GameplayModule module, int key) {
        if (conflicts(c, key)) return true;
        if (usedByHold(SpaceClient.mods,null,key)) return true;
        return usedByToggle(SpaceClient.mods, module, key);
    }
    public static boolean usedByToggle(ModSettings settings, GameplayModule except, int key) {
        if (settings == null || key == 0) return false;
        for (GameplayModule m : GameplayModule.values()) if (m != except && settings.toggleKey(m) == key) return true;
        return false;
    }
    public static boolean usedByHold(ModSettings settings,GameplayModule except,int key) {
        if(settings==null||key==0)return false;
        for(GameplayModule m:HOLD_MODULES)if(m!=except&&settings.holdKey(m)==key)return true;
        return false;
    }
    public static boolean conflicts(MinecraftClient c, int key) {
        if (key == 0 || key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RSHIFT
                || (key >= Keyboard.KEY_F1 && key <= Keyboard.KEY_F10) || key == Keyboard.KEY_F11 || key == Keyboard.KEY_F12) return true;
        if (c == null || c.options == null || c.options.allKeys == null) return true;
        for (KeyBinding binding : c.options.allKeys) if (binding != null && binding.getCode() == key) return true;
        int uses=0;
        if(SpaceClient.mods!=null)for(GameplayModule m:HOLD_MODULES)if(SpaceClient.mods.holdKey(m)==key)uses++;
        return uses>1;
    }
    public static boolean held(MinecraftClient c, int key) {
        return key > 0 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isCreated() && !conflicts(c, key)
            && !usedByToggle(SpaceClient.mods, null, key) && Keyboard.isKeyDown(key);
    }
    public static String label(int key) {
        if (key == 0) return "Unbound";
        String name = Keyboard.getKeyName(key);
        return name == null ? "Key " + key : name;
    }
}
