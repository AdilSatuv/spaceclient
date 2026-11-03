package dev.spaceclient.qa.mixin;
import dev.spaceclient.qa.InputNetworkCheck;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.minecraft.network.NetworkThreadUtils$1",priority=500)
public abstract class QueuedObserve {
 @Shadow @Final private PacketListener field_11581;
 @Inject(method="run()V",at=@At("HEAD"))
 private void qa$closed(CallbackInfo ci){if(field_11581 instanceof ClientPlayNetworkHandler&&!((ClientPlayNetworkHandler)field_11581).getClientConnection().isOpen())InputNetworkCheck.closedPlayPackets++;}
}
