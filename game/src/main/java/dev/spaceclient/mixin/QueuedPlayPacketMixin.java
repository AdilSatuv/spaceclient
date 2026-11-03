package dev.spaceclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A queued play packet can outlive its connection and the world it belongs to. */
@Mixin(targets = "net.minecraft.network.NetworkThreadUtils$1")
public abstract class QueuedPlayPacketMixin {
    @Shadow @Final private PacketListener field_11581;

    @Inject(method = "run()V", at = @At("HEAD"), cancellable = true)
    private void spaceclient$discardClosedPlayConnection(CallbackInfo callback) {
        if (field_11581 instanceof ClientPlayNetworkHandler
                && !((ClientPlayNetworkHandler) field_11581).getClientConnection().isOpen()) {
            callback.cancel();
        }
    }
}
