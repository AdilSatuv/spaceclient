package dev.spaceclient.qa.mixin;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.LanServerQueryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Test-only: render the native empty server list without opening a multicast socket. */
@Mixin(MultiplayerScreen.class)
public abstract class NoLanProbe {
    @Redirect(method="init",at=@At(value="NEW",target="net/minecraft/client/network/LanServerQueryManager$LanServerDetector"))
    private LanServerQueryManager.LanServerDetector qa$noSocket(LanServerQueryManager.LanServerEntryList entries){return null;}
    @Redirect(method="init",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/LanServerQueryManager$LanServerDetector;start()V"))
    private void qa$noStart(LanServerQueryManager.LanServerDetector ignored){}
}
