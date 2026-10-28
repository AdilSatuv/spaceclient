package dev.spaceclient.hud;

/** Four cards fit the established 720p / Auto menu; navigation uses absolute indexes. */
public final class HudModulePages {
    public static final int PAGE_SIZE = 4;
    private HudModulePages() { }

    public static int count(int modules) {
        return count(modules, PAGE_SIZE);
    }
    public static int count(int modules, int pageSize) {
        if (modules < 0) throw new IllegalArgumentException("Invalid module count");
        if (pageSize < 1) throw new IllegalArgumentException("Invalid page size");
        return Math.max(1, (modules + pageSize - 1) / pageSize);
    }

    public static int clamp(int page, int modules) {
        return Math.max(0, Math.min(page, count(modules) - 1));
    }

    public static int move(int page, int delta, int modules) {
        return Math.floorMod(clamp(page, modules) + delta, count(modules));
    }

    public static int first(int page, int modules) { return clamp(page, modules) * PAGE_SIZE; }
    public static int end(int page, int modules) { return Math.min(modules, first(page, modules) + PAGE_SIZE); }
    public static int forIndex(int index, int modules) {
        return forIndex(index, modules, PAGE_SIZE);
    }
    public static int clamp(int page, int modules, int size) { return Math.max(0, Math.min(page, count(modules, size) - 1)); }
    public static int move(int page, int delta, int modules, int size) { return Math.floorMod(clamp(page, modules, size) + delta, count(modules, size)); }
    public static int first(int page, int modules, int size) { return clamp(page, modules, size) * size; }
    public static int end(int page, int modules, int size) { return Math.min(modules, first(page, modules, size) + size); }
    public static int forIndex(int index, int modules, int size) {
        if (index < 0 || index >= modules) throw new IllegalArgumentException("Invalid module index");
        if (size < 1) throw new IllegalArgumentException("Invalid page size");
        return index / size;
    }
}
