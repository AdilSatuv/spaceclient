package dev.spaceclient.hud;

/** The synchronized level and fraction of the current level, never total earned XP. */
public final class ExperienceProgress {
    public final boolean available;
    public final int level;
    public final float progress;

    private ExperienceProgress(int level, float progress) {
        available = level >= 0 && Float.isFinite(progress) && progress >= 0 && progress <= 1;
        this.level = available ? level : 0;
        this.progress = available ? progress : 0;
    }
    public static ExperienceProgress of(int level, float progress) { return new ExperienceProgress(level, progress); }
    public static ExperienceProgress unavailable() { return new ExperienceProgress(-1, 0); }
    public int percent() { return (int)Math.floor(progress * 100); }
    public String text() { return available ? "XP Lv. " + level + " | " + percent() + "%" : "XP --"; }
}
