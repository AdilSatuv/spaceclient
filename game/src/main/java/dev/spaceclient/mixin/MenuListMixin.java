package dev.spaceclient.mixin;

import dev.spaceclient.menu.MenuTheme;
import dev.spaceclient.menu.MenuBackdrop;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only the list backing texture changes; rows, clipping, scrollbars and inputs stay native. */
@Mixin(ListWidget.class)
public abstract class MenuListMixin {
    @Redirect(method = {"render", "renderHoleBackground"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/texture/TextureManager;bindTexture(Lnet/minecraft/util/Identifier;)V"))
    private void spaceclient$list(TextureManager textures, Identifier original) {
        MinecraftClient client = MinecraftClient.getInstance();
        textures.bindTexture(MenuTheme.applies(client.currentScreen) ? MenuBackdrop.listTexture(client) : original);
    }
}
