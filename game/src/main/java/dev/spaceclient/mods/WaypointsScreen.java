package dev.spaceclient.mods;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.*;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Local marker editing; no teleports or chat commands. */
public final class WaypointsScreen extends Screen {
    private final Screen parent;private HudViewport viewport;private int left,top,selected,page,focus;
    private int editing=-1;private String buffer="",notice="";
    public WaypointsScreen(Screen parent){this.parent=parent;}
    @Override public void init(){viewport=new HudViewport(width,height);left=(viewport.width-500)/2;top=(viewport.height-354)/2;}
    @Override public boolean shouldPauseGame(){return true;}
    @Override public void removed(){SpaceUiFont.release();}
    private List<WaypointStore.Point> points(){return WaypointRuntime.current(client);}
    private WaypointStore.Point point(){List<WaypointStore.Point> p=points();selected=Math.max(0,Math.min(selected,p.size()-1));return p.isEmpty()?null:p.get(selected);}
    private void button(int id,String text,int x,int y,int w,int mx,int my){boolean hover=mx>=x&&mx<x+w&&my>=y&&my<y+27;UiShapes.rounded(x,y,w,27,4,focus==id?0xFFD7AF6A:hover?0xFF3A3B3E:0xFF24262C);SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(text,w-14),x+7,y+9,focus==id?0xFF101114:0xFFF2EEE7);}
    @Override public void render(int mx,int my,float delta){
        fillGradient(0,0,width,height,0x99070709,0xB00A0B0E);if(width<320||height<240)return;mx=viewport.toUi(mx);my=viewport.toUi(my);
        GlStateManager.pushMatrix();try{GlStateManager.scale(viewport.scale,viewport.scale,1);UiShapes.rounded(left,top,500,354,6,0xFF101114);
            SpaceUiFont.draw(client,"SPACE / WAYPOINTS",left+18,top+17,0xFFD7AF6A);SpaceUiFont.draw(client,"Local to this world/server and dimension",left+18,top+39,0xFFB3B0AA);
            List<WaypointStore.Point> ps=points();int pages=Math.max(1,(ps.size()+5)/6);page=Math.min(page,pages-1);
            for(int i=0;i<6;i++){int index=page*6+i;if(index>=ps.size())break;WaypointStore.Point p=ps.get(index);int y=top+70+i*32;button(20+i,(selected==index?"> ":"")+p.name,left+18,y,180,mx,my);}
            WaypointStore.Point p=point();
            if(p!=null){String[] values={"Name: "+p.name,"X: "+p.x,"Y: "+p.y,"Z: "+p.z,"Color: "+p.color.name(),"Visible: "+(p.visible?"On":"Off")};
                for(int i=0;i<6;i++)button(i,editing==i?buffer+"|":values[i],left+215,top+70+i*32,265,mx,my);
            }else SpaceUiFont.draw(client,"Add a marker at your position",left+215,top+82,0xFFB3B0AA);
            button(6,"Add here",left+18,top+276,105,mx,my);button(7,"Delete selected",left+133,top+276,125,mx,my);button(8,"<",left+270,top+276,32,mx,my);button(9,">",left+310,top+276,32,mx,my);button(10,"Back",left+375,top+276,105,mx,my);
            String msg=SpaceClient.waypoints.error();if(msg.isEmpty())msg=editing>=0?"Enter: save / Esc: cancel":notice.isEmpty()?"Page "+(page+1)+" / "+pages+"   Tab: select / Enter: change":notice;
            SpaceUiFont.draw(client,SpaceUiFont.trimToWidth(msg,464),left+18,top+320,0xFFB3B0AA);
        }finally{GlStateManager.popMatrix();}
    }
    private void activate(int id){
        if(id==10){client.setScreen(parent);return;}if(SpaceClient.waypoints.readOnly())return;WaypointStore.Point p=point();
        if(id>=20){selected=page*6+id-20;return;}
        if(id<4&&p!=null){editing=id;buffer=id==0?p.name:Integer.toString(id==1?p.x:id==2?p.y:p.z);return;}
        if(id==4&&p!=null)SpaceClient.waypoints.replace(p,p.color(VisualPalette.values()[(p.color.ordinal()+1)%VisualPalette.values().length]));
        if(id==5&&p!=null)SpaceClient.waypoints.replace(p,p.visible(!p.visible));
        if(id==6&&client.player!=null){String context=WaypointRuntime.context(client);if(context.isEmpty()){notice="Join a world before adding a marker";return;}WaypointStore.Point n=new WaypointStore.Point(context,"Marker "+(points().size()+1),client.player.dimension,(int)Math.floor(client.player.x),Math.max(0,Math.min(255,(int)Math.floor(client.player.y))),(int)Math.floor(client.player.z),VisualPalette.GOLD,true);if(SpaceClient.waypoints.add(n)){selected=points().size()-1;page=selected/6;editing=0;buffer=n.name;}}
        if(id==7&&p!=null){SpaceClient.waypoints.replace(p,null);notice="Marker deleted";}
        if(id==8)page=Math.max(0,page-1);if(id==9)page=Math.min((points().size()-1)/6,page+1);
    }
    private void save(){WaypointStore.Point p=point();if(p==null){editing=-1;return;}try{int x=p.x,y=p.y,z=p.z;if(editing>0){int value=Integer.parseInt(buffer);if(editing==1)x=value;else if(editing==2)y=value;else z=value;}WaypointStore.Point n=new WaypointStore.Point(p.context,editing==0?buffer:p.name,p.dimension,x,y,z,p.color,p.visible);if(SpaceClient.waypoints.replace(p,n)){editing=-1;notice="Saved";}}catch(IllegalArgumentException e){notice="Name 1-32 chars; X/Z within world, Y 0-255";}}
    @Override protected void keyPressed(char c,int key){
        if(editing>=0){if(key==Keyboard.KEY_ESCAPE){editing=-1;return;}if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER){save();return;}if(key==Keyboard.KEY_BACK){if(!buffer.isEmpty())buffer=buffer.substring(0,buffer.offsetByCodePoints(buffer.length(),-1));return;}if(!Character.isISOControl(c)&&buffer.length()<(editing==0?32:10)){String next=buffer+c;if(ModSettings.validText(next))buffer=next;}return;}
        if(key==Keyboard.KEY_ESCAPE){client.setScreen(parent);return;}if(key==Keyboard.KEY_TAB){focus=Math.floorMod(focus+(hasShiftDown()?-1:1),11);return;}if(key==Keyboard.KEY_UP||key==Keyboard.KEY_DOWN){selected=Math.max(0,Math.min(points().size()-1,selected+(key==Keyboard.KEY_UP?-1:1)));page=Math.max(0,selected/6);return;}if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_SPACE)activate(focus);
    }
    @Override protected void mouseClicked(int mx,int my,int button){if(button!=0||width<320||height<240)return;if(editing>=0){save();if(editing>=0)return;}mx=viewport.toUi(mx)-left;my=viewport.toUi(my)-top;for(int i=0;i<6;i++){if(my>=70+i*32&&my<97+i*32){if(mx>=18&&mx<198&&page*6+i<points().size()){activate(20+i);return;}if(mx>=215&&mx<480){focus=i;activate(i);return;}}}int[][] buttons={{6,18,105},{7,133,125},{8,270,32},{9,310,32},{10,375,105}};if(my>=276&&my<303)for(int[] b:buttons)if(mx>=b[1]&&mx<b[1]+b[2]){focus=b[0];activate(b[0]);return;}}
}
