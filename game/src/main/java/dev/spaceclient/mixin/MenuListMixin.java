package dev.spaceclient.mixin;

import dev.spaceclient.menu.MenuTheme;
import dev.spaceclient.menu.MenuBackdrop;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Display;
import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;

/** Only the list backing texture changes; rows, clipping, scrollbars and inputs stay native. */
@Mixin(ListWidget.class)
public abstract class MenuListMixin {
    @Shadow protected int xStart;
    @Shadow protected int xEnd;
    @Shadow protected int yStart;
    @Shadow protected int yEnd;
    @Unique private boolean spaceclient$clipped;
    @Inject(method="renderList",at=@At("HEAD"))
    private void spaceclient$clipRows(int x,int y,int mx,int my,CallbackInfo ci) {
        MinecraftClient client=MinecraftClient.getInstance();
        spaceclient$clipped=MenuTheme.applies(client.currentScreen);
        if(!spaceclient$clipped)return;
        int scale=new net.minecraft.client.util.Window(client).getScaleFactor();
        int left=Math.max(0,xStart*scale),bottom=Math.max(0,Display.getHeight()-yEnd*scale);
        int right=Math.min(Display.getWidth(),xEnd*scale),top=Math.min(Display.getHeight(),Display.getHeight()-yStart*scale);
        if(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            IntBuffer old=BufferUtils.createIntBuffer(4);GL11.glGetInteger(GL11.GL_SCISSOR_BOX,old);
            left=Math.max(left,old.get(0));bottom=Math.max(bottom,old.get(1));
            right=Math.min(right,old.get(0)+old.get(2));top=Math.min(top,old.get(1)+old.get(3));
        }
        // Vanilla opaque masks used to hide overflowing rows. A translucent mask needs real clipping.
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left,bottom,Math.max(0,right-left),Math.max(0,top-bottom));
    }
    @Inject(method="renderList",at=@At("RETURN"))
    private void spaceclient$unclipRows(int x,int y,int mx,int my,CallbackInfo ci) {
        if(spaceclient$clipped){GL11.glPopAttrib();spaceclient$clipped=false;}
    }
    @Redirect(method = {"render", "renderHoleBackground"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/texture/TextureManager;bindTexture(Lnet/minecraft/util/Identifier;)V"))
    private void spaceclient$list(TextureManager textures, Identifier original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (MenuTheme.applies(client.currentScreen)) {
            // Native lists paint both the scrolling body and the header/footer masks.
            // Blend the quiet texture so the shared scene survives those native passes.
            com.mojang.blaze3d.platform.GlStateManager.enableBlend();
            com.mojang.blaze3d.platform.GlStateManager.blendFuncSeparate(770, 771, 1, 0);
            textures.bindTexture(MenuBackdrop.listTexture(client));
        } else textures.bindTexture(original);
    }
}
