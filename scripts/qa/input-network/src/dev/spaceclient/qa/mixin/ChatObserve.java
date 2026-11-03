package dev.spaceclient.qa.mixin;
import dev.spaceclient.qa.InputNetworkCheck;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ChatObserve {
 @Inject(method="onChatMessage",at=@At("TAIL"))
 private void qa$chat(ChatMessageC2SPacket packet,CallbackInfo ci){InputNetworkCheck.chat(packet.getChatMessage());}
}
