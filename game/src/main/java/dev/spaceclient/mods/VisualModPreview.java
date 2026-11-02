package dev.spaceclient.mods;

import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
/** Small live vector previews using the selected cosmetic settings. */
public final class VisualModPreview {
    private VisualModPreview() { }
    public static void draw(MinecraftClient c,GameplayModule m,ModSettings s,int x,int y,int w){
        UiShapes.rounded(x,y,w,39,5,0xFF242A30);SpaceUiFont.draw(c,"Live preview",x+12,y+15,0xB3B0AA);
        int color=m==GameplayModule.BLOCK_OVERLAY?s.blockColor().rgb():m==GameplayModule.HIT_COLOR?s.damageColor().rgb():0xD7AF6A;
        if(m==GameplayModule.HIT_COLOR){UiShapes.rounded(x+w-65,y+4,12,11,3,(s.hitOpacity()*255/100<<24)|color);UiShapes.rounded(x+w-72,y+16,26,19,3,(s.hitOpacity()*255/100<<24)|color);return;}
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);
            int cx=x+w-61,cy=y+19;
            if(m==GameplayModule.FOV_CONTROL){
                double spread=Math.tan(Math.toRadians(s.cameraFov()/2.0))*12;
                GL11.glBegin(GL11.GL_LINE_STRIP);GL11.glVertex2d(cx+22,cy-spread);GL11.glVertex2d(cx,cy);GL11.glVertex2d(cx+22,cy+spread);GL11.glEnd();
                SpaceUiFont.draw(c,s.dynamicFov()+"% dynamics",x+110,y+15,0xD7AF6A);
            }else{
                if(s.blockMode().fill){GL11.glColor4f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,s.blockOpacity()/100f);GL11.glBegin(GL11.GL_QUADS);GL11.glVertex2i(cx-14,cy-10);GL11.glVertex2i(cx+8,cy-10);GL11.glVertex2i(cx+8,cy+12);GL11.glVertex2i(cx-14,cy+12);GL11.glEnd();}
                if(s.blockMode().outline){GL11.glColor4f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);GL11.glLineWidth(s.blockWidth());GL11.glBegin(GL11.GL_LINE_LOOP);GL11.glVertex2i(cx-14,cy-10);GL11.glVertex2i(cx+8,cy-10);GL11.glVertex2i(cx+8,cy+12);GL11.glVertex2i(cx-14,cy+12);GL11.glEnd();}
            }
        }finally{GL11.glPopAttrib();}
    }
}
