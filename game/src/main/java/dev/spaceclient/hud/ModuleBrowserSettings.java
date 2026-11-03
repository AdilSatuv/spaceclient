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
    public enum View { GRID, LIST }
    private View view=View.GRID;
    private boolean brand,legacy;
    private final List<Set<String>> locked=Arrays.asList(new TreeSet<String>(),new TreeSet<String>());
    private String error = "";
    private ModuleBrowserSettings(Path file) { this.file = file; }
    public static ModuleBrowserSettings open(Path file) {
        ModuleBrowserSettings s = new ModuleBrowserSettings(file);
        try {
            if (!Files.exists(file)) return s;
            if (Files.size(file) > 4096) throw new IOException();
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            String version=p.getProperty("schemaVersion");
            s.legacy="1".equals(version);
            if ((!s.legacy&&!"2".equals(version)) || p.size()!=(s.legacy?2:6) || p.getProperty("favorites")==null) throw new IOException();
            String ids = p.getProperty("favorites");
            if (!ids.isEmpty()) for (String id : ids.split(",", -1)) {
                if (!valid(id) || !s.favorites.add(id)) throw new IOException();
            }
            if(!s.legacy) {
                s.view=View.valueOf(p.getProperty("view",""));
                String brand=p.getProperty("brand");if(!"true".equals(brand)&&!"false".equals(brand))throw new IOException();
                s.brand=Boolean.parseBoolean(brand);
                for(int i=0;i<2;i++) {
                    String list=p.getProperty("locked."+i);if(list==null)throw new IOException();
                    if(!list.isEmpty())for(String id:list.split(",",-1))if(!hudId(id)||!s.locked.get(i).add(id))throw new IOException();
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            s.favorites.clear();s.locked.get(0).clear();s.locked.get(1).clear();s.view=View.GRID;s.brand=false;
            s.readOnly = true; s.error = "Browser preferences unreadable; original kept.";
        }
        return s;
    }
    private static boolean valid(String id) {
        for (HudModule m : HudModule.values()) if (id.equals("hud." + m.getId())) return true;
        for (GameplayModule m : GameplayModule.values()) if (id.equals("game." + m.name())) return true;
        return false;
    }
    public boolean favorite(String id) { return favorites.contains(id); }
    public View view() { return view; }
    public boolean brand() { return brand; }
    public boolean isReadOnly() { return readOnly; }
    private static boolean hudId(String id) { for(HudModule m:HudModule.values())if(id.equals(m.getId()))return true;return false; }
    public boolean locked(int profile,HudModule module) { return locked.get(profile).contains(module.getId()); }
    public boolean view(View value) { if(value==null||readOnly)return false;return save(new TreeSet<>(favorites),value,brand,locked); }
    public boolean brand(boolean value) { return !readOnly&&save(new TreeSet<>(favorites),view,value,locked); }
    public boolean lock(int profile,HudModule module,boolean value) {
        if(readOnly||module==null||profile<0||profile>1)return false;
        List<Set<String>> next=Arrays.asList(new TreeSet<>(locked.get(0)),new TreeSet<>(locked.get(1)));
        if(value)next.get(profile).add(module.getId());else next.get(profile).remove(module.getId());
        return save(new TreeSet<>(favorites),view,brand,next);
    }
    public String error() { return error; }
    public boolean toggleFavorite(String id) {
        if (readOnly || !valid(id)) return false;
        Set<String> next = new TreeSet<String>(favorites);
        if (!next.remove(id)) next.add(id);
        return save(next,view,brand,locked);
    }
    private boolean save(Set<String> next,View nextView,boolean nextBrand,List<Set<String>> nextLocked) {
        Path tmp = null;
        try {
            Properties p = new Properties(); p.setProperty("schemaVersion", "2");
            p.setProperty("favorites", String.join(",", next));
            p.setProperty("view",nextView.name());p.setProperty("brand",Boolean.toString(nextBrand));
            for(int i=0;i<2;i++)p.setProperty("locked."+i,String.join(",",nextLocked.get(i)));
            Path parent = file.toAbsolutePath().getParent(); Files.createDirectories(parent);
            if(legacy) {
                Path backup=file.resolveSibling(file.getFileName()+".v1-"+UUID.randomUUID()+".bak");
                Files.copy(file,backup);
                if(!Arrays.equals(Files.readAllBytes(file),Files.readAllBytes(backup)))throw new IOException();
                legacy=false;
            }
            tmp = Files.createTempFile(parent, "space-browser-", ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) { p.store(out, "Space module favorites"); }
            try { Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
            Set<String> lock0=new TreeSet<>(nextLocked.get(0)),lock1=new TreeSet<>(nextLocked.get(1));
            favorites.clear(); favorites.addAll(next);view=nextView;brand=nextBrand;
            locked.get(0).clear();locked.get(0).addAll(lock0);locked.get(1).clear();locked.get(1).addAll(lock1);
            error = ""; return true;
        } catch (IOException ex) { error = "Could not save favorites; change reverted."; return false; }
        finally { if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignored) { } }
    }
}
