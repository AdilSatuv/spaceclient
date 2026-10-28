package dev.spaceclient.hud;

/** Two deliberate activations are required before replacing a profile or its layout. */
final class HudEditorConfirmation {
    private int pending;

    boolean confirm(int action) {
        if (action != 45 && action != 47) throw new IllegalArgumentException("Not a protected action");
        if (pending == action) {
            pending = 0;
            return true;
        }
        pending = action;
        return false;
    }

    boolean isPending(int action) { return pending != 0 && pending == action; }

    boolean cancel() {
        boolean wasPending = pending != 0;
        pending = 0;
        return wasPending;
    }

    String message() {
        if (pending == 45) return "Reset all: repeat Home or Yes; Esc cancels";
        if (pending == 47) return "Again: Import/I replaces profile. Esc cancels";
        return "";
    }
}
