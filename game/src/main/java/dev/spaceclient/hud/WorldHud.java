package dev.spaceclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

/** Reads synced world time and already loaded lighting; never loads chunks or edits the world. */
public final class WorldHud {
    private WorldHud(){}
    public static String day(MinecraftClient c) {
        return WorldLabels.day(c==null||c.player==null||c.world==null?null:c.world.getTimeOfDay());
    }
    public static String light(MinecraftClient c) {
        if(c==null||c.player==null||c.world==null)return WorldLabels.light(null,null);
        BlockPos pos=new BlockPos(c.player.x,c.player.y,c.player.z);
        if(!c.world.isLoaded(pos,false))return WorldLabels.light(null,null);
        return WorldLabels.light(c.world.getLuminance(LightType.SKY,pos),c.world.getLuminance(LightType.BLOCK,pos));
    }
}
