package dev.spaceclient.mods;

/** Supplies an optional sprint-key signal without changing vanilla movement rules. */
public final class AutoSprintPolicy {
    private AutoSprintPolicy() { }

    public static boolean sprintKey(boolean physicalKey, boolean enabled, boolean inWorld,
                                    boolean focused, boolean screenOpen) {
        return physicalKey || (enabled && inWorld && focused && !screenOpen);
    }
}
