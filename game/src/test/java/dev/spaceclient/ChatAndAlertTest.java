package dev.spaceclient;
import dev.spaceclient.mods.*;
import dev.spaceclient.hud.DurabilityWarningRenderer;
import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.Properties;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class ChatAndAlertTest {
    @Rule public TemporaryFolder dir=new TemporaryFolder();
    private Path file(){return dir.getRoot().toPath().resolve("mods");}
    private Properties read()throws Exception{Properties p=new Properties();try(InputStream in=Files.newInputStream(file())){p.load(in);}return p;}
    private void write(Properties p)throws Exception{try(OutputStream out=Files.newOutputStream(file())){p.store(out,"test");}}
    @Test public void localTimeFormatsCoverMidnightNoonSecondsAndLocaleIndependentAmPm(){
        assertEquals("[00:02] ",ChatTimestamp.prefix(LocalTime.of(0,2,7),ModSettings.ChatTimeFormat.HOURS_MINUTES));
        assertEquals("[23:59:58] ",ChatTimestamp.prefix(LocalTime.of(23,59,58),ModSettings.ChatTimeFormat.SECONDS));
        assertEquals("[12:02 AM] ",ChatTimestamp.prefix(LocalTime.of(0,2),ModSettings.ChatTimeFormat.TWELVE_HOUR));
        assertEquals("[12:02 PM] ",ChatTimestamp.prefix(LocalTime.of(12,2),ModSettings.ChatTimeFormat.TWELVE_HOUR));
        assertEquals("[11:02 PM] ",ChatTimestamp.prefix(LocalTime.of(23,2),ModSettings.ChatTimeFormat.TWELVE_HOUR));
    }
    @Test public void warningThresholdUsesExactFractionsAndIncludesBrokenItems(){
        assertTrue(DurabilityAlert.low(true,90,100,10));assertFalse(DurabilityAlert.low(true,89,100,10));
        assertFalse(DurabilityAlert.low(true,89,99,10));assertTrue(DurabilityAlert.low(true,90,99,10));
        assertTrue(DurabilityAlert.low(true,100,100,5));assertTrue(DurabilityAlert.low(true,101,100,5));
        assertFalse(DurabilityAlert.low(true,-1,100,20));assertFalse(DurabilityAlert.low(false,99,100,20));
        assertFalse(DurabilityAlert.low(true,99,0,20));assertFalse(DurabilityAlert.low(true,99,-1,20));
        assertFalse(DurabilityAlert.low(true,99,100,0));assertFalse(DurabilityAlert.low(true,99,100,101));
        assertTrue(DurabilityAlert.low(true,Integer.MAX_VALUE,Integer.MAX_VALUE,5));
        assertFalse(DurabilityAlert.low(true,0,Integer.MAX_VALUE,20));
    }
    private static class Tool extends Item { Tool(int max){setMaxDamage(max);} }
    @Test public void worstEquippedStackUsesRelativeDurabilityWithoutChangingItems(){
        ItemStack a=new ItemStack(new Tool(100),1,91),b=new ItemStack(new Tool(1000),1,950),full=new ItemStack(new Tool(100),1,0);
        ItemStack empty=new ItemStack(new Tool(100),0,99),invalid=new ItemStack((Item)null,1,99),plain=new ItemStack(new Item());
        assertSame(b,DurabilityWarningRenderer.lowest(new ItemStack[]{a,b,full,empty,invalid,plain},10));
        assertSame(a,DurabilityWarningRenderer.lowest(new ItemStack[]{a},10));assertNull(DurabilityWarningRenderer.lowest(new ItemStack[]{full,empty,invalid,plain},20));
        assertNull(DurabilityWarningRenderer.lowest(null,10));assertEquals(91,a.getDamage());assertEquals(950,b.getDamage());assertEquals(1,b.count);
    }
    @Test public void realSchemaSixKeepsAllNineOldModsAndNewModsAreOffAndUnbound()throws Exception {
        byte[] bytes;try(InputStream in=getClass().getResourceAsStream("/hud-legacy/mods-schema6-bb5739c.properties")){
            assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
        Files.write(file(),bytes);ModSettings s=ModSettings.open(file());assertFalse(s.isReadOnly());assertArrayEquals(bytes,Files.readAllBytes(file()));
        int[] keys={30,31,32,33,37,38,39,51,52};for(GameplayModule m:GameplayModule.values()){
            boolean old=m.ordinal()<GameplayModule.CHAT_TIMESTAMPS.ordinal();assertEquals(old,m.enabled(s));assertEquals(old?keys[m.ordinal()]:0,s.toggleKey(m));}
        assertEquals(50,s.snaplookKey());assertEquals(ModSettings.SnapPerspective.BACK,s.snapPerspective());assertEquals(ModSettings.WeatherMode.SNOW,s.weatherMode());
        assertEquals(48,s.freeLookKey());assertEquals(34,s.zoomKey());assertEquals(7,s.zoomFactor());assertFalse(s.zoomSmooth());
        assertEquals(ModSettings.ChatTimeFormat.HOURS_MINUTES,s.chatTimeFormat());assertEquals(ModSettings.DurabilityThreshold.TEN,s.durabilityThreshold());
        assertTrue(s.toggleChatTimestamps());assertEquals("9",read().getProperty("schemaVersion"));assertEquals(94,read().size());
        s=ModSettings.open(file());assertFalse(s.isReadOnly());assertTrue(s.chatTimestamps());assertTrue(s.snaplook());assertEquals(52,s.toggleKey(GameplayModule.WEATHER_CHANGER));
    }
    @Test public void eachFormatAndThresholdPersistsAcrossIndependentToggles()throws Exception {
        ModSettings s=ModSettings.open(file());for(ModSettings.ChatTimeFormat f:ModSettings.ChatTimeFormat.values()){
            assertTrue(s.chatTimeFormat(f));assertEquals(f,ModSettings.open(file()).chatTimeFormat());}
        for(ModSettings.DurabilityThreshold t:ModSettings.DurabilityThreshold.values()){
            assertTrue(s.durabilityThreshold(t));assertEquals(t,ModSettings.open(file()).durabilityThreshold());}
        s.toggleChatTimestamps();s.toggleDurabilityWarning();s.toggleKey(GameplayModule.CHAT_TIMESTAMPS,53);s.toggleKey(GameplayModule.DURABILITY_WARNING,54);
        s=ModSettings.open(file());assertTrue(s.chatTimestamps());assertTrue(s.durabilityWarning());assertEquals(53,s.toggleKey(GameplayModule.CHAT_TIMESTAMPS));assertEquals(54,s.toggleKey(GameplayModule.DURABILITY_WARNING));
    }
    @Test public void resetPreservesEnabledAndOtherModulePreferences()throws Exception {
        ModSettings s=ModSettings.open(file());s.toggleChatTimestamps();s.toggleDurabilityWarning();s.chatTimeFormat(ModSettings.ChatTimeFormat.SECONDS);
        s.durabilityThreshold(ModSettings.DurabilityThreshold.TWENTY);s.toggleKey(GameplayModule.CHAT_TIMESTAMPS,53);s.toggleKey(GameplayModule.DURABILITY_WARNING,54);
        assertTrue(s.reset(GameplayModule.CHAT_TIMESTAMPS));assertTrue(s.chatTimestamps());assertEquals(ModSettings.ChatTimeFormat.HOURS_MINUTES,s.chatTimeFormat());
        assertEquals(0,s.toggleKey(GameplayModule.CHAT_TIMESTAMPS));assertEquals(54,s.toggleKey(GameplayModule.DURABILITY_WARNING));assertEquals(ModSettings.DurabilityThreshold.TWENTY,s.durabilityThreshold());
        assertTrue(s.reset(GameplayModule.DURABILITY_WARNING));assertTrue(s.durabilityWarning());assertEquals(ModSettings.DurabilityThreshold.TEN,s.durabilityThreshold());assertEquals(0,s.toggleKey(GameplayModule.DURABILITY_WARNING));
    }
    @Test public void malformedFutureAndPartialSettingsAreReadOnlyAndOriginalBytesSurvive()throws Exception {
        ModSettings s=ModSettings.open(file());s.toggleChatTimestamps();Properties good=read();
        for(String[] change:new String[][]{{"chatTimestamps",null},{"durabilityWarning","yes"},{"chatTimeFormat","UTC"},{"durabilityThreshold","ZERO"},
            {"toggleKey.CHAT_TIMESTAMPS",null},{"toggleKey.DURABILITY_WARNING","256"},{"schemaVersion","10"},{"schemaVersion","07"},{"unknown","keep"}}){
            Properties p=new Properties();p.putAll(good);if(change[1]==null)p.remove(change[0]);else p.setProperty(change[0],change[1]);write(p);byte[] bytes=Files.readAllBytes(file());
            ModSettings r=ModSettings.open(file());assertTrue(change[0],r.isReadOnly());assertFalse(r.toggleChatTimestamps());assertFalse(r.toggleDurabilityWarning());assertArrayEquals(bytes,Files.readAllBytes(file()));}
    }
    @Test public void invalidOptionsAndFailedIoRevertTheWholeChange()throws Exception {
        ModSettings s=ModSettings.open(file());s.toggleChatTimestamps();byte[] bytes=Files.readAllBytes(file());assertFalse(s.chatTimeFormat(null));assertFalse(s.durabilityThreshold(null));assertArrayEquals(bytes,Files.readAllBytes(file()));
        Path blocked=dir.newFile("blocked").toPath();s=ModSettings.open(blocked.resolve("mods"));assertFalse(s.toggleChatTimestamps());assertFalse(s.toggleDurabilityWarning());assertFalse(s.chatTimeFormat(ModSettings.ChatTimeFormat.SECONDS));assertFalse(s.durabilityThreshold(ModSettings.DurabilityThreshold.TWENTY));
        assertFalse(s.chatTimestamps());assertFalse(s.durabilityWarning());assertEquals(ModSettings.ChatTimeFormat.HOURS_MINUTES,s.chatTimeFormat());assertEquals(ModSettings.DurabilityThreshold.TEN,s.durabilityThreshold());
    }
    @Test public void newShortcutsRespectGuiRepeatsAndOtherModuleState()throws Exception {
        ModSettings s=ModSettings.open(file());s.toggleKey(GameplayModule.CHAT_TIMESTAMPS,53);s.toggleKey(GameplayModule.DURABILITY_WARNING,54);ModuleShortcuts k=new ModuleShortcuts();
        assertNull(k.onKey(s,53,1,true,false,false,m->true));assertNull(k.onKey(s,53,2,true,true,true,m->true));
        assertEquals(GameplayModule.CHAT_TIMESTAMPS,k.onKey(s,53,3,true,false,true,m->true));assertTrue(s.chatTimestamps());assertFalse(s.durabilityWarning());
        assertNull(k.onKey(s,54,4,true,false,true,m->false));assertEquals(GameplayModule.DURABILITY_WARNING,k.onKey(s,54,5,true,false,true,m->true));assertTrue(s.durabilityWarning());assertTrue(s.chatTimestamps());
    }
}
