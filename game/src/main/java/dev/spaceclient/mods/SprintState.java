package dev.spaceclient.mods;

/** Toggle state is a session value; module enablement and mode remain persisted settings. */
public final class SprintState {
    private boolean toggled;
    public void press() { toggled = !toggled; }
    public void reset() { toggled = false; }
    public boolean requested(boolean enabled, ModSettings.SprintMode mode) {
        return enabled && (mode == ModSettings.SprintMode.AUTO || toggled);
    }
}
