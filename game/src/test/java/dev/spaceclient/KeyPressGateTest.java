package dev.spaceclient;

import org.junit.Test;
import static org.junit.Assert.*;

public class KeyPressGateTest {
    @Test public void openingEventIsNotHandledAgainByNewScreen() {
        KeyPressGate gate = new KeyPressGate();
        assertTrue(gate.accept(0L, true, false));
        assertFalse(gate.accept(0L, true, false));
        assertFalse(gate.accept(1L, false, false));
        assertTrue(gate.accept(2L, true, false));
        assertFalse(gate.accept(2L, true, false));
    }

    @Test public void holdingShortcutDoesNotToggleOnRepeatEvents() {
        KeyPressGate gate = new KeyPressGate();
        assertTrue(gate.accept(10L, true, false));
        assertFalse(gate.accept(11L, true, true));
        assertFalse(gate.accept(12L, true, true));
        assertFalse(gate.accept(13L, false, false));
        assertTrue(gate.accept(14L, true, false));
    }

    @Test public void releaseAllowsNewPressEvenWithSameTimestamp() {
        KeyPressGate gate = new KeyPressGate();
        assertTrue(gate.accept(10L, true, false));
        assertFalse(gate.accept(10L, false, false));
        assertTrue(gate.accept(10L, true, false));
    }

    @Test public void freshEventRecoversWhenFocusLossSwallowsRelease() {
        KeyPressGate gate = new KeyPressGate();
        assertTrue(gate.accept(10L, true, false));
        assertTrue(gate.accept(20L, true, false));
        assertFalse(gate.accept(20L, true, false));
    }
}
