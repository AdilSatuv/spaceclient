package dev.spaceclient.mods;

/** Always honours a lower user cap. Zero native cap means unlimited. */
public final class IdleFramePolicy {
    private long activity;
    public void active(long now){activity=now;}
    public int limit(int nativeLimit,boolean enabled,boolean focused,long now,int background,int idle,int delaySeconds){
        if(!enabled)return nativeLimit;
        int cap=!focused?background:now>=activity&&now-activity>=delaySeconds*1_000_000_000L?idle:Integer.MAX_VALUE;
        return cap==Integer.MAX_VALUE?nativeLimit:nativeLimit<=0?cap:Math.min(nativeLimit,cap);
    }
}
