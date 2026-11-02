package dev.spaceclient.mods;

/** A perspective lease for one render frame; input/tick perspective remains vanilla. */
public final class SnaplookState {
    private boolean active;
    private int original,target;
    public int begin(int current,boolean eligible,boolean held,int target) {
        current=end(current);
        if(!eligible||!held||current<0||current>2||target<0||target>2)return current;
        this.original=current;this.target=target;active=true;return target;
    }
    public int end(int current) {
        if(!active)return current;
        active=false;return current==target?original:current;
    }
    public boolean active(){return active;}
}
