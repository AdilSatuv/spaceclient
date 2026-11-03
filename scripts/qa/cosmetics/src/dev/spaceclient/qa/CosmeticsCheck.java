package dev.spaceclient.qa;
import dev.spaceclient.*;
import dev.spaceclient.cosmetics.*;
import dev.spaceclient.hud.HudModule;
import dev.spaceclient.hud.HudEditorScreen;
import dev.spaceclient.settings.*;
import dev.spaceclient.qa.mixin.ScreenAccess;
import java.io.*;import java.lang.reflect.*;import java.nio.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;
import javax.imageio.ImageIO;import java.awt.image.BufferedImage;
import net.minecraft.client.MinecraftClient;import net.minecraft.client.gui.screen.*;import net.minecraft.client.gui.screen.ingame.SurvivalInventoryScreen;
import net.minecraft.client.network.*;import net.minecraft.entity.player.OtherClientPlayerEntity;import net.minecraft.client.render.entity.*;import net.minecraft.world.level.*;import com.mojang.authlib.GameProfile;
import org.lwjgl.BufferUtils;import org.lwjgl.input.Keyboard;import org.lwjgl.opengl.*;
public final class CosmeticsCheck {
 private static int frames,stage,next,captures,keys;private static boolean stopped;private static String pending;
 private static Path root;private static net.minecraft.util.Identifier nativeSkin,nativeCape;
 private interface Check {void run() throws Exception;}private static final List<Check> checks=new ArrayList<>();private static final List<String> names=new ArrayList<>();
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private static Method method(Class<?> cls,String n,Class<?>...t)throws Exception{while(cls!=null){try{Method m=cls.getDeclaredMethod(n,t);m.setAccessible(true);return m;}catch(NoSuchMethodException e){cls=cls.getSuperclass();}}throw new NoSuchMethodException(n);}
 private static Object field(Object o,String n)throws Exception{Class<?> cls=o.getClass();while(cls!=null){try{Field f=cls.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){cls=cls.getSuperclass();}}throw new NoSuchFieldException(n);}
 private static void add(String n,Check c){names.add(n);checks.add(c);}
 private static void key(Screen s,int k){((ScreenAccess)(Object)s).qa$key((char)0,k);keys++;}
 private static void build(MinecraftClient c)throws Exception{
  root=c.runDirectory.toPath();Properties p=new Properties();try(InputStream in=BuildIdentity.class.getResourceAsStream("/spaceclient-build.properties")){p.load(in);}
  Files.write(root.resolve("qa-loaded-build.txt"),(p.getProperty("gameSourceTree")+"\n"+BuildIdentity.label()).getBytes(StandardCharsets.UTF_8));
  CosmeticsRuntime.settings().change(v->v.enabled=false);nativeSkin=c.player.getCapeId();nativeCape=c.player.getSkinId();
  CosmeticsRuntime.settings().change(v->{v.enabled=true;v.wings=CosmeticsSettings.Wings.AURORA;v.halo=CosmeticsSettings.Halo.ORBIT;});
  require(CosmeticsRuntime.appearance().hasSkin()&&CosmeticsRuntime.appearance().hasCape(),"Verified appearance missing");
  require(!c.player.getCapeId().equals(nativeSkin),"Skin mapping did not apply");require(c.player.getSkinId()!=null&&!c.player.getSkinId().equals(nativeCape),"Cape mapping did not apply");
  String expected=CosmeticsRuntime.appearance().skinModel.equals("slim")?"slim":"default";require(c.player.getModel().equals(expected),"Skin model dispatch");
  OtherClientPlayerEntity remote=new OtherClientPlayerEntity(c.world,new GameProfile(UUID.randomUUID(),"RemoteQA"));
  require(!CosmeticsRuntime.localPlayer(remote)&&CosmeticsRuntime.skin(remote)==null&&CosmeticsRuntime.cape(remote)==null,"Remote player appearance overwritten");
  require(remote.getSkinId()==null,"Remote cape overwritten");
  List<Object> renderers=Arrays.asList(c.getEntityRenderManager().getRenderer(c.player),new PlayerEntityRenderer(c.getEntityRenderManager(),false),new PlayerEntityRenderer(c.getEntityRenderManager(),true));
  for(Object renderer:renderers) {int count=0;for(Field featureField:LivingEntityRenderer.class.getDeclaredFields())if(featureField.getType()==List.class){featureField.setAccessible(true);for(Object f:(List<?>)featureField.get(renderer))if(f instanceof CosmeticFeature)count++;}require(count==1,"Duplicate/missing cosmetic feature "+count);}
  add("title-preview",()->{c.connect(null);c.setScreen(new CosmeticsScreen(new TitleScreen()));});
  add("compact-inventory-inspector",()->{
   c.options.guiScale=3;
   require(SpaceClient.hud.place(HudModule.INVENTORY,SpaceClient.hud.getConfig().active().placement(HudModule.INVENTORY).withScale(3)),"Inventory fixture placement");
   HudEditorScreen editor=new HudEditorScreen(null,HudModule.INVENTORY);c.setScreen(editor);
   method(editor.getClass(),"activate",int.class).invoke(editor,53);
   Screen inspector=c.currentScreen;
   @SuppressWarnings("unchecked") List<SettingRow> rows=(List<SettingRow>)method(inspector.getClass(),"rows").invoke(inspector);
   require(!rows.get(0).enabled||!rows.get(1).enabled,"Filled inventory axis must be disabled");
   for(int i=0;i<2;i++)require(rows.get(i).range.max>rows.get(i).range.min,"Coordinate control must retain a valid numeric range");
  });
 }
 private static void worldChecks(MinecraftClient c)throws Exception {
  for(CosmeticsSettings.Wings wing:CosmeticsSettings.Wings.values()){final CosmeticsSettings.Wings w=wing;add("world-"+w.name(),()->{CosmeticsRuntime.settings().change(v->{v.enabled=true;v.wings=w;v.animated=false;});c.setScreen(new Preview());});}
  add("disabled",()->{CosmeticsRuntime.settings().change(v->v.enabled=false);require(c.player.getCapeId().equals(nativeSkin),"Skin did not revert");require(Objects.equals(c.player.getSkinId(),nativeCape),"Cape did not revert");});
  add("skin-cape-toggles",()->{CosmeticsRuntime.settings().change(v->{v.enabled=true;v.skin=false;v.cape=false;});require(c.player.getCapeId().equals(nativeSkin),"Skin toggle");require(Objects.equals(c.player.getSkinId(),nativeCape),"Cape toggle");CosmeticsRuntime.settings().change(v->{v.skin=true;v.cape=true;v.wings=CosmeticsSettings.Wings.AURORA;});});
  add("invisible",()->{c.player.setInvisible(true);});add("visible",()->{c.player.setInvisible(false);});
  for(String lang:new String[]{"en_US","ru_RU"})for(int scale:new int[]{2,3}){final String l=lang;final int s=scale;
   add("screen-"+l+"-"+s,()->{for(net.minecraft.client.resource.language.LanguageDefinition d:c.getLanguageManager().getAllLanguages())if(d.getCode().equals(l))c.getLanguageManager().setLanguage(d);c.options.language=l;c.options.guiScale=s;c.reloadResources();c.setScreen(new CosmeticsScreen(new TitleScreen()));});
   add("keyboard-"+l+"-"+s,()->{Screen screen=c.currentScreen;@SuppressWarnings("unchecked") List<SettingRow> rows=(List<SettingRow>)method(screen.getClass(),"rows").invoke(screen);int count=rows.size()+2;int start=(Integer)field(screen,"focus");for(int i=0;i<count;i++){SettingsLayout layout=(SettingsLayout)field(screen,"layout");int focus=(Integer)field(screen,"focus"),offset=(Integer)field(screen,"offset");if(focus<rows.size()){int y=layout.rowY(focus,offset);require(y>=layout.bodyTop&&y+40<=layout.top+layout.height-80,"Obscured focused cosmetic row");}key(screen,Keyboard.KEY_TAB);}require(start==(Integer)field(screen,"focus"),"Keyboard wrap");});
  }
 }
 public static void frame(MinecraftClient c){if(stopped||SpaceClient.mods==null||++frames<12)return;frames=0;try {
  require(GL11.glGetError()==GL11.GL_NO_ERROR,"Native GL error");
  if(stage==0){if(c.currentScreen==null)return;root=c.runDirectory.toPath();c.startIntegratedServer("cosmetics-qa-20261103","Cosmetics QA",new LevelInfo(45321L,LevelInfo.GameMode.CREATIVE,false,false,LevelGeneratorType.FLAT));stage=1;return;}
  if(stage==1){if(c.player==null||c.world==null)return;build(c);Check title=checks.remove(0);String titleName=names.remove(0);worldChecks(c);checks.add(title);names.add(titleName);stage=2;}
  if(pending!=null){save(pending);pending=null;}
  if(next<checks.size()){checks.get(next).run();pending=names.get(next++);}else {stopped=true;Files.write(root.resolve("qa-cosmetics-result.json"),("{\"ok\":true,\"captures\":"+captures+",\"keys\":"+keys+",\"build\":\""+BuildIdentity.label()+"\"}").getBytes(StandardCharsets.UTF_8));c.scheduleStop();}
 }catch(Throwable e){stopped=true;e.printStackTrace();try{StringWriter sw=new StringWriter();e.printStackTrace(new PrintWriter(sw));Files.write(c.runDirectory.toPath().resolve("qa-cosmetics-failure.txt"),("next="+next+"\n"+sw).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}c.scheduleStop();}}
 private static final class Preview extends Screen {public boolean shouldPauseGame(){return true;}public void render(int x,int y,float delta){fillGradient(0,0,width,height,0xFF101114,0xFF252731);SurvivalInventoryScreen.renderEntity(width/2,height-30,100,0,0,client.player);}}
 private static void save(String name)throws Exception {int w=Display.getWidth(),h=Display.getHeight();ByteBuffer b=BufferUtils.createByteBuffer(w*h*4);GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);BufferedImage img=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=((h-1-y)*w+x)*4;img.setRGB(x,y,((b.get(i)&255)<<16)|((b.get(i+1)&255)<<8)|(b.get(i+2)&255));}Path dir=root.resolve("qa-captures");Files.createDirectories(dir);ImageIO.write(img,"png",dir.resolve(name+".png").toFile());captures++;}
}
