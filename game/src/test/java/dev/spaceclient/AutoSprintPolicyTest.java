package dev.spaceclient;

import dev.spaceclient.mods.AutoSprintPolicy;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AutoSprintPolicyTest {
    @Test public void automaticSprintRequiresFocusedGameplay() {
        assertTrue(AutoSprintPolicy.sprintKey(false, true, true, true, false));
        assertFalse(AutoSprintPolicy.sprintKey(false, false, true, true, false));
        assertFalse(AutoSprintPolicy.sprintKey(false, true, false, true, false));
        assertFalse(AutoSprintPolicy.sprintKey(false, true, true, false, false));
        assertFalse(AutoSprintPolicy.sprintKey(false, true, true, true, true));
    }

    @Test public void physicalSprintRemainsVanillaEvenWhenFeatureIsOff() {
        assertTrue(AutoSprintPolicy.sprintKey(true, false, true, true, false));
        assertTrue(AutoSprintPolicy.sprintKey(true, true, false, false, true));
    }
}
