package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import dev.spaceclient.settings.ModuleSettingsScreen;
import dev.spaceclient.settings.SettingRow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Local marker list and editor; browsing remains available when the store is read-only. */
public final class WaypointsScreen extends ModuleSettingsScreen {
    private int selected,page,tab;
    private WaypointEdit edit;
    private boolean deleteArmed;
    public WaypointsScreen(Screen parent) { super(parent); }
    private WaypointStore store() { return SpaceClient.waypoints; }
    private List<WaypointStore.Point> points() { return client==null?Collections.<WaypointStore.Point>emptyList():WaypointRuntime.current(client); }
    private WaypointStore.Point point() {
        List<WaypointStore.Point> list=points();selected=Math.max(0,Math.min(selected,list.size()-1));
        return list.isEmpty()?null:list.get(selected);
    }
    private boolean hasWorld() { return client!=null&&client.player!=null&&client.world!=null&&!WaypointRuntime.context(client).isEmpty(); }
    @Override protected String title() { return "Waypoints"; }
    @Override protected String subtitle() { return "Local to this world / server and dimension  /  "+points().size()+" of 64 markers"; }
    @Override protected String[] sections() { return new String[]{"Markers","Edit selected marker"}; }
    @Override protected int section() { return tab; }
    @Override protected void section(int value) { tab=value;deleteArmed=false;status=""; }
    @Override protected boolean navigation(int id) { return id==10||id==8||id==9||id>=20; }
    @Override protected boolean readOnly() { return store()==null||store().readOnly(); }
    @Override protected boolean modal() { return edit!=null; }
    @Override protected String notice() {
        if(edit!=null) return edit.error().isEmpty()?"Enter: save / Esc: cancel / Backspace: erase / Ctrl+V: paste":edit.error();
        if(store()!=null&&!store().error().isEmpty())return store().error();
        if(!status.isEmpty())return status;
        return hasWorld()?"":"Join a world to view and add its local markers.";
    }
    @Override protected SettingRow[] footer() {
        SettingRow add=SettingRow.action(6,"Add here","Create a marker at your current position, then edit its name.","");
        if(!hasWorld())add=add.disabled("Join a world before adding a marker.");
        else if(points().size()>=64||store().all().size()>=512)add=add.disabled("Marker limit reached: 64 here, 512 in total.");
        return new SettingRow[]{add,SettingRow.action(10,"Back","Return to Waypoints settings.","")};
    }
    @Override protected List<SettingRow> rows() {
        List<SettingRow> rows=new ArrayList<>();List<WaypointStore.Point> list=points();
        page=Math.max(0,Math.min(page,Math.max(0,(list.size()-1)/6)));
        if(tab==0) {
            for(int i=0;i<6&&page*6+i<list.size();i++) {
                int index=page*6+i;WaypointStore.Point p=list.get(index);
                rows.add(SettingRow.action(20+i,p.name,"X "+p.x+"  /  Y "+p.y+"  /  Z "+p.z+"  /  "+(p.visible?"Visible":"Hidden"),index==selected?"Selected / Edit":"Select / Edit"));
            }
            if(list.isEmpty())rows.add(SettingRow.action(20,"No markers yet","Add a marker at your position to start.","Empty").disabled("Use Add here while in a world."));
            String value=(page+1)+" / "+Math.max(1,(list.size()+5)/6);
            SettingRow prev=SettingRow.action(8,"Previous page","Browse six local markers at a time.",value);
            SettingRow next=SettingRow.action(9,"Next page","Browse six local markers at a time.",value);
            rows.add(page>0?prev:prev.disabled("You are on the first page."));
            rows.add((page+1)*6<list.size()?next:next.disabled("You are on the last page."));
        } else {
            WaypointStore.Point p=point();
            if(p==null) {
                rows.add(SettingRow.action(0,"No marker selected","Select a marker on the Markers tab.","Unavailable").disabled("Add or select a marker first."));return rows;
            }
            rows.add(SettingRow.text(0,"Name","1-32 plain characters; Enter edits the full name.",p.name));
            rows.add(SettingRow.text(1,"X coordinate","Enter: type; Left / Right: 1 block; Shift: 10.",Integer.toString(p.x)));
            rows.add(SettingRow.text(2,"Y coordinate","Height 0-255; Enter: type; arrows: adjust.",Integer.toString(p.y)));
            rows.add(SettingRow.text(3,"Z coordinate","Enter: type; Left / Right: 1 block; Shift: 10.",Integer.toString(p.z)));
            rows.add(SettingRow.choice(4,"Marker color","Use a fixed color or animated Rainbow.",label(p.color.name())));
            rows.add(SettingRow.toggle(5,"Visible","Hide this marker without deleting its saved position.",p.visible));
            rows.add(SettingRow.action(7,"Delete marker",deleteArmed?"Press again to permanently remove this local marker.":"Requires a second press; other markers are kept.",deleteArmed?"Confirm delete":"Delete"));
        }
        return rows;
    }
    @Override protected int previewHeight() { return 46; }
    @Override protected void preview(int x,int y,int w,int h) {
        UiShapes.rounded(x,y,w,h,4,0xFF242A30);WaypointStore.Point p=point();
        if(edit!=null) { SpaceUiFont.draw(client,"Editing / "+edit.buffer(),x+10,y+13,TEXT);return; }
        if(p==null) { SpaceUiFont.draw(client,"No marker selected in this dimension",x+10,y+13,MUTED);return; }
        SpaceUiFont.draw(client,p.name,x+10,y+5,TEXT);
        SpaceUiFont.draw(client,"X "+p.x+"  /  Y "+p.y+"  /  Z "+p.z+"   "+(p.visible?"Visible":"Hidden"),x+10,y+22,MUTED);
    }
    @Override protected void number(int id,int value) { }
    private void saved(boolean ok) { status=ok?"Marker saved automatically.":store().error(); }
    @Override protected void change(int id,int direction) {
        if(id==10) { client.setScreen(parent);return; }
        if(id==8||id==9) { page=Math.max(0,Math.min(Math.max(0,(points().size()-1)/6),page+(id==8?-1:1)));deleteArmed=false;return; }
        if(id>=20) { selected=page*6+id-20;tab=1;deleteArmed=false;focusRow(0);return; }
        if(readOnly())return;
        WaypointStore.Point p=point();if(id!=7)deleteArmed=false;
        if(id>=0&&id<=3&&p!=null) { edit=new WaypointEdit(p,id);status="";return; }
        if(id==4&&p!=null)saved(store().replace(p,p.color(VisualPalette.values()[Math.floorMod(p.color.ordinal()+direction,VisualPalette.values().length)])));
        else if(id==5&&p!=null)saved(store().replace(p,p.visible(!p.visible)));
        else if(id==6&&hasWorld()) {
            WaypointStore.Point n=new WaypointStore.Point(WaypointRuntime.context(client),"Marker "+(points().size()+1),client.player.dimension,
                (int)Math.floor(client.player.x),Math.max(0,Math.min(255,(int)Math.floor(client.player.y))),(int)Math.floor(client.player.z),VisualPalette.GOLD,true);
            if(store().add(n)) { selected=points().size()-1;page=selected/6;tab=1;edit=new WaypointEdit(n,0);focusRow(0);status=""; } else status=store().error();
        } else if(id==7&&p!=null) {
            if(!deleteArmed) { deleteArmed=true;status="Press Delete again to remove "+p.name+". Esc cancels.";return; }
            if(store().replace(p,null)) { deleteArmed=false;point();page=selected/6;tab=0;focusRow(20);status="Marker deleted."; } else status=store().error();
        }
    }
    @Override protected boolean modalKey(char c,int key) {
        if(edit!=null) {
            if(key==Keyboard.KEY_ESCAPE) { edit=null;status="Edit cancelled; saved marker kept.";return true; }
            if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER) { if(edit.save(store())) { edit=null;status="Marker saved."; }return true; }
            if(key==Keyboard.KEY_BACK) { edit.backspace();return true; }
            if(key==Keyboard.KEY_V&&hasControlDown()) { edit.append(getClipboard());return true; }
            if(c>=32&&c!=127)edit.append(String.valueOf(c));return true;
        }
        if(deleteArmed&&key==Keyboard.KEY_ESCAPE) { deleteArmed=false;status="Deletion cancelled.";return true; }
        if(deleteArmed&&key!=Keyboard.KEY_RETURN&&key!=Keyboard.KEY_NUMPADENTER&&key!=Keyboard.KEY_SPACE)deleteArmed=false;
        SettingRow row=focusedRow();WaypointStore.Point p=point();
        if(!readOnly()&&tab==1&&p!=null&&row!=null&&row.id>=1&&row.id<=3&&(key==Keyboard.KEY_LEFT||key==Keyboard.KEY_RIGHT)) {
            int amount=(key==Keyboard.KEY_LEFT?-1:1)*(shiftInput()?10:1);
            WaypointStore.Point n=WaypointEdit.nudge(p,row.id,amount);
            if(n.x!=p.x||n.y!=p.y||n.z!=p.z)saved(store().replace(p,n));
            return true;
        }
        return false;
    }
    private static String label(String value) { return value.substring(0,1)+value.substring(1).toLowerCase(Locale.ROOT); }
}
