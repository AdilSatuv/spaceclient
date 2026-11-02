package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

/** Uses the native selected shape and its camera transform; respects depth occlusion. */
public final class BlockOverlayRenderer {
    private BlockOverlayRenderer() { }
    public static void draw(Box b){
        ModSettings s=SpaceClient.mods;if(s==null||!s.blockOverlay()){WorldRenderer.drawBox(b);return;}
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_DEPTH_BUFFER_BIT|GL11.GL_LINE_BIT);
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            int rgb=s.blockColor().rgb();float red=((rgb>>16)&255)/255f,green=((rgb>>8)&255)/255f,blue=(rgb&255)/255f;
            if(s.blockMode().fill){
                GL11.glColor4f(red,green,blue,s.blockOpacity()/100f);
                GL11.glBegin(GL11.GL_QUADS);
                face(b.minX,b.minY,b.minZ,b.maxX,b.minY,b.minZ,b.maxX,b.maxY,b.minZ,b.minX,b.maxY,b.minZ);
                face(b.minX,b.minY,b.maxZ,b.maxX,b.minY,b.maxZ,b.maxX,b.maxY,b.maxZ,b.minX,b.maxY,b.maxZ);
                face(b.minX,b.minY,b.minZ,b.minX,b.minY,b.maxZ,b.minX,b.maxY,b.maxZ,b.minX,b.maxY,b.minZ);
                face(b.maxX,b.minY,b.minZ,b.maxX,b.minY,b.maxZ,b.maxX,b.maxY,b.maxZ,b.maxX,b.maxY,b.minZ);
                face(b.minX,b.minY,b.minZ,b.maxX,b.minY,b.minZ,b.maxX,b.minY,b.maxZ,b.minX,b.minY,b.maxZ);
                face(b.minX,b.maxY,b.minZ,b.maxX,b.maxY,b.minZ,b.maxX,b.maxY,b.maxZ,b.minX,b.maxY,b.maxZ);
                GL11.glEnd();
            }
            if(s.blockMode().outline){GL11.glColor4f(red,green,blue,.95f);GL11.glLineWidth(s.blockWidth());WorldRenderer.drawBox(b);}
        }finally{GL11.glPopAttrib();}
    }
    private static void face(double x1,double y1,double z1,double x2,double y2,double z2,double x3,double y3,double z3,double x4,double y4,double z4){
        GL11.glVertex3d(x1,y1,z1);GL11.glVertex3d(x2,y2,z2);GL11.glVertex3d(x3,y3,z3);GL11.glVertex3d(x4,y4,z4);
    }
}
