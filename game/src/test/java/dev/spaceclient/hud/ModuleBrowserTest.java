package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import dev.spaceclient.mods.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModuleBrowserTest {
    @Test public void clientPackHasSearchAliasesAndAppropriateSections()throws Exception{
        assertEquals(GameplayModule.GUI,HudSection.MISC.catalog("масштаб").get(0).game);assertEquals(GameplayModule.AUTO_TEXT,HudSection.MISC.catalog("auto text").get(0).game);assertEquals(GameplayModule.HITBOXES,HudSection.COMBAT.catalog("hit box").get(0).game);assertEquals(GameplayModule.ITEM_PHYSICS,HudSection.WORLD.catalog("item physics").get(0).game);
        assertTrue(HudSection.VISUALS.catalog("auto text").isEmpty());assertTrue(HudSection.VISUALS.catalog("sounds").isEmpty());ModuleBrowserSettings s=ModuleBrowserSettings.open(Files.createTempDirectory("space-pack-favs").resolve("browser"));for(GameplayModule m:GameplayModule.values())assertTrue(s.toggleFavorite("game."+m.name()));
    }
    @Test public void newVisualModsHaveSearchAliasesSectionsAndFavorites()throws Exception {
        assertEquals(GameplayModule.BLOCK_OVERLAY,HudSection.VISUALS.catalog("заливка").get(0).game);
        assertEquals(GameplayModule.HIT_COLOR,HudSection.COMBAT.catalog("hit color").get(0).game);
        assertEquals(GameplayModule.FOV_CONTROL,HudSection.PLAYER.catalog("fov control").get(0).game);
        assertTrue(HudSection.MISC.catalog("block overlay").isEmpty());assertTrue(HudSection.WORLD.catalog("hit color").isEmpty());
        ModuleBrowserSettings s=ModuleBrowserSettings.open(Files.createTempDirectory("space-style-favs").resolve("browser"));
        for(GameplayModule m:new GameplayModule[]{GameplayModule.BLOCK_OVERLAY,GameplayModule.HIT_COLOR,GameplayModule.FOV_CONTROL})assertTrue(s.toggleFavorite("game."+m.name()));
    }
    @Test public void newUtilityModsAreSearchableInAppropriateSectionsAndCanBeFavorited() throws Exception {
        assertEquals(55,HudSection.HOME.catalog("").size());
        assertEquals(GameplayModule.CHAT_TIMESTAMPS,HudSection.MISC.catalog("чат").get(0).game);
        assertEquals(GameplayModule.DURABILITY_WARNING,HudSection.PLAYER.catalog("прочность").get(0).game);
        assertTrue(HudSection.VISUALS.catalog("timestamps").isEmpty());assertTrue(HudSection.WORLD.catalog("durability warning").isEmpty());
        ModuleBrowserSettings s=ModuleBrowserSettings.open(Files.createTempDirectory("space-new-utils").resolve("browser"));
        assertTrue(s.toggleFavorite("game.CHAT_TIMESTAMPS"));assertTrue(s.toggleFavorite("game.DURABILITY_WARNING"));
        assertTrue(s.favorite("game.CHAT_TIMESTAMPS"));assertTrue(s.favorite("game.DURABILITY_WARNING"));
    }
    @Test public void favoritesPersistWithoutEnablingModulesAndCombineWithSearchAndCategory() throws Exception {
        Path dir=Files.createTempDirectory("space-browser");
        ModSettings mods=ModSettings.open(dir.resolve("mods")); HudConfig hud=new HudConfig();
        ModuleBrowserSettings prefs=ModuleBrowserSettings.open(dir.resolve("browser"));
        assertTrue(prefs.toggleFavorite("game.ZOOM")); assertTrue(prefs.toggleFavorite("hud.ping"));
        prefs=ModuleBrowserSettings.open(dir.resolve("browser"));
        assertTrue(prefs.favorite("game.ZOOM")); assertFalse(mods.zoom()); assertFalse(hud.active().ping);
        List<CatalogModule> favorites=ModuleFilter.FAVORITES.apply(HudSection.HOME.catalog(""),hud,mods,prefs);
        assertEquals(2,favorites.size()); assertEquals("game.ZOOM",favorites.get(0).id());
        assertEquals(1,ModuleFilter.FAVORITES.apply(HudSection.HOME.catalog("zoom"),hud,mods,prefs).size());
        assertTrue(ModuleFilter.FAVORITES.apply(HudSection.PLAYER.catalog(""),hud,mods,prefs).isEmpty());
        assertTrue(prefs.toggleFavorite("game.ZOOM")); assertFalse(ModuleBrowserSettings.open(dir.resolve("browser")).favorite("game.ZOOM"));
    }
    @Test public void enabledFilterTracksGameplayAndActiveHudProfileEvenWhenMasterIsHidden() throws Exception {
        Path dir=Files.createTempDirectory("space-filter");ModSettings mods=ModSettings.open(dir.resolve("mods"));
        HudConfig hud=new HudConfig();ModuleBrowserSettings prefs=ModuleBrowserSettings.open(dir.resolve("browser"));
        mods.toggleZoom();hud.active().ping=true;hud.active().enabled=false;
        List<CatalogModule> list=ModuleFilter.ENABLED.apply(HudSection.HOME.catalog(""),hud,mods,prefs);
        assertTrue(list.stream().anyMatch(m->m.game==GameplayModule.ZOOM));
        assertTrue(list.stream().anyMatch(m->m.hud==HudModule.PING));
        hud.select(1);mods.toggleZoom();
        list=ModuleFilter.ENABLED.apply(HudSection.HOME.catalog(""),hud,mods,prefs);
        assertFalse(list.stream().anyMatch(m->m.game==GameplayModule.ZOOM||m.hud==HudModule.PING));
    }
    @Test public void failedOrUnsupportedFavoriteFilesArePreserved() throws Exception {
        Path dir=Files.createTempDirectory("space-browser-fail"),file=dir.resolve("browser");
        byte[] future="schemaVersion=2\nfavorites=game.ZOOM\n".getBytes(StandardCharsets.ISO_8859_1);Files.write(file,future);
        ModuleBrowserSettings s=ModuleBrowserSettings.open(file);assertFalse(s.toggleFavorite("hud.ping"));
        assertArrayEquals(future,Files.readAllBytes(file));assertFalse(s.favorite("game.ZOOM"));
        Path blocked=Files.createTempFile("space-browser-blocked",".file");s=ModuleBrowserSettings.open(blocked.resolve("browser"));
        assertFalse(s.toggleFavorite("game.ZOOM"));assertFalse(s.favorite("game.ZOOM"));assertFalse(s.error().isEmpty());
    }
}
