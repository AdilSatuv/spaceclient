package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetFormatsTest {
    private byte[] fixture(String name)throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();
        }
    }
    @Test public void schemaTwelveMigrationPreservesLayoutAndFormatsAndCreatesVerifiedBackup()throws Exception {
        Path file=Files.createTempDirectory("space-widgets-v12").resolve("hud");byte[] old=fixture("schema12-57e1593.properties");Files.write(file,old);
        HudSettings s=HudSettings.open(file);assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));
        assertEquals(HudContentSettings.KeyLayout.LINE,s.getConfig().profile(0).content().keyLayout);
        assertEquals(HudContentSettings.SprintDisplay.FULL,s.getConfig().profile(0).content().sprintDisplay);
        assertFalse(s.getConfig().profile(0).content().pingLabel);assertFalse(s.getConfig().profile(0).content().keystrokesJump);
        assertEquals(1,s.getConfig().profile(0).content().packNames);assertEquals(31,s.getConfig().profile(0).placement(HudModule.KEYSTROKES).x,0);
        try(java.util.stream.Stream<Path> paths=Files.list(file.getParent())) {
            assertArrayEquals(old,Files.readAllBytes(paths.filter(p->p.getFileName().toString().startsWith("hud.v12-")).findFirst().get()));
        }
        assertTrue(s.content(s.getConfig().active().content().withKeyLayout(HudContentSettings.KeyLayout.BLOCK).withSprintDisplay(HudContentSettings.SprintDisplay.COMPACT)));
        HudConfig r=HudConfig.load(file);assertEquals(0,r.migrationSourceVersion());
        assertEquals(HudContentSettings.KeyLayout.BLOCK,r.profile(1).content().keyLayout);assertEquals(HudContentSettings.KeyLayout.LINE,r.profile(0).content().keyLayout);
        Path export=file.resolveSibling("export");HudConfig.exportProfile(export,r.profile(1));
        assertEquals(HudContentSettings.SprintDisplay.COMPACT,HudConfig.importProfile(export).content().sprintDisplay);
        Files.write(export,fixture("profile-v7-57e1593.properties"));HudProfile imported=HudConfig.importProfile(export);
        assertEquals("Widgets A",imported.name);assertFalse(imported.content().packLabel);assertEquals(HudContentSettings.KeyLayout.LINE,imported.content().keyLayout);
    }
    @Test public void unrelatedEditsRetainBothNewFormatsAndAllExistingContent() {
        HudContentSettings c=HudContentSettings.defaults().withKeyLayout(HudContentSettings.KeyLayout.BLOCK).withSprintDisplay(HudContentSettings.SprintDisplay.COMPACT)
            .withCoordinatesPrecision(2).withClockFormat(HudContentSettings.ClockFormat.H12).withClockSeconds(false).withFpsLabel(false)
            .withCpsMode(HudContentSettings.CpsMode.RIGHT).withKeystrokesJump(false).withKeystrokesMouse(false)
            .withPingLabel(false).withBiomeLabel(false).withPackLabel(false).withPackNames(3);
        assertEquals(HudContentSettings.KeyLayout.BLOCK,c.keyLayout);assertEquals(HudContentSettings.SprintDisplay.COMPACT,c.sprintDisplay);
        c=c.withKeyLayout(HudContentSettings.KeyLayout.LINE).withSprintDisplay(HudContentSettings.SprintDisplay.FULL);
        assertEquals(2,c.coordinatesPrecision);assertFalse(c.keystrokesMouse);assertFalse(c.keystrokesJump);assertEquals(3,c.packNames);assertFalse(c.packLabel);
    }
    @Test public void currentSnapshotsRejectMissingOrUnknownFormatsInsteadOfResettingThem()throws Exception {
        Path file=Files.createTempFile("space-widget-formats",".properties");new HudConfig().save(file);
        Properties original=new Properties();try(InputStream in=Files.newInputStream(file)){original.load(in);}
        for(String key:new String[]{"keystrokes.layout","sprint.display"})for(String bad:new String[]{null,"UNKNOWN"}) {
            Properties broken=new Properties();broken.putAll(original);
            if(bad==null)broken.remove("profile.0.content."+key);else broken.setProperty("profile.0.content."+key,bad);
            try(OutputStream out=Files.newOutputStream(file)){broken.store(out,"Broken");}
            try{HudConfig.load(file);fail(key);}catch(HudConfig.InvalidConfigException expected){}
        }
    }
}
