package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.*;

/** Font metrics stay independent of an OpenGL context, including when text is trimmed. */
public class SpaceUiFontTest {
    @Test public void bundledFontMeasuresLatinAndCyrillicWithoutAGraphicsContext() {
        assertTrue(SpaceUiFont.width("Settings") > 0);
        assertTrue(SpaceUiFont.width("Настройки") > 0);
        assertTrue(SpaceUiFont.height() > 0);
        assertEquals(0, SpaceUiFont.width(""));
    }
    @Test public void trimmingFitsAndDoesNotSeparateCombiningMarks() {
        String text = "Cafe\u0301 HUD";
        int limit = SpaceUiFont.width("Cafe\u0301");
        String result = SpaceUiFont.trimToWidth(text, limit);
        assertEquals("Cafe\u0301", result);
        assertTrue(SpaceUiFont.width(result) <= limit);
        assertEquals("", SpaceUiFont.trimToWidth(text, 0));
    }
}
