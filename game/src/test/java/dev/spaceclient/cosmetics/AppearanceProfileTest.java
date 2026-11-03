package dev.spaceclient.cosmetics;
import org.junit.Test;
import static org.junit.Assert.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AppearanceProfileTest {
    private String sha(byte[] data) throws Exception { StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))out.append(String.format("%02x",b&255));return out.toString(); }
    private String image(Path root,String kind,int width,int height) throws Exception {
        Path assets=root.resolve("space-appearance");Files.createDirectories(assets);Path temp=assets.resolve("fixture.png");
        ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB),"png",temp.toFile());
        byte[] bytes=Files.readAllBytes(temp);String hash=sha(bytes);Files.move(temp,assets.resolve(kind+"-"+hash+".png"));return hash;
    }
    private void profile(Path root,String skin,String cape,String extra) throws Exception {
        Files.write(root.resolve("space-appearance.properties"),("schemaVersion=1\naccountId=12345678-1234-1234-1234-123456789abc\nusername=space_user\nnickname=SpaceUser\nskinModel=classic\nskinSha256="+skin+"\ncapeSha256="+cape+"\n"+extra).getBytes(StandardCharsets.UTF_8));
    }
    @Test public void fixedVerifiedAssetsLoadWithoutCredentialsOrNetwork() throws Exception {
        Path root=Files.createTempDirectory("space-appearance");String skin=image(root,"skin",64,64),cape=image(root,"cape",128,64);profile(root,skin,cape,"");
        AppearanceProfile data=AppearanceProfile.load(root);assertTrue(data.linked);assertEquals("SpaceUser",data.nickname);assertTrue(data.hasSkin());assertTrue(data.hasCape());assertTrue(data.notice.isEmpty());
    }
    @Test public void aTamperedAssetFallsBackWithoutDiscardingTheOtherImage() throws Exception {
        Path root=Files.createTempDirectory("space-appearance-tamper");String skin=image(root,"skin",64,32),cape=image(root,"cape",64,32);profile(root,skin,cape,"");
        Files.write(root.resolve("space-appearance/skin-"+skin+".png"),new byte[]{1,2,3});AppearanceProfile data=AppearanceProfile.load(root);
        assertTrue(data.linked);assertFalse(data.hasSkin());assertTrue(data.hasCape());assertFalse(data.notice.isEmpty());
    }
    @Test public void unsafeHashesUnsupportedDimensionsAndOversizedMetadataAreRejected() throws Exception {
        Path root=Files.createTempDirectory("space-appearance-bounds");String invalid=image(root,"skin",128,128);profile(root,invalid,"","");assertFalse(AppearanceProfile.load(root).hasSkin());
        profile(root,"../../outside","","");assertFalse(AppearanceProfile.load(root).hasSkin());
        Files.write(root.resolve("space-appearance.properties"),new byte[4097]);assertFalse(AppearanceProfile.load(root).linked);
    }
    @Test public void unknownFieldsAndFutureSchemasCannotBecomeAnActiveProfile() throws Exception {
        Path root=Files.createTempDirectory("space-appearance-unknown");profile(root,"","","sessionToken=must-not-be-here\n");assertFalse(AppearanceProfile.load(root).linked);
        Files.write(root.resolve("space-appearance.properties"),"schemaVersion=2\n".getBytes(StandardCharsets.UTF_8));assertFalse(AppearanceProfile.load(root).linked);
        assertFalse(AppearanceProfile.load(root.resolve("missing")).linked);
    }
}
