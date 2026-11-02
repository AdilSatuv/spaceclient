package dev.spaceclient;

import dev.spaceclient.mods.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class VisualModsTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();
    private Path file() { return directory.getRoot().toPath().resolve("mods.properties"); }
    private Properties read() throws Exception {
        Properties p=new Properties();try(InputStream in=Files.newInputStream(file())){p.load(in);}return p;
    }
    private void write(Properties p) throws Exception {
        try(OutputStream out=Files.newOutputStream(file())){p.store(out,"test");}
    }
    @Test public void realSchemaThreeKeepsFourModulesAndKeysWithNewVisualsOptIn()throws Exception {
        byte[] original;
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/mods-schema3-38a30c2.properties")) {
            assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);original=out.toByteArray();
        }
        Files.write(file(),original);ModSettings s=ModSettings.open(file());assertFalse(s.isReadOnly());
        assertArrayEquals(original,Files.readAllBytes(file()));assertFalse(s.fullbright());assertFalse(s.timeChanger());
        assertEquals(ModSettings.Brightness.FULL,s.brightness());assertEquals(ModSettings.TimePreset.DAY,s.timePreset());
        for(GameplayModule m:GameplayModule.values()) {
            boolean old=m.ordinal()<GameplayModule.FULLBRIGHT.ordinal();assertEquals(old,m.enabled(s));
            assertEquals(old?30+m.ordinal():0,s.toggleKey(m));
        }
        assertEquals(48,s.freeLookKey());assertEquals(34,s.zoomKey());assertEquals(7,s.zoomFactor());assertFalse(s.zoomSmooth());
        assertEquals(ModSettings.SprintMode.TOGGLE,s.sprintMode());assertEquals(CrosshairStyle.Shape.RING,s.crosshairStyle().shape);
        assertEquals(9,s.crosshairStyle().size);assertFalse(s.crosshairStyle().outline);
        assertTrue(s.toggleFullbright());assertEquals("8",read().getProperty("schemaVersion"));
        ModSettings saved=ModSettings.open(file());assertTrue(saved.fullbright());assertFalse(saved.timeChanger());
        assertEquals(33,saved.toggleKey(GameplayModule.CROSSHAIR));assertEquals(7,saved.zoomFactor());
    }
    @Test public void gammaOverrideIsReadOnlyAndReleasesToTheCurrentVanillaValue()throws Exception {
        ModSettings s=ModSettings.open(file());assertEquals(.25f,s.renderGamma(.25f),0);
        assertTrue(s.toggleFullbright());assertEquals(16,s.renderGamma(.25f),0);
        assertTrue(s.brightness(ModSettings.Brightness.SOFT));assertEquals(4,s.renderGamma(.25f),0);
        assertEquals(20,s.renderGamma(20),0);byte[] saved=Files.readAllBytes(file());
        for(int i=0;i<256;i++)s.renderGamma(.5f);
        assertArrayEquals(saved,Files.readAllBytes(file()));assertTrue(s.toggleFullbright());
        assertEquals(.75f,s.renderGamma(.75f),0);assertEquals(0,s.renderGamma(0),0);
    }
    @Test public void visualSettingsAndKeysPersistAndResetOnlyTheirOwnOptions()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.toggleFullbright());assertTrue(s.toggleTimeChanger());
        assertTrue(s.brightness(ModSettings.Brightness.BRIGHT));assertTrue(s.timePreset(ModSettings.TimePreset.NIGHT));
        assertTrue(s.toggleKey(GameplayModule.FULLBRIGHT,37));assertTrue(s.toggleKey(GameplayModule.TIME_CHANGER,38));
        s=ModSettings.open(file());assertTrue(s.fullbright());assertTrue(s.timeChanger());
        assertEquals(ModSettings.Brightness.BRIGHT,s.brightness());assertEquals(18000,s.timePreset().ticks);
        assertTrue(s.reset(GameplayModule.FULLBRIGHT));assertTrue(s.fullbright());assertEquals(0,s.toggleKey(GameplayModule.FULLBRIGHT));
        assertEquals(ModSettings.Brightness.FULL,s.brightness());assertEquals(ModSettings.TimePreset.NIGHT,s.timePreset());
        assertEquals(38,s.toggleKey(GameplayModule.TIME_CHANGER));assertTrue(s.reset(GameplayModule.TIME_CHANGER));
        assertTrue(s.timeChanger());assertEquals(ModSettings.TimePreset.DAY,s.timePreset());
        assertEquals(0,ModSettings.open(file()).toggleKey(GameplayModule.TIME_CHANGER));
    }
    @Test public void malformedNewOrFutureFilesStayReadOnlyAndByteForByteUnchanged()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.toggleZoom());Properties original=read();
        for(String[] change:new String[][]{{"fullbright",null},{"brightness","GLOW"},{"timePreset","MIDNIGHT"},
                {"timeChanger","yes"},{"toggleKey.FULLBRIGHT","256"},{"toggleKey.TIME_CHANGER",null},
                {"schemaVersion","9"},{"schemaVersion","04"},{"unrecognized","keep"}}) {
            Properties bad=new Properties();bad.putAll(original);
            if(change[1]==null)bad.remove(change[0]);else bad.setProperty(change[0],change[1]);write(bad);
            byte[] bytes=Files.readAllBytes(file());ModSettings readonly=ModSettings.open(file());
            assertTrue(change[0],readonly.isReadOnly());assertFalse(readonly.toggleFullbright());assertFalse(readonly.toggleTimeChanger());
            assertArrayEquals(bytes,Files.readAllBytes(file()));
        }
    }
    @Test public void failedWritesAndInvalidValuesCannotEnableVisualsOrChangePresets()throws Exception {
        Path blocked=directory.newFile("blocked").toPath();ModSettings s=ModSettings.open(blocked.resolve("mods"));
        assertFalse(s.toggleFullbright());assertFalse(s.fullbright());assertFalse(s.toggleTimeChanger());assertFalse(s.timeChanger());
        assertFalse(s.brightness(ModSettings.Brightness.SOFT));assertEquals(ModSettings.Brightness.FULL,s.brightness());
        assertFalse(s.timePreset(ModSettings.TimePreset.NIGHT));assertEquals(ModSettings.TimePreset.DAY,s.timePreset());
        ModSettings valid=ModSettings.open(file());assertTrue(valid.toggleZoom());byte[] old=Files.readAllBytes(file());
        assertFalse(valid.brightness(null));assertFalse(valid.timePreset(null));assertArrayEquals(old,Files.readAllBytes(file()));
    }
    @Test public void visualShortcutsIgnoreGuiRepeatsAndDuplicatesAndPreserveEachOthersState()throws Exception {
        ModSettings s=ModSettings.open(file());assertTrue(s.toggleKey(GameplayModule.FULLBRIGHT,37));assertTrue(s.toggleKey(GameplayModule.TIME_CHANGER,38));
        assertTrue(ModuleKeys.usedByToggle(s,GameplayModule.ZOOM,37));ModuleShortcuts keys=new ModuleShortcuts();
        assertNull(keys.onKey(s,37,1,true,false,false,m->true));assertFalse(s.fullbright());
        assertEquals(GameplayModule.FULLBRIGHT,keys.onKey(s,37,2,true,false,true,m->true));
        assertNull(keys.onKey(s,37,2,true,false,true,m->true));assertNull(keys.onKey(s,37,3,true,true,true,m->true));
        assertTrue(s.fullbright());assertFalse(s.timeChanger());
        assertEquals(GameplayModule.TIME_CHANGER,keys.onKey(s,38,4,true,false,true,m->true));
        assertTrue(s.fullbright());assertTrue(s.timeChanger());
        assertEquals(GameplayModule.FULLBRIGHT,keys.onKey(s,37,5,true,false,true,m->true));assertFalse(s.fullbright());assertTrue(s.timeChanger());
    }
}
