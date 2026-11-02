package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.DurabilityAlert;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/** A quiet, live warning. No sounds, chat messages, item changes or timers. */
public final class DurabilityWarningRenderer {
    private DurabilityWarningRenderer() { }
    public static ItemStack lowest(ItemStack[] items,int threshold) {
        ItemStack worst=null;
        if(items==null)return null;
        for(ItemStack item:items) {
            if(item==null||item.count<=0||item.getItem()==null
                    ||!DurabilityAlert.low(item.isDamageable(),item.getDamage(),item.getMaxDamage(),threshold))continue;
            if(worst==null||(long)DurabilityAlert.remaining(item.getDamage(),item.getMaxDamage())*worst.getMaxDamage()
                    <(long)DurabilityAlert.remaining(worst.getDamage(),worst.getMaxDamage())*item.getMaxDamage())worst=item;
        }
        return worst;
    }
    public static String message(MinecraftClient c) {
        if(c==null||c.player==null||c.world==null||c.currentScreen!=null||c.getCameraEntity()!=c.player||c.player.getHealth()<=0
                ||c.options==null||c.options.hudHidden||c.options.debugEnabled||SpaceClient.mods==null||!SpaceClient.mods.durabilityWarning()
                ||SpaceClient.hud==null||!SpaceClient.hud.getConfig().active().enabled)return "";
        ItemStack item=lowest(ArmorHud.slots(c,true),SpaceClient.mods.durabilityThreshold().percent);
        return item==null?"":ContextLabels.clean(item.getCustomName(),96)+"  "+DurabilityAlert.remaining(item.getDamage(),item.getMaxDamage())+" / "+item.getMaxDamage();
    }
    public static void render(MinecraftClient c,int width,int height) {
        String message=message(c);if(message.isEmpty()||width<100||height<100)return;
        int w=Math.min(width-24,Math.max(180,Math.min(270,SpaceUiFont.width(message)+24))),x=(width-w)/2,y=38;
        try(HudIconState state=new HudIconState()) {
            EquipmentHudRenderer.textState();
            UiShapes.rounded(x,y,w,35,5,0xED141519);
            UiShapes.rounded(x,y,3,35,1,0xFFD7AF6A);
            SpaceUiFont.draw(c,"Low durability",x+12,y+6,0xD7AF6A);
            int split=message.lastIndexOf("  ");String name=message.substring(0,split),count=message.substring(split+2);
            int countWidth=SpaceUiFont.width(count);
            SpaceUiFont.draw(c,SpaceUiFont.trimToWidth(name,w-36-countWidth),x+12,y+21,0xF2EEE7);
            SpaceUiFont.draw(c,count,x+w-12-countWidth,y+21,0xF2EEE7);
        }
    }
}
