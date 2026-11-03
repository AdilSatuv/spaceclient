package dev.spaceclient.cosmetics;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.lwjgl.opengl.GL11;

/** Code-native geometry attached to the local player's posed body/head. No network or frame I/O. */
public final class CosmeticFeature implements FeatureRenderer<AbstractClientPlayerEntity> {
    private final PlayerEntityRenderer renderer;
    public CosmeticFeature(PlayerEntityRenderer renderer) { this.renderer=renderer; }
    @Override public boolean combineTextures() { return false; }
    @Override public void render(AbstractClientPlayerEntity player,float limb,float amount,float delta,float age,float yaw,float pitch,float modelScale) {
        if(!CosmeticsRuntime.localPlayer(player)||player.isInvisible()||player.isSpectator())return;
        CosmeticsSettings.State state=CosmeticsRuntime.settings().view();
        if(!state.enabled||(state.wings==CosmeticsSettings.Wings.NONE&&state.halo==CosmeticsSettings.Halo.NONE))return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();
        try {
            // Raw enable changes are restored together, without changing GlStateManager's caches.
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);GL11.glLineWidth(1.5f);
            float time=state.animated?player.ticksAlive+delta:0;
            if(state.wings!=CosmeticsSettings.Wings.NONE) {
                GlStateManager.pushMatrix();
                try {
                    renderer.getModel().body.preRender(modelScale);
                    GlStateManager.translate(0,0.12,0.18);
                    double size=state.scale/100.0;GlStateManager.scale(size,size,size);
                    for(int side=-1;side<=1;side+=2) {
                        GlStateManager.pushMatrix();
                        try {
                            GlStateManager.scale(side,1,1);
                            GlStateManager.rotate((float)(15+Math.sin(time*.065)*7),0,1,0);
                            wing(state);
                        } finally { GlStateManager.popMatrix(); }
                    }
                } finally { GlStateManager.popMatrix(); }
            }
            if(state.halo!=CosmeticsSettings.Halo.NONE) {
                GlStateManager.pushMatrix();
                try {
                    renderer.getModel().head.preRender(modelScale);
                    GlStateManager.translate(0,-.67+Math.sin(time*.04)*.018,0);
                    halo(state,time);
                } finally { GlStateManager.popMatrix(); }
            }
        } finally { GlStateManager.popMatrix();GL11.glPopAttrib(); }
    }
    private static void color(int rgb,float alpha) { GL11.glColor4f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,alpha); }
    /** The same wing geometry in the settings preview; no GL resources or world are needed. */
    public static void preview(CosmeticsSettings.State state,int x,int y) {
        if(!state.enabled||state.wings==CosmeticsSettings.Wings.NONE)return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GlStateManager.pushMatrix();
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(1);GlStateManager.translate(x,y,0);
            float size=30*state.scale/100f;GlStateManager.scale(size,size,1);
            for(int side=-1;side<=1;side+=2) {
                GlStateManager.pushMatrix();try { GlStateManager.scale(side,1,1);wing(state); }
                finally { GlStateManager.popMatrix(); }
            }
        } finally { GlStateManager.popMatrix();GL11.glPopAttrib(); }
    }
    private static void polygon(int rgb,float alpha,double... xy) {
        color(rgb,alpha);GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        for(int i=0;i<xy.length;i+=2)GL11.glVertex3d(xy[i],xy[i+1],0);
        GL11.glEnd();
        color(rgb,1);GL11.glBegin(GL11.GL_LINE_LOOP);
        for(int i=0;i<xy.length;i+=2)GL11.glVertex3d(xy[i],xy[i+1],-.002);
        GL11.glEnd();
    }
    private static void wing(CosmeticsSettings.State state) {
        int a=state.tint.primary,b=state.tint.secondary;
        switch(state.wings) {
            case AURORA:
                polygon(b,.60f,.08,0,.95,-.60,1.20,-.33,.57,.68,.12,.38);
                for(int i=0;i<6;i++) {
                    double t=i/5.0;
                    polygon(i%2==0?a:b,.88f,.14,.02+t*.05,1.22-t*.12,-.43+t*.21,.62-t*.28,.73-t*.035,.31,.34);
                }
                break;
            case EMBER:
                polygon(b,.76f,.10,.03,.72,-.55,1.24,-.35,.88,-.10,.96,.22,.61,.14,.64,.56,.35,.35,.17,.62);
                polygon(a,.92f,.10,.03,.72,-.55,.61,-.27,.21,.35);
                color(a,1);GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex3d(.1,.03,-.003);GL11.glVertex3d(1.24,-.35,-.003);
                GL11.glVertex3d(.1,.03,-.003);GL11.glVertex3d(.96,.22,-.003);
                GL11.glVertex3d(.1,.03,-.003);GL11.glVertex3d(.64,.56,-.003);GL11.glEnd();
                break;
            case CRYSTAL:
                polygon(a,.88f,.10,.03,.90,-.62,1.20,-.39,.28,.28);
                polygon(b,.83f,.12,.13,1.15,-.12,1.06,.17,.28,.40);
                polygon(a,.88f,.14,.24,.83,.34,.57,.72,.23,.48);
                break;
            default:break;
        }
    }
    private static void halo(CosmeticsSettings.State state,float time) {
        color(state.tint.primary,1);GL11.glBegin(GL11.GL_LINE_LOOP);
        for(int i=0;i<48;i++) { double theta=i*Math.PI/24;GL11.glVertex3d(Math.cos(theta)*.34,0,Math.sin(theta)*.34); }
        GL11.glEnd();
        if(state.halo==CosmeticsSettings.Halo.ORBIT) {
            color(state.tint.secondary,1);GL11.glBegin(GL11.GL_LINE_LOOP);
            for(int i=0;i<48;i++) { double theta=i*Math.PI/24;GL11.glVertex3d(Math.cos(theta)*.43,Math.sin(theta)*.11,Math.sin(theta)*.29); }
            GL11.glEnd();
            double t=time*.025;GL11.glPointSize(5);GL11.glBegin(GL11.GL_POINTS);
            GL11.glVertex3d(Math.cos(t)*.43,Math.sin(t)*.11,Math.sin(t)*.29);GL11.glEnd();
        }
    }
}
