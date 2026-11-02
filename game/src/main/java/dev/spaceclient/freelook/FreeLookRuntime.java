package dev.spaceclient.freelook;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.ModuleKeys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;

/** Shared state accessed by renderer and tick on Minecraft's client thread. */
public final class FreeLookRuntime {
    private FreeLookRuntime() { }
    public static final FreeLookState state = new FreeLookState();
    public static boolean active;
    public static ClientPlayerEntity owner;

    /** Preserve every existing vanilla or mod key binding; never steal its input. */
    public static boolean keyConflicts(MinecraftClient c) {
        return SpaceClient.mods == null || ModuleKeys.conflicts(c, SpaceClient.mods.freeLookKey());
    }
    public static boolean eligible(MinecraftClient c) {
        return c != null && SpaceClient.mods != null && SpaceClient.mods.freeLook()
            && c.options != null && c.player != null && c.getCameraEntity() == c.player
            && c.currentScreen == null && c.focused && Keyboard.isCreated()
            && ModuleKeys.held(c, SpaceClient.mods.freeLookKey());
    }
    public static void update(MinecraftClient c, boolean held) {
        if (c == null || c.options == null) return;
        if (state.active() && owner != c.player) held = false;
        int oldView = c.options.perspective;
        float yaw = c.player == null ? 0 : c.player.yaw;
        float pitch = c.player == null ? 0 : c.player.pitch;
        c.options.perspective = state.update(held, oldView, yaw, pitch);
        owner = state.active() ? c.player : null;
        active = state.active();
        if (oldView != c.options.perspective && c.worldRenderer != null) c.worldRenderer.scheduleTerrainUpdate();
    }
}
