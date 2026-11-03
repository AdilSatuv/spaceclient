package dev.spaceclient.mixin;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.ScoreboardRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class ScoreboardMixin {
 @Inject(method="renderScoreboardObjective(Lnet/minecraft/scoreboard/ScoreboardObjective;Lnet/minecraft/client/util/Window;)V",at=@At("HEAD"),cancellable=true)
 private void spaceclient$sidebar(ScoreboardObjective o,Window w,CallbackInfo ci){if(SpaceClient.mods!=null&&SpaceClient.mods.scoreboard()){ScoreboardRenderer.draw(o,w);ci.cancel();}}
}
