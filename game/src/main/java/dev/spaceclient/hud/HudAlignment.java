package dev.spaceclient.hud;

import java.util.List;

/** Resolve a drag against an 8-unit grid or nearby widget edges/centres. */
public final class HudAlignment {
    private HudAlignment() { }
    public static double[] snap(double x,double y,double w,double h,int width,int height,List<HudPlacement.Bounds> others,boolean grid) {
        if(grid)return new double[]{Math.round(x/8)*8,Math.round(y/8)*8};
        double sx=x,sy=y,dx=4.01,dy=4.01;
        double[] screenX={0,width/2.0,width},screenY={0,height/2.0,height};
        for(double anchor:screenX)for(double offset:new double[]{0,w/2,w})if(Math.abs(anchor-offset-x)<dx){dx=Math.abs(anchor-offset-x);sx=anchor-offset;}
        for(double anchor:screenY)for(double offset:new double[]{0,h/2,h})if(Math.abs(anchor-offset-y)<dy){dy=Math.abs(anchor-offset-y);sy=anchor-offset;}
        for(HudPlacement.Bounds b:others){
            for(double anchor:new double[]{b.x,b.x+b.width/2,b.x+b.width})for(double offset:new double[]{0,w/2,w})
                if(Math.abs(anchor-offset-x)<dx){dx=Math.abs(anchor-offset-x);sx=anchor-offset;}
            for(double anchor:new double[]{b.y,b.y+b.height/2,b.y+b.height})for(double offset:new double[]{0,h/2,h})
                if(Math.abs(anchor-offset-y)<dy){dy=Math.abs(anchor-offset-y);sy=anchor-offset;}
        }
        return new double[]{sx,sy};
    }
}
