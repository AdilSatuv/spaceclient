package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
/** Camera-only math. Never changes movement attributes or saved Minecraft options. */
public final class FovRuntime {
    private FovRuntime() { }
    public static boolean active(MinecraftClient c){return c!=null&&c.player!=null&&c.world!=null&&c.getCameraEntity()==c.player&&SpaceClient.mods!=null&&SpaceClient.mods.fovControl();}
    public static float dynamic(float vanilla,int percent){return 1+(vanilla-1)*Math.max(0,Math.min(100,percent))/100f;}
}
