package dev.spaceclient.cosmetics;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Public launch snapshot only. Fixed paths, bounded PNGs, no credentials or network. */
public final class AppearanceProfile {
    private static final Set<String> KEYS=new HashSet<String>(Arrays.asList("schemaVersion","accountId","username","nickname","skinSha256","skinModel","capeSha256"));
    public final String nickname, skinModel, notice;
    public final boolean linked;
    final BufferedImage skin, cape;
    private AppearanceProfile(String nickname,String model,BufferedImage skin,BufferedImage cape,boolean linked,String notice) {
        this.nickname=nickname;this.skinModel=model;this.skin=skin;this.cape=cape;this.linked=linked;this.notice=notice;
    }
    public static AppearanceProfile empty() { return new AppearanceProfile("","classic",null,null,false,""); }
    public boolean hasSkin() { return skin!=null; }
    public boolean hasCape() { return cape!=null; }
    public static AppearanceProfile load(Path config) {
        Path file=config.resolve("space-appearance.properties");
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))return empty();
        try {
            if(Files.isSymbolicLink(config))throw new IOException();
            Properties p=new Properties();p.load(new StringReader(new String(readBounded(file,4096),StandardCharsets.UTF_8)));
            for(String key:p.stringPropertyNames())if(!KEYS.contains(key))throw new IOException();
            if(!"1".equals(p.getProperty("schemaVersion")))throw new IOException();
            String id=p.getProperty("accountId",""),username=p.getProperty("username",""),nickname=p.getProperty("nickname","");
            if(!id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
                ||!username.matches("[a-zA-Z0-9_]{3,24}")||!nickname.matches("[a-zA-Z0-9_]{3,16}"))throw new IOException();
            String model=p.getProperty("skinModel","classic");if(!model.equals("classic")&&!model.equals("slim"))throw new IOException();
            Path assets=config.resolve("space-appearance");
            if(Files.isSymbolicLink(assets))throw new IOException();
            BufferedImage skin=null,cape=null;String notice="";
            try { skin=loadImage(assets,"skin",p.getProperty("skinSha256",""));if(skin!=null&&skin.getHeight()==32&&!model.equals("classic"))throw new IOException(); }
            catch(IOException invalid) { skin=null;notice="A profile image failed validation; the default appearance is used."; }
            try { cape=loadImage(assets,"cape",p.getProperty("capeSha256","")); }
            catch(IOException invalid) { cape=null;notice="A profile image failed validation; the default appearance is used."; }
            return new AppearanceProfile(nickname,model,skin,cape,true,notice);
        } catch(Exception invalid) { return new AppearanceProfile("","classic",null,null,false,"The appearance profile is invalid; default textures are used."); }
    }
    static byte[] readBounded(Path file,int limit) throws IOException {
        if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>limit)throw new IOException();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream input=Files.newInputStream(file)) {
            byte[] chunk=new byte[4096];int count;
            while((count=input.read(chunk))!=-1) { if(bytes.size()+count>limit)throw new IOException();bytes.write(chunk,0,count); }
        }
        if(Files.isSymbolicLink(file))throw new IOException();
        return bytes.toByteArray();
    }
    private static BufferedImage loadImage(Path assets,String kind,String sha) throws IOException {
        if(sha.isEmpty())return null;
        if(!sha.matches("[a-f0-9]{64}"))throw new IOException();
        byte[] data=readBounded(assets.resolve(kind+"-"+sha+".png"),256*1024);
        try { if(!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(data),hex(sha)))throw new IOException(); }
        catch(java.security.NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
        if(data.length<33||data[0]!=(byte)137||data[1]!=80||data[2]!=78||data[3]!=71||data[4]!=13||data[5]!=10||data[6]!=26||data[7]!=10
            ||data[12]!=73||data[13]!=72||data[14]!=68||data[15]!=82)throw new IOException();
        int width=integer(data,16),height=integer(data,20);
        if(kind.equals("skin") ? width!=64||(height!=32&&height!=64) : (width!=64&&width!=128&&width!=256)||height!=width/2)throw new IOException();
        Iterator<ImageReader> readers=ImageIO.getImageReadersByFormatName("png");
        if(!readers.hasNext())throw new IOException();ImageReader reader=readers.next();
        try(ImageInputStream input=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            reader.setInput(input,true,true);
            if(reader.getWidth(0)!=width||reader.getHeight(0)!=height)throw new IOException();
            BufferedImage result=reader.read(0);if(result==null)throw new IOException();return result;
        } finally { reader.dispose(); }
    }
    private static int integer(byte[] bytes,int offset) { return (bytes[offset]&255)<<24|(bytes[offset+1]&255)<<16|(bytes[offset+2]&255)<<8|bytes[offset+3]&255; }
    private static byte[] hex(String text) { byte[] result=new byte[32];for(int i=0;i<32;i++)result[i]=(byte)Integer.parseInt(text.substring(i*2,i*2+2),16);return result; }
}
