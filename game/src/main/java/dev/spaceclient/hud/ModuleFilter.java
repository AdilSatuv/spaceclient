package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import dev.spaceclient.mods.ModSettings;
import java.util.ArrayList;
import java.util.List;

enum ModuleFilter {
    ALL("All"), ENABLED("Enabled"), FAVORITES("Favorites");
    final String label;
    ModuleFilter(String label) { this.label = label; }
    List<CatalogModule> apply(List<CatalogModule> source, HudConfig hud, ModSettings mods, ModuleBrowserSettings browser) {
        List<CatalogModule> result = new ArrayList<CatalogModule>();
        for (CatalogModule module : source)
            if (this == ALL || (this == ENABLED && module.isEnabled(hud, mods))
                    || (this == FAVORITES && browser.favorite(module.id()))) result.add(module);
        return result;
    }
}
