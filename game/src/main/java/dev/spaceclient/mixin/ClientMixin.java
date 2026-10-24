package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.HudModulesScreen;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class ClientMixin {
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void spaceclient$tick(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.player == null || !client.focused) SpaceClient.clicks.clear();
    }

    @Inject(method = "handleKeyInput()V", at = @At("HEAD"))
    private void spaceclient$keyEvent(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent() || client.currentScreen != null
                || client.player == null || !client.focused) return;
        if (Keyboard.getEventKey() == Keyboard.KEY_F8) SpaceClient.toggleHud();
        if (Keyboard.getEventKey() == Keyboard.KEY_RSHIFT) client.setScreen(new HudModulesScreen());
    }

    // Observe the event already consumed by vanilla. Never call Mouse.next() here.
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", ordinal = 0))
    private void spaceclient$mouseEvent(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean gameplay = client.currentScreen == null && client.player != null && client.focused;
        SpaceClient.clicks.onMouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState(), gameplay);
    }
}
