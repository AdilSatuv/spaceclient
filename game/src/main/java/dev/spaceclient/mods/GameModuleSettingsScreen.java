package dev.spaceclient.mods;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import dev.spaceclient.settings.ModuleSettingsScreen;
import dev.spaceclient.settings.SettingRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Presents existing parameters; validation and persistence remain with ModSettings. */
public final class GameModuleSettingsScreen extends ModuleSettingsScreen {
    private final GameplayModule module;
    private int captureId=-1,macroSlot;
    private boolean editingText;
    private String macroBuffer="";
    public GameModuleSettingsScreen(Screen parent,GameplayModule module) {
        super(parent);
        if(module==null)throw new IllegalArgumentException("Module is required");
        this.module=module;
    }
    private ModSettings settings() { return SpaceClient.mods; }
    @Override protected String title() { return module.title; }
    @Override protected String subtitle() { return module.description; }
    @Override protected boolean readOnly() { return settings().isReadOnly(); }
    public boolean capturingKey() { return captureId!=-1||editingText; }
    @Override protected boolean modal() { return capturingKey(); }
    @Override protected boolean navigation(int id) { return id==99||id==31; }
    @Override protected String notice() {
        if(!settings().error().isEmpty())return settings().error();
        if(editingText)return (status.isEmpty()?"Enter: save / Esc: cancel / Ctrl+V: paste":status)+"  |  "+macroBuffer.length()+" / 100";
        if(captureId!=-1)return status.isEmpty()?"Press a key / Esc: cancel / Backspace: unbind":status;
        return status;
    }
    @Override protected List<SettingRow> rows() {
        ModSettings s=settings(); List<SettingRow>a=new ArrayList<>();
        a.add(SettingRow.toggle(0,"Enabled",module.enabled(s)?"Changes apply while this module is on.":"Off: configure now, enable when ready.",module.enabled(s)));
        int toggle=s.toggleKey(module);
        a.add(SettingRow.action(12,"Toggle key",toggle!=0&&ModuleKeys.toggleConflicts(client,module,toggle)?"Conflict: assign an unused key.":"Press once to enable or disable this module.",keyValue(12,toggle)));
        switch(module) {
            case TOGGLE_SPRINT:
                a.add(SettingRow.choice(1,"Sprint mode",s.sprintMode()==ModSettings.SprintMode.AUTO?"Forward movement; vanilla restrictions apply.":"Press your Minecraft sprint key to toggle.",label(s.sprintMode().name()))); break;
            case FREE_LOOK:hold(a,s.freeLookKey(),"Release to restore the player's view.");break;
            case SNAPLOOK:
                hold(a,s.snaplookKey(),"Release to restore your previous perspective.");
                a.add(SettingRow.choice(15,"Held perspective","Only used while the hold key is down.",s.snapPerspective().label)); break;
            case ZOOM:
                hold(a,s.zoomKey(),"Hold to zoom; release to restore FOV.");
                a.add(n(3,"Magnification","Higher values bring the scene closer.",s.zoomFactor(),2,8,1,"x"));
                a.add(SettingRow.toggle(4,"Smooth zoom","Blend camera changes on press and release.",s.zoomSmooth())); break;
            case CROSSHAIR:
                CrosshairStyle c=s.crosshairStyle();
                a.add(SettingRow.choice(5,"Shape","Replaces the default first-person crosshair.",label(c.shape.name())));
                a.add(SettingRow.choice(6,"Color","Rainbow changes with time.",label(c.color.name())));
                a.add(n(7,"Size","Length or radius in HUD pixels.",c.size,2,12,1," px"));
                a.add(n(8,"Center gap","Space between the center and the arms.",c.gap,0,8,1," px"));
                a.add(n(9,"Thickness","Stroke width in HUD pixels.",c.thickness,1,4,1," px"));
                a.add(SettingRow.toggle(10,"Center dot","Keep a visible point in the center.",c.dot));
                a.add(SettingRow.toggle(11,"Dark outline","Separate the crosshair from the scene.",c.outline)); break;
            case FULLBRIGHT:
                a.add(SettingRow.choice(13,"Brightness","Rendering only; saved Minecraft gamma is kept.",label(s.brightness().name()))); break;
            case TIME_CHANGER:
                a.add(SettingRow.choice(14,"Sky time","Overworld sky only; world time stays live.",label(s.timePreset().name()))); break;
            case NO_HURT_CAM:
                a.set(0,SettingRow.toggle(0,"Enabled","Steady damage camera; death view stays native.",s.noHurtCam())); break;
            case WEATHER_CHANGER:
                a.add(SettingRow.choice(16,"Visual weather","Overworld display; server weather stays live.",label(s.weatherMode().name()))); break;
            case CHAT_TIMESTAMPS:
                a.add(SettingRow.choice(17,"Time format","Local arrival time for new chat messages.",s.chatTimeFormat().label)); break;
            case DURABILITY_WARNING:
                a.add(SettingRow.choice(18,"Warning threshold","Worn armor and held item; requires visible HUD.",s.durabilityThreshold().percent+"% or less")); break;
            case BLOCK_OVERLAY:
                a.add(SettingRow.choice(19,"Color","Uses the selected block's shape and depth.",label(s.blockColor().name())));
                a.add(SettingRow.choice(20,"Style","Outline, translucent fill, or both.",label(s.blockMode().name())));
                SettingRow fill=n(21,"Fill opacity","Higher values make the fill more solid.",s.blockOpacity(),5,60,5,"%");
                a.add(s.blockMode().fill?fill:fill.disabled("Choose Fill or Both to adjust opacity."));
                SettingRow line=n(22,"Outline width","Stroke width in screen pixels.",s.blockWidth(),1,5,1," px");
                a.add(s.blockMode().outline?line:line.disabled("Choose Outline or Both to adjust width.")); break;
            case HIT_COLOR:
                a.add(SettingRow.choice(23,"Damage color","Visible hurt models; death tint stays native.",label(s.damageColor().name())));
                a.add(n(24,"Tint strength","More of the selected color on hurt models.",s.hitOpacity(),5,80,5,"%"));
                a.add(SettingRow.toggle(25,"Players only","Off also tints other visible living entities.",s.hitPlayersOnly())); break;
            case FOV_CONTROL:
                a.add(n(26,"View angle","Camera FOV; saved Minecraft FOV is kept.",s.cameraFov(),60,110,5," deg"));
                a.add(n(27,"Dynamic FOV","0% = steady; 100% = full sprint / bow effects.",s.dynamicFov(),0,100,25,"%")); break;
            case ANIMATIONS:
                a.add(SettingRow.toggle(28,"Block-hit swing","Cosmetic sword swing; combat timing stays native.",s.animationBlockHit()));
                a.add(SettingRow.toggle(29,"Eating pose","Legacy-style first-person food movement.",s.animationEat()));
                a.add(SettingRow.toggle(30,"Bow pose","Legacy-style first-person bow movement.",s.animationBow())); break;
            case AUTO_TEXT:
                a.add(SettingRow.choice(31,"Message slot","Three slots, each with its own key and text.",(macroSlot+1)+" / 3"));
                int key=s.autoTextKey(macroSlot);
                a.add(SettingRow.action(32,"Message hotkey",key!=0&&ModuleKeys.autoTextConflicts(client,macroSlot,key)?"Conflict: assign an unused key.":"Gameplay only; at most one message per second.",keyValue(32,key)));
                a.add(SettingRow.text(33,"Message text","Single line; Enter saves, Esc cancels.",editingText?"Typing...":s.autoTextMessage(macroSlot).isEmpty()?"Write message":"Edit message")); break;
            case GUI:
                a.add(SettingRow.choice(34,"Interface scale","Effective: "+new net.minecraft.client.util.Window(client).getScaleFactor()+"x; capped by window size.",s.guiScale()==0?"Auto":s.guiScale()+"x")); break;
            case HITBOXES:
                a.add(SettingRow.choice(35,"Box color","Visible bounds; depth and collisions stay native.",label(s.hitboxColor().name())));
                a.add(n(36,"Line width","Stroke width in screen pixels.",s.hitboxWidth(),1,3,1," px"));
                a.add(SettingRow.toggle(37,"Players","Other visible players' bounding boxes.",s.hitboxPlayers()));
                a.add(SettingRow.toggle(38,"Mobs","Visible living entities other than players.",s.hitboxMobs()));
                a.add(SettingRow.toggle(39,"Dropped items","Bounding boxes around dropped items.",s.hitboxItems())); break;
            case ITEM_PHYSICS:
                a.add(n(40,"Air spin speed","Visual rotation only; pickup stays native.",s.physicsSpeed(),1,5,1,"x"));
                a.add(SettingRow.toggle(41,"Flat on ground","Lay flat items down and remove bobbing.",s.physicsFlat())); break;
            case ITEM_SIZE:
                a.add(n(42,"First-person size","100% = native size; maps stay unchanged.",s.heldItemScale(),50,150,10,"%"));
                a.add(n(43,"Dropped item size","100% = native; works with Item Physics.",s.droppedItemScale(),25,200,25,"%")); break;
            case SOUNDS:
                a.add(SettingRow.choice(44,"Blocks and footsteps","Choose the local block / step sound theme.",label(s.blockSounds().name())));
                a.add(SettingRow.choice(45,"Combat theme","Choose the local hurt sound theme.",label(s.combatSounds().name())));
                a.add(SettingRow.choice(46,"Target-down cue","Target attacked within 3s; not kill credit.",s.killChime().label));
                a.add(n(47,"Effect volume","Also respects Minecraft's sound categories.",s.soundsVolume(),0,100,10,"%"));
                SettingRow play=SettingRow.action(48,"Preview target-down cue","Plays locally using the effect volume.","Play");
                a.add(s.killChime()==ModSettings.KillChime.OFF?play.disabled("Choose a target-down cue first."):s.soundsVolume()==0?play.disabled("Raise effect volume to hear the preview."):play); break;
            default: break; // New modules retain enabled / toggle controls.
        }
        return a;
    }
    private SettingRow n(int id,String title,String detail,int value,int min,int max,int step,String unit) {
        return SettingRow.number(id,title,detail,value,min,max,step,v->v+unit);
    }
    private void hold(List<SettingRow>a,int key,String detail) {
        boolean conflict=key!=0&&(ModuleKeys.conflicts(client,key)||ModuleKeys.usedByHold(settings(),module,key)||ModuleKeys.usedByToggle(settings(),null,key)||ModuleKeys.usedByAutoText(settings(),-1,key));
        a.add(SettingRow.action(2,"Hold key",conflict?"Conflict: assign an unused key.":key==0?"Assign a hold key to use this module.":detail,keyValue(2,key)));
    }
    private String keyValue(int id,int key) { return captureId==id?"Press a key...":ModuleKeys.label(key); }
    private static String label(String s) { return s.substring(0,1)+s.substring(1).toLowerCase(Locale.ROOT).replace('_',' '); }
    @Override protected int previewHeight() {
        return module==GameplayModule.AUTO_TEXT?58:module==GameplayModule.CROSSHAIR||module==GameplayModule.BLOCK_OVERLAY||module==GameplayModule.HIT_COLOR||module==GameplayModule.FOV_CONTROL?46:0;
    }
    @Override protected void preview(int x,int y,int w,int h) {
        if(module==GameplayModule.AUTO_TEXT) {
            UiShapes.rounded(x,y,w,h,4,0xFF242A30);
            String message=editingText?macroBuffer+"|":settings().autoTextMessage(macroSlot);
            drawLines(message.isEmpty()?"Slot "+(macroSlot+1)+": no message saved":message,x+10,y+7,w-20,3,TEXT);
        } else if(module==GameplayModule.CROSSHAIR) {
            UiShapes.rounded(x,y,w,h,4,0xFF242A30);
            SpaceUiFont.draw(client,"Live preview",x+12,y+14,MUTED);
            CrosshairStyle style=settings().crosshairStyle();float fit=Math.min(1,15/style.extent());
            GlStateManager.pushMatrix();
            try { GlStateManager.translate(x+w/2,y+h/2,0);GlStateManager.scale(fit,fit,1);CrosshairRenderer.draw(0,0,style); }
            finally { GlStateManager.popMatrix(); }
        } else VisualModPreview.draw(client,module,settings(),x,y,w);
    }
    @Override protected boolean modalKey(char c,int key) {
        if(editingText) {
            if(key==Keyboard.KEY_ESCAPE) { editingText=false;status="Text edit cancelled; saved message kept.";return true; }
            if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER) {
                if(settings().autoTextMessage(macroSlot,macroBuffer)) { editingText=false;status="Message saved."; }
                else status="Could not save. Use up to 100 plain characters.";
                return true;
            }
            if(key==Keyboard.KEY_BACK) { if(!macroBuffer.isEmpty())macroBuffer=macroBuffer.substring(0,macroBuffer.offsetByCodePoints(macroBuffer.length(),-1));status="";return true; }
            if(key==Keyboard.KEY_V&&hasControlDown()) {
                String pasted=getClipboard();String candidate=macroBuffer+(pasted==null?"":pasted);
                if(ModSettings.validText(candidate)) { macroBuffer=candidate;status=""; }
                else status="Paste rejected: use one plain line, up to 100 characters.";
                return true;
            }
            if(c>=32&&c!=127&&c!=167) {
                if(macroBuffer.length()<100) { macroBuffer+=c;status=""; } else status="Message limit reached: 100 characters.";
            }
            return true;
        }
        if(captureId==-1)return false;
        if(Keyboard.isRepeatEvent())return true;
        if(key==Keyboard.KEY_ESCAPE) { captureId=-1;status="Key assignment cancelled.";return true; }
        int code=key==Keyboard.KEY_BACK?0:key;
        boolean conflict=captureId==32?ModuleKeys.autoTextConflicts(client,macroSlot,code):captureId==12?ModuleKeys.toggleConflicts(client,module,code)
            :ModuleKeys.conflicts(client,code)||ModuleKeys.usedByHold(settings(),module,code)||ModuleKeys.usedByToggle(settings(),null,code)||ModuleKeys.usedByAutoText(settings(),-1,code);
        if(code!=0&&conflict) { status="Key unavailable. Choose another / Esc: cancel.";return true; }
        boolean saved=captureId==32?settings().autoTextKey(macroSlot,code):captureId==12?settings().toggleKey(module,code):settings().holdKey(module,code);
        if(saved)captureId=-1;
        status=saved?(code==0?"Key unbound.":"Key saved."):"Could not save key.";
        return true;
    }
    private void saved(boolean result) { status=result?"Saved automatically.":"Could not save settings; previous values kept."; }
    @Override protected void number(int id,int value) {
        ModSettings s=settings();CrosshairStyle c=s.crosshairStyle();boolean ok;
        switch(id) {
            case 3:ok=s.zoomFactor(value);break;
            case 7:ok=s.crosshairStyle(c.withSize(value));break;
            case 8:ok=s.crosshairStyle(c.withGap(value));break;
            case 9:ok=s.crosshairStyle(c.withThickness(value));break;
            case 21:ok=s.blockOpacity(value);break;
            case 22:ok=s.blockWidth(value);break;
            case 24:ok=s.hitOpacity(value);break;
            case 26:ok=s.cameraFov(value);break;
            case 27:ok=s.dynamicFov(value);break;
            case 36:ok=s.hitboxWidth(value);break;
            case 40:ok=s.physicsSpeed(value);break;
            case 42:ok=s.heldItemScale(value);break;
            case 43:ok=s.droppedItemScale(value);break;
            case 47:ok=s.soundsVolume(value);break;
            default:return;
        }
        saved(ok);
    }
    private static <E> E next(E[] values,int current,int direction) { return values[Math.floorMod(current+direction,values.length)]; }
    @Override protected void change(int id,int direction) {
        ModSettings s=settings();CrosshairStyle c=s.crosshairStyle();boolean ok;
        if(id==99) { client.setScreen(parent);return; }
        if(id==31) { macroSlot=Math.floorMod(macroSlot+direction,3);status="";return; }
        if(readOnly())return;
        if(id==2||id==12||id==32) { captureId=id;status="";return; }
        if(id==33) { editingText=true;macroBuffer=s.autoTextMessage(macroSlot);status="";return; }
        switch(id) {
            case 0:ok=module.toggle(s);break;
            case 1:ok=s.sprintMode(s.sprintMode()==ModSettings.SprintMode.AUTO?ModSettings.SprintMode.TOGGLE:ModSettings.SprintMode.AUTO);break;
            case 4:ok=s.zoomSmooth(!s.zoomSmooth());break;
            case 5:ok=s.crosshairStyle(c.withShape(next(CrosshairStyle.Shape.values(),c.shape.ordinal(),direction)));break;
            case 6:ok=s.crosshairStyle(c.withColor(next(CrosshairStyle.Color.values(),c.color.ordinal(),direction)));break;
            case 10:ok=s.crosshairStyle(c.withDot(!c.dot));break;
            case 11:ok=s.crosshairStyle(c.withOutline(!c.outline));break;
            case 13:ok=s.brightness(next(ModSettings.Brightness.values(),s.brightness().ordinal(),direction));break;
            case 14:ok=s.timePreset(next(ModSettings.TimePreset.values(),s.timePreset().ordinal(),direction));break;
            case 15:ok=s.snapPerspective(next(ModSettings.SnapPerspective.values(),s.snapPerspective().ordinal(),direction));break;
            case 16:ok=s.weatherMode(next(ModSettings.WeatherMode.values(),s.weatherMode().ordinal(),direction));break;
            case 17:ok=s.chatTimeFormat(next(ModSettings.ChatTimeFormat.values(),s.chatTimeFormat().ordinal(),direction));break;
            case 18:ok=s.durabilityThreshold(next(ModSettings.DurabilityThreshold.values(),s.durabilityThreshold().ordinal(),direction));break;
            case 19:ok=s.blockColor(next(VisualPalette.values(),s.blockColor().ordinal(),direction));break;
            case 20:ok=s.blockMode(next(ModSettings.BlockMode.values(),s.blockMode().ordinal(),direction));break;
            case 23:ok=s.damageColor(next(VisualPalette.values(),s.damageColor().ordinal(),direction));break;
            case 25:ok=s.hitPlayersOnly(!s.hitPlayersOnly());break;
            case 28:ok=s.animationBlockHit(!s.animationBlockHit());break;
            case 29:ok=s.animationEat(!s.animationEat());break;
            case 30:ok=s.animationBow(!s.animationBow());break;
            case 34:ok=s.guiScale(Math.floorMod(s.guiScale()+direction,5));break;
            case 35:ok=s.hitboxColor(next(VisualPalette.values(),s.hitboxColor().ordinal(),direction));break;
            case 37:ok=s.hitboxPlayers(!s.hitboxPlayers());break;
            case 38:ok=s.hitboxMobs(!s.hitboxMobs());break;
            case 39:ok=s.hitboxItems(!s.hitboxItems());break;
            case 41:ok=s.physicsFlat(!s.physicsFlat());break;
            case 44:ok=s.blockSounds(next(ModSettings.SoundTheme.values(),s.blockSounds().ordinal(),direction));break;
            case 45:ok=s.combatSounds(next(ModSettings.SoundTheme.values(),s.combatSounds().ordinal(),direction));break;
            case 46:ok=s.killChime(next(ModSettings.KillChime.values(),s.killChime().ordinal(),direction));break;
            case 48:SoundRuntime.preview(client);status="Local preview requested; Minecraft volume also applies.";return;
            case 98:ok=s.reset(module);if(ok) { status="Settings and keys reset; enabled state kept.";return; }break;
            default:return;
        }
        saved(ok);
    }
}
