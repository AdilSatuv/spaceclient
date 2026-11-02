package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import dev.spaceclient.mods.GameplayModule;
import dev.spaceclient.mods.ModSettings;

/** One searchable card; gameplay state never enters HUD placement/profile serialization. */
final class CatalogModule {
    final HudModule hud;
    final GameplayModule game;
    private CatalogModule(HudModule hud, GameplayModule game) { this.hud = hud; this.game = game; }
    static CatalogModule hud(HudModule module) { return new CatalogModule(module, null); }
    static CatalogModule game(GameplayModule module) { return new CatalogModule(null, module); }
    String id() { return hud == null ? "game." + game.name() : "hud." + hud.getId(); }
    String getTitle() { return hud == null ? game.title : hud.getTitle(); }
    String getDescription() { return hud == null ? game.description : hud.getDescription(); }
    String getDisplayDescription() { return hud == null ? game.description : hud.getDisplayDescription(); }
    boolean isEnabled(HudConfig h, ModSettings m) { return hud == null ? game.enabled(m) : hud.isEnabled(h); }
}
