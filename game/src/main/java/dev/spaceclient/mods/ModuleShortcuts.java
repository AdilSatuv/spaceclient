package dev.spaceclient.mods;

import dev.spaceclient.KeyPressGate;
import java.util.function.Predicate;

/** Handles one observed keyboard event. Never polls or consumes the native event queue. */
public final class ModuleShortcuts {
    private final KeyPressGate press = new KeyPressGate();
    public GameplayModule onKey(ModSettings settings, int key, long eventTime, boolean down,
                                boolean repeated, boolean gameplay, Predicate<GameplayModule> available) {
        if (!gameplay || !down || repeated || key == 0) return null;
        for (GameplayModule m : GameplayModule.values()) {
            if (settings.toggleKey(m) == key && available.test(m) && press.accept(eventTime, down, repeated))
                return m.toggle(settings) ? m : null;
        }
        return null;
    }
}
