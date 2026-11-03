package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.HudModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FoodItem;
import net.minecraft.item.ItemStack;

public final class NutritionRuntime {
    private NutritionRuntime(){}
    public static String text(MinecraftClient c){return c.player==null?"Saturation: --":String.format(Locale.ROOT,"Saturation%s: %.1f",c.isInSingleplayer()?"":" ~",c.player.getHungerManager().getSaturationLevel());}
    public static List<String> tooltip(List<String> original,ItemStack stack){
        if(stack==null||!(stack.getItem() instanceof FoodItem)||SpaceClient.hud==null||!HudModule.SATURATION.isEnabled(SpaceClient.hud.getConfig()))return original;
        FoodItem food=(FoodItem)stack.getItem();int hunger=food.getHungerPoints(stack);
        List<String> copy=new ArrayList<String>(original);
        copy.add(String.format(Locale.ROOT,"\u00a7eFood +%d  Saturation +%.1f",hunger,hunger*food.getSaturation(stack)*2));return copy;
    }
}
