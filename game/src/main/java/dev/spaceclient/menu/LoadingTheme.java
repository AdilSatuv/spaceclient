package dev.spaceclient.menu;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.BuildIdentity;
import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;

/** Paints native stages; no wall-clock percentages or invented completion estimates. */
public final class LoadingTheme extends DrawableHelper {
    private LoadingTheme(){ }
    public static int knownProgress(int nativeProgress){return nativeProgress>=0&&nativeProgress<=100?nativeProgress:-1;}
    private static void center(MinecraftClient c,String text,int x,int y,int color){SpaceUiFont.draw(c,text,x-SpaceUiFont.width(text)/2f,y,color);}
    public static void header(MinecraftClient c,int w,int h){
        center(c,"S P A C E",w/2,20,MenuTheme.GOLD);
        fill(w/2-44,39,w/2+44,40,0xFF6F6250);
    }
    public static void render(MinecraftClient c,int w,int h,String title,String task,int nativeProgress){
        MenuBackdrop.render(c,w,h);fill(0,0,w,h,0xB0101114);header(c,w,h);
        int panel=Math.min(400,w-32),x=(w-panel)/2,y=Math.max(54,h/2-40);
        MenuTheme.panel(x-8,y-12,panel+16,104);
        center(c,SpaceUiFont.trimToWidth(title==null?"":title,panel-16),w/2,y,MenuTheme.TEXT);
        center(c,SpaceUiFont.trimToWidth(task==null?"":task,panel-16),w/2,y+24,MenuTheme.MUTED);
        int progress=knownProgress(nativeProgress),barW=Math.min(240,panel-24),barX=(w-barW)/2;
        UiShapes.rounded(barX,y+48,barW,4,2,0xFF34363B);
        if(progress>=0){if(progress>0)UiShapes.rounded(barX,y+48,Math.max(1,barW*progress/100),4,2,0xFFD7AF6A);center(c,Integer.toString(progress)+"%",w/2,y+60,MenuTheme.GOLD);}
        else center(c,MenuStrings.get(c,"loading.wait"),w/2,y+60,MenuTheme.MUTED);
        credits(c,w,h);
    }
    public static void credits(MinecraftClient c,int w,int h){
        SpaceUiFont.draw(c,BuildIdentity.label(),8,h-27,MenuTheme.MUTED);
        SpaceUiFont.draw(c,SpaceUiFont.trimToWidth("Minecraft © Mojang AB. Do not distribute!",w-16),8,h-13,MenuTheme.MUTED);
    }
    public static void early(MinecraftClient c,TextureManager textures,Identifier mojang){
        Window window=new Window(c);int w=window.getWidth(),h=window.getHeight();
        MenuTheme.background(w,h,false);
        for(int i=0;i<19;i++){int x=(i*97+23)%Math.max(1,w),y=(i*43+11)%Math.max(1,h);fill(x,y,x+1,y+1,0xFF71695C);}
        GlStateManager.pushMatrix();try{GlStateManager.scale(2,2,1);center(c,"S P A C E",w/4,Math.max(12,h/4-26),MenuTheme.GOLD);}finally{GlStateManager.popMatrix();}
        center(c,"Preparing Minecraft resources",w/2,h/2,MenuTheme.TEXT);
        fill(w/2-56,h/2+22,w/2+56,h/2+23,0xFF6F6250);
        // Retain Mojang's actual loaded logo and explicit credit in the early render.
        if(mojang!=null){textures.bindTexture(mojang);GlStateManager.color(1,1,1,1);drawTexture(w-66,h-82,0,0,56,56,56,56);}
        credits(c,w,h);
    }
}
