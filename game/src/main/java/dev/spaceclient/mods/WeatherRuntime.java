package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

/** Visual policy is restricted to the client Overworld. Never writes weather fields. */
public final class WeatherRuntime {
    private WeatherRuntime(){}
    public static ModSettings.WeatherMode mode(World world) {
        return world instanceof ClientWorld&&world.dimension.getType()==0&&SpaceClient.mods!=null&&SpaceClient.mods.weatherChanger()
            ?SpaceClient.mods.weatherMode():null;
    }
}
