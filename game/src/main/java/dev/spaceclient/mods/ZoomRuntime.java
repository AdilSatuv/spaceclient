package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.freelook.FreeLookRuntime;
import net.minecraft.client.MinecraftClient;

public final class ZoomRuntime {
    public static final ZoomState state = new ZoomState();
    private ZoomRuntime() { }
    public static boolean eligible(MinecraftClient c) {
        return c != null && c.options != null && c.player != null && c.world != null && c.getCameraEntity() == c.player
            && c.focused && c.currentScreen == null && !FreeLookRuntime.active && !FreeLookRuntime.eligible(c)
            && SpaceClient.mods != null && SpaceClient.mods.zoom();
    }
    public static void update(MinecraftClient c) {
        boolean eligible = eligible(c);
        state.update(System.nanoTime(), eligible, eligible && ModuleKeys.held(c, SpaceClient.mods.zoomKey()),
            SpaceClient.mods == null ? 4 : SpaceClient.mods.zoomFactor(), SpaceClient.mods == null || SpaceClient.mods.zoomSmooth());
    }
    public static float sensitivity() { return (float) (1 / state.divisor()); }
}
