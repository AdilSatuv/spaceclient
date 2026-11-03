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
    private boolean capturingToggle,capturingMacro,editingText;
    private int macroSlot;
    private String macroBuffer="";
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
    public boolean capturingKey(){return capturing||editingText;}
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
                if(a.id==33){String message=editingText?macroBuffer+"|":settings().autoTextMessage(macroSlot);SpaceUiFont.draw(client,SpaceUiFont.trimToWidth("Text: "+(message.isEmpty()?"Click to type...":message),a.w-18),a.x+9,a.y+8,TEXT);continue;}
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
                int infoTop=Math.max(192,86+((actions.size()-1)/2)*34+4);
                SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(hint,panelWidth-40),left+20,top+infoTop,MUTED);
                SpaceUiFont.draw(client,"Toggle key enables or disables this module",left+20,top+infoTop+21,MUTED);
                SpaceUiFont.draw(client,"Settings save automatically",left+20,top+infoTop+42,MUTED);
            }
            if(module==GameplayModule.BLOCK_OVERLAY||module==GameplayModule.HIT_COLOR||module==GameplayModule.FOV_CONTROL)
                VisualModPreview.draw(client,module,settings(),left+20,top+259,panelWidth-40);
            String notice=settings().error();
            if(editingText)notice="Enter: save text / Esc: cancel / Ctrl+V: paste";
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
        if(module==GameplayModule.SCOREBOARD)return "Styles the server sidebar; move with X/Y offsets.";
        if(module==GameplayModule.TNT_COUNTDOWN)return "Visible TNT only; fuse time is a client estimate.";
        if(module==GameplayModule.SCROLLABLE_TOOLTIPS)return "Long tooltips: wheel vertically, Shift+wheel horizontally.";
        if(module==GameplayModule.WAYPOINTS)return "Named markers are saved per world/server and dimension.";
        if(module==GameplayModule.PARTICLES)return "Factory particles can multiply; block debris keeps native counts.";
        if(module==GameplayModule.ENCHANTMENT_GLINT)return "Item and armor shimmer; resource-pack textures are preserved.";
        if(module==GameplayModule.INVENTORY_BLUR)return "Own blur behind inventories; needs OpenGL 2.0 and framebuffer.";
        if(module==GameplayModule.DYNAMIC_FPS)return "A lower Minecraft FPS cap always takes priority.";
        if(module==GameplayModule.ANIMATIONS)return "Legacy-style first-person poses; combat timing stays native.";
        if(module==GameplayModule.AUTO_TEXT)return "Only during gameplay; at most one message per second.";
        if(module==GameplayModule.GUI)return "Effective: "+new net.minecraft.client.util.Window(client).getScaleFactor()+"x; limited by your window size.";
        if(module==GameplayModule.HITBOXES)return "Depth-tested boxes; choose the entity types below.";
        if(module==GameplayModule.ITEM_PHYSICS)return "Visual motion only; item pickup stays unchanged.";
        if(module==GameplayModule.ITEM_SIZE)return "First-person items and drops; maps keep their native size.";
        if(module==GameplayModule.SOUNDS)return "Blocks include steps; target down: attacked in the last 3s.";
        if(module==GameplayModule.BLOCK_OVERLAY)return "Styles the block you point at; respects its shape and depth.";
        if(module==GameplayModule.HIT_COLOR)return "Damage tint on visible models; death tint stays native.";
        if(module==GameplayModule.FOV_CONTROL)return "0% dynamics: steady view. Saved Minecraft FOV stays unchanged.";
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
        if(module==GameplayModule.SCOREBOARD){row(a,49,"Show numbers",on(settings().scoreboardNumbers()));row(a,50,"Background",on(settings().scoreboardBackground()));row(a,51,"Scale",Integer.toString(settings().scoreboardScale()));row(a,52,"X offset",Integer.toString(settings().scoreboardX()));row(a,53,"Y offset",Integer.toString(settings().scoreboardY()));}
        else if(module==GameplayModule.TNT_COUNTDOWN){row(a,54,"Color",label(settings().tntColor().name()));row(a,55,"Background",on(settings().tntBackground()));row(a,56,"Max distance",Integer.toString(settings().tntDistance()));}
        else if(module==GameplayModule.SCROLLABLE_TOOLTIPS){row(a,57,"Scroll pixels",Integer.toString(settings().tooltipSpeed()));}
        else if(module==GameplayModule.WAYPOINTS){row(a,58,"Marker beam",on(settings().waypointBeam()));row(a,59,"Show distance",on(settings().waypointDistance()));row(a,60,"Manage markers",Integer.toString(WaypointRuntime.current(client).size()));}
        else if(module==GameplayModule.PARTICLES){row(a,61,"Amount %",Integer.toString(settings().particleAmount()));row(a,62,"Size %",Integer.toString(settings().particleScale()));row(a,63,"Custom tint",on(settings().particleTint()));row(a,64,"Color",label(settings().particleColor().name()));}
        else if(module==GameplayModule.ENCHANTMENT_GLINT){row(a,65,"Color",label(settings().glintColor().name()));row(a,66,"Speed %",Integer.toString(settings().glintSpeed()));row(a,67,"Strength %",Integer.toString(settings().glintStrength()));}
        else if(module==GameplayModule.INVENTORY_BLUR){row(a,68,"Blur radius",Integer.toString(settings().blurStrength()));}
        else if(module==GameplayModule.DYNAMIC_FPS){row(a,69,"Background FPS",Integer.toString(settings().backgroundFps()));row(a,70,"Idle FPS",Integer.toString(settings().idleFps()));row(a,71,"Idle after (sec)",Integer.toString(settings().idleSeconds()));}
        else if(module==GameplayModule.ANIMATIONS){row(a,28,"Block hit",on(settings().animationBlockHit()));row(a,29,"Eating",on(settings().animationEat()));row(a,30,"Bow pose",on(settings().animationBow()));}
        else if(module==GameplayModule.AUTO_TEXT){row(a,31,"Slot",Integer.toString(macroSlot+1));row(a,32,"Hotkey",capturing&&capturingMacro?"Press key...":ModuleKeys.label(settings().autoTextKey(macroSlot)));a.add(new Action(33,"Text","",left+20,top+154,panelWidth-40));}
        else if(module==GameplayModule.GUI)row(a,34,"GUI scale",settings().guiScale()==0?"Auto":settings().guiScale()+"x");
        else if(module==GameplayModule.HITBOXES){row(a,35,"Color",label(settings().hitboxColor().name()));row(a,36,"Line width",Integer.toString(settings().hitboxWidth()));row(a,37,"Players",on(settings().hitboxPlayers()));row(a,38,"Mobs",on(settings().hitboxMobs()));row(a,39,"Items",on(settings().hitboxItems()));}
        else if(module==GameplayModule.ITEM_PHYSICS){row(a,40,"Spin speed",settings().physicsSpeed()+"x");row(a,41,"Flat on ground",on(settings().physicsFlat()));}
        else if(module==GameplayModule.ITEM_SIZE){row(a,42,"First person",settings().heldItemScale()+"%");row(a,43,"Dropped items",settings().droppedItemScale()+"%");}
        else if(module==GameplayModule.SOUNDS){row(a,44,"Blocks / steps",label(settings().blockSounds().name()));row(a,45,"Combat",label(settings().combatSounds().name()));row(a,46,"Target down",settings().killChime().label);row(a,47,"Effect volume",settings().soundsVolume()+"%");row(a,48,"Preview sound","Play");}
        else if(module==GameplayModule.TOGGLE_SPRINT)row(a,1,"Mode",settings().sprintMode()==ModSettings.SprintMode.AUTO?"Auto":"Toggle");
        else if(module==GameplayModule.FREE_LOOK)row(a,2,"Hold key",capturing&&!capturingToggle?"Press key...":ModuleKeys.label(settings().freeLookKey()));
        else if(module==GameplayModule.SNAPLOOK){
            row(a,2,"Hold key",capturing&&!capturingToggle?"Press key...":ModuleKeys.label(settings().snaplookKey()));
            row(a,15,"Perspective",settings().snapPerspective().label);
        }
        else if(module==GameplayModule.BLOCK_OVERLAY){
            row(a,19,"Color",label(settings().blockColor().name()));row(a,20,"Style",label(settings().blockMode().name()));
            row(a,21,"Fill opacity",settings().blockOpacity()+"%");row(a,22,"Line width",Integer.toString(settings().blockWidth()));
        }else if(module==GameplayModule.HIT_COLOR){
            row(a,23,"Color",label(settings().damageColor().name()));row(a,24,"Tint strength",settings().hitOpacity()+"%");row(a,25,"Only players",settings().hitPlayersOnly()?"On":"Off");
        }else if(module==GameplayModule.FOV_CONTROL){row(a,26,"View angle",settings().cameraFov()+" degrees");row(a,27,"Dynamic FOV",settings().dynamicFov()+"%");}
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
    private static int step(int current,int min,int max,int amount,int direction){return min+Math.floorMod((current-min)/amount+direction,(max-min)/amount+1)*amount;}
    private static String on(boolean value){return value?"On":"Off";}
    private void saveText(){boolean saved=settings().autoTextMessage(macroSlot,macroBuffer);editingText=false;status=saved?"Text saved":"Could not save text";}
    private static String label(String s){return s.substring(0,1)+s.substring(1).toLowerCase(Locale.ROOT);}
    @Override protected void keyPressed(char c,int key){
        if(editingText){
            if(key==Keyboard.KEY_ESCAPE){editingText=false;status="Edit cancelled";return;}
            if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER){saveText();return;}
            if(key==Keyboard.KEY_BACK){if(!macroBuffer.isEmpty())macroBuffer=macroBuffer.substring(0,macroBuffer.offsetByCodePoints(macroBuffer.length(),-1));return;}
            if(key==Keyboard.KEY_V&&hasControlDown()){String pasted=getClipboard();if(pasted!=null){String candidate=macroBuffer+pasted;if(ModSettings.validText(candidate))macroBuffer=candidate;}return;}
            if(c>=32&&c!=127&&c!=167&&macroBuffer.length()<100)macroBuffer+=c;return;
        }
        if(capturing){
            if(Keyboard.isRepeatEvent())return;
            if(key==Keyboard.KEY_ESCAPE){capturing=false;return;}
            int code=key==Keyboard.KEY_BACK?0:key;
            boolean conflict = capturingMacro?ModuleKeys.autoTextConflicts(client,macroSlot,code):capturingToggle ? ModuleKeys.toggleConflicts(client,module,code)
                : ModuleKeys.conflicts(client,code)||ModuleKeys.usedByHold(settings(),module,code)||ModuleKeys.usedByToggle(settings(),null,code)||ModuleKeys.usedByAutoText(settings(),-1,code);
            if(code!=0&&conflict){status="Key already used; choose another key";capturing=false;return;}
            boolean saved=capturingMacro?settings().autoTextKey(macroSlot,code):capturingToggle?settings().toggleKey(module,code):settings().holdKey(module,code);
            capturing=false;status=saved?"Key saved":"Could not save key";return;
        }
        if(key==Keyboard.KEY_ESCAPE){client.setScreen(parent);return;}
        if(width<320||height<240)return;
        if(key==Keyboard.KEY_TAB||key==Keyboard.KEY_DOWN||key==Keyboard.KEY_UP){focus=Math.floorMod(focus+(key==Keyboard.KEY_UP||(key==Keyboard.KEY_TAB&&hasShiftDown())?-1:1),controls().size());return;}
        if(!Keyboard.isRepeatEvent()&&(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER||key==Keyboard.KEY_SPACE||key==Keyboard.KEY_LEFT||key==Keyboard.KEY_RIGHT))activate(controls().get(focus).id,key==Keyboard.KEY_LEFT?-1:1);
    }
    @Override protected void mouseClicked(int x,int y,int button){
        if(button!=0||capturing||width<320||height<240)return;if(editingText)saveText();x=viewport.toUi(x);y=viewport.toUi(y);
        List<Action>a=controls();for(int i=0;i<a.size();i++)if(a.get(i).contains(x,y)){focus=i;activate(a.get(i).id,1);return;}
    }
    private void activate(int id,int direction){
        if(id==60){client.setScreen(new WaypointsScreen(this));return;}
        if(id==99){client.setScreen(parent);return;}if(settings().isReadOnly())return;
        if(id==2||id==12||id==32){capturing=true;capturingToggle=id==12;capturingMacro=id==32;status="";return;}
        if(id==33){editingText=true;macroBuffer=settings().autoTextMessage(macroSlot);return;}
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
        else if(id==19)saved=settings().blockColor(VisualPalette.values()[Math.floorMod(settings().blockColor().ordinal()+direction,VisualPalette.values().length)]);
        else if(id==20)saved=settings().blockMode(ModSettings.BlockMode.values()[Math.floorMod(settings().blockMode().ordinal()+direction,ModSettings.BlockMode.values().length)]);
        else if(id==21)saved=settings().blockOpacity(step(settings().blockOpacity(),5,60,5,direction));
        else if(id==22)saved=settings().blockWidth(1+Math.floorMod(settings().blockWidth()-1+direction,5));
        else if(id==23)saved=settings().damageColor(VisualPalette.values()[Math.floorMod(settings().damageColor().ordinal()+direction,VisualPalette.values().length)]);
        else if(id==24)saved=settings().hitOpacity(step(settings().hitOpacity(),5,80,5,direction));
        else if(id==25)saved=settings().hitPlayersOnly(!settings().hitPlayersOnly());
        else if(id==26)saved=settings().cameraFov(step(settings().cameraFov(),60,110,5,direction));
        else if(id==27)saved=settings().dynamicFov(step(settings().dynamicFov(),0,100,25,direction));
        else if(id==28)saved=settings().animationBlockHit(!settings().animationBlockHit());
        else if(id==29)saved=settings().animationEat(!settings().animationEat());
        else if(id==30)saved=settings().animationBow(!settings().animationBow());
        else if(id==31){macroSlot=Math.floorMod(macroSlot+direction,3);return;}
        else if(id==34)saved=settings().guiScale(Math.floorMod(settings().guiScale()+direction,5));
        else if(id==35)saved=settings().hitboxColor(VisualPalette.values()[Math.floorMod(settings().hitboxColor().ordinal()+direction,VisualPalette.values().length)]);
        else if(id==36)saved=settings().hitboxWidth(1+Math.floorMod(settings().hitboxWidth()-1+direction,3));
        else if(id==37)saved=settings().hitboxPlayers(!settings().hitboxPlayers());
        else if(id==38)saved=settings().hitboxMobs(!settings().hitboxMobs());
        else if(id==39)saved=settings().hitboxItems(!settings().hitboxItems());
        else if(id==40)saved=settings().physicsSpeed(1+Math.floorMod(settings().physicsSpeed()-1+direction,5));
        else if(id==41)saved=settings().physicsFlat(!settings().physicsFlat());
        else if(id==42)saved=settings().heldItemScale(step(settings().heldItemScale(),50,150,10,direction));
        else if(id==43)saved=settings().droppedItemScale(step(settings().droppedItemScale(),25,200,25,direction));
        else if(id==44)saved=settings().blockSounds(ModSettings.SoundTheme.values()[Math.floorMod(settings().blockSounds().ordinal()+direction,ModSettings.SoundTheme.values().length)]);
        else if(id==45)saved=settings().combatSounds(ModSettings.SoundTheme.values()[Math.floorMod(settings().combatSounds().ordinal()+direction,ModSettings.SoundTheme.values().length)]);
        else if(id==46)saved=settings().killChime(ModSettings.KillChime.values()[Math.floorMod(settings().killChime().ordinal()+direction,ModSettings.KillChime.values().length)]);
        else if(id==47)saved=settings().soundsVolume(step(settings().soundsVolume(),0,100,10,direction));
        else if(id==48){SoundRuntime.preview(client);status=settings().killChime()==ModSettings.KillChime.OFF?"Choose a target-down sound first":"Preview played";return;}
        else if(id==49)saved=settings().scoreboardNumbers(!settings().scoreboardNumbers());
        else if(id==50)saved=settings().scoreboardBackground(!settings().scoreboardBackground());
        else if(id==51)saved=settings().scoreboardScale(step(settings().scoreboardScale(),50,150,10,direction));
        else if(id==52)saved=settings().scoreboardX(step(settings().scoreboardX(),-500,500,10,direction));
        else if(id==53)saved=settings().scoreboardY(step(settings().scoreboardY(),-500,500,10,direction));
        else if(id==54)saved=settings().tntColor(VisualPalette.values()[Math.floorMod(settings().tntColor().ordinal()+direction,VisualPalette.values().length)]);
        else if(id==55)saved=settings().tntBackground(!settings().tntBackground());
        else if(id==56)saved=settings().tntDistance(step(settings().tntDistance(),8,128,8,direction));
        else if(id==57)saved=settings().tooltipSpeed(step(settings().tooltipSpeed(),2,30,2,direction));
        else if(id==58)saved=settings().waypointBeam(!settings().waypointBeam());
        else if(id==59)saved=settings().waypointDistance(!settings().waypointDistance());
        else if(id==61)saved=settings().particleAmount(step(settings().particleAmount(),0,300,25,direction));
        else if(id==62)saved=settings().particleScale(step(settings().particleScale(),50,200,25,direction));
        else if(id==63)saved=settings().particleTint(!settings().particleTint());
        else if(id==64)saved=settings().particleColor(VisualPalette.values()[Math.floorMod(settings().particleColor().ordinal()+direction,VisualPalette.values().length)]);
        else if(id==65)saved=settings().glintColor(VisualPalette.values()[Math.floorMod(settings().glintColor().ordinal()+direction,VisualPalette.values().length)]);
        else if(id==66)saved=settings().glintSpeed(step(settings().glintSpeed(),25,300,25,direction));
        else if(id==67)saved=settings().glintStrength(step(settings().glintStrength(),10,100,10,direction));
        else if(id==68)saved=settings().blurStrength(step(settings().blurStrength(),1,8,1,direction));
        else if(id==69)saved=settings().backgroundFps(step(settings().backgroundFps(),5,60,5,direction));
        else if(id==70)saved=settings().idleFps(step(settings().idleFps(),10,120,10,direction));
        else if(id==71)saved=settings().idleSeconds(step(settings().idleSeconds(),10,300,10,direction));
        else if(id==98)saved=settings().reset(module);else return;
        status=saved?"Saved":"Could not save settings";
    }
    private static final class Action{
        final int id,x,y,w;final String label,value;
        Action(int id,String label,String value,int x,int y,int w){this.id=id;this.label=label;this.value=value;this.x=x;this.y=y;this.w=w;}
        boolean contains(int px,int py){return px>=x&&px<x+w&&py>=y&&py<y+27;}
    }
}
