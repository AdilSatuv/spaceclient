package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.lwjgl.opengl.GL11;

public final class WaypointRuntime {
    private static java.lang.ref.WeakReference<Object> owner=new java.lang.ref.WeakReference<Object>(null);
    private static String cached="";
    private WaypointRuntime(){}
    public static String context(MinecraftClient c){
        if(c.world==null||c.player==null)return "";
        if(owner.get()==c.world)return cached;
        String id=c.isInSingleplayer()&&c.getServer()!=null?"local:"+c.getServer().getLevelName():c.getCurrentServerEntry()!=null?"server:"+c.getCurrentServerEntry().address.toLowerCase(Locale.ROOT):"";
        if(id.isEmpty())return "";
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));char[] hex="0123456789abcdef".toCharArray(),out=new char[64];for(int i=0;i<digest.length;i++){out[i*2]=hex[(digest[i]>>4)&15];out[i*2+1]=hex[digest[i]&15];}cached=new String(out);owner=new java.lang.ref.WeakReference<Object>(c.world);return cached;}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static List<WaypointStore.Point> current(MinecraftClient c){return SpaceClient.waypoints==null||c.player==null?Collections.<WaypointStore.Point>emptyList():SpaceClient.waypoints.current(context(c),c.player.dimension);}
    public static void draw(float delta){
        MinecraftClient c=MinecraftClient.getInstance();ModSettings s=SpaceClient.mods;
        if(s==null||!s.waypoints()||c.player==null||c.world==null||c.options.hudHidden)return;
        EntityRenderDispatcher d=c.getEntityRenderManager();
        for(WaypointStore.Point p:current(c))if(p.visible){double x=p.x+.5-d.cameraX,y=p.y+1-d.cameraY,z=p.z+.5-d.cameraZ;double distance=Math.sqrt(x*x+y*y+z*z);if(distance>8192)continue;int rgb=p.color.rgb();
            if(s.waypointBeam()){GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_DEPTH_BUFFER_BIT|GL11.GL_LINE_BIT);try{GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glLineWidth(2);GL11.glColor3f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f);GL11.glBegin(GL11.GL_LINES);GL11.glVertex3d(x,y,z);GL11.glVertex3d(x,y+4,z);GL11.glEnd();}finally{GL11.glPopAttrib();}}
            WorldLabel.draw(p.name+(s.waypointDistance()?" ["+Math.round(distance)+"m]":""),x,y+4.3,z,rgb,true);
        }
    }
}
