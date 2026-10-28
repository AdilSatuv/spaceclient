package dev.spaceclient.hud;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class KeystrokesHudTest {
    @Test public void describesActualBindingsAndPressedState() {
        assertEquals("[UP]  Mouse 4  [S]  RIGHT ", KeystrokesHud.text("UP", true, "Mouse 4", false, "S", true, "RIGHT", false));
    }

    @Test public void boundsAndSanitizesLabels() {
        assertEquals("?", KeystrokesHud.safeLabel(null));
        assertEquals("?", KeystrokesHud.safeLabel("\u00a7\n"));
        assertEquals("abc", KeystrokesHud.safeLabel(" a\u00a7b\nc "));
        assertEquals("123456789012345678", KeystrokesHud.safeLabel("12345678901234567890"));
        assertEquals("🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀", KeystrokesHud.safeLabel("🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀"));
    }
}
