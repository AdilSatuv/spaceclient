package dev.spaceclient.freelook;

/** State of the held free camera; never mutates the player's actual rotation. */
public final class FreeLookState {
    private boolean active;
    private int savedPerspective;
    private float yaw, pitch;

    /** Returns the view to use, preserving a deliberate F5 change made while active. */
    public int update(boolean held, int perspective, float playerYaw, float playerPitch) {
        if (held && !active) {
            active = true;
            savedPerspective = perspective;
            yaw = playerYaw;
            pitch = playerPitch;
            return 1;
        }
        if (!held && active) {
            active = false;
            return perspective == 1 ? savedPerspective : perspective;
        }
        return perspective;
    }

    public void turn(float deltaX, float deltaY) {
        if (!active) return;
        yaw = (float) (yaw + deltaX * 0.15d);
        pitch = Math.max(-90, Math.min(90, (float) (pitch - deltaY * 0.15d)));
    }
    public boolean active() { return active; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
}
