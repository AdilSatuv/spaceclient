package dev.spaceclient.mixin;

import dev.spaceclient.mods.ChatTimestampRuntime;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public abstract class ChatTimestampMixin {
    // reset() rewraps the private four-argument overload, so old times never change.
    @ModifyVariable(method="addMessage(Lnet/minecraft/text/Text;I)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Text spaceclient$timestamp(Text message) { return ChatTimestampRuntime.decorate(message); }
}
