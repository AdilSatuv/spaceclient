package dev.spaceclient.hud;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.settings.*;
import java.util.*;
import org.lwjgl.input.Keyboard;

/** Additional inspector keeps group/numeric controls usable on the compact canvas. */
public final class HudArrangeScreen extends ModuleSettingsScreen {
    private final HudEditorScreen editor;
    public HudArrangeScreen(HudEditorScreen editor){super(editor);this.editor=editor;}
    protected String title(){return ui("editor.tools");}
    protected String subtitle(){return editor.primary().getTitle()+" / "+ui("editor.selection")+" "+editor.selectedModules().size();}
    protected boolean readOnly(){return SpaceClient.hud.isReadOnly();}
    protected String notice(){return !SpaceClient.hud.getError().isEmpty()?SpaceClient.hud.getError():status;}
    protected boolean navigation(int id){return id==99;}
    static int coordinateLimit(int viewport,double extent){return (int)Math.round(Math.max(0,viewport-extent));}
    static SettingRow coordinateRow(int id,String title,String detail,int value,int limit,String fullReason){
        SettingRow row=SettingRow.number(id,title,detail,value,0,Math.max(1,limit),1,Integer::toString);
        return limit==0?row.disabled(fullReason):row;
    }
    protected List<SettingRow> rows(){
        List<SettingRow> a=new ArrayList<>();HudPlacement.Bounds b=editor.primaryBounds();
        for(int i=0;i<3;i++){
            String title=ui("editor."+(i==0?"x":i==1?"y":"scale")),detail=ui("editor.numeric.hint");
            SettingRow r=i<2?coordinateRow(i,title,detail,(int)Math.round(i==0?b.x:b.y),coordinateLimit(i==0?editor.width:editor.height,i==0?b.width:b.height),ui("editor.axisfull"))
                :SettingRow.number(i,title,detail,(int)Math.round(SpaceClient.hud.getConfig().active().placement(editor.primary()).scale*100),50,300,5,Integer::toString);
            a.add(editor.primaryLocked()?r.disabled(ui("editor.locked")):r);
        }
        a.add(SettingRow.action(3,ui("editor.selectall"),ui("editor.selection.hint"),"Ctrl+A"));
        a.add(SettingRow.action(4,ui("editor.hide"),ui("editor.visibility.hint"),""));
        a.add(SettingRow.action(5,ui("editor.show"),ui("editor.visibility.hint"),""));
        SettingRow lock=SettingRow.toggle(6,ui("editor.lock"),ui("editor.lock.hint"),editor.primaryLocked());
        a.add(SpaceClient.browser.isReadOnly()?lock.disabled(ui("settings.help.readonly")):lock);
        SettingRow brand=SettingRow.toggle(7,ui("editor.brand"),ui("editor.brand.hint"),SpaceClient.browser.brand());
        a.add(SpaceClient.browser.isReadOnly()?brand.disabled(ui("settings.help.readonly")):brand);
        for(HudLayoutOperations.Align alignment:HudLayoutOperations.Align.values()) {
            SettingRow r=SettingRow.action(10+alignment.ordinal(),ui("editor.align."+alignment.name().toLowerCase(Locale.ROOT)),ui("editor.align.hint"),"");
            a.add(editor.selectedModules().size()<2?r.disabled(ui("editor.need2")):r);
        }
        for(int i=0;i<2;i++){
            SettingRow r=SettingRow.action(16+i,ui("editor.distribute."+(i==0?"x":"y")),ui("editor.distribute.hint"),"");
            a.add(editor.selectedModules().size()<3?r.disabled(ui("editor.need3")):r);
        }return a;
    }
    protected SettingRow[] footer(){return new SettingRow[]{undoRow(90,false),undoRow(91,true),SettingRow.action(99,ui("back"),"","")};}
    private SettingRow undoRow(int id,boolean redo){SettingRow r=SettingRow.action(id,ui(redo?"editor.redo":"editor.undo"),ui("editor.history.hint"),"");return editor.canUndo(redo)?r:r.disabled(ui("editor.history.empty"));}
    protected void number(int id,int value){status=editor.numeric(id,value)?ui("editor.saved"):SpaceClient.hud.getError();}
    protected void change(int id,int direction){
        boolean ok=true;
        if(id==99){client.setScreen(editor);return;}
        if(id==3)editor.selectAll();
        else if(id==4||id==5)ok=editor.visibility(id==5);
        else if(id==6)ok=editor.toggleLock();
        else if(id==7)ok=SpaceClient.browser.brand(!SpaceClient.browser.brand());
        else if(id>=10&&id<=15)ok=editor.arrange(HudLayoutOperations.Align.values()[id-10],null);
        else if(id==16||id==17)ok=editor.arrange(null,id==16);
        else if(id==90||id==91)ok=editor.undo(id==91);
        status=ok?ui("editor.saved"):SpaceClient.hud.getError();
    }
    protected boolean modalKey(char c,int key){if(hasControlDown()&&(key==Keyboard.KEY_Z||key==Keyboard.KEY_Y)){if(!repeatedInput())editor.undo(key==Keyboard.KEY_Y);return true;}return false;}
}
