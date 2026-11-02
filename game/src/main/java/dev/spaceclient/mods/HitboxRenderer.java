package dev.spaceclient.mods;
import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;
public final class HitboxRenderer {
 private HitboxRenderer() { }
 public static boolean visible(Entity e){
  ModSettings s=SpaceClient.mods;MinecraftClient c=MinecraftClient.getInstance();
  if(s==null||!s.hitboxes()||e==null||e.isInvisible()||e.removed||c==null||c.options==null||c.options.hudHidden)return false;
  if(e==c.player&&c.options.perspective==0)return false;
  return e instanceof PlayerEntity?s.hitboxPlayers():e instanceof LivingEntity?s.hitboxMobs():e instanceof ItemEntity&&s.hitboxItems();
 }
 public static void draw(Entity e,double x,double y,double z){
  if(!visible(e))return;Box box=e.getBoundingBox().offset(x-e.x,y-e.y,z-e.z);int rgb=SpaceClient.mods.hitboxColor().rgb();
  GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LINE_BIT);
  try{GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColor4f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,.85f);GL11.glLineWidth(SpaceClient.mods.hitboxWidth());WorldRenderer.drawBox(box);}finally{GL11.glPopAttrib();}
 }
}
