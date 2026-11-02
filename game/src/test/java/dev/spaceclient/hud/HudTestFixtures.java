package dev.spaceclient.hud;

import java.util.Properties;

/** Reconstruct the historical shape when a test deliberately creates an old export. */
public final class HudTestFixtures {
    private HudTestFixtures() { }
    public static void removeNewModules(Properties p, String prefix) {
        for(String key:new String[]{"armor.style","armor.durability","armor.hand","effects.style","effects.limit","effects.time"})p.remove(prefix+"content."+key);
        p.remove(prefix+"content.keystrokes.layout");p.remove(prefix+"content.sprint.display");
        for(String key:new String[]{"ping.label","biome.label","packs.label","packs.names"})p.remove(prefix+"content."+key);
        p.remove(prefix+"content.keystrokes.jump");p.remove(prefix+"content.keystrokes.mouse");
        for(String id:new String[]{"memory","playtime","itemcounter","sprintstatus","ping","biome","packs","daycounter","lightlevel"}) {
            p.remove(prefix+id);
            for(String f:new String[]{"horizontal","vertical","x","y","scale"})p.remove(prefix+"layout."+id+"."+f);
            for(String f:new String[]{"color","backdrop","shadow"})p.remove(prefix+"appearance."+id+"."+f);
        }
    }
}
