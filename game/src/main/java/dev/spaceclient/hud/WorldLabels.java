package dev.spaceclient.hud;

/** World measurements keep missing values distinct from a real zero. */
public final class WorldLabels {
    private WorldLabels(){}
    public static String day(Long ticks) {
        return ticks==null||ticks<0?"Day: --":"Day: "+(ticks/24000+1);
    }
    public static String light(Integer sky,Integer block) {
        return "Light S:"+level(sky)+" B:"+level(block);
    }
    private static String level(Integer value){return value==null||value<0||value>15?"--":Integer.toString(value);}
}
