package dev.spaceclient.mods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.Consumer;

/** Gameplay settings remain independent of HUD profiles. Writes commit atomically. */
public final class ModSettings {
    public enum SprintMode { AUTO, TOGGLE }
    public enum SnapPerspective {
        FIRST("First person",0), BACK("Behind",1), FRONT("Front",2);
        public final String label; public final int view;
        SnapPerspective(String label,int view){this.label=label;this.view=view;}
    }
    public enum WeatherMode { CLEAR, RAIN, SNOW }
    public enum Brightness {
        SOFT(4), BRIGHT(8), FULL(16);
        public final float gamma;
        Brightness(float gamma) { this.gamma = gamma; }
    }
    public enum TimePreset {
        DAY(6000), SUNSET(12000), NIGHT(18000), SUNRISE(23000);
        public final long ticks;
        TimePreset(long ticks) { this.ticks = ticks; }
    }
    private final Path file;
    private Options value = new Options();
    private boolean readOnly;
    private String error = "";
    private static final class Options {
        boolean autoSprint, freeLook, zoom, crosshair, fullbright, timeChanger, noHurtCam, snaplook, weatherChanger;
        int snaplookKey; // Unbound: preserve all existing user hold/toggle bindings.
        SnapPerspective snapPerspective=SnapPerspective.FRONT;
        WeatherMode weatherMode=WeatherMode.CLEAR;
        Brightness brightness = Brightness.FULL;
        TimePreset timePreset = TimePreset.DAY;
        SprintMode sprintMode = SprintMode.AUTO;
        int freeLookKey = 47, zoomKey = 46, zoomFactor = 4; // LWJGL V / C.
        boolean zoomSmooth = true;
        CrosshairStyle style = CrosshairStyle.defaults();
        int[] toggleKeys = new int[GameplayModule.values().length];
        Options copy() {
            Options o = new Options();
            o.autoSprint = autoSprint; o.freeLook = freeLook; o.zoom = zoom; o.crosshair = crosshair;
            o.fullbright = fullbright; o.timeChanger = timeChanger; o.brightness = brightness; o.timePreset = timePreset;
            o.noHurtCam = noHurtCam;
            o.snaplook=snaplook;o.weatherChanger=weatherChanger;o.snaplookKey=snaplookKey;o.snapPerspective=snapPerspective;o.weatherMode=weatherMode;
            o.sprintMode = sprintMode; o.freeLookKey = freeLookKey; o.zoomKey = zoomKey;
            o.zoomFactor = zoomFactor; o.zoomSmooth = zoomSmooth; o.style = style;
            o.toggleKeys = toggleKeys.clone();
            return o;
        }
    }
    private ModSettings(Path file) { this.file = file; }
    public static ModSettings open(Path file) {
        ModSettings s = new ModSettings(file);
        try {
            if (!Files.exists(file)) return s;
            if (Files.size(file) > 4096) throw new IOException("File too large");
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            String schema = p.getProperty("schemaVersion", "");
            if (!schema.matches("[1-6]")) throw new IOException("Unsupported settings");
            int version = Integer.parseInt(schema);
            boolean legacy = version == 1;
            if (p.size() != (legacy ? 3 : version == 2 ? 17 : version == 3 ? 21 : version == 4 ? 27 : version==5 ? 29 : 36))
                throw new IOException("Unsupported settings");
            Options o = new Options();
            o.autoSprint = bool(p, "autoSprint"); o.freeLook = bool(p, "freeLook");
            if (!legacy) {
                o.zoom = bool(p, "zoom"); o.crosshair = bool(p, "crosshair");
                o.sprintMode = SprintMode.valueOf(p.getProperty("sprintMode", ""));
                o.freeLookKey = integer(p, "freeLookKey", 0, 255); o.zoomKey = integer(p, "zoomKey", 0, 255);
                o.zoomFactor = integer(p, "zoomFactor", 2, 8); o.zoomSmooth = bool(p, "zoomSmooth");
                o.style = new CrosshairStyle(CrosshairStyle.Shape.valueOf(p.getProperty("crosshairShape", "")),
                    CrosshairStyle.Color.valueOf(p.getProperty("crosshairColor", "")),
                    integer(p, "crosshairSize", 2, 12), integer(p, "crosshairGap", 0, 8),
                    integer(p, "crosshairThickness", 1, 4), bool(p, "crosshairDot"), bool(p, "crosshairOutline"));
            }
            if (version >= 3) for (GameplayModule m : GameplayModule.values())
                if (version >= 6 || (version==5 && m.ordinal()<GameplayModule.SNAPLOOK.ordinal())
                        || (version == 4 && m.ordinal() < GameplayModule.NO_HURT_CAM.ordinal())
                        || (version == 3 && m.ordinal() < GameplayModule.FULLBRIGHT.ordinal()))
                    o.toggleKeys[m.ordinal()] = integer(p, "toggleKey." + m.name(), 0, 255);
            if (version >= 4) {
                o.fullbright = bool(p, "fullbright"); o.timeChanger = bool(p, "timeChanger");
                o.brightness = Brightness.valueOf(p.getProperty("brightness", ""));
                o.timePreset = TimePreset.valueOf(p.getProperty("timePreset", ""));
            }
            if (version >= 5) o.noHurtCam = bool(p, "noHurtCam");
            if(version>=6) {
                o.snaplook=bool(p,"snaplook");o.weatherChanger=bool(p,"weatherChanger");o.snaplookKey=integer(p,"snaplookKey",0,255);
                o.snapPerspective=SnapPerspective.valueOf(p.getProperty("snapPerspective",""));o.weatherMode=WeatherMode.valueOf(p.getProperty("weatherMode",""));
            }
            s.value = o;
        } catch (IOException | IllegalArgumentException ex) {
            s.readOnly = true;
            s.error = "Mod settings unreadable; original kept (read-only).";
        }
        return s;
    }
    private static boolean bool(Properties p, String key) throws IOException {
        String v = p.getProperty(key);
        if (!"true".equals(v) && !"false".equals(v)) throw new IOException("Invalid boolean");
        return Boolean.parseBoolean(v);
    }
    private static int integer(Properties p, String key, int min, int max) {
        int v = Integer.parseInt(p.getProperty(key, ""));
        if (v < min || v > max) throw new IllegalArgumentException("Out of range");
        return v;
    }
    public boolean autoSprint() { return value.autoSprint; }
    public boolean freeLook() { return value.freeLook; }
    public boolean zoom() { return value.zoom; }
    public boolean crosshair() { return value.crosshair; }
    public boolean fullbright() { return value.fullbright; }
    public boolean timeChanger() { return value.timeChanger; }
    public boolean noHurtCam() { return value.noHurtCam; }
    public boolean snaplook(){return value.snaplook;}
    public boolean weatherChanger(){return value.weatherChanger;}
    public int snaplookKey(){return value.snaplookKey;}
    public SnapPerspective snapPerspective(){return value.snapPerspective;}
    public WeatherMode weatherMode(){return value.weatherMode;}
    public int holdKey(GameplayModule m) {
        switch(m){case FREE_LOOK:return freeLookKey();case ZOOM:return zoomKey();case SNAPLOOK:return snaplookKey();default:throw new IllegalArgumentException("Not a hold module");}
    }
    public boolean holdKey(GameplayModule m,int key) {
        switch(m){case FREE_LOOK:return freeLookKey(key);case ZOOM:return zoomKey(key);case SNAPLOOK:return snaplookKey(key);default:throw new IllegalArgumentException("Not a hold module");}
    }
    public Brightness brightness() { return value.brightness; }
    public TimePreset timePreset() { return value.timePreset; }
    /** Overrides the lightmap read only; never assigns to GameOptions.gamma. */
    public float renderGamma(float vanilla) { return fullbright() ? Math.max(vanilla, value.brightness.gamma) : vanilla; }
    public SprintMode sprintMode() { return value.sprintMode; }
    public int freeLookKey() { return value.freeLookKey; }
    public int zoomKey() { return value.zoomKey; }
    public int zoomFactor() { return value.zoomFactor; }
    public boolean zoomSmooth() { return value.zoomSmooth; }
    public CrosshairStyle crosshairStyle() { return value.style; }
    public int toggleKey(GameplayModule module) { return value.toggleKeys[module.ordinal()]; }
    public boolean toggleKey(GameplayModule module, int key) {
        return change(o -> { checkKey(key); o.toggleKeys[module.ordinal()] = key; });
    }
    public boolean isReadOnly() { return readOnly; }
    public String error() { return error; }
    public boolean toggleAutoSprint() { return change(o -> o.autoSprint = !o.autoSprint); }
    public boolean toggleFreeLook() { return change(o -> o.freeLook = !o.freeLook); }
    public boolean toggleZoom() { return change(o -> o.zoom = !o.zoom); }
    public boolean toggleCrosshair() { return change(o -> o.crosshair = !o.crosshair); }
    public boolean toggleFullbright() { return change(o -> o.fullbright = !o.fullbright); }
    public boolean toggleTimeChanger() { return change(o -> o.timeChanger = !o.timeChanger); }
    public boolean toggleNoHurtCam() { return change(o -> o.noHurtCam = !o.noHurtCam); }
    public boolean toggleSnaplook(){return change(o->o.snaplook=!o.snaplook);}
    public boolean toggleWeatherChanger(){return change(o->o.weatherChanger=!o.weatherChanger);}
    public boolean snaplookKey(int v){return change(o->{checkKey(v);o.snaplookKey=v;});}
    public boolean snapPerspective(SnapPerspective v){return change(o->{if(v==null)throw new IllegalArgumentException();o.snapPerspective=v;});}
    public boolean weatherMode(WeatherMode v){return change(o->{if(v==null)throw new IllegalArgumentException();o.weatherMode=v;});}
    public boolean brightness(Brightness v) { return change(o -> { if(v==null)throw new IllegalArgumentException(); o.brightness=v; }); }
    public boolean timePreset(TimePreset v) { return change(o -> { if(v==null)throw new IllegalArgumentException(); o.timePreset=v; }); }
    public boolean sprintMode(SprintMode v) { return change(o -> { if (v == null) throw new IllegalArgumentException(); o.sprintMode = v; }); }
    public boolean freeLookKey(int v) { return change(o -> { checkKey(v); o.freeLookKey = v; }); }
    public boolean zoomKey(int v) { return change(o -> { checkKey(v); o.zoomKey = v; }); }
    public boolean zoomFactor(int v) { return change(o -> { if (v < 2 || v > 8) throw new IllegalArgumentException(); o.zoomFactor = v; }); }
    public boolean zoomSmooth(boolean v) { return change(o -> o.zoomSmooth = v); }
    public boolean crosshairStyle(CrosshairStyle v) { return change(o -> { if (v == null) throw new IllegalArgumentException(); o.style = v; }); }
    public boolean reset(GameplayModule module) {
        return change(o -> {
            o.toggleKeys[module.ordinal()] = 0;
            switch (module) {
                case TOGGLE_SPRINT: o.sprintMode = SprintMode.AUTO; break;
                case FREE_LOOK: o.freeLookKey = 47; break;
                case ZOOM: o.zoomKey = 46; o.zoomFactor = 4; o.zoomSmooth = true; break;
                case CROSSHAIR: o.style = CrosshairStyle.defaults(); break;
                case FULLBRIGHT: o.brightness = Brightness.FULL; break;
                case TIME_CHANGER: o.timePreset = TimePreset.DAY; break;
                case NO_HURT_CAM: break;
                case SNAPLOOK: o.snaplookKey=0;o.snapPerspective=SnapPerspective.FRONT;break;
                case WEATHER_CHANGER: o.weatherMode=WeatherMode.CLEAR;break;
                default: throw new IllegalArgumentException();
            }
        });
    }
    private static void checkKey(int v) { if (v < 0 || v > 255) throw new IllegalArgumentException(); }
    private boolean change(Consumer<Options> mutation) {
        if (readOnly) return false;
        Path tmp = null;
        try {
            Options o = value.copy(); mutation.accept(o);
            Properties p = new Properties();
            p.setProperty("schemaVersion", "6");
            p.setProperty("snaplook",Boolean.toString(o.snaplook));p.setProperty("weatherChanger",Boolean.toString(o.weatherChanger));
            p.setProperty("snaplookKey",Integer.toString(o.snaplookKey));p.setProperty("snapPerspective",o.snapPerspective.name());p.setProperty("weatherMode",o.weatherMode.name());
            p.setProperty("noHurtCam", Boolean.toString(o.noHurtCam));
            p.setProperty("fullbright", Boolean.toString(o.fullbright)); p.setProperty("timeChanger", Boolean.toString(o.timeChanger));
            p.setProperty("brightness", o.brightness.name()); p.setProperty("timePreset", o.timePreset.name());
            for (GameplayModule m : GameplayModule.values()) p.setProperty("toggleKey." + m.name(), Integer.toString(o.toggleKeys[m.ordinal()]));
            p.setProperty("autoSprint", Boolean.toString(o.autoSprint)); p.setProperty("freeLook", Boolean.toString(o.freeLook));
            p.setProperty("zoom", Boolean.toString(o.zoom)); p.setProperty("crosshair", Boolean.toString(o.crosshair));
            p.setProperty("sprintMode", o.sprintMode.name());
            p.setProperty("freeLookKey", Integer.toString(o.freeLookKey)); p.setProperty("zoomKey", Integer.toString(o.zoomKey));
            p.setProperty("zoomFactor", Integer.toString(o.zoomFactor)); p.setProperty("zoomSmooth", Boolean.toString(o.zoomSmooth));
            p.setProperty("crosshairShape", o.style.shape.name()); p.setProperty("crosshairColor", o.style.color.name());
            p.setProperty("crosshairSize", Integer.toString(o.style.size)); p.setProperty("crosshairGap", Integer.toString(o.style.gap));
            p.setProperty("crosshairThickness", Integer.toString(o.style.thickness));
            p.setProperty("crosshairDot", Boolean.toString(o.style.dot)); p.setProperty("crosshairOutline", Boolean.toString(o.style.outline));
            Files.createDirectories(file.toAbsolutePath().getParent());
            tmp = Files.createTempFile(file.toAbsolutePath().getParent(), "space-mods-", ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) { p.store(out, "Space Client controls"); }
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
            value = o; error = "";
            return true;
        } catch (IOException | IllegalArgumentException ex) { error = "Could not save; change reverted."; return false; }
        finally { if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignored) { } }
    }
}
