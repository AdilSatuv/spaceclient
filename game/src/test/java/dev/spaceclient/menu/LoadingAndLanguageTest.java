package dev.spaceclient.menu;
import dev.spaceclient.mods.GameplayModule;
import dev.spaceclient.hud.HudModule;
import org.junit.Test;
import static org.junit.Assert.*;

public class LoadingAndLanguageTest {
    @Test public void onlyNativePercentagesInRangeAreDisplayed(){assertEquals(-1,LoadingTheme.knownProgress(-1));assertEquals(-1,LoadingTheme.knownProgress(101));assertEquals(0,LoadingTheme.knownProgress(0));assertEquals(47,LoadingTheme.knownProgress(47));assertEquals(100,LoadingTheme.knownProgress(100));}
    @Test public void everyCatalogSummaryHasRussianTextAndUnknownPhrasesFallback(){for(GameplayModule m:GameplayModule.values())assertNotEquals(m.title,m.description,UiTranslations.translate(true,m.description));for(HudModule m:HudModule.values())assertNotEquals(m.getTitle(),m.getDescription(),UiTranslations.translate(true,m.getDescription()));assertEquals("New future phrase",UiTranslations.translate(true,"New future phrase"));assertEquals("Scale",UiTranslations.translate(false,"Scale"));assertEquals("Масштаб",UiTranslations.translate(true,"Scale"));}
    @Test public void loadingTargetsExistWithoutStartingMinecraft()throws Exception{ClassLoader l=getClass().getClassLoader();Class<?> client=Class.forName("net.minecraft.client.MinecraftClient",false,l),textures=Class.forName("net.minecraft.client.texture.TextureManager",false,l);assertEquals(void.class,client.getDeclaredMethod("loadLogo",textures).getReturnType());Class<?> loading=Class.forName("net.minecraft.client.render.LoadingScreenRenderer",false,l);assertEquals(void.class,loading.getDeclaredMethod("setProgressPercentage",int.class).getReturnType());for(String n:new String[]{"ProgressScreen","DownloadingTerrainScreen"})assertEquals(void.class,Class.forName("net.minecraft.client.gui.screen."+n,false,l).getDeclaredMethod("render",int.class,int.class,float.class).getReturnType());}
    @Test public void nativeConnectionScreensHaveIndependentKeyboardOverrides()throws Exception{for(String n:new String[]{"ConnectScreen","DisconnectedScreen"})assertEquals(void.class,Class.forName("net.minecraft.client.gui.screen."+n,false,getClass().getClassLoader()).getDeclaredMethod("keyPressed",char.class,int.class).getReturnType());}
}
