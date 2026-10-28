package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudEditorConfirmationTest {
    @Test public void eachOperationNeedsTwoActivationsAndClearsAfterConfirmation() {
        HudEditorConfirmation confirmation = new HudEditorConfirmation();
        for (int action : new int[] {45, 47}) {
            assertFalse(confirmation.confirm(action));
            assertTrue(confirmation.isPending(action));
            assertFalse(confirmation.message().isEmpty());
            assertTrue(confirmation.confirm(action));
            assertFalse(confirmation.isPending(action));
            assertEquals("", confirmation.message());
        }
    }

    @Test public void changingActionOrCancellingCannotConfirmAnEarlierAction() {
        HudEditorConfirmation confirmation = new HudEditorConfirmation();
        assertFalse(confirmation.confirm(45));
        assertFalse(confirmation.confirm(47));
        assertFalse(confirmation.isPending(45));
        assertTrue(confirmation.isPending(47));
        assertTrue(confirmation.cancel());
        assertFalse(confirmation.cancel());
        assertFalse(confirmation.confirm(47));
        assertFalse(confirmation.confirm(45));
        assertTrue(confirmation.confirm(45));
    }
}
