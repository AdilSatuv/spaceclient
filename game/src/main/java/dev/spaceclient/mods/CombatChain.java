package dev.spaceclient.mods;

/** Client observations, never server kill/hit attribution. Monotonic nanoseconds. */
public final class CombatChain {
    private int target=-1,combo;
    private long attacked,confirmed;
    private boolean pending;
    public void attack(int id,long now){if(target!=id||now-confirmed>2_000_000_000L)combo=0;target=id;attacked=now;pending=true;}
    public void hurt(int id,boolean self,long now){
        if(self){reset();return;}
        if(pending&&id==target&&now>=attacked&&now-attacked<=1_000_000_000L){combo++;confirmed=now;pending=false;}
    }
    public int count(long now){if(now<confirmed||now-confirmed>2_000_000_000L)combo=0;return combo;}
    public void reset(){target=-1;combo=0;pending=false;attacked=confirmed=0;}
}
