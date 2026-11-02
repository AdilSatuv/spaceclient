package dev.spaceclient.mods;
import dev.spaceclient.SpaceClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.UseAction;
public final class AnimationsRuntime {
 public static final SwingClock swing=new SwingClock();
 private AnimationsRuntime() { }
 public static boolean blocking(MinecraftClient c){return c!=null&&c.player!=null&&c.player.isUsingItem()&&c.player.getUsedItem()!=null&&c.player.getUsedItem().getUseAction()==UseAction.BLOCK;}
 public static void mouse(MinecraftClient c,int button,boolean down){if(button==0&&down&&c.focused&&c.currentScreen==null&&SpaceClient.mods!=null&&SpaceClient.mods.animations()&&SpaceClient.mods.animationBlockHit()&&blocking(c))swing.begin(System.nanoTime());}
 public static float swing(MinecraftClient c,float nativeSwing,float delta){return SpaceClient.mods!=null&&SpaceClient.mods.animations()&&SpaceClient.mods.animationBlockHit()&&blocking(c)?Math.max(c.player.getHandSwingProgress(delta),swing.progress(System.nanoTime())):nativeSwing;}
}
