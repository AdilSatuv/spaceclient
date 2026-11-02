package dev.spaceclient.hud;

import dev.spaceclient.mods.GameplayModule;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Bookmarks belong to the browser, independently of HUD profiles and enabled flags. */
public final class ModuleBrowserSettings {
    private final Path file;
    private final Set<String> favorites = new TreeSet<String>();
    private boolean readOnly;
    private String error = "";
    private ModuleBrowserSettings(Path file) { this.file = file; }
    public static ModuleBrowserSettings open(Path file) {
        ModuleBrowserSettings s = new ModuleBrowserSettings(file);
        try {
            if (!Files.exists(file)) return s;
            if (Files.size(file) > 4096) throw new IOException();
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            if (p.size() != 2 || !"1".equals(p.getProperty("schemaVersion")) || p.getProperty("favorites") == null) throw new IOException();
            String ids = p.getProperty("favorites");
            if (!ids.isEmpty()) for (String id : ids.split(",", -1)) {
                if (!valid(id) || !s.favorites.add(id)) throw new IOException();
            }
        } catch (IOException | IllegalArgumentException ex) {
            s.favorites.clear(); s.readOnly = true; s.error = "Favorites unreadable; original kept.";
        }
        return s;
    }
    private static boolean valid(String id) {
        for (HudModule m : HudModule.values()) if (id.equals("hud." + m.getId())) return true;
        for (GameplayModule m : GameplayModule.values()) if (id.equals("game." + m.name())) return true;
        return false;
    }
    public boolean favorite(String id) { return favorites.contains(id); }
    public String error() { return error; }
    public boolean toggleFavorite(String id) {
        if (readOnly || !valid(id)) return false;
        Set<String> next = new TreeSet<String>(favorites);
        if (!next.remove(id)) next.add(id);
        Path tmp = null;
        try {
            Properties p = new Properties(); p.setProperty("schemaVersion", "1");
            p.setProperty("favorites", String.join(",", next));
            Path parent = file.toAbsolutePath().getParent(); Files.createDirectories(parent);
            tmp = Files.createTempFile(parent, "space-browser-", ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) { p.store(out, "Space module favorites"); }
            try { Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
            favorites.clear(); favorites.addAll(next); error = ""; return true;
        } catch (IOException ex) { error = "Could not save favorites; change reverted."; return false; }
        finally { if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignored) { } }
    }
}
