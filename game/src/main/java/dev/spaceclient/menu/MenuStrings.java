package dev.spaceclient.menu;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import net.minecraft.client.MinecraftClient;

/** Legacy Fabric does not automatically add mod assets to Minecraft 1.8.9's resource packs. */
public final class MenuStrings {
    private static final Properties ENGLISH = load("en_US");
    private static final Properties RUSSIAN = load("ru_RU");
    private MenuStrings() { }
    private static Properties load(String locale) {
        Properties result = new Properties();
        try (InputStream stream = MenuStrings.class.getResourceAsStream("/assets/spaceclient/lang/" + locale + ".lang")) {
            if (stream != null) result.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
        return result;
    }
    public static String get(MinecraftClient client, String suffix) {
        String key = "space.menu." + suffix;
        boolean russian = client.getLanguageManager().getLanguage().getCode().equalsIgnoreCase("ru_RU");
        return (russian ? RUSSIAN : ENGLISH).getProperty(key, ENGLISH.getProperty(key, ""));
    }
}
