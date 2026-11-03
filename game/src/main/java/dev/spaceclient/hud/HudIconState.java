package dev.spaceclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/** Restore native icon rendering through GlStateManager so its cache agrees with OpenGL. */
public final class HudIconState implements AutoCloseable {
    private final int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final boolean lighting=on(GL11.GL_LIGHTING),light0=on(GL11.GL_LIGHT0),light1=on(GL11.GL_LIGHT1),material=on(GL11.GL_COLOR_MATERIAL);
    private final boolean depth=on(GL11.GL_DEPTH_TEST),blend=on(GL11.GL_BLEND),alpha=on(GL11.GL_ALPHA_TEST),cull=on(GL11.GL_CULL_FACE),rescale=on(GL12.GL_RESCALE_NORMAL),normalize=on(GL11.GL_NORMALIZE);
    private final boolean depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),alphaFunc=GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),cullFace=GL11.glGetInteger(GL11.GL_CULL_FACE_MODE),shade=GL11.glGetInteger(GL11.GL_SHADE_MODEL);
    private final int materialFace=GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE),materialMode=GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER);
    private final int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final float alphaRef=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
    private final FloatBuffer color=BufferUtils.createFloatBuffer(16);
    private final int texture;
    private final boolean textured;
    public HudIconState() {
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);textured=on(GL11.GL_TEXTURE_2D);
        GL11.glPushAttrib(GL11.GL_LIGHTING_BIT|GL11.GL_CURRENT_BIT);
    }
    private static boolean on(int cap){return GL11.glIsEnabled(cap);}
    @Override public void close() {
        GL11.glPopAttrib();
        if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();
        if(light0)GlStateManager.enableLight(0);else GlStateManager.disableLight(0);
        if(light1)GlStateManager.enableLight(1);else GlStateManager.disableLight(1);
        if(material)GlStateManager.enableColorMaterial();else GlStateManager.disableColorMaterial();
        GlStateManager.colorMaterial(materialFace,materialMode);GlStateManager.shadeModel(shade);
        if(depth)GlStateManager.enableDepthTest();else GlStateManager.disableDepthTest();GlStateManager.depthMask(depthMask);GlStateManager.depthFunc(depthFunc);
        if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();GlStateManager.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);
        // Vanilla's blendFunc changes both GL pairs but only caches the RGB pair.
        // Reapply the recorded values even when the cache skipped the alpha restore.
        GL14.glBlendFuncSeparate(src,dst,srcAlpha,dstAlpha);
        if(alpha)GlStateManager.enableAlphaTest();else GlStateManager.disableAlphaTest();GlStateManager.alphaFunc(alphaFunc,alphaRef);
        if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();GlStateManager.cullFace(cullFace);
        if(rescale)GlStateManager.enableRescaleNormal();else GlStateManager.disableRescaleNormal();
        if(normalize)GlStateManager.enableNormalize();else GlStateManager.disableNormalize();
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);GlStateManager.bindTexture(texture);
        if(textured)GlStateManager.enableTexture();else GlStateManager.disableTexture();GlStateManager.activeTexture(active);
        GlStateManager.color(color.get(0),color.get(1),color.get(2),color.get(3));
    }
}
