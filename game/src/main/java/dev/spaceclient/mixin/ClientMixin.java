package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.KeyPressGate;
import dev.spaceclient.hud.HudModulesScreen;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class ClientMixin {
    @Unique private final KeyPressGate spaceclient$menuPress = new KeyPressGate();

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void spaceclient$tick(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.player == null || !client.focused) SpaceClient.clicks.clear();
    }

    @Inject(method = "handleKeyInput()V", at = @At("HEAD"))
    private void spaceclient$keyEvent(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (Keyboard.getEventKey() == Keyboard.KEY_RSHIFT) {
            // tick calls handleKeyInput before Screen.handleKeyboard, which calls it again.
            // Closing in Screen.keyPressed would also reopen the menu in that second call.
            if (!spaceclient$menuPress.accept(Keyboard.getEventNanoseconds(),
                    Keyboard.getEventKeyState(), Keyboard.isRepeatEvent()) || client.player == null) return;
            if (client.currentScreen instanceof HudModulesScreen) client.setScreen(null);
            else if (client.currentScreen == null && client.focused) client.setScreen(new HudModulesScreen());
            return;
        }
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent() || client.currentScreen != null
                || client.player == null || !client.focused) return;
        if (Keyboard.getEventKey() == Keyboard.KEY_F8) SpaceClient.toggleHud();
    }

    // Observe the event already consumed by vanilla. Never call Mouse.next() here.
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", ordinal = 0))
    private void spaceclient$mouseEvent(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean gameplay = client.currentScreen == null && client.player != null && client.focused;
        SpaceClient.clicks.onMouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState(), gameplay);
    }
}
