package dev.spaceclient.mods;
/** Debounce and cooldown do not depend on the network transport. */
public final class AutoTextGate {
 private boolean sent;private long previous;
 public void clear(){sent=false;previous=0;}
 public String trigger(ModSettings s,int key,long nanos,boolean gameplay){
  if(s==null||!s.autoText()||!gameplay||key<=0)return "";
  for(int i=0;i<3;i++)if(s.autoTextKey(i)==key){String text=s.autoTextMessage(i);if(text.trim().isEmpty()||(sent&&nanos-previous<1000000000L))return "";previous=nanos;sent=true;return text;}return "";
 }
}
