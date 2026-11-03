package dev.spaceclient.hud;

import java.util.*;

/** Session-only undo; the UI advances a stack only after an atomic save succeeds. */
public final class HudLayoutHistory {
    private final Deque<Map<HudModule,HudPlacement>> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    public void clear(){undo.clear();redo.clear();}
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean canRedo(){return !redo.isEmpty();}
    public Map<HudModule,HudPlacement> undo(){return new EnumMap<>(undo.peek());}
    public Map<HudModule,HudPlacement> redo(){return new EnumMap<>(redo.peek());}
    public static boolean same(Map<HudModule,HudPlacement>a,Map<HudModule,HudPlacement>b){
        if(!a.keySet().equals(b.keySet()))return false;
        for(HudModule m:a.keySet()){HudPlacement x=a.get(m),y=b.get(m);if(x.horizontal!=y.horizontal||x.vertical!=y.vertical||x.x!=y.x||x.y!=y.y||x.scale!=y.scale)return false;}return true;
    }
    public void record(Map<HudModule,HudPlacement> before,Map<HudModule,HudPlacement> after){
        if(same(before,after))return;undo.push(new EnumMap<>(before));while(undo.size()>32)undo.removeLast();redo.clear();
    }
    public void didUndo(Map<HudModule,HudPlacement> current){undo.pop();redo.push(new EnumMap<>(current));}
    public void didRedo(Map<HudModule,HudPlacement> current){redo.pop();undo.push(new EnumMap<>(current));}
}
