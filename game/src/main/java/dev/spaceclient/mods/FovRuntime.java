package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
/** Camera-only math. Never changes movement attributes or saved Minecraft options. */
public final class FovRuntime {
    private static float lastProjection=Float.NaN;
    private static boolean wasActive;
    private FovRuntime() { }
    /** Called by the shared world-FOV return hook after composing Zoom. */
    public static void projection(MinecraftClient c,float value){
        if(c==null||c.world==null){lastProjection=Float.NaN;wasActive=false;return;}
        boolean enabled=active(c);
        if(c.world!=null&&c.worldRenderer!=null&&(enabled||wasActive)
                &&(enabled!=wasActive||Float.isNaN(lastProjection)||Math.abs(value-lastProjection)>.01f))
            c.worldRenderer.scheduleTerrainUpdate();
        lastProjection=value;wasActive=enabled;
    }
    public static boolean active(MinecraftClient c){return c!=null&&c.player!=null&&c.world!=null&&c.getCameraEntity()==c.player&&SpaceClient.mods!=null&&SpaceClient.mods.fovControl();}
    public static float dynamic(float vanilla,int percent){return 1+(vanilla-1)*Math.max(0,Math.min(100,percent))/100f;}
}
