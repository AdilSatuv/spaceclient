package dev.spaceclient.menu;

import java.util.Arrays;
import java.util.Collections;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.junit.Test;
import static org.junit.Assert.*;

public class MenuTest {
    @Test public void cardDescriptionsHaveEnglishAndRussianResources() throws Exception {
        for (String locale : new String[] {"en_US", "ru_RU"}) {
            java.util.Properties strings = new java.util.Properties();
            try (java.io.InputStream stream = getClass().getResourceAsStream("/assets/spaceclient/lang/" + locale + ".lang")) {
                assertNotNull(stream);
                strings.load(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
            }
            for (String key : new String[] {"keyboard", "worlds", "servers", "realms", "settings", "language", "quit"})
                assertFalse("Missing " + locale + "/" + key, strings.getProperty("space.menu." + key, "").isEmpty());
        }
    }
    @Test public void menuFitsScaledWindowsAndKeepsHeadingAndFooterClear() {
        for (int w : new int[] {240, 320, 427, 640, 960, 1920})
            for (int h : new int[] {220, 240, 270, 300, 360, 540, 1080})
                for (int count : new int[] {5, 6}) {
                    MenuLayout l = new MenuLayout(w, h, count);
                    assertTrue(l.left - 12 >= 0);
                    assertTrue(l.left + l.width + 12 <= w);
                    assertTrue(l.panelTop + 31 + 14 < l.top);
                    assertTrue(l.top + (count - 1) * l.step + l.buttonHeight <= l.panelBottom);
                    assertTrue(l.panelBottom <= h - 16);
                    assertTrue(l.step >= 22);
                }
    }
    @Test public void traversalWrapsAndSkipsUnavailableActionsInBothDirections() {
        ButtonWidget first = new ButtonWidget(1, 0, 0, "Play");
        ButtonWidget disabled = new ButtonWidget(2, 0, 0, "Unavailable");
        ButtonWidget hidden = new ButtonWidget(3, 0, 0, "Hidden");
        ButtonWidget last = new ButtonWidget(4, 0, 0, "Quit");
        disabled.active = false; hidden.visible = false;
        java.util.List<ButtonWidget> buttons = Arrays.asList(first, disabled, hidden, last);
        assertSame(first, MenuNavigation.next(buttons, null, 1));
        assertSame(last, MenuNavigation.next(buttons, null, -1));
        assertSame(last, MenuNavigation.next(buttons, first, 1));
        assertSame(first, MenuNavigation.next(buttons, last, 1));
        assertSame(first, MenuNavigation.next(buttons, last, -1));
        assertSame(last, MenuNavigation.next(buttons, first, -1));
        assertNull(MenuNavigation.next(Arrays.asList(disabled, hidden), null, 1));
        assertNull(MenuNavigation.next(Collections.<ButtonWidget>emptyList(), null, -1));
    }
    @Test public void injectionTargetsExistInPinnedMinecraftWithoutInitializingGame() throws Exception {
        // Resolves pinned mappings/signatures only. Does not prove runtime mixin application or rendering.
        ClassLoader loader = getClass().getClassLoader();
        Class<?> screen = Class.forName("net.minecraft.client.gui.screen.Screen", false, loader);
        Class<?> title = Class.forName("net.minecraft.client.gui.screen.TitleScreen", false, loader);
        Class<?> client = Class.forName("net.minecraft.client.MinecraftClient", false, loader);
        assertEquals(void.class, screen.getDeclaredMethod("renderBackground", int.class).getReturnType());
        assertEquals(void.class, screen.getDeclaredMethod("keyPressed", char.class, int.class).getReturnType());
        assertEquals(void.class, title.getDeclaredMethod("render", int.class, int.class, float.class).getReturnType());
        assertEquals(void.class, title.getDeclaredMethod("init").getReturnType());
        assertEquals(void.class, title.getDeclaredMethod("removed").getReturnType());
        assertEquals(String.class, title.getDeclaredField("oldGl1").getType());
        assertEquals(screen, title.getDeclaredField("realmsNotificationScreen").getType());
        assertEquals(boolean.class, title.getDeclaredMethod("areRealmsNotificationsEnabled").getReturnType());
        Method render = ButtonWidget.class.getDeclaredMethod("render", client, int.class, int.class);
        assertTrue(Modifier.isPublic(render.getModifiers()));
    }
    @Test public void pauseTraversalFollowsVisualRowsIncludingSpaceButtons() {
        ButtonWidget top = new ButtonWidget(1, 20, 10, "Back");
        ButtonWidget left = new ButtonWidget(2, 20, 40, "HUD");
        ButtonWidget right = new ButtonWidget(3, 122, 40, "Mods");
        ButtonWidget bottom = new ButtonWidget(4, 20, 70, "Options");
        java.util.List<ButtonWidget> vanillaOrder = Arrays.asList(top, bottom, right, left);
        assertSame(left, MenuNavigation.next(vanillaOrder, top, 1));
        assertSame(right, MenuNavigation.next(vanillaOrder, left, 1));
        assertSame(bottom, MenuNavigation.next(vanillaOrder, right, 1));
        assertSame(right, MenuNavigation.next(vanillaOrder, bottom, -1));
    }
}
