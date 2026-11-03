package dev.spaceclient.cosmetics;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import java.util.function.Consumer;

/** Local visual preferences, independent of gameplay/HUD schemas and account secrets. */
public final class CosmeticsSettings {
    public enum Wings { NONE, AURORA, EMBER, CRYSTAL }
    public enum Halo { NONE, RING, ORBIT }
    public enum Tint {
        AURORA(0x6FE8D3,0x9A84F3), GOLD(0xEBCB8F,0xDF8754), ROSE(0xEB9BBF,0xAC83E9), ICE(0xB9E9FA,0x6299DF);
        public final int primary, secondary;
        Tint(int primary,int secondary) { this.primary=primary;this.secondary=secondary; }
    }
    public static final class State {
        public boolean enabled=true, animated=true, skin=true, cape=true;
        public Wings wings=Wings.NONE;
        public Halo halo=Halo.NONE;
        public Tint tint=Tint.AURORA;
        public int scale=100;
        public State copy() {
            State copy=new State();copy.enabled=enabled;copy.animated=animated;copy.skin=skin;copy.cape=cape;
            copy.wings=wings;copy.halo=halo;copy.tint=tint;copy.scale=scale;return copy;
        }
    }
    private final Path file;
    private State state=new State();
    private boolean readOnly;
    private String error="",notice="";
    private CosmeticsSettings(Path file) { this.file=file; }
    public static CosmeticsSettings open(Path file) {
        CosmeticsSettings settings=new CosmeticsSettings(file);
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))return settings;
        try {
            Properties data=new Properties();
            data.load(new StringReader(new String(AppearanceProfile.readBounded(file,8192),StandardCharsets.UTF_8)));
            int schema=Integer.parseInt(data.getProperty("schemaVersion","1"));
            if(schema!=1) { settings.readOnly=true;settings.notice="Newer cosmetic settings are preserved read-only.";return settings; }
            State value=new State();
            value.enabled=bool(data,"enabled",true);value.animated=bool(data,"animated",true);
            value.skin=bool(data,"skin",true);value.cape=bool(data,"cape",true);
            value.wings=Wings.valueOf(data.getProperty("wings","NONE"));
            value.halo=Halo.valueOf(data.getProperty("halo","NONE"));
            value.tint=Tint.valueOf(data.getProperty("tint","AURORA"));
            value.scale=Integer.parseInt(data.getProperty("scale","100"));
            validate(value);settings.state=value;
        } catch(Exception failure) { settings.notice="Invalid cosmetic settings: safe defaults are in use; the file is preserved."; }
        return settings;
    }
    private static boolean bool(Properties p,String key,boolean fallback) {
        String value=p.getProperty(key,Boolean.toString(fallback));
        if(!value.equals("true")&&!value.equals("false"))throw new IllegalArgumentException();
        return Boolean.parseBoolean(value);
    }
    private static void validate(State value) {
        if(value.wings==null||value.halo==null||value.tint==null||value.scale<60||value.scale>140)throw new IllegalArgumentException();
    }
    public synchronized State view() { return state.copy(); }
    public synchronized String error() { return error; }
    public synchronized String notice() { return notice; }
    public boolean readOnly() { return readOnly; }
    public synchronized boolean change(Consumer<State> change) {
        if(readOnly) { error="Cosmetic settings are read-only.";return false; }
        State next=state.copy();
        try { change.accept(next);validate(next);save(next);state=next;error="";notice="";return true; }
        catch(Exception failure) { error="Could not save cosmetic settings. The previous values are preserved.";return false; }
    }
    public boolean reset() { return change(next->{State value=new State();next.enabled=value.enabled;next.animated=value.animated;
        next.skin=value.skin;next.cape=value.cape;next.wings=value.wings;next.halo=value.halo;next.tint=value.tint;next.scale=value.scale;}); }
    private void save(State value) throws IOException {
        Path parent=file.toAbsolutePath().getParent();
        if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(file))throw new IOException("Linked settings target");
        Files.createDirectories(parent);
        Properties p=new Properties();p.setProperty("schemaVersion","1");
        p.setProperty("enabled",Boolean.toString(value.enabled));p.setProperty("animated",Boolean.toString(value.animated));
        p.setProperty("skin",Boolean.toString(value.skin));p.setProperty("cape",Boolean.toString(value.cape));
        p.setProperty("wings",value.wings.name());p.setProperty("halo",value.halo.name());p.setProperty("tint",value.tint.name());p.setProperty("scale",Integer.toString(value.scale));
        StringWriter text=new StringWriter();p.store(text,"Space cosmetics");
        Path temp=Files.createTempFile(parent,".space-cosmetics-",".tmp");
        try {
            Files.write(temp,text.toString().getBytes(StandardCharsets.UTF_8));
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException unsupported) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
