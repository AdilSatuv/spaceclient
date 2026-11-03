package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public final class DynamicFpsRuntime {
    private static final IdleFramePolicy policy=new IdleFramePolicy();
    private static int mouseX=Integer.MIN_VALUE,mouseY;
    private static boolean enabled,active;
    private DynamicFpsRuntime(){}
    public static void activity(){policy.active(System.nanoTime());}
    public static int limit(int vanilla){
        ModSettings s=SpaceClient.mods;if(s==null)return vanilla;
        int x=Mouse.isCreated()?Mouse.getX():0,y=Mouse.isCreated()?Mouse.getY():0;boolean focus=Display.isActive();
        MinecraftClient c=MinecraftClient.getInstance();
        if(s.dynamicFps()&&focus&&c.options!=null&&c.options.allKeys!=null)
            for(KeyBinding binding:c.options.allKeys)if(binding!=null&&binding.isPressed()){activity();break;}
        if(x!=mouseX||y!=mouseY||focus!=active||s.dynamicFps()!=enabled)activity();
        mouseX=x;mouseY=y;active=focus;enabled=s.dynamicFps();
        return policy.limit(vanilla,enabled,focus,System.nanoTime(),s.backgroundFps(),s.idleFps(),s.idleSeconds());
    }
}
