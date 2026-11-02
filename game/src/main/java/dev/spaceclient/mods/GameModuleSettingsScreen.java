package dev.spaceclient.mods;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.HudViewport;
import dev.spaceclient.hud.HudSwitch;
import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Keyboard and pointer use the same logical canvas as the main module browser. */
public final class GameModuleSettingsScreen extends Screen {
    private static final int TEXT=0xF2EEE7, MUTED=0xB3B0AA, ACCENT=0xD7AF6A;
    private final Screen parent;
    private final GameplayModule module;
    private final HudSwitch.Motion motion;
    private HudViewport viewport;
    private int left,top,panelWidth,focus;
    private boolean capturing;
    private boolean capturingToggle;
    private String status="";
    public GameModuleSettingsScreen(Screen parent,GameplayModule module) {
        this.parent=parent;this.module=module;motion=new HudSwitch.Motion(module.enabled(SpaceClient.mods));
    }
    @Override public void init() {
        viewport=new HudViewport(width,height);panelWidth=Math.min(500,viewport.width-24);
        left=(viewport.width-panelWidth)/2;top=(viewport.height-354)/2;SpaceClient.clicks.clear();
    }
    @Override public boolean shouldPauseGame(){return true;}
    @Override public void removed(){SpaceUiFont.release();SpaceClient.clicks.clear();}
    public boolean capturingKey(){return capturing;}
    private ModSettings settings(){return SpaceClient.mods;}
    @Override public void render(int mx,int my,float delta){
        fillGradient(0,0,width,height,0x85070709,0xA70A0B0E);
        if(width<320||height<240){SpaceUiFont.draw(client,"Resize the window / Esc: back",12,20,TEXT);return;}
        mx=viewport.toUi(mx);my=viewport.toUi(my);
        GlStateManager.pushMatrix();
        try{
            GlStateManager.scale(viewport.scale,viewport.scale,1);
            UiShapes.rounded(left+2,top+3,panelWidth,354,6,0x90000000);
            UiShapes.rounded(left,top,panelWidth,354,6,0xFF3A3B3E);
            UiShapes.rounded(left+1,top+1,panelWidth-2,352,5,0xFF101114);
            SpaceUiFont.draw(client,"SPACE / MODULE",left+20,top+16,ACCENT);
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.8f,1.8f,1);
            SpaceUiFont.draw(client,module.title,(left+20)/1.8f,(top+35)/1.8f,TEXT);
            GlStateManager.popMatrix();
            SpaceUiFont.draw(client,module.description,left+20,top+62,MUTED);
            List<Action> actions=controls();
            for(int i=0;i<actions.size();i++){
                Action a=actions.get(i);boolean selected=focus==i,hovered=a.contains(mx,my);
                UiShapes.rounded(a.x,a.y,a.w,27,4,selected?0xFFD7AF6A:0xFF3A3B3E);
                UiShapes.rounded(a.x+1,a.y+1,a.w-2,25,3,selected||hovered?0xFF2A2824:0xFF191B20);
                SpaceUiFont.draw(client,a.label,a.x+9,a.y+8,settings().isReadOnly()&&a.id!=99?MUTED:TEXT);
                if(a.id==0)HudSwitch.draw(a.x+a.w-37,a.y+6,motion.sample(module.enabled(settings()),System.nanoTime()));
                else SpaceUiFont.draw(client,a.value,a.x+a.w-9-SpaceUiFont.width(a.value),a.y+8,a.id==99?TEXT:ACCENT);
            }
            String hint=hint();
            if(module==GameplayModule.CROSSHAIR){
                UiShapes.rounded(left+20,top+266,panelWidth-40,35,5,0xFF242A30);
                SpaceUiFont.draw(client,"Live preview",left+32,top+279,MUTED);
                float fit=Math.min(1,14/settings().crosshairStyle().extent());
                GlStateManager.pushMatrix();GlStateManager.translate(left+panelWidth-75,top+283,0);GlStateManager.scale(fit,fit,1);
                CrosshairRenderer.draw(0,0,settings().crosshairStyle());GlStateManager.popMatrix();
            }else{
                SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(hint,panelWidth-40),left+20,top+192,MUTED);
                SpaceUiFont.draw(client,"Toggle key enables or disables this module",left+20,top+213,MUTED);
                SpaceUiFont.draw(client,"Settings save automatically",left+20,top+234,MUTED);
            }
            String notice=settings().error();
            if(notice.isEmpty())notice=capturing?"Press a key / Esc: cancel / Backspace: unbind":status;
            if(notice.isEmpty())notice="Tab: select  |  Enter: change  |  Left / Right: adjust";
            SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(notice,panelWidth-40),left+20,top+311,notice.equals(settings().error())&&!notice.isEmpty()?ACCENT:MUTED);
        }finally{GlStateManager.popMatrix();}
    }
    private String hint(){
        if(module==GameplayModule.SNAPLOOK) {
            int key=settings().snaplookKey();
            return key==0?"Choose a hold key to use Snaplook.":ModuleKeys.conflicts(client,key)||ModuleKeys.usedByToggle(settings(),null,key)
                ?"Hold key unavailable: choose an unused key":"Hold "+ModuleKeys.label(key)+" for this view. Release to return.";
        }
        if(module==GameplayModule.CHAT_TIMESTAMPS)return "Local arrival time; existing messages keep their timestamps.";
        if(module==GameplayModule.DURABILITY_WARNING)return "Checks worn armor and your held item while the HUD is visible.";
        if(module==GameplayModule.WEATHER_CHANGER)return "Overworld display only; server weather stays unchanged.";
        if(module==GameplayModule.NO_HURT_CAM)return "Keeps the camera steady on damage; death view stays enabled.";
        if(module==GameplayModule.FULLBRIGHT)return "Brightens rendering; your saved Minecraft brightness stays unchanged.";
        if(module==GameplayModule.TIME_CHANGER)return "Overworld sky only; world time and Day Counter stay live.";
        if(module==GameplayModule.FREE_LOOK)return ModuleKeys.conflicts(client,settings().freeLookKey())?"Key unavailable: choose an unused key":"Hold "+ModuleKeys.label(settings().freeLookKey())+" to look around. Release to restore your view.";
        if(module==GameplayModule.ZOOM)return ModuleKeys.conflicts(client,settings().zoomKey())?"Key unavailable: choose an unused key":"Hold "+ModuleKeys.label(settings().zoomKey())+" to zoom. FOV restores on release.";
        return settings().sprintMode()==ModSettings.SprintMode.AUTO?"Auto: sprint while moving forward; vanilla restrictions apply.":"Toggle: press "+ModuleKeys.label(client.options.sprintKey.getCode())+" to switch sprint on or off.";
    }
    private List<Action> controls(){
        List<Action>a=new ArrayList<Action>();
        row(a,0,"Enabled","");
        row(a,12,"Toggle key",capturing&&capturingToggle?"Press key...":ModuleKeys.label(settings().toggleKey(module)));
        if(module==GameplayModule.TOGGLE_SPRINT)row(a,1,"Mode",settings().sprintMode()==ModSettings.SprintMode.AUTO?"Auto":"Toggle");
        else if(module==GameplayModule.FREE_LOOK)row(a,2,"Hold key",capturing&&!capturingToggle?"Press key...":ModuleKeys.label(settings().freeLookKey()));
        else if(module==GameplayModule.SNAPLOOK){
            row(a,2,"Hold key",capturing&&!capturingToggle?"Press key...":ModuleKeys.label(settings().snaplookKey()));
            row(a,15,"Perspective",settings().snapPerspective().label);
        }
        else if(module==GameplayModule.CHAT_TIMESTAMPS)row(a,17,"Time format",settings().chatTimeFormat().label);
        else if(module==GameplayModule.DURABILITY_WARNING)row(a,18,"Warn at",settings().durabilityThreshold().percent+"% or less");
        else if(module==GameplayModule.WEATHER_CHANGER)row(a,16,"Weather",label(settings().weatherMode().name()));
        else if(module==GameplayModule.ZOOM){
            row(a,2,"Hold key",capturing&&!capturingToggle?"Press key...":ModuleKeys.label(settings().zoomKey()));
            row(a,3,"Magnification",settings().zoomFactor()+"x");row(a,4,"Smooth zoom",settings().zoomSmooth()?"On":"Off");
        }else if(module==GameplayModule.FULLBRIGHT)row(a,13,"Intensity",label(settings().brightness().name()));
        else if(module==GameplayModule.TIME_CHANGER)row(a,14,"Sky time",label(settings().timePreset().name()));
        else if(module==GameplayModule.CROSSHAIR){
            CrosshairStyle s=settings().crosshairStyle();
            row(a,5,"Shape",label(s.shape.name()));row(a,6,"Color",label(s.color.name()));row(a,7,"Size",Integer.toString(s.size));
            row(a,8,"Gap",Integer.toString(s.gap));row(a,9,"Thickness",Integer.toString(s.thickness));
            row(a,10,"Center dot",s.dot?"On":"Off");row(a,11,"Outline",s.outline?"On":"Off");
        }
        a.add(new Action(98,"Reset settings","",left+20,top+322,128));
        a.add(new Action(99,"Back","Esc",left+panelWidth-116,top+322,96));
        return a;
    }
    private void row(List<Action>a,int id,String label,String value){
        int i=a.size(),w=(panelWidth-52)/2;
        a.add(new Action(id,label,value,left+20+(i%2)*(w+12),top+86+(i/2)*34,w));
    }
    private static String label(String s){return s.substring(0,1)+s.substring(1).toLowerCase(Locale.ROOT);}
    @Override protected void keyPressed(char c,int key){
        if(capturing){
            if(Keyboard.isRepeatEvent())return;
            if(key==Keyboard.KEY_ESCAPE){capturing=false;return;}
            int code=key==Keyboard.KEY_BACK?0:key;
            boolean conflict = capturingToggle ? ModuleKeys.toggleConflicts(client,module,code)
                : ModuleKeys.conflicts(client,code)||ModuleKeys.usedByHold(settings(),module,code)||ModuleKeys.usedByToggle(settings(),null,code);
            if(code!=0&&conflict){status="Key already used; choose another key";capturing=false;return;}
            boolean saved=capturingToggle?settings().toggleKey(module,code):settings().holdKey(module,code);
            capturing=false;status=saved?"Key saved":"Could not save key";return;
        }
        if(key==Keyboard.KEY_ESCAPE){client.setScreen(parent);return;}
        if(width<320||height<240)return;
        if(key==Keyboard.KEY_TAB||key==Keyboard.KEY_DOWN||key==Keyboard.KEY_UP){focus=Math.floorMod(focus+(key==Keyboard.KEY_UP||(key==Keyboard.KEY_TAB&&hasShiftDown())?-1:1),controls().size());return;}
        if(!Keyboard.isRepeatEvent()&&(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER||key==Keyboard.KEY_SPACE||key==Keyboard.KEY_LEFT||key==Keyboard.KEY_RIGHT))activate(controls().get(focus).id,key==Keyboard.KEY_LEFT?-1:1);
    }
    @Override protected void mouseClicked(int x,int y,int button){
        if(button!=0||capturing||width<320||height<240)return;x=viewport.toUi(x);y=viewport.toUi(y);
        List<Action>a=controls();for(int i=0;i<a.size();i++)if(a.get(i).contains(x,y)){focus=i;activate(a.get(i).id,1);return;}
    }
    private void activate(int id,int direction){
        if(id==99){client.setScreen(parent);return;}if(settings().isReadOnly())return;
        if(id==2||id==12){capturing=true;capturingToggle=id==12;status="";return;}
        CrosshairStyle s=settings().crosshairStyle();boolean saved;
        if(id==0)saved=module.toggle(settings());
        else if(id==1)saved=settings().sprintMode(settings().sprintMode()==ModSettings.SprintMode.AUTO?ModSettings.SprintMode.TOGGLE:ModSettings.SprintMode.AUTO);
        else if(id==3)saved=settings().zoomFactor(2+Math.floorMod(settings().zoomFactor()-2+direction,7));
        else if(id==4)saved=settings().zoomSmooth(!settings().zoomSmooth());
        else if(id==5)saved=settings().crosshairStyle(s.withShape(CrosshairStyle.Shape.values()[Math.floorMod(s.shape.ordinal()+direction,CrosshairStyle.Shape.values().length)]));
        else if(id==6)saved=settings().crosshairStyle(s.withColor(CrosshairStyle.Color.values()[Math.floorMod(s.color.ordinal()+direction,CrosshairStyle.Color.values().length)]));
        else if(id==7)saved=settings().crosshairStyle(s.withSize(2+Math.floorMod(s.size-2+direction,11)));
        else if(id==8)saved=settings().crosshairStyle(s.withGap(Math.floorMod(s.gap+direction,9)));
        else if(id==9)saved=settings().crosshairStyle(s.withThickness(1+Math.floorMod(s.thickness-1+direction,4)));
        else if(id==10)saved=settings().crosshairStyle(s.withDot(!s.dot));
        else if(id==11)saved=settings().crosshairStyle(s.withOutline(!s.outline));
        else if(id==13)saved=settings().brightness(ModSettings.Brightness.values()[Math.floorMod(settings().brightness().ordinal()+direction,ModSettings.Brightness.values().length)]);
        else if(id==14)saved=settings().timePreset(ModSettings.TimePreset.values()[Math.floorMod(settings().timePreset().ordinal()+direction,ModSettings.TimePreset.values().length)]);
        else if(id==15)saved=settings().snapPerspective(ModSettings.SnapPerspective.values()[Math.floorMod(settings().snapPerspective().ordinal()+direction,ModSettings.SnapPerspective.values().length)]);
        else if(id==16)saved=settings().weatherMode(ModSettings.WeatherMode.values()[Math.floorMod(settings().weatherMode().ordinal()+direction,ModSettings.WeatherMode.values().length)]);
        else if(id==17)saved=settings().chatTimeFormat(ModSettings.ChatTimeFormat.values()[Math.floorMod(settings().chatTimeFormat().ordinal()+direction,ModSettings.ChatTimeFormat.values().length)]);
        else if(id==18)saved=settings().durabilityThreshold(ModSettings.DurabilityThreshold.values()[Math.floorMod(settings().durabilityThreshold().ordinal()+direction,ModSettings.DurabilityThreshold.values().length)]);
        else if(id==98)saved=settings().reset(module);else return;
        status=saved?"Saved":"Could not save settings";
    }
    private static final class Action{
        final int id,x,y,w;final String label,value;
        Action(int id,String label,String value,int x,int y,int w){this.id=id;this.label=label;this.value=value;this.x=x;this.y=y;this.w=w;}
        boolean contains(int px,int py){return px>=x&&px<x+w&&py>=y&&py<y+27;}
    }
}
