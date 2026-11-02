package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContextContentTest {
    private byte[] fixture(String name) throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/hud-legacy/"+name)) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();
        }
    }
    @Test public void realSchemaElevenAddsFormatsWithoutChangingOldFlagsLayoutOrContent() throws Exception {
        Path file=Files.createTempDirectory("space-context-v11").resolve("hud");byte[] old=fixture("schema11-78546a6.properties");
        Files.write(file,old);HudSettings s=HudSettings.open(file);assertFalse(s.isReadOnly());assertArrayEquals(old,Files.readAllBytes(file));
        assertEquals(1,s.getConfig().activeIndex());assertTrue(s.getConfig().profile(0).ping);assertTrue(s.getConfig().profile(0).packs);
        assertTrue(s.getConfig().profile(1).biome);assertFalse(s.getConfig().profile(0).content().keystrokesMouse);
        assertEquals(31,s.getConfig().profile(0).placement(HudModule.PING).x,0);
        try(java.util.stream.Stream<Path> files=Files.list(file.getParent())) {
            Path backup=files.filter(p->p.getFileName().toString().startsWith("hud.v11-")).findFirst().get();
            assertArrayEquals(old,Files.readAllBytes(backup));
        }
        assertTrue(s.content(s.getConfig().active().content().withPingLabel(false).withBiomeLabel(false).withPackLabel(false).withPackNames(1)));
        HudConfig r=HudConfig.load(file);assertEquals(0,r.migrationSourceVersion());
        assertFalse(r.profile(1).content().pingLabel);assertTrue(r.profile(0).content().pingLabel);assertEquals(1,r.profile(1).content().packNames);
        Path export=file.resolveSibling("profile");HudConfig.exportProfile(export,r.profile(1));
        HudProfile imported=HudConfig.importProfile(export);assertFalse(imported.content().packLabel);assertEquals(1,imported.content().packNames);
        Files.write(export,fixture("profile-v6-78546a6.properties"));imported=HudConfig.importProfile(export);
        assertTrue(imported.ping);assertFalse(imported.content().keystrokesJump);assertTrue(imported.content().packLabel);assertEquals(2,imported.content().packNames);
    }
    @Test public void unrelatedContentEditsPreserveContextFormatsAndLabelsHandleUnknownValues() {
        HudContentSettings c=HudContentSettings.defaults().withPingLabel(false).withBiomeLabel(false).withPackLabel(false).withPackNames(3)
            .withCoordinatesPrecision(2).withClockFormat(HudContentSettings.ClockFormat.H12).withClockSeconds(false)
            .withFpsLabel(false).withCpsMode(HudContentSettings.CpsMode.RIGHT).withKeystrokesJump(false).withKeystrokesMouse(false);
        assertFalse(c.pingLabel);assertFalse(c.biomeLabel);assertFalse(c.packLabel);assertEquals(3,c.packNames);
        assertEquals("42 ms",ContextLabels.ping(false,42,false));assertEquals("--",ContextLabels.ping(false,-1,false));
        assertEquals("Singleplayer",ContextLabels.ping(true,null,false));assertEquals("Plains",ContextLabels.biome("Plains",false));
        List<String> packs=Arrays.asList("One","Two","Three");
        assertEquals("One +2",ContextLabels.packs(packs,false,false,1));
        assertEquals("Packs: Server, One +2",ContextLabels.packs(packs,true,true,2));
        assertEquals("Server, One, Two +1",ContextLabels.packs(packs,true,false,3));
        assertEquals("Default",ContextLabels.packs(Collections.emptyList(),false,false,1));
    }
    @Test public void partialAndInvalidCurrentContentCannotSilentlyResetAProfile() throws Exception {
        Path file=Files.createTempFile("space-context-format",".properties");new HudConfig().save(file);
        Properties original=new Properties();try(InputStream in=Files.newInputStream(file)){original.load(in);}
        for(String key:Arrays.asList("ping.label","biome.label","packs.label","packs.names")) {
            Properties broken=new Properties();broken.putAll(original);broken.remove("profile.0.content."+key);
            try(OutputStream out=Files.newOutputStream(file)){broken.store(out,"Missing field");}
            try{HudConfig.load(file);fail(key);}catch(HudConfig.InvalidConfigException expected){}
        }
        for(String value:Arrays.asList("0","4","abc")) {
            Properties broken=new Properties();broken.putAll(original);broken.setProperty("profile.0.content.packs.names",value);
            try(OutputStream out=Files.newOutputStream(file)){broken.store(out,"Bad count");}
            try{HudConfig.load(file);fail(value);}catch(HudConfig.InvalidConfigException expected){}
        }
    }
}
