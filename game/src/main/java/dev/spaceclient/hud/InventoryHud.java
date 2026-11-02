package dev.spaceclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;

/** Own inventory only. Three storage rows, with the hotbar optionally appended last. */
public final class InventoryHud {
    public static final int WIDTH=166;
    private InventoryHud() { }
    public static int cells(boolean hotbar) { return hotbar ? 36 : 27; }
    public static int height(boolean hotbar) { return 16+(hotbar?4:3)*18; }
    public static int slotIndex(int cell,boolean hotbar) {
        if(cell<0||cell>=cells(hotbar))throw new IllegalArgumentException("Invalid inventory cell");
        return cell<27?cell+9:cell-27;
    }
    private static ItemStack at(ItemStack[] main,int index) { return main!=null&&index<main.length?main[index]:null; }
    private static boolean present(ItemStack stack) { return stack!=null&&stack.count>0&&stack.getItem()!=null; }
    public static int used(ItemStack[] main,boolean hotbar) {
        int used=0;for(int i=0;i<cells(hotbar);i++)if(present(at(main,slotIndex(i,hotbar))))used++;return used;
    }
    public static String text(MinecraftClient c,boolean hotbar) {
        return c==null||c.player==null||c.player.inventory.main==null?"Inventory --":"Inventory "+used(c.player.inventory.main,hotbar)+"/"+cells(hotbar);
    }
    public static void draw(MinecraftClient c,HudContentSettings content,HudPlacement.Bounds b,HudAppearance appearance,int color) {
        boolean hotbar=content.inventoryHotbar;ItemStack[] main=c.player==null?null:c.player.inventory.main;
        int alpha=HudRenderer.backdropAlpha(appearance);
        GlStateManager.pushMatrix();
        try(HudIconState state=new HudIconState()) {
            GlStateManager.translate(b.x,b.y,0);GlStateManager.scale(b.scale,b.scale,1);EquipmentHudRenderer.textState();
            if(alpha!=0)UiShapes.rounded(0,0,WIDTH,height(hotbar),4,(alpha<<24)|HudRenderer.BACKDROP_RGB);
            c.textRenderer.draw("Inventory",4,3,color,appearance.shadow);
            String count=main==null?"--":used(main,hotbar)+"/"+cells(hotbar);
            c.textRenderer.draw(count,WIDTH-4-c.textRenderer.getStringWidth(count),3,color,appearance.shadow);
            for(int i=0;i<cells(hotbar);i++) {
                int x=2+(i%9)*18,y=14+(i/9)*18;
                UiShapes.rounded(x,y,17,17,2,0x683B414C);
                ItemStack stack=at(main,slotIndex(i,hotbar));if(!present(stack))continue;
                DiffuseLighting.enable();GlStateManager.enableDepthTest();GlStateManager.enableTexture();GlStateManager.color(1,1,1,1);
                c.getItemRenderer().renderInGuiWithOverrides(stack,x,y);
                if(content.inventoryOverlays)c.getItemRenderer().renderGuiItemOverlay(c.textRenderer,stack,x,y);
                EquipmentHudRenderer.textState();
            }
        } finally { GlStateManager.popMatrix(); }
    }
}
