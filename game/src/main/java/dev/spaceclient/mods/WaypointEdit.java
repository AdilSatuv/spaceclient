package dev.spaceclient.mods;

/** A draft stays separate from the saved point until validated persistence succeeds. */
public final class WaypointEdit {
    private final WaypointStore.Point original;
    private final int field;
    private String buffer,error="";
    public WaypointEdit(WaypointStore.Point point,int field) {
        if(point==null||field<0||field>3)throw new IllegalArgumentException("Invalid marker field");
        this.original=point;this.field=field;
        buffer=field==0?point.name:Integer.toString(field==1?point.x:field==2?point.y:point.z);
    }
    public String buffer() { return buffer; }
    public String error() { return error; }
    public boolean append(String text) {
        String next=buffer+(text==null?"":text);
        if(next.length()>(field==0?32:11)||!ModSettings.validText(next)||field>0&&!next.matches("[+-]?[0-9]*")) {
            error=field==0?"Use up to 32 plain characters.":"Use a whole number; Enter saves, Esc cancels.";return false;
        }
        buffer=next;error="";return true;
    }
    public void backspace() {
        if(!buffer.isEmpty())buffer=buffer.substring(0,buffer.offsetByCodePoints(buffer.length(),-1));
        error="";
    }
    public boolean save(WaypointStore store) {
        try {
            int value=field==0?0:Integer.parseInt(buffer);
            WaypointStore.Point next=new WaypointStore.Point(original.context,field==0?buffer:original.name,original.dimension,
                field==1?value:original.x,field==2?value:original.y,field==3?value:original.z,original.color,original.visible);
            if(store.replace(original,next)) { error="";return true; }
            error=store.error().isEmpty()?"Marker changed; reopen it before editing.":store.error();return false;
        } catch(IllegalArgumentException failure) {
            error=field==0?"Enter a name with 1-32 plain characters.":field==2?"Y must be a whole number from 0 to 255.":"X / Z must be a whole number from -30000000 to 30000000.";
            return false;
        }
    }
    public static WaypointStore.Point nudge(WaypointStore.Point p,int field,int amount) {
        if(field<1||field>3)throw new IllegalArgumentException("Invalid coordinate");
        int current=field==1?p.x:field==2?p.y:p.z;
        int value=(int)Math.max(field==2?0:-30_000_000,Math.min(field==2?255:30_000_000,(long)current+amount));
        return new WaypointStore.Point(p.context,p.name,p.dimension,field==1?value:p.x,field==2?value:p.y,field==3?value:p.z,p.color,p.visible);
    }
}
