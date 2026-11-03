package dev.spaceclient;
import dev.spaceclient.mods.*;
import java.io.*;import java.nio.file.*;import java.util.*;
import org.junit.*;import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class ClientPackTest {
 @Rule public TemporaryFolder dir=new TemporaryFolder();
 private Path file(){return dir.getRoot().toPath().resolve("mods");}
 private Properties read()throws Exception{Properties p=new Properties();try(InputStream in=Files.newInputStream(file())){p.load(in);}return p;}
 private void write(Properties p)throws Exception{try(OutputStream out=Files.newOutputStream(file())){p.store(out,"test");}}
 @Test public void realSchemaEightPreservesFourteenModulesAndNewControlsStayUnbound()throws Exception{
  byte[] bytes;try(InputStream in=getClass().getResourceAsStream("/hud-legacy/mods-schema8-154db3d.properties")){assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
  Files.write(file(),bytes);ModSettings s=ModSettings.open(file());assertFalse(s.isReadOnly());assertArrayEquals(bytes,Files.readAllBytes(file()));
  int[] keys={30,31,32,33,37,38,39,51,52,58,41,40,43,44};for(GameplayModule m:GameplayModule.values()){boolean old=m.ordinal()<GameplayModule.ANIMATIONS.ordinal();assertEquals(old,m.enabled(s));assertEquals(old?keys[m.ordinal()]:0,s.toggleKey(m));}
  for(int i=0;i<3;i++){assertEquals(0,s.autoTextKey(i));assertEquals("",s.autoTextMessage(i));}
  assertEquals(VisualPalette.RAINBOW,s.blockColor());assertEquals(105,s.cameraFov());assertEquals(50,s.dynamicFov());assertEquals(35,s.blockOpacity());assertEquals(VisualPalette.GOLD,s.damageColor());assertTrue(s.hitPlayersOnly());assertEquals(ModSettings.WeatherMode.SNOW,s.weatherMode());assertEquals(50,s.snaplookKey());
  assertTrue(s.toggleAnimations());assertEquals("10",read().getProperty("schemaVersion"));assertEquals(132,read().size());s=ModSettings.open(file());assertTrue(s.animations());assertTrue(s.fovControl());assertEquals(44,s.toggleKey(GameplayModule.FOV_CONTROL));
 }
 @Test public void newOptionsAndThreeIndependentMessagesRoundTripWithoutArrayAliasing()throws Exception{
  ModSettings s=ModSettings.open(file());for(GameplayModule m:GameplayModule.values())if(m.ordinal()>=GameplayModule.ANIMATIONS.ordinal()&&m.ordinal()<GameplayModule.SCOREBOARD.ordinal())assertTrue(m.toggle(s));
  s.animationBlockHit(false);s.animationEat(false);s.animationBow(false);s.guiScale(4);s.hitboxColor(VisualPalette.VIOLET);s.hitboxWidth(3);s.hitboxPlayers(false);s.hitboxMobs(false);s.hitboxItems(true);s.physicsSpeed(5);s.physicsFlat(false);s.heldItemScale(50);s.droppedItemScale(200);s.blockSounds(ModSettings.SoundTheme.ARCADE);s.combatSounds(ModSettings.SoundTheme.CRISP);s.killChime(ModSettings.KillChime.LEVEL_UP);s.soundsVolume(35);
  for(int i=0;i<3;i++){assertTrue(s.autoTextKey(i,40+i));assertTrue(s.autoTextMessage(i,"/test "+i));}s.zoomFactor(8);s=ModSettings.open(file());
  for(GameplayModule m:GameplayModule.values())if(m.ordinal()>=GameplayModule.ANIMATIONS.ordinal()&&m.ordinal()<GameplayModule.SCOREBOARD.ordinal())assertTrue(m.enabled(s));
  assertFalse(s.animationBlockHit());assertFalse(s.animationEat());assertFalse(s.animationBow());assertEquals(4,s.guiScale());assertEquals(VisualPalette.VIOLET,s.hitboxColor());assertEquals(3,s.hitboxWidth());assertFalse(s.hitboxPlayers());assertFalse(s.hitboxMobs());assertTrue(s.hitboxItems());assertEquals(5,s.physicsSpeed());assertFalse(s.physicsFlat());assertEquals(50,s.heldItemScale());assertEquals(200,s.droppedItemScale());assertEquals(ModSettings.SoundTheme.ARCADE,s.blockSounds());assertEquals(ModSettings.SoundTheme.CRISP,s.combatSounds());assertEquals(ModSettings.KillChime.LEVEL_UP,s.killChime());assertEquals(35,s.soundsVolume());
  for(int i=0;i<3;i++){assertEquals(40+i,s.autoTextKey(i));assertEquals("/test "+i,s.autoTextMessage(i));}byte[] bytes=Files.readAllBytes(file());assertFalse(s.autoTextKey(1,256));assertFalse(s.autoTextMessage(1,"bad\nline"));assertArrayEquals(bytes,Files.readAllBytes(file()));assertEquals("/test 1",s.autoTextMessage(1));
 }
 @Test public void resetOnlySelectedOptionsAndKeysWhileKeepingEnabled()throws Exception{
  ModSettings s=ModSettings.open(file());for(GameplayModule m:GameplayModule.values())if(m.ordinal()>=GameplayModule.ANIMATIONS.ordinal()&&m.ordinal()<GameplayModule.SCOREBOARD.ordinal()){m.toggle(s);s.toggleKey(m,40+m.ordinal());}
  s.animationBow(false);s.guiScale(4);s.hitboxItems(true);s.physicsSpeed(5);s.heldItemScale(150);s.blockSounds(ModSettings.SoundTheme.ARCADE);s.autoTextMessage(0,"keep");s.autoTextKey(0,40);
  assertTrue(s.reset(GameplayModule.ANIMATIONS));assertTrue(s.animations());assertTrue(s.animationBow());assertEquals(4,s.guiScale());assertEquals("keep",s.autoTextMessage(0));
  assertTrue(s.reset(GameplayModule.GUI));assertTrue(s.gui());assertEquals(2,s.guiScale());assertTrue(s.hitboxItems());
  assertTrue(s.reset(GameplayModule.HITBOXES));assertTrue(s.hitboxes());assertFalse(s.hitboxItems());assertEquals(5,s.physicsSpeed());
  assertTrue(s.reset(GameplayModule.ITEM_PHYSICS));assertTrue(s.itemPhysics());assertEquals(2,s.physicsSpeed());assertEquals(150,s.heldItemScale());
  assertTrue(s.reset(GameplayModule.ITEM_SIZE));assertTrue(s.itemSize());assertEquals(80,s.heldItemScale());assertEquals(ModSettings.SoundTheme.ARCADE,s.blockSounds());
  assertTrue(s.reset(GameplayModule.SOUNDS));assertTrue(s.sounds());assertEquals(ModSettings.SoundTheme.CRISP,s.blockSounds());assertEquals("keep",s.autoTextMessage(0));
  assertTrue(s.reset(GameplayModule.AUTO_TEXT));assertTrue(s.autoText());for(int i=0;i<3;i++){assertEquals(0,s.autoTextKey(i));assertEquals("",s.autoTextMessage(i));}
  for(GameplayModule m:GameplayModule.values())if(m.ordinal()>=GameplayModule.ANIMATIONS.ordinal()&&m.ordinal()<GameplayModule.SCOREBOARD.ordinal())assertEquals(0,s.toggleKey(m));
 }
 @Test public void malformedOrFutureSettingsArePreservedReadOnly()throws Exception{
  ModSettings s=ModSettings.open(file());s.toggleSounds();Properties good=read();
  for(String[] change:new String[][]{{"animations",null},{"autoText","yes"},{"guiScale","5"},{"hitboxWidth","0"},{"physicsSpeed","6"},{"heldItemScale","49"},{"droppedItemScale","201"},{"blockSounds","BOOM"},{"killChime","BELL"},{"soundsVolume","101"},{"autoText.key.2",null},{"autoText.message.1","bad\nline"},{"schemaVersion","11"},{"schemaVersion","09"},{"extra","keep"}}){Properties p=new Properties();p.putAll(good);if(change[1]==null)p.remove(change[0]);else p.setProperty(change[0],change[1]);write(p);byte[] bytes=Files.readAllBytes(file());ModSettings r=ModSettings.open(file());assertTrue(change[0],r.isReadOnly());assertFalse(r.toggleSounds());assertArrayEquals(bytes,Files.readAllBytes(file()));}
  Files.write(file(),new byte[8193]);assertTrue(ModSettings.open(file()).isReadOnly());
 }
 @Test public void invalidInputAndIoFailureCannotChangeNewState()throws Exception{
  ModSettings s=ModSettings.open(file());s.toggleAnimations();byte[] bytes=Files.readAllBytes(file());assertFalse(s.guiScale(-1));assertFalse(s.physicsSpeed(0));assertFalse(s.hitboxColor(null));assertFalse(s.blockSounds(null));assertFalse(s.killChime(null));assertFalse(s.autoTextMessage(3,"x"));assertFalse(s.autoTextKey(-1,40));assertArrayEquals(bytes,Files.readAllBytes(file()));
  Path blocked=dir.newFile("blocked").toPath();s=ModSettings.open(blocked.resolve("mods"));assertFalse(s.toggleSounds());assertFalse(s.toggleAutoText());assertFalse(s.autoTextMessage(0,"keep"));assertFalse(s.guiScale(3));assertEquals(2,s.guiScale());assertEquals("",s.autoTextMessage(0));assertFalse(s.sounds());
 }
 @Test public void messagesAreBoundedSingleLineUnicodeAndDoNotAdmitControlCharacters(){
  assertTrue(ModSettings.validText(""));assertTrue(ModSettings.validText("/command argument"));assertTrue(ModSettings.validText("Привет мир"));assertTrue(ModSettings.validText(String.join("",Collections.nCopies(100,"я"))));assertFalse(ModSettings.validText(String.join("",Collections.nCopies(101,"x"))));assertFalse(ModSettings.validText(null));assertFalse(ModSettings.validText("a\r\nb"));assertFalse(ModSettings.validText("a"+(char)0x202E+"b"));assertFalse(ModSettings.validText("a"+(char)0x00A7+"cb"));assertFalse(ModSettings.validText(new String(new char[]{(char)0xD800})));
 }
 @Test public void macroGateDebouncesAcrossSlotsAndRequiresGameplay()throws Exception{
  ModSettings s=ModSettings.open(file());s.toggleAutoText();s.autoTextKey(0,40);s.autoTextMessage(0,"first");s.autoTextKey(1,41);s.autoTextMessage(1,"second");AutoTextGate g=new AutoTextGate();
  assertEquals("",g.trigger(s,40,1,false));assertEquals("first",g.trigger(s,40,1,true));assertEquals("",g.trigger(s,40,1,true));assertEquals("",g.trigger(s,41,999999999L,true));assertEquals("second",g.trigger(s,41,1000000001L,true));g.clear();assertEquals("first",g.trigger(s,40,1,true));s.toggleAutoText();assertEquals("",g.trigger(s,40,2000000001L,true));
 }
 @Test public void emptyAndUnboundMacrosDoNothingAndBindingsCanDetectCollisions()throws Exception{
  ModSettings s=ModSettings.open(file());s.toggleAutoText();AutoTextGate g=new AutoTextGate();assertEquals("",g.trigger(s,0,1,true));s.autoTextKey(0,40);assertEquals("",g.trigger(s,40,1,true));s.autoTextMessage(0,"   ");assertEquals("",g.trigger(s,40,2,true));assertTrue(ModuleKeys.usedByAutoText(s,-1,40));assertFalse(ModuleKeys.usedByAutoText(s,0,40));s.autoTextKey(1,40);assertTrue(ModuleKeys.usedByAutoText(s,0,40));assertFalse(ModuleKeys.usedByAutoText(s,-1,0));
 }
 @Test public void cosmeticSwingExpiresAndCanRestartWithoutAttacks(){SwingClock c=new SwingClock();assertEquals(0,c.progress(0),0);c.begin(0);assertTrue(c.progress(125000000L)>.49f);assertEquals(0,c.progress(250000000L),0);c.begin(300000000L);assertEquals(0,c.progress(299999999L),0);assertTrue(c.progress(425000000L)>.49f);}
 @Test public void targetDeathCueIsSameTargetRecentAndSingleUse(){RecentTarget r=new RecentTarget();Object a=new Object(),b=new Object();r.mark(a,1);assertFalse(r.consume(b,2));assertTrue(r.consume(a,3000000001L));assertFalse(r.consume(a,3000000002L));r.mark(a,1);assertFalse(r.consume(a,3000000002L));r.mark(a,10);assertFalse(r.consume(a,9));r.mark(a,1);r.clear();assertFalse(r.consume(a,2));}
 @Test public void soundMappingsPreserveUnknownNamespacesAndClassifyOnlySupportedEvents(){
  for(String id:new String[]{"minecraft:dig.stone","step.wood"}){assertEquals(1,SoundPolicy.group(id));assertEquals("dig.cloth",SoundPolicy.map(id,ModSettings.SoundTheme.SOFT));assertEquals("note.hat",SoundPolicy.map(id,ModSettings.SoundTheme.ARCADE));assertEquals(id,SoundPolicy.map(id,ModSettings.SoundTheme.VANILLA));}
  for(String id:new String[]{"minecraft:game.player.hurt","mob.zombie.hurt"}){assertEquals(2,SoundPolicy.group(id));assertEquals("random.successful_hit",SoundPolicy.map(id,ModSettings.SoundTheme.CRISP));}
  for(String id:new String[]{"music.game","mob.zombie.death","custom:dig.stone","note.pling"}){assertEquals(0,SoundPolicy.group(id));assertEquals(id,SoundPolicy.map(id,ModSettings.SoundTheme.ARCADE));}
  for(ModSettings.SoundTheme t:ModSettings.SoundTheme.values())assertTrue(SoundPolicy.pitch(t)>=.5&&SoundPolicy.pitch(t)<=2);
 }
 @Test public void sizeAndGuiOverridesRestoreNativeValuesWhenDisabled()throws Exception{
  ModSettings previous=SpaceClient.mods;try{ModSettings s=ModSettings.open(file());SpaceClient.mods=s;assertEquals(3,GuiScaleRuntime.requested(3));assertEquals(1,ItemPresentation.heldScale(),0);s.toggleGui();s.guiScale(1);assertEquals(1,GuiScaleRuntime.requested(3));s.toggleItemSize();s.heldItemScale(50);s.droppedItemScale(200);assertEquals(.5,ItemPresentation.heldScale(),0);assertEquals(2,ItemPresentation.droppedScale(),0);s.toggleItemSize();assertEquals(1,ItemPresentation.heldScale(),0);s.toggleGui();assertEquals(3,GuiScaleRuntime.requested(3));assertNotEquals(ItemPresentation.spin(1,1,7),ItemPresentation.spin(2,1,7),0);}finally{SpaceClient.mods=previous;}
 }
}
