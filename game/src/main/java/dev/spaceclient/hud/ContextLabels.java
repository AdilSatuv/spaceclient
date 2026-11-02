package dev.spaceclient.hud;

import java.util.ArrayList;
import java.util.List;

/** Bounded labels. Missing measurements remain unavailable instead of becoming a fabricated zero. */
public final class ContextLabels {
    private ContextLabels() { }
    public static String ping(boolean singleplayer, Integer latency) {
        if (singleplayer) return "Ping: Singleplayer";
        return latency == null || latency < 0 ? "Ping: --" : "Ping: " + latency + " ms";
    }
    public static String biome(String name) {
        String value=clean(name,32);
        return "Biome: " + (value.isEmpty()?"--":value);
    }
    public static String packs(List<String> names, boolean serverPack) {
        List<String> labels=new ArrayList<String>();
        if(serverPack)labels.add("Server");
        if(names!=null)for(String name:names){String value=clean(name,24);if(!value.isEmpty())labels.add(value);}
        if(labels.isEmpty())return "Pack: Default";
        String text=(labels.size()==1?"Pack: ":"Packs: ")+labels.get(0);
        if(labels.size()>1)text+=", "+labels.get(1);
        if(labels.size()>2)text+=" +"+(labels.size()-2);
        return text;
    }
    static String clean(String name,int limit) {
        if(name==null)return "";
        StringBuilder out=new StringBuilder();
        name.codePoints().filter(cp->!Character.isISOControl(cp)&&Character.getType(cp)!=Character.FORMAT
            &&Character.getType(cp)!=Character.SURROGATE&&cp!=0x00a7).limit(limit).forEach(out::appendCodePoint);
        return out.toString().trim();
    }
}
