package dev.spaceclient.qa;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.freelook.FreeLookRuntime;
import dev.spaceclient.hud.*;
import dev.spaceclient.mods.*;
import dev.spaceclient.qa.mixin.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.level.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.*;
import org.lwjgl.opengl.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Disposable native handler and TCP checks. No OS input, public listener or user world. */
public final class InputNetworkCheck {
 private static final class Retry extends RuntimeException {}
 private static long caseStarted;
 interface Check {void run() throws Exception;}
 private static final Path sync=Paths.get(System.getProperty("space.qa.sync"));
 private static final String role=System.getProperty("space.qa.role");
 private static final boolean host=role.startsWith("host-");
 private static final List<Check> checks=new ArrayList<>();
 private static final List<String> names=new ArrayList<>();
 private static final Set<Integer> held=new HashSet<>();
 private static int frames,stage,next,port,events,shortcutChecks,hudChecks,bodyChecks;
 public static int chimes,closedPlayPackets;
 private static boolean stopped,virtualFocus,forward,pigRequested,killRequested;
 private static String pending,contextA;
 private static Object firstPlayer;
 private static float yaw,pitch,fov,mouseSensitivity,projection;
 private static long eventNanos=System.nanoTime(),macroNanos;
 private static PigEntity pig;
 private static Entity clientPig;
 private static final long started=System.nanoTime();
 private static void eventually(boolean b,String message){if(!b){require(System.nanoTime()-caseStarted<5_000_000_000L,message);throw new Retry();}}
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
 private static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
 private static void write(String name,String text)throws Exception{Files.write(sync.resolve(name),text.getBytes(StandardCharsets.UTF_8));}
 private static int readPort(String id)throws Exception{return Integer.parseInt(new String(Files.readAllBytes(sync.resolve(id+".port")),StandardCharsets.UTF_8).trim());}
 public static synchronized void chat(String text){if(host&&text.startsWith("SPACE_QA_"))try{Files.write(sync.resolve(role+"-chat.txt"),(text+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
 private static int chatCount(String id)throws Exception{Path p=sync.resolve(id+"-chat.txt");return Files.exists(p)?Files.readAllLines(p,StandardCharsets.UTF_8).size():0;}
 public static void apply(MinecraftClient c){
  if(host||stage<2||stopped)return;
  try{
   ByteBuffer keys=(ByteBuffer)field(Keyboard.class,"keyDownBuffer").get(null);
   for(int key:new int[]{Keyboard.KEY_V,Keyboard.KEY_C,Keyboard.KEY_W})keys.put(key,(byte)(held.contains(key)?1:0));
   c.focused=virtualFocus;
   if(c.options!=null)KeyBinding.setKeyPressed(c.options.forwardKey.getCode(),forward);
  }catch(Exception e){fail(c,e);}
 }
 private static void event(MinecraftClient c,int key,boolean down,boolean repeat,boolean duplicate)throws Exception{eventAt(c,key,down,repeat,duplicate,++eventNanos);}
 private static void eventAt(MinecraftClient c,int key,boolean down,boolean repeat,boolean duplicate,long nanos)throws Exception{
  Object e=field(Keyboard.class,"current_event").get(null);Map<Field,Object> before=new LinkedHashMap<>();
  Object[] values={key,down,0,nanos,repeat};String[] keys={"key","state","character","nanos","repeat"};
  try{for(int i=0;i<keys.length;i++){Field f=field(e.getClass(),keys[i]);before.put(f,f.get(e));f.set(e,values[i]);}c.handleKeyInput();events++;if(duplicate){c.handleKeyInput();events++;}}
  finally{for(Map.Entry<Field,Object> item:before.entrySet())item.getKey().set(e,item.getValue());}
 }
 private static void add(String name,Check check){names.add(name);checks.add(check);}
 private static void gameplay(MinecraftClient c){virtualFocus=true;held.clear();forward=false;c.setScreen(null);c.focused=true;}
 private static void disconnect(MinecraftClient c){held.clear();forward=false;virtualFocus=false;c.world.disconnect();c.connect(null);c.setScreen(new TitleScreen());}
 private static void connect(MinecraftClient c,String id)throws Exception{
  int p=readPort(id);ServerInfo info=new ServerInfo("Private QA "+id,"127.0.0.1:"+p,false);
  c.setCurrentServerEntry(info);c.setScreen(new ConnectScreen(new TitleScreen(),c,info));
 }
 private static boolean joined(MinecraftClient c){return c.world!=null&&c.player!=null&&c.getNetworkHandler()!=null&&c.currentScreen==null;}
 private static void setup(MinecraftClient c)throws Exception{
  for(WaypointStore.Point point:new ArrayList<WaypointStore.Point>(SpaceClient.waypoints.all()))SpaceClient.waypoints.replace(point,null);
  for(GameplayModule m:GameplayModule.values()){if(m.enabled(SpaceClient.mods))m.toggle(SpaceClient.mods);SpaceClient.mods.toggleKey(m,0);}
  SpaceClient.mods.snaplookKey(0);
  for(GameplayModule m:new GameplayModule[]{GameplayModule.FREE_LOOK,GameplayModule.TOGGLE_SPRINT,GameplayModule.ZOOM,GameplayModule.AUTO_TEXT,GameplayModule.SOUNDS,GameplayModule.WAYPOINTS})m.toggle(SpaceClient.mods);
  SpaceClient.mods.sprintMode(ModSettings.SprintMode.TOGGLE);SpaceClient.mods.zoomSmooth(false);SpaceClient.mods.zoomFactor(4);
  SpaceClient.mods.autoTextKey(0,Keyboard.KEY_APOSTROPHE);SpaceClient.mods.autoTextMessage(0,"SPACE_QA_MACRO");
  SpaceClient.mods.autoTextKey(1,0);SpaceClient.mods.autoTextKey(2,0);
  SpaceClient.mods.killChime(ModSettings.KillChime.ORB);
  c.options.guiScale=2;c.options.perspective=0;c.options.hudHidden=false;c.options.debugEnabled=false;
  fov=c.options.fov;mouseSensitivity=c.options.sensitivity;firstPlayer=c.player;contextA=WaypointRuntime.context(c);
  require(SpaceClient.waypoints.add(new WaypointStore.Point(contextA,"Host A",c.player.dimension,(int)c.player.x,4,(int)c.player.z,VisualPalette.GOLD,true)),"QA marker save");
  gameplay(c);build(c);
 }
 private static void build(MinecraftClient c){
  add("tcp-world-hud",()->{
   require(!c.isInSingleplayer(),"TCP client classified as singleplayer");require(c.getNetworkHandler().getClientConnection().isOpen(),"TCP channel closed");
   require(!ContextHud.ping(c).contains("Local"),"Multiplayer ping shows Local");
   for(HudModule m:HudModule.values()){String t=HudRenderer.text(c,m);require(t!=null&&HudRenderer.moduleWidth(c,m,t)>=0,"HUD sample "+m);hudChecks++;}
   yaw=c.player.yaw;pitch=c.player.pitch;held.add(Keyboard.KEY_V);
  });
  for(int i=0;i<6;i++)add("freelook-held-"+i,()->{require(FreeLookRuntime.active&&FreeLookRuntime.owner==c.player,"Freelook dropped while held: eligible="+FreeLookRuntime.eligible(c)+" held="+Keyboard.isKeyDown(Keyboard.KEY_V)+" focus="+c.focused+" screen="+c.currentScreen+" conflicts="+ModuleKeys.conflicts(c,Keyboard.KEY_V));require(c.options.perspective==1,"Freelook perspective");require(c.player.yaw==yaw&&c.player.pitch==pitch,"Freelook changed body aim");bodyChecks++;});
  add("freelook-menu",()->{c.setScreen(new GameMenuScreen());});
  add("freelook-menu-restored",()->{require(!FreeLookRuntime.active&&c.options.perspective==0,"Menu did not restore camera");held.clear();gameplay(c);held.add(Keyboard.KEY_V);virtualFocus=false;c.focused=false;});
  add("freelook-focus-loss",()->{require(!FreeLookRuntime.active&&c.options.perspective==0,"Unfocused camera active");gameplay(c);held.add(Keyboard.KEY_V);});
  add("freelook-release",()->{require(FreeLookRuntime.active,"Freelook did not resume");held.clear();});
  add("zoom-start",()->{require(!FreeLookRuntime.active&&c.options.perspective==0,"Release did not restore camera");projection=((RendererPorts)(Object)c.gameRenderer).qa$fov(1,true);held.add(Keyboard.KEY_C);});
  add("zoom-held",()->{require(ZoomRuntime.state.divisor()==4,"Zoom key not applied");require(Math.abs(((RendererPorts)(Object)c.gameRenderer).qa$fov(1,true)-projection/4)<0.01,"Native zoom projection not divided");require(c.options.fov==fov&&c.options.sensitivity==mouseSensitivity,"Zoom modified native options");held.add(Keyboard.KEY_V);});
  add("freelook-over-zoom",()->{require(FreeLookRuntime.active&&ZoomRuntime.state.divisor()==1,"Zoom/free-look precedence");held.clear();held.add(Keyboard.KEY_C);c.setScreen(new HudModulesScreen());});
  add("zoom-menu-restored",()->{require(!FreeLookRuntime.active&&ZoomRuntime.state.divisor()==1,"Menu did not reset zoom");gameplay(c);held.add(Keyboard.KEY_C);virtualFocus=false;c.focused=false;});
  add("zoom-focus-restored",()->{require(ZoomRuntime.state.divisor()==1,"Focus loss did not reset zoom");gameplay(c);});
  add("right-shift-open",()->{event(c,Keyboard.KEY_RSHIFT,true,false,true);require(c.currentScreen instanceof HudModulesScreen,"Duplicate RightShift reopened/closed menu");});
  add("right-shift-close",()->{event(c,Keyboard.KEY_RSHIFT,true,false,true);require(c.currentScreen==null,"Duplicate RightShift reopened menu");});
  add("all-gameplay-shortcuts",()->{
   for(GameplayModule m:GameplayModule.values()){
    boolean before=m.enabled(SpaceClient.mods);SpaceClient.mods.toggleKey(m,Keyboard.KEY_SEMICOLON);
    c.setScreen(new GameMenuScreen());event(c,Keyboard.KEY_SEMICOLON,true,false,false);require(m.enabled(SpaceClient.mods)==before,"Shortcut active in menu "+m);gameplay(c);
    event(c,Keyboard.KEY_SEMICOLON,true,true,false);require(m.enabled(SpaceClient.mods)==before,"Repeat toggled "+m);
    event(c,Keyboard.KEY_SEMICOLON,false,false,false);require(m.enabled(SpaceClient.mods)==before,"Release toggled "+m);
    event(c,Keyboard.KEY_SEMICOLON,true,false,true);require(m.enabled(SpaceClient.mods)!=before,"Shortcut/duplicate "+m);
    event(c,Keyboard.KEY_SEMICOLON,true,false,true);require(m.enabled(SpaceClient.mods)==before,"Shortcut restore "+m);
    SpaceClient.mods.toggleKey(m,0);shortcutChecks++;
   }
  });
  add("sprint-toggle-on",()->{gameplay(c);event(c,c.options.sprintKey.getCode(),true,false,true);require(SprintRuntime.state.requested(true,ModSettings.SprintMode.TOGGLE),"Sprint toggle duplicate");forward=true;});
  add("sprint-server-observed",()->{require(c.player.isSprinting(),"Vanilla sprint not active");eventually(Files.exists(sync.resolve("host-A-sprinting.flag")),"Server did not observe sprint packet");require(HudRenderer.text(c,HudModule.SPRINT_STATUS).contains("Sprinting"),"Sprint HUD not actual");capture(c,"sprint-running-server-confirmed");forward=false;c.setScreen(new GameMenuScreen());});
  add("sprint-menu-no-toggle",()->{event(c,c.options.sprintKey.getCode(),true,false,false);require(SprintRuntime.state.requested(true,ModSettings.SprintMode.TOGGLE),"Menu changed armed sprint");require(!c.player.isSprinting(),"Sprinting remained without movement");gameplay(c);event(c,c.options.sprintKey.getCode(),true,false,true);require(!SprintRuntime.state.requested(true,ModSettings.SprintMode.TOGGLE),"Sprint toggle off");});
  add("macro-down-once",()->{macroNanos=++eventNanos;eventAt(c,Keyboard.KEY_APOSTROPHE,true,false,true,macroNanos);eventAt(c,Keyboard.KEY_APOSTROPHE,true,false,false,macroNanos+1);});
  add("macro-server-once",()->{eventually(chatCount("host-A")==1,"Macro duplicate/cooldown sent extra packet");event(c,Keyboard.KEY_APOSTROPHE,true,true,false);event(c,Keyboard.KEY_APOSTROPHE,false,false,false);c.setScreen(new ChatScreen());event(c,Keyboard.KEY_APOSTROPHE,true,false,false);gameplay(c);virtualFocus=false;c.focused=false;event(c,Keyboard.KEY_APOSTROPHE,true,false,false);});
  add("macro-gui-focus-guards",()->{eventually(chatCount("host-A")==1,"Macro sent from GUI/focus loss");gameplay(c);write("spawn-pig.flag","1");});
  add("pig-network-arrived",()->{
   eventually(Files.exists(sync.resolve("pig.id")),"Server pig task pending");int id=Integer.parseInt(new String(Files.readAllBytes(sync.resolve("pig.id")),StandardCharsets.UTF_8).trim());clientPig=c.world.getEntityById(id);eventually(clientPig!=null,"Spawn packet missing");
   c.player.updatePosition(clientPig.x,clientPig.y,clientPig.z-1.5);c.result=new BlockHitResult(clientPig,new Vec3d(clientPig.x,clientPig.y+0.5,clientPig.z));
   held.add(Keyboard.KEY_V);
  });
  add("freelook-blocks-interaction",()->{require(FreeLookRuntime.active,"Freelook interaction guard not active");c.result=new BlockHitResult(clientPig,new Vec3d(clientPig.x,clientPig.y+0.5,clientPig.z));((ClientPorts)(Object)c).qa$attack();require(CombatMetricsRuntime.combo(c)==0&&CombatMetricsRuntime.reach(c).contains("--"),"Freelook attack reached interaction manager");held.clear();});
  add("native-attack-after-release",()->{require(!FreeLookRuntime.active,"FreeLook did not release before attack");c.result=new BlockHitResult(clientPig,new Vec3d(clientPig.x,clientPig.y+0.5,clientPig.z));c.interactionManager.attackEntity(c.player,clientPig);});
  add("hurt-packet-combo",()->{eventually(CombatMetricsRuntime.combo(c)==1,"Native server hurt status not counted");require(!CombatMetricsRuntime.reach(c).contains("--"),"Observed reach sample missing");write("kill-pig.flag","1");});
  add("death-packet-sound",()->{eventually(chimes==1,"Native server death chime missing/duplicate");});
  add("disconnect-armed",()->{event(c,c.options.sprintKey.getCode(),true,false,false);held.add(Keyboard.KEY_V);});
  add("disconnect-active-camera",()->{require(FreeLookRuntime.active,"Pre-disconnect camera inactive");
   final net.minecraft.client.network.ClientPlayNetworkHandler handler=c.getNetworkHandler();
   final net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket packet=new net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket(c.player,(byte)2);
   Thread incoming=new Thread(()->{try{handler.onEntityStatus(packet);}catch(net.minecraft.network.OffThreadException expected){}},"QA scheduled play packet");incoming.setDaemon(true);incoming.start();incoming.join(2000);require(!incoming.isAlive(),"Packet was not queued");disconnect(c);});
 }
 private static void hostFrame(MinecraftClient c)throws Exception{
  if(stage==0){if(c.currentScreen==null)return;c.startIntegratedServer("space-"+role+"-20261103","Private "+role,new LevelInfo(445566L,LevelInfo.GameMode.CREATIVE,false,false,LevelGeneratorType.FLAT));stage=1;return;}
  if(stage==1){if(c.player==null||c.world==null)return;
   IntegratedServer server=c.getServer();server.setOnlineMode(false);((ServerPorts)(Object)server).qa$published(true);
   server.getNetworkIo().bind(InetAddress.getByName("127.0.0.1"),0);
   for(io.netty.channel.ChannelFuture future:((NetworkPorts)(Object)server.getNetworkIo()).qa$channels())if(future.channel().localAddress() instanceof InetSocketAddress){InetSocketAddress address=(InetSocketAddress)future.channel().localAddress();require(address.getAddress().isLoopbackAddress(),"Non-loopback listener");port=address.getPort();}
   require(port>0,"No TCP listener");write(role+".port",""+port);stage=2;c.setScreen(null);return;
  }
  IntegratedServer server=c.getServer();
  server.submit(()->{
   try{
    ServerPlayerEntity player=server.getPlayerManager().getPlayer("SpaceNet");
    if(player!=null&&player.isSprinting())write(role+"-sprinting.flag","1");
    if(role.equals("host-A")&&player!=null&&Files.exists(sync.resolve("spawn-pig.flag"))&&!pigRequested){
     pigRequested=true;ServerWorld w=server.getWorld(player.dimension);pig=new PigEntity(w);pig.updatePosition(player.x,player.y,player.z+2);w.spawnEntity(pig);write("pig.id",""+pig.getEntityId());
    }
    if(pig!=null&&Files.exists(sync.resolve("kill-pig.flag"))&&!killRequested){killRequested=true;pig.damage(DamageSource.GENERIC,100);}
   }catch(Exception e){throw new RuntimeException(e);}
  });
  if(Files.exists(sync.resolve("stop-"+role+".flag"))){stopped=true;write(role+"-done.json","{\"ok\":true,\"loopbackOnly\":true}");c.scheduleStop();}
 }
 public static void frame(MinecraftClient c){
  if(stopped||SpaceClient.mods==null||SpaceClient.hud==null)return;
  if(++frames<12)return;frames=0;
  try{
   require(!Files.exists(sync.resolve("abort.flag")),"Coordinator aborted QA");require(System.nanoTime()-started<180_000_000_000L,"QA timeout");require(!Mouse.isGrabbed(),"Desktop mouse unexpectedly grabbed");
   if(host){hostFrame(c);return;}
   if(stage==0){if(!Files.exists(sync.resolve("host-A.port")))return;connect(c,"host-A");stage=1;return;}
   if(stage==1){if(!joined(c))return;setup(c);stage=2;return;}
   if(pending!=null){capture(c,pending);pending=null;}
   if(stage==2){
    if(next<checks.size()){if(caseStarted==0)caseStarted=System.nanoTime();checks.get(next).run();pending=names.get(next++);caseStarted=0;return;}
    require(c.player==null&&!FreeLookRuntime.active&&ZoomRuntime.state.divisor()==1&&!SprintRuntime.state.requested(true,ModSettings.SprintMode.TOGGLE),"Disconnect retained transient input state");connect(c,"host-A");stage=3;return;
   }
   if(stage==3){if(!joined(c))return;require(c.player!=firstPlayer,"Reconnect reused player");require(closedPlayPackets>0,"Closed packet guard was not exercised");require(contextA.equals(WaypointRuntime.context(c))&&WaypointRuntime.current(c).size()==1,"Reconnect lost waypoint context");require(!SprintRuntime.state.requested(true,ModSettings.SprintMode.TOGGLE)&&!FreeLookRuntime.active&&ZoomRuntime.state.divisor()==1,"Reconnect retained camera/sprint");gameplay(c);eventAt(c,Keyboard.KEY_APOSTROPHE,true,false,true,macroNanos+2);stage=4;return;}
   if(stage==4){require(chatCount("host-A")==2,"Reconnect did not clear macro cooldown");capture(c,"reconnected-host-A");disconnect(c);write("stop-host-A.flag","1");stage=5;return;}
   if(stage==5){if(!Files.exists(sync.resolve("host-B.port")))return;connect(c,"host-B");stage=6;return;}
   if(stage==6){if(!joined(c))return;require(!contextA.equals(WaypointRuntime.context(c))&&WaypointRuntime.current(c).isEmpty(),"Waypoints leaked to different server");require(CombatMetricsRuntime.combo(c)==0&&CombatMetricsRuntime.reach(c).contains("--"),"Combat state leaked to server B");require(!SprintRuntime.state.requested(true,ModSettings.SprintMode.TOGGLE)&&!FreeLookRuntime.active&&ZoomRuntime.state.divisor()==1,"Input state leaked to server B");require(SpaceClient.mods.freeLook()&&SpaceClient.mods.zoom()&&SpaceClient.mods.autoText(),"Saved module options lost");gameplay(c);eventAt(c,Keyboard.KEY_APOSTROPHE,true,false,true,macroNanos+3);stage=7;return;}
   if(stage==7){require(chatCount("host-B")==1,"Auto Text did not reach server B exactly once");require(c.options.fov==fov&&c.options.sensitivity==mouseSensitivity,"Native options changed");capture(c,"server-B-clean-state");held.clear();forward=false;virtualFocus=false;
    write("closed-play-packets.txt",""+closedPlayPackets);
    write("result.json","{\"ok\":true,\"keyboardHandlerInvocations\":"+events+",\"gameplayShortcuts\":"+shortcutChecks+",\"hudNetworkSamples\":"+hudChecks+",\"freelookHoldSamples\":"+bodyChecks+",\"nativeDeathChimes\":"+chimes+",\"hostAMacros\":"+chatCount("host-A")+",\"hostBMacros\":"+chatCount("host-B")+",\"reconnect\":true,\"serverChange\":true,\"osInputInjected\":false,\"physicalInputTested\":false}");
    stopped=true;write("stop-host-B.flag","1");c.scheduleStop();
   }
  }catch(Retry pending){return;}catch(Throwable e){fail(c,e);}
 }
 private static void fail(MinecraftClient c,Throwable e){if(stopped)return;stopped=true;e.printStackTrace();try{StringWriter sw=new StringWriter();e.printStackTrace(new PrintWriter(sw));write(role+"-failure.txt","stage="+stage+" next="+next+"\n"+sw);write("stop-host-A.flag","1");write("stop-host-B.flag","1");}catch(Exception ignored){}c.scheduleStop();}
 private static void capture(MinecraftClient c,String name)throws Exception{
  if(!(name.contains("restored")||name.equals("tcp-world-hud")||name.equals("sprint-server-observed")||name.equals("sprint-running-server-confirmed")||name.equals("death-packet-sound")||name.startsWith("reconnected")||name.startsWith("server-B")))return;
  int w=Display.getWidth(),h=Display.getHeight();ByteBuffer b=BufferUtils.createByteBuffer(w*h*4);GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=((h-1-y)*w+x)*4;im.setRGB(x,y,((b.get(i)&255)<<16)|((b.get(i+1)&255)<<8)|(b.get(i+2)&255));}
  Path dir=sync.resolve("captures");Files.createDirectories(dir);ImageIO.write(im,"png",dir.resolve(name+".png").toFile());
 }
}
