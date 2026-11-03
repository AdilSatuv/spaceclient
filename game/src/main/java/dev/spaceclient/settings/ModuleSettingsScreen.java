package dev.spaceclient.settings;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.menu.MenuStrings;
import dev.spaceclient.menu.UiTranslations;
import dev.spaceclient.menu.MenuTheme;
import dev.spaceclient.menu.MenuBackdrop;
import dev.spaceclient.hud.HudSwitch;
import dev.spaceclient.hud.HudViewport;
import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Shared settings shell. Input and drawing use the same layout and complete visible rows. */
public abstract class ModuleSettingsScreen extends Screen {
    protected static final int TEXT=0xF2EEE7, MUTED=0xB3B0AA, ACCENT=0xD7AF6A;
    protected final Screen parent;
    protected String status="";
    private HudViewport viewport;
    protected SettingsLayout layout;
    private int focus, offset;
    private int dragging=-1, dragValue;
    private int pointerX, pointerY;
    private final Map<Integer,HudSwitch.Motion> motions=new HashMap<>();

    protected ModuleSettingsScreen(Screen parent) { this.parent=parent; }
    protected abstract String title();
    protected abstract String subtitle();
    protected abstract List<SettingRow> rows();
    protected abstract boolean readOnly();
    protected abstract String notice();
    protected abstract void change(int id,int direction);
    protected abstract void number(int id,int value);
    protected String[] sections() { return new String[0]; }
    protected int section() { return 0; }
    protected void section(int index) { }
    protected int previewHeight() { return 0; }
    protected void preview(int x,int y,int w,int h) { }
    protected boolean modalKey(char character,int key) { return false; }
    protected boolean modal() { return false; }
    protected boolean navigation(int id) { return false; }
    protected boolean repeatedInput() { return Keyboard.isRepeatEvent(); }
    protected boolean shiftInput() { return hasShiftDown(); }
    protected final String ui(String key) { return MenuStrings.get(client,key); }
    protected SettingRow focusedRow() {
        int index=focus-sections().length;
        List<SettingRow> items=rows();
        return index>=0&&index<items.size()?items.get(index):null;
    }
    protected void focusRow(int id) {
        List<SettingRow> items=rows();
        for(int i=0;i<items.size();i++)if(items.get(i).id==id) { focus=sections().length+i;break; }
        reveal();
    }
    protected SettingRow[] footer() { return new SettingRow[]{SettingRow.action(98,ui("settings.reset"),"",""),SettingRow.action(99,ui("back"),"","")}; }

    @Override public void init() {
        viewport=new HudViewport(width,height);
        layout=new SettingsLayout(viewport.width,viewport.height,66+(sections().length>0?30:0)+previewHeight());
        dragging=-1;
        focus=Math.min(focus,total()-1);
        reveal();
        SpaceClient.clicks.clear();
    }
    @Override public boolean shouldPauseGame() { return true; }
    @Override public void removed() { dragging=-1; SpaceUiFont.release(); SpaceClient.clicks.clear(); }
    private boolean roomy() { return width>=320&&height>=240; }
    private int total() { return sections().length+rows().size()+footer().length; }
    private void reveal() {
        focus=Math.max(0,Math.min(focus,total()-1));
        int index=focus-sections().length;
        offset=layout.offset(offset,rows().size());
        if(index>=0&&index<rows().size())offset=layout.reveal(offset,index,rows().size());
    }
    private boolean enabled(SettingRow row) { return row.enabled&&(!readOnly()||navigation(row.id)); }

