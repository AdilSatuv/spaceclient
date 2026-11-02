package dev.spaceclient.hud;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class KeystrokesHudTest {
    @Test public void keepsEveryCellWidthStableWhenPressedAndShowsJumpAndMouseBindings() {
        java.util.function.ToIntFunction<String> measure=s->{int width=0;for(char c:s.toCharArray())width+=c==' '?4:c=='['||c==']'?3:6;return width;};
        KeystrokesHud.Key up=new KeystrokesHud.Key("UP",false),held=new KeystrokesHud.Key("UP",true);
        assertEquals(KeystrokesHud.cellWidth(up,measure),KeystrokesHud.cellWidth(held,measure));
        assertEquals("[SPACE]  M1  [K]",KeystrokesHud.text(new KeystrokesHud.Key[]{new KeystrokesHud.Key("SPACE",true),new KeystrokesHud.Key("M1",false),new KeystrokesHud.Key("K",true)}));
        assertEquals("AB",KeystrokesHud.safeLabel("A\u202EB"));
    }
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
