package dev.spaceclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import dev.spaceclient.hud.HudSettings;
import dev.spaceclient.mods.ModSettings;
import dev.spaceclient.hud.SessionTimer;
import dev.spaceclient.hud.ModuleBrowserSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SpaceClient implements ClientModInitializer {
    private static final Logger LOGGER = LogManager.getLogger("SpaceClient");
    public static HudSettings hud;
    public static ModSettings mods;
    public static ModuleBrowserSettings browser;
    public static dev.spaceclient.mods.WaypointStore waypoints;
    public static final ClickCounter clicks = new ClickCounter();
    public static final SessionTimer session = new SessionTimer();
    @Override
    public void onInitializeClient() {
        hud = HudSettings.open(FabricLoader.getInstance().getConfigDir().resolve("spaceclient-hud.properties"));
        mods = ModSettings.open(FabricLoader.getInstance().getConfigDir().resolve("spaceclient-mods.properties"));
        browser = ModuleBrowserSettings.open(FabricLoader.getInstance().getConfigDir().resolve("spaceclient-browser.properties"));
        waypoints = dev.spaceclient.mods.WaypointStore.open(FabricLoader.getInstance().getConfigDir().resolve("spaceclient-waypoints.properties"));
        if (!hud.getNotice().isEmpty()) LOGGER.warn(hud.getNotice());
        LOGGER.info("Space Client 0.1.0 initialized");
    }

    public static void toggleHud() {
        if (!hud.toggleHud() && !hud.getError().isEmpty()) LOGGER.warn(hud.getError());
    }
}
