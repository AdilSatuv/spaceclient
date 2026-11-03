package dev.spaceclient.mixin;
import dev.spaceclient.menu.LoadingTheme;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.resource.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DownloadingTerrainScreen.class)
public abstract class TerrainScreenMixin extends Screen {
    @Inject(method="render(IIF)V",at=@At("HEAD"),cancellable=true)
    private void space$terrain(int x,int y,float delta,CallbackInfo ci){LoadingTheme.render(client,width,height,I18n.translate("multiplayer.downloadingTerrain"),"",-1);ci.cancel();}
}
