package dev.spaceclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Uses the game's own item renderer and effect atlas, with original card geometry. */
public final class EquipmentHudRenderer {
    private static final Identifier INVENTORY=new Identifier("textures/gui/container/inventory.png");
    private EquipmentHudRenderer(){}
    public static int armorCell(MinecraftClient c,EquipmentContent content) {
        ItemStack[] slots=ArmorHud.slots(c,content.hand);String[] widest=new String[slots.length];
        for(int i=0;i<slots.length;i++)widest[i]=content.durability==EquipmentContent.Durability.PERCENT?"100%"
            :slots[i]!=null&&slots[i].getItem()!=null&&slots[i].isDamageable()?Integer.toString(slots[i].getMaxDamage()):"n/a";
        return EquipmentLayout.armorCell(widest,c.textRenderer::getStringWidth);
    }
    public static void armor(MinecraftClient c,EquipmentContent content,HudPlacement.Bounds b,HudAppearance appearance,int color) {
        ItemStack[] slots=ArmorHud.slots(c,content.hand);int cell=armorCell(c,content),alpha=HudRenderer.backdropAlpha(appearance);
        GlStateManager.pushMatrix();
        try(HudIconState state=new HudIconState()) {
            GlStateManager.translate(b.x,b.y,0);GlStateManager.scale(b.scale,b.scale,1);
            for(int i=0;i<slots.length;i++) {
                int x=i*(cell+2);if(alpha!=0)UiShapes.rounded(x,0,cell,EquipmentLayout.ARMOR_HEIGHT,4,(alpha<<24)|HudRenderer.BACKDROP_RGB);
                ItemStack stack=slots[i];
                if(stack!=null&&stack.count>0&&stack.getItem()!=null) {
                    DiffuseLighting.enable();GlStateManager.enableDepthTest();GlStateManager.enableTexture();GlStateManager.color(1,1,1,1);
                    c.getItemRenderer().renderInGuiWithOverrides(stack,x+(cell-16)/2,2);
                } else {textState();center(c,new String[]{"H","C","L","B","Hand"}[i],x+cell/2,6,color,appearance);}
                textState();double fraction=ArmorHud.fraction(stack);
                int ink=Double.isNaN(fraction)?color:fraction<=.2?0xF28B82:color;
                center(c,ArmorHud.value(stack,content),x+cell/2,22,ink,appearance);
                if(!Double.isNaN(fraction)) {
                    UiShapes.rounded(x+3,34,cell-6,2,1,0xFF464A52);
                    int pixels=fraction<=0?0:Math.max(1,(int)Math.round((cell-6)*fraction));
                    if(pixels>0)UiShapes.rounded(x+3,34,pixels,2,1,0xFF000000|(fraction<=.2?0xF28B82:fraction<=.5?0xD7AF6A:0x81D496));
                }
            }
        } finally {GlStateManager.popMatrix();}
    }
    public static void effects(MinecraftClient c,EquipmentContent content,HudPlacement.Bounds b,HudAppearance appearance,int color) {
        List<PotionHud.Effect> effects=PotionHud.effects(c);int shown=Math.min(content.maxEffects,effects.size()),alpha=HudRenderer.backdropAlpha(appearance),row=content.effectTime?24:22;
        GlStateManager.pushMatrix();
        try(HudIconState state=new HudIconState()) {
            GlStateManager.translate(b.x,b.y,0);GlStateManager.scale(b.scale,b.scale,1);textState();
            if(effects.isEmpty()) {
                if(alpha!=0)UiShapes.rounded(0,0,EquipmentLayout.EFFECT_WIDTH,24,4,(alpha<<24)|HudRenderer.BACKDROP_RGB);
                c.textRenderer.draw(c.player==null?"Effects --":"Effects: none",7,8,color,appearance.shadow);return;
            }
            for(int i=0;i<shown;i++) {
                PotionHud.Effect effect=effects.get(i);int y=i*(row+2);
                if(alpha!=0)UiShapes.rounded(0,y,EquipmentLayout.EFFECT_WIDTH,row,4,(alpha<<24)|HudRenderer.BACKDROP_RGB);
                StatusEffect type=effect.id>=0&&effect.id<StatusEffect.STATUS_EFFECTS.length?StatusEffect.STATUS_EFFECTS[effect.id]:null;
                int icon=type!=null&&type.hasIcon()?type.getIconLevel():-1;
                if(icon>=0&&icon<24) {
                    c.getTextureManager().bindTexture(INVENTORY);GlStateManager.color(1,1,1,1);
                    DrawableHelper.drawTexture(4,y+2,icon%8*18,198+icon/8*18,18,18,256,256);
                } else center(c,"?",13,y+7,color,appearance);
                textState();String title=trim(c,PotionHud.title(effect),EquipmentLayout.EFFECT_WIDTH-32);
                c.textRenderer.draw(title,27,y+(content.effectTime?3:7),color,appearance.shadow);
                if(content.effectTime)c.textRenderer.draw(PotionHud.duration(effect.ticks,effect.permanent),27,y+13,
                    !effect.permanent&&effect.ticks>=0&&effect.ticks<200?0xF28B82:color,appearance.shadow);
            }
            if(effects.size()>shown)c.textRenderer.draw("+"+(effects.size()-shown)+" more",5,shown*(row+2),color,appearance.shadow);
        } finally {GlStateManager.popMatrix();}
    }
    private static String trim(MinecraftClient c,String text,int max) {
        return c.textRenderer.getStringWidth(text)<=max?text:c.textRenderer.trimToWidth(text,max-c.textRenderer.getStringWidth(".."))+"..";
    }
    private static void center(MinecraftClient c,String text,int x,int y,int color,HudAppearance a){c.textRenderer.draw(text,x-c.textRenderer.getStringWidth(text)/2f,y,color,a.shadow);}
    static void textState() {
        GlStateManager.disableLighting();GlStateManager.disableDepthTest();GlStateManager.enableTexture();GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(GL11.GL_GREATER,.1f);GlStateManager.enableBlend();GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);GlStateManager.color(1,1,1,1);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
    }
}
