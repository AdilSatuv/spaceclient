package dev.spaceclient;
import dev.spaceclient.mods.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class VisualStyleModsTest {
 @Rule public TemporaryFolder dir=new TemporaryFolder();
 private Path file(){return dir.getRoot().toPath().resolve("mods");}
 private Properties read()throws Exception {Properties p=new Properties();try(InputStream in=Files.newInputStream(file())){p.load(in);}return p;}
 private void write(Properties p)throws Exception {try(OutputStream out=Files.newOutputStream(file())){p.store(out,"test");}}
 @Test public void realSchemaSevenPreservesElevenModulesAndDoesNotReserveNewKeys()throws Exception {
  byte[] bytes;try(InputStream in=getClass().getResourceAsStream("/hud-legacy/mods-schema7-0c2c0f5.properties")){
   assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
  Files.write(file(),bytes);ModSettings s=ModSettings.open(file());assertFalse(s.isReadOnly());assertArrayEquals(bytes,Files.readAllBytes(file()));
  int[] keys={30,31,32,33,37,38,39,51,52,58,41};for(GameplayModule m:GameplayModule.values()){
   boolean old=m.ordinal()<GameplayModule.BLOCK_OVERLAY.ordinal();assertEquals(old,m.enabled(s));assertEquals(old?keys[m.ordinal()]:0,s.toggleKey(m));}
  assertEquals(ModSettings.ChatTimeFormat.SECONDS,s.chatTimeFormat());assertEquals(ModSettings.DurabilityThreshold.TWENTY,s.durabilityThreshold());assertEquals(ModSettings.WeatherMode.SNOW,s.weatherMode());assertEquals(50,s.snaplookKey());
  assertEquals(90,s.cameraFov());assertEquals(0,s.dynamicFov());assertEquals(VisualPalette.AQUA,s.blockColor());assertEquals(ModSettings.BlockMode.BOTH,s.blockMode());assertEquals(20,s.blockOpacity());assertEquals(2,s.blockWidth());assertEquals(VisualPalette.VIOLET,s.damageColor());assertEquals(55,s.hitOpacity());assertFalse(s.hitPlayersOnly());
  assertTrue(s.toggleBlockOverlay());assertEquals("10",read().getProperty("schemaVersion"));assertEquals(132,read().size());s=ModSettings.open(file());assertFalse(s.isReadOnly());assertTrue(s.blockOverlay());assertTrue(s.chatTimestamps());assertEquals(41,s.toggleKey(GameplayModule.DURABILITY_WARNING));
 }
 @Test public void visualModesColorsAndBoundsSurviveRoundTrips()throws Exception {
  ModSettings s=ModSettings.open(file());s.toggleBlockOverlay();s.toggleHitColor();s.toggleFovControl();
  for(VisualPalette c:VisualPalette.values()){assertTrue(s.blockColor(c));assertTrue(s.damageColor(c));ModSettings saved=ModSettings.open(file());assertEquals(c,saved.blockColor());assertEquals(c,saved.damageColor());}
  for(ModSettings.BlockMode mode:ModSettings.BlockMode.values()){assertTrue(s.blockMode(mode));assertEquals(mode,ModSettings.open(file()).blockMode());}
  for(int end:new int[]{0,1}){assertTrue(s.blockOpacity(end==0?5:60));assertTrue(s.blockWidth(end==0?1:5));assertTrue(s.hitOpacity(end==0?5:80));assertTrue(s.cameraFov(end==0?60:110));assertTrue(s.dynamicFov(end==0?0:100));}
  s.hitPlayersOnly(true);s.toggleKey(GameplayModule.BLOCK_OVERLAY,40);s.toggleKey(GameplayModule.HIT_COLOR,43);s.toggleKey(GameplayModule.FOV_CONTROL,58);s=ModSettings.open(file());
  assertTrue(s.blockOverlay());assertTrue(s.hitColor());assertTrue(s.fovControl());assertEquals(60,s.blockOpacity());assertEquals(5,s.blockWidth());assertEquals(80,s.hitOpacity());assertEquals(110,s.cameraFov());assertEquals(100,s.dynamicFov());assertTrue(s.hitPlayersOnly());
  assertEquals(40,s.toggleKey(GameplayModule.BLOCK_OVERLAY));assertEquals(43,s.toggleKey(GameplayModule.HIT_COLOR));assertEquals(58,s.toggleKey(GameplayModule.FOV_CONTROL));
 }
 @Test public void eachResetPreservesOtherModulesAndEnabledState()throws Exception {
  ModSettings s=ModSettings.open(file());s.toggleBlockOverlay();s.toggleHitColor();s.toggleFovControl();s.blockColor(VisualPalette.RAINBOW);s.blockMode(ModSettings.BlockMode.FILL);s.blockOpacity(60);s.blockWidth(5);s.damageColor(VisualPalette.GOLD);s.hitOpacity(80);s.hitPlayersOnly(true);s.cameraFov(110);s.dynamicFov(100);
  s.toggleKey(GameplayModule.BLOCK_OVERLAY,40);s.toggleKey(GameplayModule.HIT_COLOR,43);s.toggleKey(GameplayModule.FOV_CONTROL,58);
  assertTrue(s.reset(GameplayModule.BLOCK_OVERLAY));assertTrue(s.blockOverlay());assertEquals(VisualPalette.AQUA,s.blockColor());assertEquals(ModSettings.BlockMode.BOTH,s.blockMode());assertEquals(20,s.blockOpacity());assertEquals(2,s.blockWidth());assertEquals(0,s.toggleKey(GameplayModule.BLOCK_OVERLAY));assertEquals(VisualPalette.GOLD,s.damageColor());assertEquals(110,s.cameraFov());
  assertTrue(s.reset(GameplayModule.HIT_COLOR));assertTrue(s.hitColor());assertEquals(VisualPalette.VIOLET,s.damageColor());assertEquals(55,s.hitOpacity());assertFalse(s.hitPlayersOnly());assertEquals(0,s.toggleKey(GameplayModule.HIT_COLOR));assertEquals(110,s.cameraFov());
  assertTrue(s.reset(GameplayModule.FOV_CONTROL));assertTrue(s.fovControl());assertEquals(90,s.cameraFov());assertEquals(0,s.dynamicFov());assertEquals(0,s.toggleKey(GameplayModule.FOV_CONTROL));
 }
 @Test public void malformedNewFieldsAndFutureSchemaStayReadOnlyWithoutPartialState()throws Exception {
  ModSettings s=ModSettings.open(file());s.toggleHitColor();Properties good=read();
  for(String[] change:new String[][]{{"blockOverlay",null},{"hitColor","yes"},{"fovControl",null},{"blockColor","UV"},{"damageColor","UV"},{"blockMode","GLOW"},{"blockOpacity","0"},{"blockWidth","6"},{"hitOpacity","81"},{"hitPlayersOnly","maybe"},{"cameraFov","111"},{"dynamicFov","-1"},{"toggleKey.BLOCK_OVERLAY",null},{"toggleKey.HIT_COLOR","256"},{"toggleKey.FOV_CONTROL",null},{"schemaVersion","11"},{"extra","keep"}}){
   Properties p=new Properties();p.putAll(good);if(change[1]==null)p.remove(change[0]);else p.setProperty(change[0],change[1]);write(p);byte[] bytes=Files.readAllBytes(file());ModSettings r=ModSettings.open(file());assertTrue(change[0],r.isReadOnly());assertFalse(r.toggleBlockOverlay());assertFalse(r.toggleHitColor());assertFalse(r.toggleFovControl());assertArrayEquals(bytes,Files.readAllBytes(file()));}
 }
 @Test public void invalidInputAndFailedIoRevertAllNewFields()throws Exception {
  ModSettings s=ModSettings.open(file());s.toggleFovControl();byte[] bytes=Files.readAllBytes(file());
  assertFalse(s.blockColor(null));assertFalse(s.damageColor(null));assertFalse(s.blockMode(null));assertFalse(s.blockOpacity(4));assertFalse(s.blockOpacity(61));assertFalse(s.blockWidth(0));assertFalse(s.blockWidth(6));assertFalse(s.hitOpacity(4));assertFalse(s.hitOpacity(81));assertFalse(s.cameraFov(59));assertFalse(s.cameraFov(111));assertFalse(s.dynamicFov(-1));assertFalse(s.dynamicFov(101));assertArrayEquals(bytes,Files.readAllBytes(file()));
  Path blocked=dir.newFile("blocked").toPath();s=ModSettings.open(blocked.resolve("mods"));assertFalse(s.toggleBlockOverlay());assertFalse(s.toggleHitColor());assertFalse(s.toggleFovControl());assertFalse(s.blockColor(VisualPalette.RAINBOW));assertFalse(s.damageColor(VisualPalette.GOLD));assertFalse(s.cameraFov(110));assertFalse(s.hitPlayersOnly(true));assertFalse(s.blockOverlay());assertFalse(s.hitColor());assertFalse(s.fovControl());assertEquals(90,s.cameraFov());assertFalse(s.hitPlayersOnly());
 }
 @Test public void rainbowIsPeriodicBoundedAndHandlesNegativeMonotonicOrigins(){
  for(VisualPalette p:VisualPalette.values())for(long t:new long[]{0,1,1000,2999,5999,-1,Long.MIN_VALUE,Long.MAX_VALUE}){int rgb=p.rgb(t);assertEquals(0,rgb&0xFF000000);assertNotEquals(0,rgb);}
  for(int t=0;t<6000;t+=347){assertEquals(VisualPalette.RAINBOW.rgb(t),VisualPalette.RAINBOW.rgb(t+6000));assertEquals(VisualPalette.AQUA.rgb(t),VisualPalette.AQUA.rgb(t+7654321));}
  assertNotEquals(VisualPalette.RAINBOW.rgb(0),VisualPalette.RAINBOW.rgb(3000));assertEquals(VisualPalette.RAINBOW.rgb(5999),VisualPalette.RAINBOW.rgb(-1));
 }
 @Test public void dynamicFovScalesSprintFlightAndBowAroundNeutralWithoutChangingBase(){
  for(float vanilla:new float[]{.85f,1,1.1f,1.15f,1.5f}){
   assertEquals(1,FovRuntime.dynamic(vanilla,0),0);assertEquals(vanilla,FovRuntime.dynamic(vanilla,100),.000001f);assertEquals((1+vanilla)/2,FovRuntime.dynamic(vanilla,50),.000001f);}
  assertEquals(1,FovRuntime.dynamic(1.15f,-1),0);assertEquals(1.15f,FovRuntime.dynamic(1.15f,101),.000001f);assertFalse(FovRuntime.active(null));
 }
 @Test public void newShortcutsKeepModulesIndependentAndRespectContext()throws Exception {
  ModSettings s=ModSettings.open(file());s.toggleKey(GameplayModule.BLOCK_OVERLAY,40);s.toggleKey(GameplayModule.HIT_COLOR,43);s.toggleKey(GameplayModule.FOV_CONTROL,58);ModuleShortcuts k=new ModuleShortcuts();
  assertNull(k.onKey(s,40,1,true,false,false,m->true));assertNull(k.onKey(s,40,2,true,true,true,m->true));assertNull(k.onKey(s,40,3,true,false,true,m->false));
  assertEquals(GameplayModule.BLOCK_OVERLAY,k.onKey(s,40,4,true,false,true,m->true));assertTrue(s.blockOverlay());assertFalse(s.hitColor());assertFalse(s.fovControl());assertEquals(GameplayModule.HIT_COLOR,k.onKey(s,43,5,true,false,true,m->true));assertEquals(GameplayModule.FOV_CONTROL,k.onKey(s,58,6,true,false,true,m->true));assertTrue(s.hitColor());assertTrue(s.fovControl());
 }
}
