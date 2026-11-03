package dev.spaceclient.qa;

import dev.spaceclient.*;
import dev.spaceclient.hud.*;
import dev.spaceclient.menu.*;
import dev.spaceclient.mods.*;
import dev.spaceclient.settings.*;
import dev.spaceclient.cosmetics.CosmeticsScreen;
import dev.spaceclient.qa.mixin.ScreenAccess;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.options.*;
import net.minecraft.client.gui.screen.world.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.world.level.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;

/** Native rendering and input regression, with a private world and no desktop input. */
public final class MenuThemeCheck {
    private interface Check { void run() throws Exception; }
    private static final List<Check> checks=new ArrayList<>();
    private static final List<String> names=new ArrayList<>();
    private static int next,frames,captures,dragChecks,keySteps;
    private static String pending;
    private static boolean initialized,stopped,worldRequested;
    private static Path root;
    private static final List<String> manifest=new ArrayList<>();
    private static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static void add(String name,Check check){names.add(name);checks.add(check);}
    private static void screen(String name,Supplier<Screen> screen){add(name,()->MinecraftClient.getInstance().setScreen(screen.get()));}
    private static void key(Screen screen,int key){((ScreenAccess)(Object)screen).qa$key((char)0,key);keySteps++;}
    private static List<ButtonWidget> buttons(Screen screen){return ((ScreenAccess)(Object)screen).qa$buttons();}
    private static void language(MinecraftClient c,String code,int scale){
        for(net.minecraft.client.resource.language.LanguageDefinition l:c.getLanguageManager().getAllLanguages())if(l.getCode().equals(code))c.getLanguageManager().setLanguage(l);
        c.options.language=code;c.options.guiScale=scale;c.reloadResources();
    }
    private static void build(MinecraftClient c)throws Exception{
        root=c.runDirectory.toPath();
        Properties p=new Properties();try(InputStream in=BuildIdentity.class.getResourceAsStream("/spaceclient-build.properties")){p.load(in);}
        Files.write(root.resolve("loaded-tree.txt"),p.getProperty("gameSourceTree").getBytes(StandardCharsets.UTF_8));
        final Screen title=new TitleScreen();
        screen("title",()->title);
        add("title-focus",()->{key(c.currentScreen,Keyboard.KEY_TAB);key(c.currentScreen,Keyboard.KEY_DOWN);});
        screen("settings",()->new SettingsScreen(title,c.options));
        screen("video",()->new VideoOptionsScreen(title,c.options));
        add("native-option-slider-drag",()->{
            OptionSliderWidget b=new OptionSliderWidget(990,20,70,GameOptions.Option.BRIGHTNESS);
            buttons(c.currentScreen).clear();buttons(c.currentScreen).add(b);
            require(b instanceof MenuSlider,"Option slider mixin missing");
            require(b.isMouseOver(c,b.x+4,b.y+8),"Option press");
            b.render(c,b.x+b.getWidth()-4,b.y+8);
            require(((MenuSlider)b).spaceclient$progress()>.99f,"Option drag did not reach maximum");
            b.mouseReleased(0,0);b.render(c,b.x+4,b.y+8);
            require(((MenuSlider)b).spaceclient$progress()>.99f,"Option changed after release");dragChecks++;
        });
        screen("controls",()->new ControlsOptionsScreen(title,c.options));
        screen("language",()->new LanguageOptionsScreen(title,c.options,c.getLanguageManager()));
        screen("sounds",()->new SoundsScreen(title,c.options));
        add("native-sound-slider-drag",()->{
            ButtonWidget b=null;int count=0;
            for(ButtonWidget candidate:buttons(c.currentScreen))if(candidate instanceof MenuSlider&&++count==2){b=candidate;break;}
            require(b!=null,"Sound slider mixin missing");
            require(b.isMouseOver(c,b.x+4,b.y+8),"Sound press");b.render(c,b.x+b.getWidth()-4,b.y+8);
            require(((MenuSlider)b).spaceclient$progress()>.99f,"Sound drag side effect lost");
            b.mouseReleased(0,0);b.render(c,b.x+4,b.y+8);
            require(((MenuSlider)b).spaceclient$progress()>.99f,"Sound changed after release");dragChecks++;
        });
        screen("skin-options",()->new SkinOptionsScreen(title));
        screen("chat-options",()->new ChatOptionsScreen(title,c.options));
        screen("resources",()->new ResourcePackScreen(title));
        screen("snooper",()->new SnooperScreen(title,c.options));
        screen("worlds",()->new SelectWorldScreen(title));
        screen("create-world",()->new CreateWorldScreen(title));
        screen("flat-world",()->new CustomizeFlatLevelScreen(new CreateWorldScreen(title),""));
        screen("flat-presets",()->new SuperflatPresetScreen(new CustomizeFlatLevelScreen(new CreateWorldScreen(title),"")));
        screen("custom-world",()->new CustomizeWorldScreen(new CreateWorldScreen(title),""));
        add("native-generic-slider-drag",()->{
            final float[] observed={-1};
            PagedEntryListWidget.Listener listener=new PagedEntryListWidget.Listener(){
                public void setBooleanValue(int id,boolean v){}
                public void setFloatValue(int id,float v){observed[0]=v;}
                public void setStringValue(int id,String v){}
            };
            SliderWidget b=new SliderWidget(listener,991,20,70,"QA slider",0,1,.25f,(id,label,value)->label);
            buttons(c.currentScreen).clear();buttons(c.currentScreen).add(b);
            require(b.isMouseOver(c,b.x+4,b.y+8),"Generic slider press");b.render(c,b.x+b.getWidth()-4,b.y+8);
            require(b.getProgress()>.99f&&observed[0]>.99f,"Generic slider callback lost");
            b.mouseReleased(0,0);b.render(c,b.x+4,b.y+8);require(b.getProgress()>.99f,"Generic slider release");dragChecks++;
        });
        screen("custom-presets",()->new CustomizedWorldPresetsScreen(new CustomizeWorldScreen(new CreateWorldScreen(title),"")));
        screen("multiplayer",()->new MultiplayerScreen(title));
        screen("direct-connect",()->new DirectConnectScreen(title,new ServerInfo("Private QA","127.0.0.1",false)));
        screen("add-server",()->new AddServerScreen(title,new ServerInfo("Private QA","127.0.0.1",false)));
        screen("confirm",()->new ConfirmScreen((accepted,id)->{},"Space UI QA","Native confirmation controls",0));
        add("catalog-grid",()->{SpaceClient.browser.view(ModuleBrowserSettings.View.GRID);c.setScreen(new HudModulesScreen());});
        add("catalog-list",()->{key(c.currentScreen,Keyboard.KEY_F6);key(c.currentScreen,Keyboard.KEY_TAB);});
        for(Object section:Class.forName("dev.spaceclient.hud.HudSection").getEnumConstants()){
            final Object current=section;
            add("catalog-"+((Enum<?>)section).name().toLowerCase(Locale.ROOT),()->{
                c.setScreen(new HudModulesScreen());
                java.lang.reflect.Field f=HudModulesScreen.class.getDeclaredField("section");f.setAccessible(true);f.set(c.currentScreen,current);c.currentScreen.init(c,c.currentScreen.width,c.currentScreen.height);
            });
        }
        screen("hud-content",()->new HudModuleSettingsScreen(null,HudModule.KEYSTROKES));
        add("hud-appearance",()->key(c.currentScreen,Keyboard.KEY_RIGHT));
        screen("gameplay-settings",()->new GameModuleSettingsScreen(null,GameplayModule.PARTICLES));
        screen("cosmetics",()->new CosmeticsScreen(title));
        add("ru-compact-title",()->{QaInit.resize(854,480);language(c,"ru_RU",2);c.setScreen(new TitleScreen());});
        add("ru-compact-list",()->{SpaceClient.browser.view(ModuleBrowserSettings.View.LIST);c.setScreen(new HudModulesScreen());});
        screen("ru-compact-video",()->new VideoOptionsScreen(title,c.options));
        screen("ru-compact-settings",()->new HudModuleSettingsScreen(null,HudModule.INVENTORY));
        screen("ru-compact-cosmetics",()->new CosmeticsScreen(title));
        add("ru-gui3-language",()->{QaInit.resize(1280,720);language(c,"ru_RU",3);c.setScreen(new LanguageOptionsScreen(title,c.options,c.getLanguageManager()));});
        screen("ru-gui3-resources",()->new ResourcePackScreen(title));
        screen("ru-gui3-catalog",()->new HudModulesScreen());
        add("world-request",()->{QaInit.resize(1280,720);language(c,"en_US",2);c.startIntegratedServer("menu-theme-private","Menu theme QA",new LevelInfo(54321L,LevelInfo.GameMode.CREATIVE,false,false,LevelGeneratorType.FLAT));worldRequested=true;});
        screen("pause",()->new GameMenuScreen());
        screen("inworld-settings-lock",()->new SettingsScreen(new GameMenuScreen(),c.options));
        add("native-lock-states",()->{LockButtonWidget lock=null;for(ButtonWidget b:buttons(c.currentScreen))if(b instanceof LockButtonWidget)lock=(LockButtonWidget)b;require(lock!=null,"No native difficulty lock");lock.setLocked(true);lock.active=false;});
        screen("open-to-lan",()->new OpenToLanScreen(new GameMenuScreen()));
        screen("achievements",()->new AchievementsScreen(new GameMenuScreen(),c.player.getStatHandler()));
        screen("statistics",()->new StatsScreen(new GameMenuScreen(),c.player.getStatHandler()));
        screen("hud-editor",()->new HudEditorScreen(new HudModulesScreen()));
        add("hud-arrange",()->key(c.currentScreen,Keyboard.KEY_E));
        screen("inworld-cosmetics",()->new CosmeticsScreen(new GameMenuScreen()));
        screen("legacy-mod-controls",()->new ModsScreen(new GameMenuScreen()));
        screen("waypoints",()->new WaypointsScreen(new GameMenuScreen()));
    }
    public static void frame(MinecraftClient c){
        if(stopped||SpaceClient.hud==null||SpaceClient.mods==null||++frames<8)return;frames=0;
        try{
            require(GL11.glGetError()==GL11.GL_NO_ERROR,"GL error");
            if(!initialized){build(c);initialized=true;}
            if(worldRequested&&c.player==null)return;
            if(pending!=null){if(c.currentScreen!=null)save(c,pending);pending=null;}
            if(next<checks.size()){checks.get(next).run();pending=names.get(next++);}
            else{stopped=true;Files.write(root.resolve("result.json"),("{\"ok\":true,\"captures\":"+captures+",\"nativeSliderDragChecks\":"+dragChecks+",\"keySteps\":"+keySteps+",\"glErrors\":0}").getBytes(StandardCharsets.UTF_8));c.scheduleStop();}
        }catch(Throwable e){stopped=true;e.printStackTrace();try{StringWriter w=new StringWriter();e.printStackTrace(new PrintWriter(w));Files.write(root.resolve("failure.txt"),("next="+next+"\n"+w).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}c.scheduleStop();}
    }
    private static void save(MinecraftClient c,String name)throws Exception{
        int w=Display.getWidth(),h=Display.getHeight();ByteBuffer b=BufferUtils.createByteBuffer(w*h*4);GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);
        BufferedImage img=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=((h-1-y)*w+x)*4;img.setRGB(x,y,((b.get(i)&255)<<16)|((b.get(i+1)&255)<<8)|(b.get(i+2)&255));}
        Path out=root.resolve("captures");Files.createDirectories(out);ImageIO.write(img,"png",out.resolve(name+".png").toFile());captures++;
        manifest.add("{\"name\":\""+name+"\",\"guiWidth\":"+c.currentScreen.width+",\"guiHeight\":"+c.currentScreen.height+",\"buttons\":"+buttons(c.currentScreen).size()+"}");
        Files.write(root.resolve("captures.json"),("["+String.join(",",manifest)+"]").getBytes(StandardCharsets.UTF_8));
    }
}
