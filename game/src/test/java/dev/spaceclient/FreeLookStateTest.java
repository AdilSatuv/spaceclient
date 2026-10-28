package dev.spaceclient;

import dev.spaceclient.freelook.FreeLookState;
import org.junit.Test;
import static org.junit.Assert.*;

public class FreeLookStateTest {
    @Test public void restoresOriginalViewAndDoesNotMovePlayer() {
        FreeLookState state = new FreeLookState();
        assertEquals(1, state.update(true, 0, 35, -20));
        state.turn(20, -10);
        assertEquals(38, state.yaw(), 0.001);
        assertEquals(-18.5f, state.pitch(), 0.001);
        assertEquals(0, state.update(false, 1, 35, -20));
        assertFalse(state.active());
    }
    @Test public void clampsPitchAndKeepsAViewChangedWhileHeld() {
        FreeLookState state = new FreeLookState();
        assertEquals(1, state.update(true, 2, 0, 0));
        state.turn(0, -10000);
        assertEquals(90, state.pitch(), 0.001);
        state.turn(0, 20000);
        assertEquals(-90, state.pitch(), 0.001);
        assertEquals(2, state.update(false, 2, 0, 0));
    }
    @Test public void stoppingForMenuOrLostFocusRestoresPriorPerspective() {
        FreeLookState state = new FreeLookState();
        assertEquals(1, state.update(true, 2, 0, 0));
        assertEquals(2, state.update(false, 1, 0, 0));
        assertFalse(state.active());
    }
}
