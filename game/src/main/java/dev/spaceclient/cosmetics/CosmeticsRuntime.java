package dev.spaceclient.cosmetics;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DownloadedSkinParser;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class CosmeticsRuntime {
    private static volatile Data data;
    private static final class Data {
        final CosmeticsSettings settings;
        final AppearanceProfile appearance;
        Identifier skinTexture,capeTexture;
        boolean skinAttempted,capeAttempted;
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
    public static boolean localPlayer(AbstractClientPlayerEntity player) {
        return player!=null&&MinecraftClient.getInstance().player==player;
    }
    /** Called by the render thread only. Files were validated and decoded once at startup. */
    public static Identifier skin(AbstractClientPlayerEntity player) {
        if(!localPlayer(player))return null;
        CosmeticsSettings.State state=settings().view();
        if(!state.enabled||!state.skin)return null;
        if(!data.skinAttempted) {
            data.skinAttempted=true;
            if(data.appearance.hasSkin())try {
                data.skinTexture=MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("space_profile_skin",
                    new NativeImageBackedTexture(new DownloadedSkinParser().parseSkin(data.appearance.skin)));
            } catch(RuntimeException failure) { data.skinTexture=null; }
        }
        return data.skinTexture;
    }
    public static Identifier cape(AbstractClientPlayerEntity player) {
        if(!localPlayer(player))return null;
        CosmeticsSettings.State state=settings().view();
        if(!state.enabled||!state.cape)return null;
        if(!data.capeAttempted) {
            data.capeAttempted=true;
            if(data.appearance.hasCape())try {
                data.capeTexture=MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("space_profile_cape",
                    new NativeImageBackedTexture(data.appearance.cape));
            } catch(RuntimeException failure) { data.capeTexture=null; }
        }
        return data.capeTexture;
    }
}
