package dev.spaceclient;
import dev.spaceclient.mods.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class EnvironmentModsSettingsTest {
    @Rule public TemporaryFolder dir=new TemporaryFolder();
    private Path file(){return dir.getRoot().toPath().resolve("mods");}
    private Properties read()throws Exception {Properties p=new Properties();try(InputStream in=Files.newInputStream(file())){p.load(in);}return p;}
    private void write(Properties p)throws Exception {try(OutputStream out=Files.newOutputStream(file())){p.store(out,"test");}}
    @Test public void realSchemaFivePreservesSevenModulesAndLeavesNewHoldAndToggleKeysUnbound()throws Exception {
        byte[] bytes;try(InputStream in=getClass().getResourceAsStream("/hud-legacy/mods-schema5-5e7d1f5.properties")) {
            assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();
        }
        Files.write(file(),bytes);ModSettings s=ModSettings.open(file());assertFalse(s.isReadOnly());assertArrayEquals(bytes,Files.readAllBytes(file()));
        int[] keys={30,31,32,33,37,38,39};for(GameplayModule m:GameplayModule.values()) {
            boolean old=m.ordinal()<GameplayModule.SNAPLOOK.ordinal();assertEquals(old,m.enabled(s));assertEquals(old?keys[m.ordinal()]:0,s.toggleKey(m));
        }
        assertEquals(0,s.snaplookKey());assertEquals(ModSettings.SnapPerspective.FRONT,s.snapPerspective());assertEquals(ModSettings.WeatherMode.CLEAR,s.weatherMode());
        assertEquals(48,s.freeLookKey());assertEquals(34,s.zoomKey());assertEquals(7,s.zoomFactor());assertFalse(s.zoomSmooth());
        assertEquals(ModSettings.Brightness.SOFT,s.brightness());assertEquals(ModSettings.TimePreset.NIGHT,s.timePreset());
        assertTrue(s.toggleSnaplook());assertTrue(s.snaplookKey(50));assertTrue(s.snapPerspective(ModSettings.SnapPerspective.BACK));
        assertTrue(s.toggleWeatherChanger());assertTrue(s.weatherMode(ModSettings.WeatherMode.SNOW));assertTrue(s.toggleKey(GameplayModule.SNAPLOOK,51));assertTrue(s.toggleKey(GameplayModule.WEATHER_CHANGER,52));
        s=ModSettings.open(file());assertTrue(s.snaplook());assertTrue(s.weatherChanger());assertEquals(50,s.snaplookKey());assertEquals(ModSettings.SnapPerspective.BACK,s.snapPerspective());
        assertEquals(ModSettings.WeatherMode.SNOW,s.weatherMode());assertEquals(51,s.toggleKey(GameplayModule.SNAPLOOK));assertEquals(52,s.toggleKey(GameplayModule.WEATHER_CHANGER));
        assertEquals("8",read().getProperty("schemaVersion"));assertEquals(57,read().size());assertTrue(s.noHurtCam());assertEquals(39,s.toggleKey(GameplayModule.NO_HURT_CAM));
    }
    @Test public void resetOnlyTouchesTheSelectedModulesKeysAndEffectOptions()throws Exception {
        ModSettings s=ModSettings.open(file());s.toggleSnaplook();s.toggleWeatherChanger();s.snaplookKey(50);s.snapPerspective(ModSettings.SnapPerspective.BACK);
        s.weatherMode(ModSettings.WeatherMode.RAIN);s.toggleKey(GameplayModule.SNAPLOOK,51);s.toggleKey(GameplayModule.WEATHER_CHANGER,52);
        assertTrue(s.reset(GameplayModule.SNAPLOOK));assertTrue(s.snaplook());assertEquals(0,s.snaplookKey());assertEquals(0,s.toggleKey(GameplayModule.SNAPLOOK));
        assertEquals(ModSettings.SnapPerspective.FRONT,s.snapPerspective());assertEquals(ModSettings.WeatherMode.RAIN,s.weatherMode());assertEquals(52,s.toggleKey(GameplayModule.WEATHER_CHANGER));
        assertTrue(s.reset(GameplayModule.WEATHER_CHANGER));assertTrue(s.weatherChanger());assertEquals(ModSettings.WeatherMode.CLEAR,s.weatherMode());assertEquals(0,s.toggleKey(GameplayModule.WEATHER_CHANGER));
    }
    @Test public void malformedNewAndFutureFilesAreKeptReadOnlyWithoutPartialState()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.toggleZoom());Properties good=read();
        for(String[] change:new String[][]{{"snaplook",null},{"weatherChanger","yes"},{"snaplookKey","256"},{"snapPerspective","ORBIT"},{"weatherMode","STORM"},
                {"toggleKey.SNAPLOOK",null},{"toggleKey.WEATHER_CHANGER",null},{"schemaVersion","9"},{"schemaVersion","06"},{"extra","keep"}}) {
            Properties p=new Properties();p.putAll(good);if(change[1]==null)p.remove(change[0]);else p.setProperty(change[0],change[1]);write(p);byte[] bytes=Files.readAllBytes(file());
            ModSettings r=ModSettings.open(file());assertTrue(change[0],r.isReadOnly());assertFalse(r.toggleSnaplook());assertFalse(r.toggleWeatherChanger());assertArrayEquals(bytes,Files.readAllBytes(file()));
        }
    }
    @Test public void failedWritesAndInvalidValuesRevertAllNewFields()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.toggleZoom());byte[] bytes=Files.readAllBytes(file());
        assertFalse(s.snaplookKey(-1));assertFalse(s.snapPerspective(null));assertFalse(s.weatherMode(null));assertArrayEquals(bytes,Files.readAllBytes(file()));
        Path blocked=dir.newFile("blocked").toPath();s=ModSettings.open(blocked.resolve("mods"));assertFalse(s.toggleSnaplook());assertFalse(s.toggleWeatherChanger());assertFalse(s.snaplookKey(50));
        assertFalse(s.snaplook());assertFalse(s.weatherChanger());assertEquals(0,s.snaplookKey());assertEquals(ModSettings.WeatherMode.CLEAR,s.weatherMode());
    }
    @Test public void holdKeyChecksCoverAllThreeModulesAndDoNotReserveAnUnboundNewKey()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.zoomKey(19));assertFalse(ModuleKeys.usedByHold(s,GameplayModule.ZOOM,19));
        assertTrue(s.holdKey(GameplayModule.SNAPLOOK,50));assertEquals(50,s.snaplookKey());assertEquals(19,s.zoomKey());assertEquals(47,s.freeLookKey());
        assertTrue(ModuleKeys.usedByHold(s,GameplayModule.ZOOM,50));assertTrue(ModuleKeys.usedByHold(s,GameplayModule.SNAPLOOK,47));
        assertFalse(ModuleKeys.usedByHold(s,GameplayModule.SNAPLOOK,50));assertFalse(ModuleKeys.usedByHold(s,null,0));
        assertTrue(s.toggleKey(GameplayModule.WEATHER_CHANGER,51));assertTrue(ModuleKeys.usedByToggle(s,GameplayModule.SNAPLOOK,51));
    }
    @Test public void shortcutsToggleOnlyTheirOwnModulesAndIgnoreGuiAndRepeats()throws Exception {
        ModSettings s=ModSettings.open(file());s.toggleKey(GameplayModule.SNAPLOOK,51);s.toggleKey(GameplayModule.WEATHER_CHANGER,52);ModuleShortcuts keys=new ModuleShortcuts();
        assertNull(keys.onKey(s,51,1,true,false,false,m->true));assertFalse(s.snaplook());
        assertEquals(GameplayModule.SNAPLOOK,keys.onKey(s,51,2,true,false,true,m->true));assertTrue(s.snaplook());assertFalse(s.weatherChanger());
        assertNull(keys.onKey(s,51,2,true,false,true,m->true));assertNull(keys.onKey(s,52,3,true,true,true,m->true));
        assertEquals(GameplayModule.WEATHER_CHANGER,keys.onKey(s,52,4,true,false,true,m->true));assertTrue(s.snaplook());assertTrue(s.weatherChanger());
        assertEquals(GameplayModule.SNAPLOOK,keys.onKey(s,51,5,true,false,true,m->true));assertFalse(s.snaplook());assertTrue(s.weatherChanger());
    }
}