    @Override public void render(int mx,int my,float delta) {
        MenuBackdrop.render(client,width,height);
        if(!roomy()) { SpaceUiFont.draw(client,ui("settings.resize"),12,20,TEXT); return; }
        mx=viewport.toUi(mx); my=viewport.toUi(my);
        pointerX=mx; pointerY=my;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(viewport.scale,viewport.scale,1);
            int x=layout.left,y=layout.top,w=layout.width,h=layout.height;
            MenuTheme.panel(x,y,w,h);
            SpaceUiFont.draw(client,ui("settings.heading"),x+16,y+12,ACCENT);
            SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(title(),w-170),x+16,y+31,TEXT);
            String label=ui(readOnly()?"settings.readonly":"settings.autosave");
            SpaceUiFont.draw(client,label,x+w-16-SpaceUiFont.width(label),y+12,readOnly()?ACCENT:MUTED);
            SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(UiTranslations.text(client,subtitle()),w-32),x+16,y+49,MUTED);
            String[] tabs=sections();
            for(int i=0;i<tabs.length;i++) {
                int tw=(w-32)/tabs.length,tx=x+16+i*tw;
                button(tx,y+68,tw-5,22,tabs[i],focus==i,i==section(),true);
            }
            int py=y+64+(tabs.length>0?30:0);
            if(previewHeight()>0)preview(x+16,py,w-32,previewHeight()-6);
            List<SettingRow> items=rows();
            offset=layout.offset(offset,items.size());
            for(int i=offset;i<Math.min(items.size(),offset+layout.slots);i++)
                drawRow(items.get(i),layout.rowY(i,offset),focus==tabs.length+i,mx,my);
            if(items.size()>layout.slots) {
                int sy=layout.bodyTop,sh=layout.slots*SettingsLayout.ROW_HEIGHT-4;
                UiShapes.rounded(x+w-9,sy,2,sh,1,0xFF3A3B3E);
                int thumb=Math.max(10,sh*layout.slots/items.size());
                UiShapes.rounded(x+w-9,sy+(sh-thumb)*offset/(items.size()-layout.slots),2,thumb,1,0xFFD7AF6A);
            }
            String message=notice();
            if(message.isEmpty())message=status;
            if(message.isEmpty()) {
                int index=focus-tabs.length;
                if(index>=0&&index<items.size()) {
                    SettingRow row=items.get(index);
                    message=rowHelp(row);
                } else {
                    int action=index-items.size();
                    SettingRow[] actions=footer();
                    message=action>=0&&action<actions.length&&!enabled(actions[action])?rowHelp(actions[action])
                        :action>=0&&action<actions.length&&!actions[action].detail.isEmpty()?actions[action].detail:ui("settings.help.navigation");
                }
            }
            drawLines(UiTranslations.text(client,message),x+16,y+h-73,w-32,2,ACCENT);
            SettingRow[] actions=footer();
            for(int i=0;i<actions.length;i++) {
                SettingRow a=actions[i]; int bx=x+16+i*136;
                if(i==actions.length-1)bx=x+w-120;
                button(bx,y+h-33,i==actions.length-1?104:128,23,a.title,focus==tabs.length+items.size()+i,false,enabled(a));
            }
        } finally { GlStateManager.popMatrix(); }
    }
    protected final String rowHelp(SettingRow row) {
        if(!enabled(row))return ui(readOnly()?"settings.help.readonly":"settings.help.unavailable")+" "+UiTranslations.text(client,row.detail);
        String key=row.kind==SettingRow.Kind.NUMBER?"number":row.kind==SettingRow.Kind.TOGGLE?"toggle"
            :row.kind==SettingRow.Kind.CHOICE?"choice":"action";
        return UiTranslations.text(client,row.detail)+" / "+ui("settings.help."+key);
    }
    private void drawRow(SettingRow a,int y,boolean selected,int mx,int my) {
        int x=layout.left+14,w=layout.width-28,cx=layout.controlX(),cw=190;
        boolean active=enabled(a),hover=mx>=cx&&mx<cx+cw&&my>=y&&my<y+40;
        MenuTheme.card(x,y,w,40,selected,false,false);
        int labelWidth=cx-x-20;
        SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(UiTranslations.text(client,a.title),labelWidth),x+10,y+6,active?TEXT:MUTED);
        SpaceUiFont.draw(client,shortText(UiTranslations.text(client,a.detail),labelWidth),x+10,y+23,MUTED);
        MenuTheme.control(cx,y+4,cw,32,false,hover,active,false,false);
        int color=active?ACCENT:MUTED;
        if(!active) {
            center(ui(readOnly()?"settings.locked":"settings.unavailable"),cx+cw/2,y+14,MUTED);
        } else if(a.kind==SettingRow.Kind.TOGGLE) {
            HudSwitch.Motion motion=motions.get(a.id);
            if(motion==null) { motion=new HudSwitch.Motion(a.checked); motions.put(a.id,motion); }
            SpaceUiFont.draw(client,ui(a.checked?"on":"off"),cx+12,y+14,color);
            HudSwitch.draw(cx+cw-40,y+12,motion.sample(a.checked,System.nanoTime()));
        } else if(a.kind==SettingRow.Kind.NUMBER) {
            int v=dragging==a.id?dragValue:a.number;
            SpaceUiFont.draw(client,"-",cx+10,y+14,active&&v>a.range.min?TEXT:MUTED);
            SpaceUiFont.draw(client,"+",cx+cw-16,y+14,active&&v<a.range.max?TEXT:MUTED);
            center(a.format.apply(v),cx+cw/2,y+8,color);
            int tx=layout.trackX(),tw=layout.trackWidth();
            MenuTheme.slider(tx,y+28,tw,(float)a.range.fraction(v),active);
        } else {
            if(a.kind==SettingRow.Kind.CHOICE) {
                SpaceUiFont.draw(client,"<",cx+9,y+14,color);
                SpaceUiFont.draw(client,">",cx+cw-15,y+14,color);
            }
            center(SpaceUiFont.trimToWidth(UiTranslations.text(client,a.value),cw-42),cx+cw/2,y+14,color);
        }
    }
    protected void drawLines(String text,int x,int y,int width,int lines,int color) {
        String remaining=text;
        for(int i=0;i<lines&&!remaining.isEmpty();i++) {
            String part=SpaceUiFont.trimToWidth(remaining,width);
            if(part.length()<remaining.length()&&i<lines-1) {
                int split=part.lastIndexOf(' '); if(split>0)part=part.substring(0,split);
            }
            SpaceUiFont.draw(client,part,x,y+i*14,color);
            if(part.isEmpty())break;
            remaining=remaining.substring(part.length()).trim();
        }
    }
    private String shortText(String text,int width) {
        return SpaceUiFont.width(text)<=width?text:SpaceUiFont.trimToWidth(text,width-SpaceUiFont.width("..."))+"...";
    }
    protected void center(String text,int x,int y,int color) { SpaceUiFont.draw(client,text,x-SpaceUiFont.width(text)/2f,y,color); }
    private void button(int x,int y,int w,int h,String text,boolean selected,boolean current,boolean enabled) {
        MenuTheme.control(x,y,w,h,selected,pointerX>=x&&pointerX<x+w&&pointerY>=y&&pointerY<y+h,enabled,false,current);
        center(SpaceUiFont.trimToWidth(text,w-12),x+w/2,y+5,enabled?(current?ACCENT:TEXT):MUTED);
    }
    @Override protected void keyPressed(char c,int key) {
        if(dragging!=-1) { if(key==Keyboard.KEY_ESCAPE)dragging=-1; return; }
        if(modalKey(c,key))return;
        if(key==Keyboard.KEY_ESCAPE) { if(!repeatedInput())client.setScreen(parent); return; }
        if(!roomy())return;
        if(key==Keyboard.KEY_TAB||key==Keyboard.KEY_DOWN||key==Keyboard.KEY_UP) {
            status="";
            focus=Math.floorMod(focus+(key==Keyboard.KEY_UP||(key==Keyboard.KEY_TAB&&shiftInput())?-1:1),total());
            reveal(); return;
        }
        if(key==Keyboard.KEY_PRIOR||key==Keyboard.KEY_NEXT) {
            status="";
            focus=Math.max(0,Math.min(total()-1,focus+(key==Keyboard.KEY_PRIOR?-layout.slots:layout.slots)));
            reveal(); return;
        }
        if(focus<sections().length&&(key==Keyboard.KEY_LEFT||key==Keyboard.KEY_RIGHT)) {
            focus=Math.floorMod(focus+(key==Keyboard.KEY_LEFT?-1:1),sections().length);
            activateFocus(1); reveal(); return;
        }
        int index=focus-sections().length;
        List<SettingRow> items=rows();
        if(index>=0&&index<items.size()) {
            SettingRow row=items.get(index);
            if(row.kind==SettingRow.Kind.NUMBER&&enabled(row)) {
                if(key==Keyboard.KEY_HOME||key==Keyboard.KEY_END) { setNumber(row,key==Keyboard.KEY_HOME?row.range.min:row.range.max); return; }
                if(key==Keyboard.KEY_LEFT||key==Keyboard.KEY_RIGHT) {
                    setNumber(row,row.range.adjust(row.number,key==Keyboard.KEY_LEFT?-1:1,shiftInput())); return;
                }
            }
        }
        boolean choiceArrow=index>=0&&index<items.size()&&items.get(index).kind==SettingRow.Kind.CHOICE
            &&(key==Keyboard.KEY_LEFT||key==Keyboard.KEY_RIGHT);
        if(!repeatedInput()&&(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER||key==Keyboard.KEY_SPACE||choiceArrow))
            activateFocus(key==Keyboard.KEY_LEFT?-1:1);
    }
    private void setNumber(SettingRow row,int value) { if(value!=row.number)number(row.id,value); }
    private void activateFocus(int direction) {
        int tabs=sections().length,index=focus-tabs;
        if(focus<tabs) { section(focus); offset=0; dragging=-1; return; }
        List<SettingRow> items=rows();
        SettingRow row=index<items.size()?items.get(index):footer()[index-items.size()];
        if(!enabled(row)) { status=rowHelp(row); return; }
        if(row.kind==SettingRow.Kind.NUMBER)setNumber(row,row.range.adjust(row.number,direction,shiftInput()));
        else change(row.id,direction);
        reveal();
    }
    @Override protected void mouseClicked(int x,int y,int button) {
        if(button!=0||!roomy()||modal())return;
        status="";
        x=viewport.toUi(x); y=viewport.toUi(y);
        int tabs=sections().length;
        if(tabs>0&&y>=layout.top+68&&y<layout.top+90&&x>=layout.left+16&&x<layout.left+layout.width-16) {
            focus=Math.min(tabs-1,(x-layout.left-16)/((layout.width-32)/tabs));
            activateFocus(1); return;
        }
        List<SettingRow> items=rows();
        for(int i=offset;i<Math.min(items.size(),offset+layout.slots);i++) {
            SettingRow row=items.get(i); int ry=layout.rowY(i,offset),cx=layout.controlX();
            if(y>=ry&&y<ry+40&&x>=layout.left+14&&x<layout.left+layout.width-14) {
                focus=tabs+i;
                if(!enabled(row)) { status=rowHelp(row); return; }
                if(x<cx)return;
                if(row.kind==SettingRow.Kind.NUMBER&&x>=layout.trackX()&&x<=layout.trackX()+layout.trackWidth()) {
                    dragging=row.id; dragValue=row.range.at((x-layout.trackX())/(double)layout.trackWidth()); return;
                }
                activateFocus(x<cx+26?-1:1); return;
            }
        }
        if(y>=layout.top+layout.height-33&&y<layout.top+layout.height-10) {
            SettingRow[] actions=footer();
            for(int i=0;i<actions.length;i++) {
                int bx=i==actions.length-1?layout.left+layout.width-120:layout.left+16+i*136;
                if(x>=bx&&x<bx+(i==actions.length-1?104:128)) { focus=tabs+items.size()+i; activateFocus(1); return; }
            }
        }
    }
    @Override protected void mouseDragged(int x,int y,int button,long elapsed) {
        if(dragging==-1||button!=0)return;
        for(SettingRow row:rows())if(row.id==dragging)dragValue=row.range.at((viewport.toUi(x)-layout.trackX())/(double)layout.trackWidth());
    }
    @Override protected void mouseReleased(int x,int y,int button) {
        if(dragging==-1||button!=0)return;
        mouseDragged(x,y,button,0);
        int id=dragging; dragging=-1;
        for(SettingRow row:rows())if(row.id==id&&enabled(row))setNumber(row,dragValue);
    }
    @Override public void handleMouse() {
        super.handleMouse();
        int wheel=Mouse.getEventDWheel();
        if(wheel!=0&&roomy()&&!modal()&&dragging==-1) {
            status="";
            offset=layout.offset(offset+(wheel>0?-1:1),rows().size());
            int index=focus-sections().length;
            if(index>=0&&index<rows().size())focus=sections().length+Math.max(offset,Math.min(offset+layout.slots-1,index));
        }
    }
}
