package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.KeyPressGate;
import dev.spaceclient.hud.HudModulesScreen;
import dev.spaceclient.hud.HudEditorScreen;
import dev.spaceclient.hud.HudModuleSettingsScreen;
import dev.spaceclient.mods.GameModuleSettingsScreen;
import dev.spaceclient.mods.ModSettings;
import dev.spaceclient.mods.GameplayModule;
import dev.spaceclient.mods.ModuleKeys;
import dev.spaceclient.mods.ModuleShortcuts;
import dev.spaceclient.mods.SprintRuntime;
import dev.spaceclient.mods.ZoomRuntime;
import dev.spaceclient.mods.SnaplookRuntime;
import dev.spaceclient.mods.*;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Shadow;
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
    @Unique private final KeyPressGate spaceclient$sprintPress = new KeyPressGate();
    @Unique private final ModuleShortcuts spaceclient$shortcuts = new ModuleShortcuts();
    @Unique private Object spaceclient$sprintOwner;
    @Unique private int spaceclient$lastGuiScale;
    @Shadow private void onResolutionChanged(int width,int height) { }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void spaceclient$tick(CallbackInfo callback) {
        SnaplookRuntime.end();
        MinecraftClient client = MinecraftClient.getInstance();
        AutoTextRuntime.context(client);SoundRuntime.context(client);
        if(client.options!=null){int scale=new Window(client).getScaleFactor();if(spaceclient$lastGuiScale!=0&&spaceclient$lastGuiScale!=scale)onResolutionChanged(client.width,client.height);spaceclient$lastGuiScale=scale;}
        SpaceClient.session.update(System.nanoTime(), client.player != null && client.world != null, client.isPaused());
        if (client.currentScreen != null || client.player == null || !client.focused) SpaceClient.clicks.clear();
        if (spaceclient$sprintOwner != client.player || SpaceClient.mods == null || !SpaceClient.mods.autoSprint()) {
            SprintRuntime.state.reset();
            spaceclient$sprintOwner = client.player;
        }
        if (!ZoomRuntime.eligible(client)) ZoomRuntime.update(client);
    }

    @Inject(method = "handleKeyInput()V", at = @At("HEAD"))
    private void spaceclient$keyEvent(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (Keyboard.getEventKey() == Keyboard.KEY_RSHIFT) {
            if (client.currentScreen instanceof GameModuleSettingsScreen
                    && ((GameModuleSettingsScreen) client.currentScreen).capturingKey()) return;
            // tick calls handleKeyInput before Screen.handleKeyboard, which calls it again.
            // Closing in Screen.keyPressed would also reopen the menu in that second call.
            if (!spaceclient$menuPress.accept(Keyboard.getEventNanoseconds(),
                    Keyboard.getEventKeyState(), Keyboard.isRepeatEvent()) || client.player == null) return;
            if (client.currentScreen instanceof HudModulesScreen || client.currentScreen instanceof HudEditorScreen
                || client.currentScreen instanceof HudModuleSettingsScreen || client.currentScreen instanceof GameModuleSettingsScreen) client.setScreen(null);
            else if (client.currentScreen == null && client.focused) client.setScreen(new HudModulesScreen());
            return;
        }
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent() || client.currentScreen != null
                || client.player == null || !client.focused) return;
        if (Keyboard.getEventKey() == Keyboard.KEY_F8) SpaceClient.toggleHud();
        if (SpaceClient.mods != null) {
            GameplayModule changed = spaceclient$shortcuts.onKey(SpaceClient.mods, Keyboard.getEventKey(), Keyboard.getEventNanoseconds(),
                true, false, true, m -> !ModuleKeys.toggleConflicts(client, m, Keyboard.getEventKey()));
            if (changed != null) {
                if (changed == GameplayModule.TOGGLE_SPRINT && !SpaceClient.mods.autoSprint()) {
                    SprintRuntime.state.reset(); client.player.setSprinting(false);
                }
                return;
            }
        }
        if (AutoTextRuntime.dispatch(client,Keyboard.getEventKey(),Keyboard.getEventNanoseconds(),true))return;
        if (SpaceClient.mods != null && SpaceClient.mods.autoSprint()
                && SpaceClient.mods.sprintMode() == ModSettings.SprintMode.TOGGLE
                && Keyboard.getEventKey() == client.options.sprintKey.getCode()
                && spaceclient$sprintPress.accept(Keyboard.getEventNanoseconds(), true, false)) {
            SprintRuntime.state.press();
            if (!SprintRuntime.state.requested(true, ModSettings.SprintMode.TOGGLE)) client.player.setSprinting(false);
        }
    }

    // Observe the event already consumed by vanilla. Never call Mouse.next() here.
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", ordinal = 0))
    private void spaceclient$mouseEvent(CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean gameplay = client.currentScreen == null && client.player != null && client.focused;
        SpaceClient.clicks.onMouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState(), gameplay);
        AnimationsRuntime.mouse(client,Mouse.getEventButton(),Mouse.getEventButtonState());
    }
}
