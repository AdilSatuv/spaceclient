package dev.spaceclient.perf;

import com.google.gson.Gson;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.world.level.*;
import org.lwjgl.opengl.*;

/** Test-only sampler, independent of Space classes so Fabric-without-Space is a real control. */
public final class PerfCapture {
    private static final String mode=System.getProperty("space.perf.mode","baseline");
    private static final List<String> enabled=new ArrayList<>();
    private static final double[] intervals=new double[100000],renders=new double[100000];
    private static long begin,last,worldReady,sampleStart;
    private static int stage,count;
    private static boolean stopped,space;
    private static Path root;
    private static java.util.concurrent.Future<?> setup;
    private static Object call(Object target,String name)throws Exception{return target.getClass().getMethod(name).invoke(target);}
    private static void configure()throws Exception {
        Class<?> client;
        try{client=Class.forName("dev.spaceclient.SpaceClient");space=true;}catch(ClassNotFoundException absent){if(!mode.equals("baseline"))throw absent;return;}
        if(mode.equals("baseline"))throw new AssertionError("Space must be absent in the baseline");
        Object mods=client.getField("mods").get(null),hud=client.getField("hud").get(null),config=call(hud,"getConfig"),profile=call(config,"active");
        Class<?> gameplay=Class.forName("dev.spaceclient.mods.GameplayModule");
        Method active=gameplay.getMethod("enabled",mods.getClass()),toggle=gameplay.getMethod("toggle",mods.getClass());
        for(Object module:gameplay.getEnumConstants())if((Boolean)active.invoke(module,mods))toggle.invoke(module,mods);
        Class<?> widgets=Class.forName("dev.spaceclient.hud.HudModule");
        Method set=widgets.getMethod("setEnabled",config.getClass(),boolean.class),isEnabled=widgets.getMethod("isEnabled",config.getClass());
        profile.getClass().getField("enabled").setBoolean(profile,!mode.equals("off"));
        for(Object module:widgets.getEnumConstants()) {
            if(mode.equals("heavy"))set.invoke(module,config,true);
            if(!mode.equals("off")&&(Boolean)isEnabled.invoke(module,config))enabled.add(module.toString());
        }
    }
    public static void begin(){begin=System.nanoTime();}
    public static void frame(MinecraftClient c) {
        if(stopped)return;
        try {
            if(root==null)root=c.runDirectory.toPath();
            if(stage==0) {
                if(!(c.currentScreen instanceof TitleScreen))return;
                System.out.println("SPACE_PERF_TITLE_READY");
                configure();c.options.pauseOnLostFocus=false;c.options.vsync=false;Display.setVSyncEnabled(false);
                c.startIntegratedServer("perf-flat","Native performance",new LevelInfo(54321L,LevelInfo.GameMode.CREATIVE,false,false,LevelGeneratorType.FLAT));
                stage=1;return;
            }
            if(stage==1) {
                if(c.world==null||c.player==null)return;
                c.setScreen(null);c.focused=true;
                if(setup==null){
                    final net.minecraft.server.integrated.IntegratedServer server=c.getServer();
                    setup=server.submit(()->{
                        net.minecraft.server.world.ServerWorld world=server.getWorld(0);
                        world.getGameRules().setGameRule("doMobSpawning","false");
                        world.getGameRules().setGameRule("doDaylightCycle","false");
                        world.setTimeOfDay(1000L);
                        server.getPlayerManager().getPlayer("SpacePerfQA").networkHandler.requestTeleport(.5,4,.5,0,0);
                    });
                    return;
                }
                if(!setup.isDone())return;setup.get();
                if(!fixedPosition(c))return;
                worldReady=System.nanoTime();stage=2;return;
            }
            if(stage==4){if(Files.exists(root.resolve("perf-stop.flag"))){stopped=true;c.scheduleStop();}return;}
            GL11.glFinish();long now=System.nanoTime();
            if(GL11.glGetError()!=GL11.GL_NO_ERROR)throw new AssertionError("Native GL error");
            if(c.currentScreen!=null||c.isPaused())throw new AssertionError("World became paused or a screen opened");
            if(!fixedPosition(c)){
                if(stage==2){setup=null;stage=1;return;}
                throw new AssertionError("Player moved away from the controlled scene: "+Arrays.asList(c.player.x,c.player.y,c.player.z,c.player.yaw,c.player.pitch));
            }
            if(stage==2){if(now-worldReady<3000000000L)return;stage=3;sampleStart=now;last=now;return;}
            if(count>=intervals.length)throw new AssertionError("Sampler capacity exceeded");
            intervals[count]=(now-last)/1000000.0;renders[count]=(now-begin)/1000000.0;last=now;count++;
            if(now-sampleStart<6000000000L)return;
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("ok",true);result.put("mode",mode);result.put("spaceLoaded",space);result.put("enabledHud",enabled);
            result.put("frames",count);result.put("seconds",(now-sampleStart)/1e9);result.put("loopFps",count*1e9/(now-sampleStart));
            result.put("intervalMs",summary(intervals));result.put("renderWithFinishMs",summary(renders));
            Runtime r=Runtime.getRuntime();result.put("heapUsedBytes",r.totalMemory()-r.freeMemory());result.put("heapCommittedBytes",r.totalMemory());result.put("heapMaxBytes",r.maxMemory());
            result.put("renderer",GL11.glGetString(GL11.GL_RENDERER));result.put("vendor",GL11.glGetString(GL11.GL_VENDOR));result.put("glVersion",GL11.glGetString(GL11.GL_VERSION));
            result.put("width",Display.getWidth());result.put("height",Display.getHeight());result.put("maxFramerateReturn",c.getMaxFramerate());result.put("displayActive",Display.isActive());
            result.put("position",Arrays.asList(c.player.x,c.player.y,c.player.z,c.player.yaw,c.player.pitch));
            result.put("gameSourceTree",System.getProperty("space.perf.tree","none"));
            Files.write(root.resolve("perf-result.json"),new Gson().toJson(result).getBytes(StandardCharsets.UTF_8));
            System.out.println("SPACE_PERF_SAMPLE_READY");stage=4;
        } catch(Throwable failure){stopped=true;failure.printStackTrace();try{Files.write(root.resolve("perf-failure.txt"),(failure.getClass().getName()+": "+failure.getMessage()).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}c.scheduleStop();}
    }
    private static boolean fixedPosition(MinecraftClient c){return Math.abs(c.player.x-.5)<.001&&Math.abs(c.player.y-4)<.001&&Math.abs(c.player.z-.5)<.001&&Math.abs(c.player.yaw)<.001&&Math.abs(c.player.pitch)<.001;}
    private static Map<String,Double> summary(double[] source){double[] a=Arrays.copyOf(source,count);Arrays.sort(a);double sum=0;for(double v:a)sum+=v;Map<String,Double> out=new LinkedHashMap<>();out.put("mean",sum/count);out.put("median",a[(count-1)/2]);out.put("p95",a[(int)Math.ceil(count*.95)-1]);out.put("p99",a[(int)Math.ceil(count*.99)-1]);return out;}
}
