package dev.spaceclient;

import dev.spaceclient.hud.PotionHud;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PotionHudTest {
    @Test public void emptyUnavailableAndDurationBoundaries() {
        assertEquals("Effects --", PotionHud.text(null));
        assertEquals("Effects --", PotionHud.summarize(null));
        assertEquals("Effects: none", PotionHud.summarize(Collections.emptyList()));
        assertEquals("0:00", PotionHud.duration(19, false));
        assertEquals("0:01", PotionHud.duration(20, false));
        assertEquals("1:00", PotionHud.duration(1200, false));
        assertEquals("**:**", PotionHud.duration(Integer.MAX_VALUE, true));
        assertEquals("--:--", PotionHud.duration(-1, false));
    }

    @Test public void sortsByIdAndShowsHonestOverflowCount() {
        PotionHud.Effect poison = new PotionHud.Effect(19, "Poison", 1, 1200, false);
        PotionHud.Effect speed = new PotionHud.Effect(1, "Скорость", 0, 20, false);
        PotionHud.Effect strength = new PotionHud.Effect(5, "Strength", 2, 0, true);
        assertEquals("Effects: Скорость 1 0:01 | Strength 3 **:** +1",
            PotionHud.summarize(Arrays.asList(poison, null, speed, strength)));
    }

    @Test public void sanitizesControlAndFormattingCodesAndBoundsLongNames() {
        assertEquals("Effects: FastRed 1 0:00", PotionHud.summarize(Collections.singletonList(
            new PotionHud.Effect(1, "Fast\n§Red", 0, 0, false))));
        assertEquals("Effects: abcdefghijklmnopqrstuvwx ? --:--", PotionHud.summarize(Collections.singletonList(
            new PotionHud.Effect(1, "abcdefghijklmnopqrstuvwxyz", Integer.MAX_VALUE, -1, false))));
        assertEquals("Effects: Effect 9 1 0:00", PotionHud.summarize(Collections.singletonList(
            new PotionHud.Effect(9, " \n", 0, 0, false))));
    }
}
