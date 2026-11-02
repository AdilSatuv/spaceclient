package dev.spaceclient.hud;

import java.util.ArrayList;
import java.util.List;

/** Bounded labels. Missing measurements remain unavailable instead of becoming a fabricated zero. */
public final class ContextLabels {
    private ContextLabels() { }
    public static String ping(boolean singleplayer, Integer latency) {
        return ping(singleplayer, latency, true);
    }
    public static String ping(boolean singleplayer, Integer latency, boolean label) {
        return (label ? "Ping: " : "") + (singleplayer ? "Singleplayer" : latency == null || latency < 0 ? "--" : latency + " ms");
    }
    public static String biome(String name) {
        return biome(name, true);
    }
    public static String biome(String name, boolean label) {
        String value=clean(name,32);
        return (label ? "Biome: " : "") + (value.isEmpty()?"--":value);
    }
    public static String packs(List<String> names, boolean serverPack) {
        return packs(names,serverPack,true,2);
    }
    public static String packs(List<String> names, boolean serverPack, boolean label, int maxNames) {
        if (maxNames < 1 || maxNames > 3) throw new IllegalArgumentException("Invalid pack count");
        List<String> labels=new ArrayList<String>();
        if(serverPack)labels.add("Server");
        if(names!=null)for(String name:names){String value=clean(name,24);if(!value.isEmpty())labels.add(value);}
        if(labels.isEmpty())return label ? "Pack: Default" : "Default";
        String text=label ? (labels.size()==1?"Pack: ":"Packs: ") : "";
        int shown=Math.min(maxNames,labels.size());
        for(int i=0;i<shown;i++)text+=(i>0?", ":"")+labels.get(i);
        if(labels.size()>shown)text+=" +"+(labels.size()-shown);
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
