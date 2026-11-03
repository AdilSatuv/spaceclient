package dev.spaceclient.cosmetics;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class CosmeticsRuntime {
    private static volatile Data data;
    private static final class Data {
        final CosmeticsSettings settings;
        final AppearanceProfile appearance;
        Data(CosmeticsSettings settings,AppearanceProfile appearance) { this.settings=settings;this.appearance=appearance; }
    }
    private CosmeticsRuntime() { }
    public static synchronized void initialize(Path config) {
        if(data!=null)return;
        CosmeticsSettings settings=CosmeticsSettings.open(config.resolve("spaceclient-cosmetics.properties"));
        AppearanceProfile appearance=AppearanceProfile.load(config);
        data=new Data(settings,appearance);
    }
    public static CosmeticsSettings settings() { if(data==null)initialize(FabricLoader.getInstance().getConfigDir());return data.settings; }
    public static AppearanceProfile appearance() { settings();return data.appearance; }
}
