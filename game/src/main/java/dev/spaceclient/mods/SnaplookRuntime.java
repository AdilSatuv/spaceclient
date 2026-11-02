package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.freelook.FreeLookRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public final class SnaplookRuntime {
    private static final SnaplookState state=new SnaplookState();
    private static GameOptions owner;
    private static boolean renderedSnap;
    private static int renderedView;
    private SnaplookRuntime(){}
    public static boolean eligible(MinecraftClient c) {
        return c!=null&&c.options!=null&&c.player!=null&&c.world!=null&&c.getCameraEntity()==c.player
            &&focused(c)&&c.currentScreen==null&&!FreeLookRuntime.active&&!FreeLookRuntime.eligible(c)
            &&SpaceClient.mods!=null&&SpaceClient.mods.snaplook();
    }
    private static boolean pressed(MinecraftClient c){return ModuleKeys.held(c,SpaceClient.mods.snaplookKey());}
    private static boolean focused(MinecraftClient c){return c.focused;}
    public static void begin(MinecraftClient c) {
        end();if(c==null||c.options==null)return;
        boolean eligible=eligible(c);int current=c.options.perspective;
        int view=state.begin(current,eligible,eligible&&pressed(c),SpaceClient.mods==null?2:SpaceClient.mods.snapPerspective().view);
        boolean active=state.active();
        if(active){owner=c.options;owner.perspective=view;}
        if(c.world!=null&&c.worldRenderer!=null&&(active!=renderedSnap||(active&&view!=renderedView)))c.worldRenderer.scheduleTerrainUpdate();
        renderedSnap=active;renderedView=view;
    }
    public static void end() {
        if(owner!=null){GameOptions previous=owner;owner=null;previous.perspective=state.end(previous.perspective);}
    }
    public static boolean active(){return state.active();}
}
