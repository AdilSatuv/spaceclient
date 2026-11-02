package dev.spaceclient.hud;

import dev.spaceclient.mods.ModSettings;
import dev.spaceclient.mods.SprintState;
import org.junit.Test;
import static org.junit.Assert.*;

public class SprintStatusTest {
    @Test public void compactTextKeepsArmedAndActualMovementStatesDistinct() {
        assertEquals("Sprint OFF",SprintStatus.compact(false,true,true));
        assertEquals("Sprint OFF",SprintStatus.compact(true,false,false));
        assertEquals("Sprint ON / READY",SprintStatus.compact(true,true,false));
        assertEquals("Sprint ON / RUN",SprintStatus.compact(true,true,true));
        assertEquals("Sprint OFF / RUN",SprintStatus.compact(true,false,true));
    }
    @Test public void showsModuleAndToggleStateSeparatelyFromActualVanillaSprinting() {
        SprintState state=new SprintState();
        assertEquals("ToggleSprint: OFF | Disabled",SprintStatus.text(false,false,false));
        assertEquals("ToggleSprint: OFF | Disabled",SprintStatus.text(false,true,true));
        assertEquals("ToggleSprint: OFF",SprintStatus.text(true,state.requested(true,ModSettings.SprintMode.TOGGLE),false));
        state.press();
        assertEquals("ToggleSprint: ON | Ready",SprintStatus.text(true,state.requested(true,ModSettings.SprintMode.TOGGLE),false));
        assertEquals("ToggleSprint: ON | Sprinting",SprintStatus.text(true,state.requested(true,ModSettings.SprintMode.TOGGLE),true));
        state.press();
        assertEquals("ToggleSprint: OFF",SprintStatus.text(true,state.requested(true,ModSettings.SprintMode.TOGGLE),false));
        assertEquals("ToggleSprint: ON | Ready",SprintStatus.text(true,state.requested(true,ModSettings.SprintMode.AUTO),false));
        assertEquals("ToggleSprint: OFF | Sprinting",SprintStatus.text(true,false,true));
    }
    @Test public void hudStatusIsDiscoverableAndDoesNotReplaceTheGameplayModule() {
        assertEquals(HudModule.SPRINT_STATUS,HudSection.HUD.catalog("togglesprint").get(0).hud);
        assertEquals(2,HudSection.HOME.catalog("autosprint").size());
        assertNotNull(HudSection.HOME.catalog("autosprint").get(0).game);
        assertEquals(HudModule.SPRINT_STATUS,HudSection.PLAYER.search("статус").get(0));
    }
}
