package dev.spaceclient;
import dev.spaceclient.mods.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class NoHurtCamSettingsTest {
    @Rule public TemporaryFolder dir=new TemporaryFolder();
    private Path file(){return dir.getRoot().toPath().resolve("mods");}
    @Test public void realSchemaFourKeepsVisualsAndKeysAndAddsOptInHurtCamera()throws Exception {
        byte[] bytes;try(InputStream in=getClass().getResourceAsStream("/hud-legacy/mods-schema4-952b258.properties")) {
            assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();
        }
        Files.write(file(),bytes);ModSettings s=ModSettings.open(file());assertFalse(s.isReadOnly());assertFalse(s.noHurtCam());assertEquals(0,s.toggleKey(GameplayModule.NO_HURT_CAM));
        assertArrayEquals(bytes,Files.readAllBytes(file()));assertTrue(s.fullbright());assertTrue(s.timeChanger());assertEquals(ModSettings.Brightness.SOFT,s.brightness());
        assertEquals(ModSettings.TimePreset.NIGHT,s.timePreset());assertEquals(37,s.toggleKey(GameplayModule.FULLBRIGHT));assertEquals(38,s.toggleKey(GameplayModule.TIME_CHANGER));
        assertTrue(s.toggleNoHurtCam());assertTrue(s.toggleKey(GameplayModule.NO_HURT_CAM,39));s=ModSettings.open(file());assertTrue(s.noHurtCam());assertEquals(39,s.toggleKey(GameplayModule.NO_HURT_CAM));
        assertEquals(7,s.zoomFactor());assertTrue(s.fullbright());assertEquals(37,s.toggleKey(GameplayModule.FULLBRIGHT));
        assertTrue(s.reset(GameplayModule.NO_HURT_CAM));assertTrue(s.noHurtCam());assertEquals(0,s.toggleKey(GameplayModule.NO_HURT_CAM));assertTrue(s.timeChanger());
    }
    @Test public void malformedAndFailedWritesKeepOriginalAndCannotEnableHurtCamera()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.toggleZoom());Properties p=new Properties();try(InputStream in=Files.newInputStream(file())){p.load(in);}
        for(String key:new String[]{"noHurtCam","toggleKey.NO_HURT_CAM"}) {
            Properties bad=new Properties();bad.putAll(p);bad.remove(key);try(OutputStream out=Files.newOutputStream(file())){bad.store(out,"missing");}
            byte[] old=Files.readAllBytes(file());ModSettings r=ModSettings.open(file());assertTrue(r.isReadOnly());assertFalse(r.toggleNoHurtCam());assertArrayEquals(old,Files.readAllBytes(file()));
        }
        Path blocked=dir.newFile("blocked").toPath();ModSettings r=ModSettings.open(blocked.resolve("mods"));assertFalse(r.toggleNoHurtCam());assertFalse(r.noHurtCam());
    }
    @Test public void shortcutTogglesHurtCameraWithoutChangingOtherVisuals()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.toggleKey(GameplayModule.NO_HURT_CAM,39));ModuleShortcuts keys=new ModuleShortcuts();
        assertNull(keys.onKey(s,39,1,true,false,false,m->true));assertFalse(s.noHurtCam());
        assertEquals(GameplayModule.NO_HURT_CAM,keys.onKey(s,39,2,true,false,true,m->true));assertTrue(s.noHurtCam());assertFalse(s.fullbright());assertFalse(s.timeChanger());
        assertNull(keys.onKey(s,39,2,true,false,true,m->true));assertNull(keys.onKey(s,39,3,true,true,true,m->true));assertTrue(s.noHurtCam());
    }
}
