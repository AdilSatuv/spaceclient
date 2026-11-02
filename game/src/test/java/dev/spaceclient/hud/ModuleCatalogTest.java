package dev.spaceclient.hud;

import dev.spaceclient.mods.GameplayModule;
import java.util.List;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModuleCatalogTest {
    @Test public void missingGameplayModulesAreOnFirstHomePageAndSearchableByOldNames() {
        List<CatalogModule> home=HudSection.HOME.catalog("");
        assertEquals(GameplayModule.TOGGLE_SPRINT,home.get(0).game);
        assertEquals(GameplayModule.FREE_LOOK,home.get(1).game);
        assertEquals(GameplayModule.ZOOM,home.get(2).game);
        assertEquals(GameplayModule.CROSSHAIR,home.get(3).game);
        assertEquals(GameplayModule.TOGGLE_SPRINT,HudSection.HOME.catalog("autosprint").get(0).game);
        assertEquals(GameplayModule.FREE_LOOK,HudSection.HOME.catalog("freelook").get(0).game);
        assertEquals(HudModule.values().length+GameplayModule.values().length,home.size());
        HashSet<String> ids=new HashSet<String>();for(CatalogModule m:home)assertTrue(ids.add(m.id()));
    }
    @Test public void hudKeepsTheReferenceOrderAndPlayerContainsBothMovementFeatures() {
        List<CatalogModule> hud=HudSection.HUD.catalog("");
        assertEquals(HudModule.FPS,hud.get(0).hud); assertEquals(HudModule.ARMOR,hud.get(5).hud);
        for(CatalogModule m:hud)assertNull(m.game);
        assertEquals(GameplayModule.TOGGLE_SPRINT,HudSection.PLAYER.catalog("sprint").get(0).game);
        assertEquals(GameplayModule.FREE_LOOK,HudSection.VISUALS.catalog("freelook").get(0).game);
    }
    @Test public void switchesMoveSmoothlyAndReverseWithoutJumping() {
        HudSwitch.Motion m=new HudSwitch.Motion(false);
        assertEquals(0,m.sample(false,1_000_000_000L),0);
        float a=m.sample(true,1_010_000_000L);assertTrue(a>0&&a<1);
        float b=m.sample(false,1_020_000_000L);assertTrue(b>0&&b<a);
        for(int i=1;i<40;i++)m.sample(true,1_020_000_000L+i*10_000_000L);
        assertEquals(1,m.sample(true,2_000_000_000L),.001);
    }
    @Test public void visualModsHaveHomeCardsAndLocalizedSearchInTheirCategories() {
        List<CatalogModule> home=HudSection.HOME.catalog("");
        assertEquals(GameplayModule.FULLBRIGHT,home.get(4).game);assertEquals(GameplayModule.TIME_CHANGER,home.get(5).game);
        assertEquals(GameplayModule.FULLBRIGHT,HudSection.VISUALS.catalog("яркость").get(0).game);
        assertEquals(GameplayModule.TIME_CHANGER,HudSection.WORLD.catalog("закат").get(0).game);
        assertTrue(HudSection.PLAYER.catalog("fullbright").isEmpty());
    }
}
