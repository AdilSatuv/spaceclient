package dev.spaceclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Path;

public final class SpaceClient implements ClientModInitializer {
    public static HudConfig config = new HudConfig();
    private static Path configFile;
    @Override
    public void onInitializeClient() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("spaceclient-hud.properties");
        try { config = HudConfig.load(configFile); }
        catch (IOException exception) { System.err.println("Space Client: HUD config could not be loaded: " + exception.getMessage()); }
        System.out.println("Space Client 0.1.0 initialized");
    }

    public static void toggleHud() {
        config.enabled = !config.enabled;
        try { config.save(configFile); }
        catch (IOException exception) { System.err.println("Space Client: HUD config could not be saved: " + exception.getMessage()); }
    }
}
