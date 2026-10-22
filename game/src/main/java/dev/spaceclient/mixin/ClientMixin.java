package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class ClientMixin {
    @Unique private boolean spaceclient$toggleHeld;

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void spaceclient$tick(CallbackInfo callback) {
        boolean pressed = Keyboard.isCreated() && Keyboard.isKeyDown(Keyboard.KEY_F8);
        MinecraftClient client = MinecraftClient.getInstance();
        if (pressed && !spaceclient$toggleHeld && client.currentScreen == null && client.player != null) SpaceClient.toggleHud();
        spaceclient$toggleHeld = pressed;
    }
}
