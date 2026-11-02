package dev.spaceclient;

import dev.spaceclient.mods.*;
import java.nio.file.*;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModuleShortcutsTest {
    @Test public void shortcutsIgnoreGuiFocusLossRepeatsReleaseAndDuplicateEvents() throws Exception {
        Path file=Files.createTempDirectory("space-shortcuts").resolve("mods");ModSettings s=ModSettings.open(file);
        assertTrue(s.toggleKey(GameplayModule.ZOOM,34));ModuleShortcuts keys=new ModuleShortcuts();
        assertNull(keys.onKey(s,34,1,true,false,false,m->true));
        assertNull(keys.onKey(s,34,2,false,false,true,m->true));
        assertNull(keys.onKey(s,34,3,true,true,true,m->true));
        assertNull(keys.onKey(s,34,4,true,false,true,m->false));assertFalse(s.zoom());
        assertEquals(GameplayModule.ZOOM,keys.onKey(s,34,5,true,false,true,m->true));
        assertNull(keys.onKey(s,34,5,true,false,true,m->true));assertTrue(s.zoom());
        assertEquals(GameplayModule.ZOOM,keys.onKey(s,34,6,true,false,true,m->true));assertFalse(s.zoom());
        assertEquals(34,ModSettings.open(file).toggleKey(GameplayModule.ZOOM));
        assertTrue(s.toggleKey(GameplayModule.ZOOM,0));assertNull(keys.onKey(s,34,7,true,false,true,m->true));
    }
    @Test public void schemaTwoMigrationKeepsEveryGameplayOptionAndAddsUnboundKeys() throws Exception {
        Path file=Files.createTempDirectory("space-mods-v2-real").resolve("mods");
        byte[] old;try(InputStream in=getClass().getResourceAsStream("/hud-legacy/mods-schema2-78546a6.properties")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);old=out.toByteArray();
        }Files.write(file,old);ModSettings s=ModSettings.open(file);
        assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));
        for(GameplayModule m:GameplayModule.values()){assertEquals(m.ordinal()<GameplayModule.FULLBRIGHT.ordinal(),m.enabled(s));assertEquals(0,s.toggleKey(m));}
        assertTrue(s.toggleKey(GameplayModule.CROSSHAIR,35));s=ModSettings.open(file);
        assertEquals(35,s.toggleKey(GameplayModule.CROSSHAIR));assertEquals(7,s.zoomFactor());assertFalse(s.zoomSmooth());
        assertEquals(48,s.freeLookKey());assertEquals(34,s.zoomKey());assertEquals(ModSettings.SprintMode.TOGGLE,s.sprintMode());
        assertEquals(CrosshairStyle.Shape.RING,s.crosshairStyle().shape);assertEquals(CrosshairStyle.Color.AQUA,s.crosshairStyle().color);
        assertTrue(s.toggleKey(GameplayModule.ZOOM,35));assertTrue(ModuleKeys.usedByToggle(s,GameplayModule.ZOOM,35));
        assertFalse(s.toggleKey(GameplayModule.ZOOM,256));assertEquals(35,s.toggleKey(GameplayModule.ZOOM));
        assertTrue(s.reset(GameplayModule.ZOOM));assertEquals(0,s.toggleKey(GameplayModule.ZOOM));assertEquals(35,s.toggleKey(GameplayModule.CROSSHAIR));
    }
}
