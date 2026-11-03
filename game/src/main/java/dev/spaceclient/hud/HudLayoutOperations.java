package dev.spaceclient.hud;

import java.util.*;

/** Pure group geometry. A shared clamp preserves spacing at viewport edges. */
public final class HudLayoutOperations {
    public enum Align { LEFT, CENTER, RIGHT, TOP, MIDDLE, BOTTOM }
    private HudLayoutOperations() { }
    public static Map<HudModule,HudPlacement> snapshot(HudProfile profile) {
        Map<HudModule,HudPlacement> result=new EnumMap<>(HudModule.class);
        for(HudModule m:HudModule.values())result.put(m,profile.placement(m));return result;
    }
    private static List<HudModule> movable(Set<HudModule> selection,Set<HudModule> locked) {
        List<HudModule> list=new ArrayList<>();for(HudModule m:HudModule.values())if(selection.contains(m)&&!locked.contains(m))list.add(m);return list;
    }
    private static void put(Map<HudModule,HudPlacement> out,HudModule m,HudPlacement.Bounds b,double x,double y,int w,int h) {
        out.put(m,out.get(m).at(x,y,w,h,Math.max(1,(int)Math.round(b.width/b.scale)),Math.max(1,(int)Math.round(b.height/b.scale)),false));
    }
    public static Map<HudModule,HudPlacement> move(Map<HudModule,HudPlacement> source,Map<HudModule,HudPlacement.Bounds> bounds,Set<HudModule> selection,Set<HudModule> locked,double dx,double dy,int w,int h) {
        if(!Double.isFinite(dx)||!Double.isFinite(dy))throw new IllegalArgumentException("Invalid group delta");
        Map<HudModule,HudPlacement> out=new EnumMap<>(source);List<HudModule> list=movable(selection,locked);
        double minX=0,maxX=0,minY=0,maxY=0;boolean first=true;
        for(HudModule m:list){HudPlacement.Bounds b=bounds.get(m);if(first){minX=b.x;minY=b.y;maxX=b.x+b.width;maxY=b.y+b.height;first=false;}else{minX=Math.min(minX,b.x);minY=Math.min(minY,b.y);maxX=Math.max(maxX,b.x+b.width);maxY=Math.max(maxY,b.y+b.height);}}
        dx=Math.max(-minX,Math.min(w-maxX,dx));dy=Math.max(-minY,Math.min(h-maxY,dy));
        for(HudModule m:list){HudPlacement.Bounds b=bounds.get(m);put(out,m,b,b.x+dx,b.y+dy,w,h);}return out;
    }
    public static Map<HudModule,HudPlacement> align(Map<HudModule,HudPlacement> source,Map<HudModule,HudPlacement.Bounds> bounds,Set<HudModule> selection,Set<HudModule> locked,Align alignment,int w,int h) {
        Map<HudModule,HudPlacement> out=new EnumMap<>(source);List<HudModule> list=movable(selection,locked);if(list.size()<2)return out;
        double x=Double.MAX_VALUE,y=x,right=0,bottom=0;
        for(HudModule m:list){HudPlacement.Bounds b=bounds.get(m);x=Math.min(x,b.x);y=Math.min(y,b.y);right=Math.max(right,b.x+b.width);bottom=Math.max(bottom,b.y+b.height);}
        for(HudModule m:list){HudPlacement.Bounds b=bounds.get(m);double px=b.x,py=b.y;
            switch(alignment){case LEFT:px=x;break;case CENTER:px=(x+right-b.width)/2;break;case RIGHT:px=right-b.width;break;case TOP:py=y;break;case MIDDLE:py=(y+bottom-b.height)/2;break;case BOTTOM:py=bottom-b.height;break;default:throw new AssertionError(alignment);}
            put(out,m,b,px,py,w,h);
        }return out;
    }
    public static Map<HudModule,HudPlacement> distribute(Map<HudModule,HudPlacement> source,Map<HudModule,HudPlacement.Bounds> bounds,Set<HudModule> selection,Set<HudModule> locked,boolean horizontal,int w,int h) {
        Map<HudModule,HudPlacement> out=new EnumMap<>(source);List<HudModule> list=movable(selection,locked);if(list.size()<3)return out;
        list.sort(Comparator.comparingDouble((HudModule m)->horizontal?bounds.get(m).x:bounds.get(m).y).thenComparingInt(Enum::ordinal));
        HudPlacement.Bounds a=bounds.get(list.get(0)),z=bounds.get(list.get(list.size()-1));
        double start=horizontal?a.x:a.y,end=horizontal?z.x+z.width:z.y+z.height,total=0;
        for(HudModule m:list)total+=horizontal?bounds.get(m).width:bounds.get(m).height;
        double gap=(end-start-total)/(list.size()-1),position=start;
        for(HudModule m:list){HudPlacement.Bounds b=bounds.get(m);put(out,m,b,horizontal?position:b.x,horizontal?b.y:position,w,h);position+=(horizontal?b.width:b.height)+gap;}return out;
    }
}
