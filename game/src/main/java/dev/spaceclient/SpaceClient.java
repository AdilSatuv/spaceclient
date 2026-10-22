package dev.spaceclient;

import net.fabricmc.api.ClientModInitializer;

public final class SpaceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("Space Client 0.1.0 initialized");
    }
}
