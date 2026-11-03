package dev.spaceclient.qa.mixin;
import dev.spaceclient.qa.CosmeticsCheck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class QaMixin {
    @Inject(method="render(FJ)V",at=@At("HEAD"))
    private void settings$clear(float delta,long time,CallbackInfo ci) {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
    }
    @Inject(method="render(FJ)V",at=@At("RETURN"))
    private void settings$frame(float delta,long time,CallbackInfo ci) { CosmeticsCheck.frame(MinecraftClient.getInstance()); }
}
