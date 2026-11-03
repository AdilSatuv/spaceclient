package dev.spaceclient.mods;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.HudIconState;
import java.lang.ref.WeakReference;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class TooltipRuntime extends DrawableHelper {
    private static final TooltipScroll scroll=new TooltipScroll();
    private static WeakReference<Screen> owner=new WeakReference<Screen>(null);
    private static int signature,cw,ch,vw,vh;
    private static long rendered,event;
    private TooltipRuntime(){}
    public static void wheel(Screen screen){
        if(!Mouse.isCreated()||SpaceClient.mods==null||!SpaceClient.mods.scrollableTooltips()||owner.get()!=screen||System.nanoTime()-rendered>150_000_000L)return;
        long stamp=Mouse.getEventNanoseconds();if(stamp==event)return;event=stamp;
        scroll.scroll(Mouse.getEventDWheel(),Screen.hasShiftDown(),SpaceClient.mods.tooltipSpeed(),cw,ch,vw,vh);
    }
    public static boolean draw(Screen screen,List<String> lines,int mouseX,int mouseY){
        MinecraftClient c=MinecraftClient.getInstance();ModSettings s=SpaceClient.mods;
        if(s==null||!s.scrollableTooltips()||lines.isEmpty())return false;
        int contentW=0;for(String line:lines)contentW=Math.max(contentW,c.textRenderer.getStringWidth(line));int contentH=lines.size()*10+2;
        if(contentW+12<=screen.width&&contentH+12<=screen.height)return false;
        int hash=lines.hashCode();if(owner.get()!=screen||hash!=signature){scroll.reset();signature=hash;owner=new WeakReference<Screen>(screen);}
        rendered=System.nanoTime();cw=contentW;ch=contentH;vw=Math.max(1,Math.min(contentW,screen.width-16));vh=Math.max(1,Math.min(contentH,screen.height-30));
        scroll.scroll(0,false,0,cw,ch,vw,vh);scroll.scroll(0,true,0,cw,ch,vw,vh);
        int x=Math.max(8,Math.min(mouseX+12,screen.width-vw-8)),y=Math.max(8,Math.min(mouseY-12,screen.height-vh-22));
        try(HudIconState state=new HudIconState()){
            GlStateManager.disableLighting();GlStateManager.disableDepthTest();GlStateManager.enableBlend();
            fill(x-4,y-4,x+vw+4,y+vh+16,0xF2101118);fill(x-4,y-4,x+vw+4,y-3,0xFFE7BF79);
            GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
            try{Window window=new Window(c);int scale=window.getScaleFactor();GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(x*scale,c.height-(y+vh)*scale,vw*scale,vh*scale);
                for(int i=0;i<lines.size();i++)c.textRenderer.drawWithShadow(lines.get(i),x-scroll.x,y+i*10-scroll.y,0xFFFFFF);
            }finally{GL11.glPopAttrib();}
            c.textRenderer.drawWithShadow("Scroll / Shift: horizontal",x,y+vh+4,0xE7BF79);
        }return true;
    }
}
