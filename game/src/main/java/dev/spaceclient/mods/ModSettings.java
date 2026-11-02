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
    public enum SoundTheme { VANILLA, SOFT, CRISP, ARCADE }
    public enum KillChime {
        OFF("Off",""), ORB("Orb","random.orb"), PLING("Pling","note.pling"), LEVEL_UP("Level up","random.levelup");
        public final String label,id;
        KillChime(String label,String id){this.label=label;this.id=id;}
    }
    public enum SprintMode { AUTO, TOGGLE }
    public enum SnapPerspective {
        FIRST("First person",0), BACK("Behind",1), FRONT("Front",2);
        public final String label; public final int view;
        SnapPerspective(String label,int view){this.label=label;this.view=view;}
    }
    public enum BlockMode {
        OUTLINE(true,false), FILL(false,true), BOTH(true,true);
        public final boolean outline,fill;
        BlockMode(boolean outline,boolean fill){this.outline=outline;this.fill=fill;}
    }
    public enum WeatherMode { CLEAR, RAIN, SNOW }
    public enum ChatTimeFormat {
        HOURS_MINUTES("24h / minutes", "HH:mm"), SECONDS("24h / seconds", "HH:mm:ss"), TWELVE_HOUR("12h / minutes", "hh:mm a");
        public final String label, pattern;
        ChatTimeFormat(String label,String pattern){this.label=label;this.pattern=pattern;}
    }
    public enum DurabilityThreshold {
        FIVE(5), TEN(10), TWENTY(20);
        public final int percent;
        DurabilityThreshold(int percent){this.percent=percent;}
    }
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
        boolean animations,autoText,gui,hitboxes,itemPhysics,itemSize,sounds;
        boolean animationBlockHit=true;
        boolean animationEat=true;
        boolean animationBow=true;
        int guiScale=2;
        VisualPalette hitboxColor=VisualPalette.AQUA;
        int hitboxWidth=1;
        boolean hitboxPlayers=true;
        boolean hitboxMobs=true;
        boolean hitboxItems=false;
        int physicsSpeed=2;
        boolean physicsFlat=true;
        int heldItemScale=80;
        int droppedItemScale=100;
        SoundTheme blockSounds=SoundTheme.CRISP;
        SoundTheme combatSounds=SoundTheme.SOFT;
        KillChime killChime=KillChime.ORB;
        int soundsVolume=70;
        int[] autoTextKeys=new int[3];
        String[] autoTextMessages={"","",""};
        boolean autoSprint, freeLook, zoom, crosshair, fullbright, timeChanger, noHurtCam, snaplook, weatherChanger;
        boolean chatTimestamps, durabilityWarning, blockOverlay, hitColor, fovControl;
        VisualPalette blockColor=VisualPalette.AQUA, damageColor=VisualPalette.VIOLET;
        BlockMode blockMode=BlockMode.BOTH;
        int blockOpacity=20,blockWidth=2,hitOpacity=55,cameraFov=90,dynamicFov=0;
        boolean hitPlayersOnly;
        ChatTimeFormat chatTimeFormat=ChatTimeFormat.HOURS_MINUTES;
        DurabilityThreshold durabilityThreshold=DurabilityThreshold.TEN;
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
            o.animations=animations;
            o.autoText=autoText;
            o.gui=gui;
            o.hitboxes=hitboxes;
            o.itemPhysics=itemPhysics;
            o.itemSize=itemSize;
            o.sounds=sounds;
            o.animationBlockHit=animationBlockHit;
            o.animationEat=animationEat;
            o.animationBow=animationBow;
            o.guiScale=guiScale;
            o.hitboxColor=hitboxColor;
            o.hitboxWidth=hitboxWidth;
            o.hitboxPlayers=hitboxPlayers;
            o.hitboxMobs=hitboxMobs;
            o.hitboxItems=hitboxItems;
            o.physicsSpeed=physicsSpeed;
            o.physicsFlat=physicsFlat;
            o.heldItemScale=heldItemScale;
            o.droppedItemScale=droppedItemScale;
            o.blockSounds=blockSounds;
            o.combatSounds=combatSounds;
            o.killChime=killChime;
            o.soundsVolume=soundsVolume;
            o.autoTextKeys=autoTextKeys.clone();o.autoTextMessages=autoTextMessages.clone();
            o.autoSprint = autoSprint; o.freeLook = freeLook; o.zoom = zoom; o.crosshair = crosshair;
            o.fullbright = fullbright; o.timeChanger = timeChanger; o.brightness = brightness; o.timePreset = timePreset;
            o.noHurtCam = noHurtCam;
            o.snaplook=snaplook;o.weatherChanger=weatherChanger;o.snaplookKey=snaplookKey;o.snapPerspective=snapPerspective;o.weatherMode=weatherMode;
            o.blockOverlay=blockOverlay;o.hitColor=hitColor;o.fovControl=fovControl;
            o.blockColor=blockColor;o.damageColor=damageColor;o.blockMode=blockMode;o.blockOpacity=blockOpacity;o.blockWidth=blockWidth;
            o.hitOpacity=hitOpacity;o.hitPlayersOnly=hitPlayersOnly;o.cameraFov=cameraFov;o.dynamicFov=dynamicFov;
            o.chatTimestamps=chatTimestamps;o.durabilityWarning=durabilityWarning;o.chatTimeFormat=chatTimeFormat;o.durabilityThreshold=durabilityThreshold;
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
            if (Files.size(file) > 8192) throw new IOException("File too large");
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            String schema = p.getProperty("schemaVersion", "");
            if (!schema.matches("[1-9]")) throw new IOException("Unsupported settings");
            int version = Integer.parseInt(schema);
            boolean legacy = version == 1;
            if (p.size() != (legacy ? 3 : version == 2 ? 17 : version == 3 ? 21 : version == 4 ? 27 : version==5 ? 29 : version==6 ? 36 : version==7 ? 42 : version==8 ? 57 : 94))
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
                if (version >= 9 || (version==8 && m.ordinal()<GameplayModule.ANIMATIONS.ordinal()) || (version==7 && m.ordinal()<GameplayModule.BLOCK_OVERLAY.ordinal()) || (version==6 && m.ordinal()<GameplayModule.CHAT_TIMESTAMPS.ordinal()) || (version==5 && m.ordinal()<GameplayModule.SNAPLOOK.ordinal())
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
            if(version>=7) {
                o.chatTimestamps=bool(p,"chatTimestamps");o.durabilityWarning=bool(p,"durabilityWarning");
                o.chatTimeFormat=ChatTimeFormat.valueOf(p.getProperty("chatTimeFormat",""));
                o.durabilityThreshold=DurabilityThreshold.valueOf(p.getProperty("durabilityThreshold",""));
            }
            if(version>=8) {
                o.blockOverlay=bool(p,"blockOverlay");o.hitColor=bool(p,"hitColor");o.fovControl=bool(p,"fovControl");
                o.blockColor=VisualPalette.valueOf(p.getProperty("blockColor",""));o.damageColor=VisualPalette.valueOf(p.getProperty("damageColor",""));
                o.blockMode=BlockMode.valueOf(p.getProperty("blockMode",""));o.blockOpacity=integer(p,"blockOpacity",5,60);o.blockWidth=integer(p,"blockWidth",1,5);
                o.hitOpacity=integer(p,"hitOpacity",5,80);o.hitPlayersOnly=bool(p,"hitPlayersOnly");
                o.cameraFov=integer(p,"cameraFov",60,110);o.dynamicFov=integer(p,"dynamicFov",0,100);
            }
            if(version>=9){
                o.animations=bool(p,"animations");
                o.autoText=bool(p,"autoText");
                o.gui=bool(p,"gui");
                o.hitboxes=bool(p,"hitboxes");
                o.itemPhysics=bool(p,"itemPhysics");
                o.itemSize=bool(p,"itemSize");
                o.sounds=bool(p,"sounds");
                o.animationBlockHit=bool(p,"animationBlockHit");
                o.animationEat=bool(p,"animationEat");
                o.animationBow=bool(p,"animationBow");
                o.guiScale=integer(p,"guiScale",0,4);
                o.hitboxColor=VisualPalette.valueOf(p.getProperty("hitboxColor",""));
                o.hitboxWidth=integer(p,"hitboxWidth",1,3);
                o.hitboxPlayers=bool(p,"hitboxPlayers");
                o.hitboxMobs=bool(p,"hitboxMobs");
                o.hitboxItems=bool(p,"hitboxItems");
                o.physicsSpeed=integer(p,"physicsSpeed",1,5);
                o.physicsFlat=bool(p,"physicsFlat");
                o.heldItemScale=integer(p,"heldItemScale",50,150);
                o.droppedItemScale=integer(p,"droppedItemScale",25,200);
                o.blockSounds=SoundTheme.valueOf(p.getProperty("blockSounds",""));
                o.combatSounds=SoundTheme.valueOf(p.getProperty("combatSounds",""));
                o.killChime=KillChime.valueOf(p.getProperty("killChime",""));
                o.soundsVolume=integer(p,"soundsVolume",0,100);
                for(int i=0;i<3;i++){o.autoTextKeys[i]=integer(p,"autoText.key."+i,0,255);o.autoTextMessages[i]=p.getProperty("autoText.message."+i);checkText(o.autoTextMessages[i]);}
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
    public boolean animations(){return value.animations;}
    public boolean toggleAnimations(){return change(o->o.animations=!o.animations);}
    public boolean autoText(){return value.autoText;}
    public boolean toggleAutoText(){return change(o->o.autoText=!o.autoText);}
    public boolean gui(){return value.gui;}
    public boolean toggleGui(){return change(o->o.gui=!o.gui);}
    public boolean hitboxes(){return value.hitboxes;}
    public boolean toggleHitboxes(){return change(o->o.hitboxes=!o.hitboxes);}
    public boolean itemPhysics(){return value.itemPhysics;}
    public boolean toggleItemPhysics(){return change(o->o.itemPhysics=!o.itemPhysics);}
    public boolean itemSize(){return value.itemSize;}
    public boolean toggleItemSize(){return change(o->o.itemSize=!o.itemSize);}
    public boolean sounds(){return value.sounds;}
    public boolean toggleSounds(){return change(o->o.sounds=!o.sounds);}
    public boolean animationBlockHit(){return value.animationBlockHit;}
    public boolean animationBlockHit(boolean v){return change(o->{o.animationBlockHit=v;});}
    public boolean animationEat(){return value.animationEat;}
    public boolean animationEat(boolean v){return change(o->{o.animationEat=v;});}
    public boolean animationBow(){return value.animationBow;}
    public boolean animationBow(boolean v){return change(o->{o.animationBow=v;});}
    public int guiScale(){return value.guiScale;}
    public boolean guiScale(int v){return change(o->{range(v,0,4);o.guiScale=v;});}
    public VisualPalette hitboxColor(){return value.hitboxColor;}
    public boolean hitboxColor(VisualPalette v){return change(o->{if(v==null)throw new IllegalArgumentException();o.hitboxColor=v;});}
    public int hitboxWidth(){return value.hitboxWidth;}
    public boolean hitboxWidth(int v){return change(o->{range(v,1,3);o.hitboxWidth=v;});}
    public boolean hitboxPlayers(){return value.hitboxPlayers;}
    public boolean hitboxPlayers(boolean v){return change(o->{o.hitboxPlayers=v;});}
    public boolean hitboxMobs(){return value.hitboxMobs;}
    public boolean hitboxMobs(boolean v){return change(o->{o.hitboxMobs=v;});}
    public boolean hitboxItems(){return value.hitboxItems;}
    public boolean hitboxItems(boolean v){return change(o->{o.hitboxItems=v;});}
    public int physicsSpeed(){return value.physicsSpeed;}
    public boolean physicsSpeed(int v){return change(o->{range(v,1,5);o.physicsSpeed=v;});}
    public boolean physicsFlat(){return value.physicsFlat;}
    public boolean physicsFlat(boolean v){return change(o->{o.physicsFlat=v;});}
    public int heldItemScale(){return value.heldItemScale;}
    public boolean heldItemScale(int v){return change(o->{range(v,50,150);o.heldItemScale=v;});}
    public int droppedItemScale(){return value.droppedItemScale;}
    public boolean droppedItemScale(int v){return change(o->{range(v,25,200);o.droppedItemScale=v;});}
    public SoundTheme blockSounds(){return value.blockSounds;}
    public boolean blockSounds(SoundTheme v){return change(o->{if(v==null)throw new IllegalArgumentException();o.blockSounds=v;});}
    public SoundTheme combatSounds(){return value.combatSounds;}
    public boolean combatSounds(SoundTheme v){return change(o->{if(v==null)throw new IllegalArgumentException();o.combatSounds=v;});}
    public KillChime killChime(){return value.killChime;}
    public boolean killChime(KillChime v){return change(o->{if(v==null)throw new IllegalArgumentException();o.killChime=v;});}
    public int soundsVolume(){return value.soundsVolume;}
    public boolean soundsVolume(int v){return change(o->{range(v,0,100);o.soundsVolume=v;});}
    private static void slot(int slot){range(slot,0,2);}
    public int autoTextKey(int slot){slot(slot);return value.autoTextKeys[slot];}
    public String autoTextMessage(int slot){slot(slot);return value.autoTextMessages[slot];}
    public boolean autoTextKey(int slot,int key){return change(o->{slot(slot);checkKey(key);o.autoTextKeys[slot]=key;});}
    public boolean autoTextMessage(int slot,String text){return change(o->{slot(slot);checkText(text);o.autoTextMessages[slot]=text;});}
    public static boolean validText(String text){return text!=null&&text.length()<=100&&text.codePoints().allMatch(cp->!Character.isISOControl(cp)&&Character.getType(cp)!=Character.FORMAT&&Character.getType(cp)!=Character.SURROGATE&&cp!=0x00A7);}
    private static void checkText(String text){if(!validText(text))throw new IllegalArgumentException("Invalid message");}
    public boolean blockOverlay(){return value.blockOverlay;}
    public boolean hitColor(){return value.hitColor;}
    public boolean fovControl(){return value.fovControl;}
    public VisualPalette blockColor(){return value.blockColor;}
    public VisualPalette damageColor(){return value.damageColor;}
    public BlockMode blockMode(){return value.blockMode;}
    public int blockOpacity(){return value.blockOpacity;}
    public int blockWidth(){return value.blockWidth;}
    public int hitOpacity(){return value.hitOpacity;}
    public boolean hitPlayersOnly(){return value.hitPlayersOnly;}
    public int cameraFov(){return value.cameraFov;}
    public int dynamicFov(){return value.dynamicFov;}
    public boolean chatTimestamps(){return value.chatTimestamps;}
    public boolean durabilityWarning(){return value.durabilityWarning;}
    public ChatTimeFormat chatTimeFormat(){return value.chatTimeFormat;}
    public DurabilityThreshold durabilityThreshold(){return value.durabilityThreshold;}
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
    public boolean toggleBlockOverlay(){return change(o->o.blockOverlay=!o.blockOverlay);}
    public boolean toggleHitColor(){return change(o->o.hitColor=!o.hitColor);}
    public boolean toggleFovControl(){return change(o->o.fovControl=!o.fovControl);}
    public boolean blockColor(VisualPalette v){return change(o->{if(v==null)throw new IllegalArgumentException();o.blockColor=v;});}
    public boolean damageColor(VisualPalette v){return change(o->{if(v==null)throw new IllegalArgumentException();o.damageColor=v;});}
    public boolean blockMode(BlockMode v){return change(o->{if(v==null)throw new IllegalArgumentException();o.blockMode=v;});}
    public boolean blockOpacity(int v){return change(o->{range(v,5,60);o.blockOpacity=v;});}
    public boolean blockWidth(int v){return change(o->{range(v,1,5);o.blockWidth=v;});}
    public boolean hitOpacity(int v){return change(o->{range(v,5,80);o.hitOpacity=v;});}
    public boolean hitPlayersOnly(boolean v){return change(o->o.hitPlayersOnly=v);}
    public boolean cameraFov(int v){return change(o->{range(v,60,110);o.cameraFov=v;});}
    public boolean dynamicFov(int v){return change(o->{range(v,0,100);o.dynamicFov=v;});}
    private static void range(int v,int min,int max){if(v<min||v>max)throw new IllegalArgumentException();}
    public boolean toggleChatTimestamps(){return change(o->o.chatTimestamps=!o.chatTimestamps);}
    public boolean toggleDurabilityWarning(){return change(o->o.durabilityWarning=!o.durabilityWarning);}
    public boolean chatTimeFormat(ChatTimeFormat v){return change(o->{if(v==null)throw new IllegalArgumentException();o.chatTimeFormat=v;});}
    public boolean durabilityThreshold(DurabilityThreshold v){return change(o->{if(v==null)throw new IllegalArgumentException();o.durabilityThreshold=v;});}
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
                case ANIMATIONS:o.animationBlockHit=true;o.animationEat=true;o.animationBow=true;break;
                case AUTO_TEXT:o.autoTextKeys=new int[3];o.autoTextMessages=new String[]{"","",""};break;
                case GUI:o.guiScale=2;break;
                case HITBOXES:o.hitboxColor=VisualPalette.AQUA;o.hitboxWidth=1;o.hitboxPlayers=true;o.hitboxMobs=true;o.hitboxItems=false;break;
                case ITEM_PHYSICS:o.physicsSpeed=2;o.physicsFlat=true;break;
                case ITEM_SIZE:o.heldItemScale=80;o.droppedItemScale=100;break;
                case SOUNDS:o.blockSounds=SoundTheme.CRISP;o.combatSounds=SoundTheme.SOFT;o.killChime=KillChime.ORB;o.soundsVolume=70;break;
                case BLOCK_OVERLAY:o.blockColor=VisualPalette.AQUA;o.blockMode=BlockMode.BOTH;o.blockOpacity=20;o.blockWidth=2;break;
                case HIT_COLOR:o.damageColor=VisualPalette.VIOLET;o.hitOpacity=55;o.hitPlayersOnly=false;break;
                case FOV_CONTROL:o.cameraFov=90;o.dynamicFov=0;break;
                case CHAT_TIMESTAMPS: o.chatTimeFormat=ChatTimeFormat.HOURS_MINUTES;break;
                case DURABILITY_WARNING: o.durabilityThreshold=DurabilityThreshold.TEN;break;
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
            p.setProperty("schemaVersion", "9");
            p.setProperty("animations",Boolean.toString(o.animations));
            p.setProperty("autoText",Boolean.toString(o.autoText));
            p.setProperty("gui",Boolean.toString(o.gui));
            p.setProperty("hitboxes",Boolean.toString(o.hitboxes));
            p.setProperty("itemPhysics",Boolean.toString(o.itemPhysics));
            p.setProperty("itemSize",Boolean.toString(o.itemSize));
            p.setProperty("sounds",Boolean.toString(o.sounds));
            p.setProperty("animationBlockHit",Boolean.toString(o.animationBlockHit));
            p.setProperty("animationEat",Boolean.toString(o.animationEat));
            p.setProperty("animationBow",Boolean.toString(o.animationBow));
            p.setProperty("guiScale",Integer.toString(o.guiScale));
            p.setProperty("hitboxColor",o.hitboxColor.name());
            p.setProperty("hitboxWidth",Integer.toString(o.hitboxWidth));
            p.setProperty("hitboxPlayers",Boolean.toString(o.hitboxPlayers));
            p.setProperty("hitboxMobs",Boolean.toString(o.hitboxMobs));
            p.setProperty("hitboxItems",Boolean.toString(o.hitboxItems));
            p.setProperty("physicsSpeed",Integer.toString(o.physicsSpeed));
            p.setProperty("physicsFlat",Boolean.toString(o.physicsFlat));
            p.setProperty("heldItemScale",Integer.toString(o.heldItemScale));
            p.setProperty("droppedItemScale",Integer.toString(o.droppedItemScale));
            p.setProperty("blockSounds",o.blockSounds.name());
            p.setProperty("combatSounds",o.combatSounds.name());
            p.setProperty("killChime",o.killChime.name());
            p.setProperty("soundsVolume",Integer.toString(o.soundsVolume));
            for(int i=0;i<3;i++){p.setProperty("autoText.key."+i,Integer.toString(o.autoTextKeys[i]));p.setProperty("autoText.message."+i,o.autoTextMessages[i]);}
            p.setProperty("blockOverlay",Boolean.toString(o.blockOverlay));p.setProperty("hitColor",Boolean.toString(o.hitColor));p.setProperty("fovControl",Boolean.toString(o.fovControl));
            p.setProperty("blockColor",o.blockColor.name());p.setProperty("damageColor",o.damageColor.name());p.setProperty("blockMode",o.blockMode.name());
            p.setProperty("blockOpacity",Integer.toString(o.blockOpacity));p.setProperty("blockWidth",Integer.toString(o.blockWidth));
            p.setProperty("hitOpacity",Integer.toString(o.hitOpacity));p.setProperty("hitPlayersOnly",Boolean.toString(o.hitPlayersOnly));
            p.setProperty("cameraFov",Integer.toString(o.cameraFov));p.setProperty("dynamicFov",Integer.toString(o.dynamicFov));
            p.setProperty("chatTimestamps",Boolean.toString(o.chatTimestamps));p.setProperty("durabilityWarning",Boolean.toString(o.durabilityWarning));
            p.setProperty("chatTimeFormat",o.chatTimeFormat.name());p.setProperty("durabilityThreshold",o.durabilityThreshold.name());
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
