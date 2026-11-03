package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.settings.SettingRow;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameSettingsCoverageTest {
    @Test public void everyGameplayModuleHasExplainedUniqueControlsAndScopedReset() throws Exception {
        ModSettings previous=SpaceClient.mods;
        try {
            SpaceClient.mods=ModSettings.open(Files.createTempDirectory("space-game-settings").resolve("mods.properties"));
            for(GameplayModule module:GameplayModule.values()) {
                GameModuleSettingsScreen screen=new GameModuleSettingsScreen(null,module);
                Set<Integer> ids=new HashSet<>();
                for(SettingRow row:screen.rows()) {
                    assertTrue(module+" duplicate control",ids.add(row.id));
                    assertFalse(module+" missing label",row.title.isEmpty());assertFalse(module+" missing explanation",row.detail.isEmpty());
                }
                assertTrue(ids.contains(0));assertTrue(ids.contains(12));
                screen.change(0,1);assertTrue(module.enabled(SpaceClient.mods));
                screen.change(98,1);assertTrue(module+" reset changed enabled",module.enabled(SpaceClient.mods));
                screen.change(0,1);assertFalse(module.enabled(SpaceClient.mods));
            }
        } finally { SpaceClient.mods=previous; }
    }
    @Test public void everyGameplayNumberDispatchesToItsOwnPersistedParameter() throws Exception {
        ModSettings previous=SpaceClient.mods;Path file=Files.createTempDirectory("space-game-controls").resolve("mods.properties");
        try {
            SpaceClient.mods=ModSettings.open(file);
            for(GameplayModule module:GameplayModule.values()) {
                GameModuleSettingsScreen screen=new GameModuleSettingsScreen(null,module);
                for(SettingRow row:new ArrayList<>(screen.rows()))if(row.range!=null) {
                    screen.number(row.id,row.range.max);
                    SpaceClient.mods=ModSettings.open(file);
                    assertEquals(module+" parameter "+row.id,row.range.max,find(screen,row.id).number);
                    screen.number(row.id,row.range.min);SpaceClient.mods=ModSettings.open(file);
                    assertEquals(module+" parameter "+row.id,row.range.min,find(screen,row.id).number);
                }
            }
        } finally { SpaceClient.mods=previous; }
    }
    @Test public void dependentControlsExplainWhyTheyAreUnavailable() throws Exception {
        ModSettings previous=SpaceClient.mods;
        try {
            SpaceClient.mods=ModSettings.open(Files.createTempDirectory("space-game-dependent").resolve("mods.properties"));
            GameModuleSettingsScreen block=new GameModuleSettingsScreen(null,GameplayModule.BLOCK_OVERLAY);
            SpaceClient.mods.blockMode(ModSettings.BlockMode.OUTLINE);assertFalse(find(block,21).enabled);assertTrue(find(block,22).enabled);
            SpaceClient.mods.blockMode(ModSettings.BlockMode.FILL);assertTrue(find(block,21).enabled);assertFalse(find(block,22).enabled);
            GameModuleSettingsScreen sounds=new GameModuleSettingsScreen(null,GameplayModule.SOUNDS);
            SpaceClient.mods.killChime(ModSettings.KillChime.OFF);assertFalse(find(sounds,48).enabled);
            SpaceClient.mods.killChime(ModSettings.KillChime.ORB);SpaceClient.mods.soundsVolume(0);assertFalse(find(sounds,48).enabled);
        } finally { SpaceClient.mods=previous; }
    }
    private static SettingRow find(GameModuleSettingsScreen screen,int id) {
        for(SettingRow row:screen.rows())if(row.id==id)return row;throw new AssertionError("Missing control "+id);
    }
}
