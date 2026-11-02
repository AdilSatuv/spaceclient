package dev.spaceclient.hud;

import java.util.Objects;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/** Counts exact matches in the 36 main inventory/hotbar slots; excludes equipped armor and containers. */
public final class ItemCounterHud {
    private ItemCounterHud() { }
    public static String text(MinecraftClient client) {
        if (client == null || client.player == null) return "Items --";
        ItemStack held = client.player.getMainHandStack();
        if (held == null || held.count <= 0 || held.getItem()==null) return "Items: hold an item";
        long count = count(held, client.player.inventory.main);
        String name = client.textRenderer.trimToWidth(held.getCustomName(), 120);
        return name + ": " + count + " / " + stacks(held,client.player.inventory.main) + " stacks";
    }
    public static int stacks(ItemStack held,ItemStack[] slots){
        if(held==null||held.count<=0||held.getItem()==null||slots==null)return 0;
        int n=0;for(ItemStack stack:slots)if(stack!=null&&stack.count>0&&stack.getItem()==held.getItem()&&stack.getDamage()==held.getDamage()&&Objects.equals(stack.getNbt(),held.getNbt()))n++;return n;
    }
    public static void draw(MinecraftClient c,String text,HudPlacement.Bounds b,HudAppearance appearance,int color){
        GlStateManager.pushMatrix();try(HudIconState state=new HudIconState()){
            GlStateManager.translate(b.x,b.y,0);GlStateManager.scale(b.scale,b.scale,1);EquipmentHudRenderer.textState();
            int width=HudRenderer.moduleWidth(c,HudModule.ITEM_COUNTER,text),alpha=HudRenderer.backdropAlpha(appearance);
            if(alpha!=0)UiShapes.rounded(0,0,width,22,3,(alpha<<24)|HudRenderer.BACKDROP_RGB);
            ItemStack held=c.player==null?null:c.player.getMainHandStack();
            if(held!=null&&held.getItem()!=null&&held.count>0){DiffuseLighting.enable();GlStateManager.enableDepthTest();GlStateManager.enableTexture();GlStateManager.color(1,1,1,1);c.getItemRenderer().renderInGuiWithOverrides(held,3,3);}
            EquipmentHudRenderer.textState();c.textRenderer.draw(text,23,7,color,appearance.shadow);
        }finally{GlStateManager.popMatrix();}
    }
    static long count(ItemStack held, ItemStack[] slots) {
        if (held == null || held.count <= 0 || held.getItem()==null || slots == null) return 0;
        long count = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.count > 0 && stack.getItem() == held.getItem()
                    && stack.getDamage() == held.getDamage() && Objects.equals(stack.getNbt(), held.getNbt())) count += stack.count;
        }
        return count;
    }
}
