package dev.spaceclient.menu;

import dev.spaceclient.hud.SpaceUiFont;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class MenuStringsTest {
    private Properties language(String locale) throws Exception {
        Properties p=new Properties();
        try(InputStream in=getClass().getResourceAsStream("/assets/spaceclient/lang/"+locale+".lang")) {
            assertNotNull(in);p.load(new InputStreamReader(in,StandardCharsets.UTF_8));
        }
        return p;
    }
    @Test public void bothLanguagesCoverEverySharedLabelAndFallbackUsesEnglish() throws Exception {
        Properties en=language("en_US"),ru=language("ru_RU");
        assertEquals(en.stringPropertyNames(),ru.stringPropertyNames());
        for(String key:en.stringPropertyNames()) {
            assertFalse(en.getProperty(key).trim().isEmpty());assertFalse(ru.getProperty(key).trim().isEmpty());
            assertEquals(en.getProperty(key),MenuStrings.get("de_DE",key.substring("space.menu.".length())));
        }
        assertEquals("Модули",MenuStrings.get("ru_RU","catalog.title"));
    }
    @Test public void filterLabelsAndEditorFitTheirCompactHitboxes() {
        for(String locale:new String[]{"en_US","ru_RU"}) {
            for(String filter:new String[]{"all","enabled","favorites"})
                assertTrue(locale+" "+filter,SpaceUiFont.width(MenuStrings.get(locale,"catalog.filter."+filter))<=47);
            assertTrue(SpaceUiFont.width(MenuStrings.get(locale,"catalog.edit"))<=74);
        }
    }
}
