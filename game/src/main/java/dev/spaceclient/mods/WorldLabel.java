package dev.spaceclient.mods;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.hud.HudIconState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.entity.EntityRenderDispatcher;

public final class WorldLabel extends DrawableHelper {
    private WorldLabel(){}
    public static void draw(String text,double x,double y,double z,int rgb,boolean background){
        MinecraftClient c=MinecraftClient.getInstance();EntityRenderDispatcher d=c.getEntityRenderManager();
        try(HudIconState state=new HudIconState()){
            GlStateManager.pushMatrix();try{GlStateManager.translate(x,y,z);GlStateManager.rotate(-d.yaw,0,1,0);GlStateManager.rotate(d.pitch,1,0,0);GlStateManager.scale(-.025f,-.025f,.025f);
                GlStateManager.disableLighting();GlStateManager.enableDepthTest();GlStateManager.depthMask(false);GlStateManager.enableBlend();GlStateManager.blendFunc(770,771);
                int w=c.textRenderer.getStringWidth(text);if(background)fill(-w/2-3,-2,w/2+3,10,0xB0101114);c.textRenderer.drawWithShadow(text,-w/2f,0,rgb);
            }finally{GlStateManager.popMatrix();}
        }
    }
}
