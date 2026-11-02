package dev.spaceclient.hud;

import java.util.Properties;

/** Reconstruct the historical shape when a test deliberately creates an old export. */
public final class HudTestFixtures {
    private HudTestFixtures() { }
    public static void removeNewModules(Properties p, String prefix) {
        for(String id:new String[]{"memory","playtime","itemcounter"}) {
            p.remove(prefix+id);
            for(String f:new String[]{"horizontal","vertical","x","y","scale"})p.remove(prefix+"layout."+id+"."+f);
            for(String f:new String[]{"color","backdrop","shadow"})p.remove(prefix+"appearance."+id+"."+f);
        }
    }
}
