package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.settings.SettingRow;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudSettingsCoverageTest {
    @Test public void allHudModulesExposeExplainedControlsAndBoundedScaleInTheirProfiles() throws Exception {
        HudSettings previous=SpaceClient.hud;Path file=Files.createTempDirectory("space-hud-controls").resolve("hud.properties");
        try {
            SpaceClient.hud=HudSettings.open(file);
            for(HudModule module:HudModule.values()) {
                HudModuleSettingsScreen screen=new HudModuleSettingsScreen(null,module);
                double other=SpaceClient.hud.getConfig().profile(1).placement(module).scale;
                for(int tab=0;tab<screen.sections().length;tab++) {
                    screen.section(tab);Set<Integer> ids=new HashSet<>();
                    for(SettingRow row:new ArrayList<>(screen.rows())) {
                        assertTrue(module+" duplicate control",ids.add(row.id));assertFalse(row.title.isEmpty());assertFalse(row.detail.isEmpty());
                        if(row.range!=null) {
                            screen.number(row.id,row.range.max);SpaceClient.hud=HudSettings.open(file);
                            assertEquals(module+" control "+row.id,row.range.max,find(screen,row.id).number);
                            screen.number(row.id,row.range.min);SpaceClient.hud=HudSettings.open(file);
                            assertEquals(row.range.min,find(screen,row.id).number);
                        }
                    }
                }
                assertEquals(other,SpaceClient.hud.getConfig().profile(1).placement(module).scale,0);
                screen.section(0);assertFalse(find(screen,3).enabled);
            }
        } finally { SpaceClient.hud=previous; }
    }
    @Test public void resetScopesPreserveOtherModuleContentAndAppearance() throws Exception {
        HudSettings previous=SpaceClient.hud;
        try {
            SpaceClient.hud=HudSettings.open(Files.createTempDirectory("space-hud-reset").resolve("hud.properties"));
            SpaceClient.hud.content(SpaceClient.hud.getConfig().active().content().withFpsLabel(false).withCoordinatesPrecision(2));
            SpaceClient.hud.appearance(HudModule.FPS,new HudAppearance(HudAppearance.Color.GOLD,HudAppearance.Backdrop.OFF,false));
            HudModuleSettingsScreen screen=new HudModuleSettingsScreen(null,HudModule.FPS);
            screen.change(17,1);assertTrue(SpaceClient.hud.getConfig().active().content().fpsLabel);
            assertEquals(2,SpaceClient.hud.getConfig().active().content().coordinatesPrecision);
            assertEquals(HudAppearance.Color.GOLD,SpaceClient.hud.getConfig().active().appearance(HudModule.FPS).color);
            screen.change(4,1);assertEquals(HudAppearance.Backdrop.OFF,SpaceClient.hud.getConfig().active().appearance(HudModule.FPS).backdrop);
            screen.change(11,1);assertEquals(HudAppearance.Color.AUTO,SpaceClient.hud.getConfig().active().appearance(HudModule.FPS).color);
        } finally { SpaceClient.hud=previous; }
    }
    private static SettingRow find(HudModuleSettingsScreen screen,int id) {
        for(SettingRow row:screen.rows())if(row.id==id)return row;throw new AssertionError("Missing control "+id);
    }
}
