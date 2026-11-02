package dev.spaceclient.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.resource.ResourcePackLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/** Reads existing client data only; does not ping servers, load packs or create world chunks. */
public final class ContextHud {
    private ContextHud() { }
    public static String ping(MinecraftClient c) {
        if(c==null||c.player==null||c.world==null)return ContextLabels.ping(false,null);
        if(c.isInSingleplayer())return ContextLabels.ping(true,null);
        PlayerListEntry entry=c.getNetworkHandler()==null?null:c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
        return ContextLabels.ping(false,entry==null?null:entry.getLatency());
    }
    public static String biome(MinecraftClient c) {
        if(c==null||c.player==null||c.world==null)return ContextLabels.biome(null);
        // Legacy biomes are columns; keep the loading check valid above/below build height too.
        BlockPos pos=new BlockPos(c.player.x,0,c.player.z);
        if(!c.world.isLoaded(pos,false))return ContextLabels.biome(null);
        Biome biome=c.world.getBiome(pos);
        return ContextLabels.biome(biome==null?null:biome.name);
    }
    public static String packs(MinecraftClient c) {
        if(c==null||c.getResourcePackLoader()==null)return "Pack: --";
        ResourcePackLoader loader=c.getResourcePackLoader();
        List<String> names=new ArrayList<String>();
        for(ResourcePackLoader.Entry entry:loader.getSelectedResourcePacks())if(entry!=null)names.add(entry.getName());
        return ContextLabels.packs(names,loader.getServerContainer()!=null);
    }
}
