package dev.spaceclient;

/** Accepts a fresh key-down once when vanilla dispatches the same LWJGL event twice. */
public final class KeyPressGate {
    private boolean seen;
    private long lastEventTime;

    public boolean accept(long eventTime, boolean pressed, boolean repeat) {
        if (!pressed) {
            seen = false;
            return false;
        }
        if (repeat || (seen && lastEventTime == eventTime)) return false;
        seen = true;
        lastEventTime = eventTime;
        return true;
    }
}
