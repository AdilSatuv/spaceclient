package dev.spaceclient.mixin;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.freelook.FreeLookRuntime;
import dev.spaceclient.mods.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replace only the vanilla crosshair draw, after vanilla's spectator/debug eligibility gate. */
@Mixin(InGameHud.class)
public abstract class CrosshairMixin {
    @Redirect(method = "render(F)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V", ordinal = 0))
    private void spaceclient$crosshair(InGameHud hud, int x, int y, int u, int v, int w, int h) {
        if (SpaceClient.mods == null || !SpaceClient.mods.crosshair()) { hud.drawTexture(x,y,u,v,w,h); return; }
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.currentScreen != null || c.options.hudHidden || c.options.debugEnabled || c.options.perspective != 0 || FreeLookRuntime.active) return;
        Window window = new Window(c);
        CrosshairRenderer.draw(window.getWidth() / 2, window.getHeight() / 2, SpaceClient.mods.crosshairStyle());
    }
}
