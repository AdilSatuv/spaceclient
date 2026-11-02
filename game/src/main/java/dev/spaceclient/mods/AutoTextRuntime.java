package dev.spaceclient.mods;
import dev.spaceclient.SpaceClient;
import java.lang.ref.WeakReference;
import net.minecraft.client.MinecraftClient;
public final class AutoTextRuntime {
 private static final AutoTextGate gate=new AutoTextGate();private static WeakReference<Object> owner=new WeakReference<Object>(null);
 private AutoTextRuntime() { }
 public static void context(MinecraftClient c){Object p=c==null?null:c.player;if(owner.get()!=p){gate.clear();owner=new WeakReference<Object>(p);}if(c==null||c.world==null)gate.clear();}
 public static boolean dispatch(MinecraftClient c,int key,long nanos,boolean gameplay){
  context(c);ModSettings s=SpaceClient.mods;
  if(c==null||c.player==null||c.world==null||c.currentScreen!=null||c.player.getHealth()<=0||s==null||!gameplay)return false;
  if(ModuleKeys.conflicts(c,key)||ModuleKeys.usedByHold(s,null,key)||ModuleKeys.usedByToggle(s,null,key))return false;
  for(int i=0;i<3;i++)if(s.autoTextKey(i)==key&&ModuleKeys.usedByAutoText(s,i,key))return false;
  String text=gate.trigger(s,key,nanos,true);if(text.isEmpty())return false;send(c,text);return true;
 }
 private static void send(MinecraftClient c,String text){c.player.sendChatMessage(text);}
}
