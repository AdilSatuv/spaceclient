package dev.spaceclient.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

/** Stable block geometry, independent of pressed state and the Minecraft renderer. */
public final class KeystrokesLayout {
    public final int width, height;
    public final List<Cell> cells;
    private KeystrokesLayout(int width,int height,List<Cell> cells) {
        this.width=width;this.height=height;this.cells=Collections.unmodifiableList(cells);
    }
    public static KeystrokesLayout block(KeystrokesHud.Key[] keys,boolean jump,boolean mouse,ToIntFunction<String> measure) {
        if(keys.length!=4+(jump?1:0)+(mouse?2:0))throw new IllegalArgumentException("Key count does not match content");
        int size=18;
        for(int i=0;i<4;i++)size=Math.max(size,Math.min(40,measure.applyAsInt(keys[i].label)+6));
        int width=size*3+4;
        List<Cell> cells=new ArrayList<Cell>();
        cells.add(new Cell(0,size+2,0,size,18));
        for(int i=1;i<4;i++)cells.add(new Cell(i,(i-1)*(size+2),20,size,18));
        int y=40,index=4;
        if(jump){cells.add(new Cell(index++,0,y,width,18));y+=20;}
        if(mouse){int half=(width-2)/2;cells.add(new Cell(index++,0,y,half,18));cells.add(new Cell(index,half+2,y,width-half-2,18));y+=20;}
        return new KeystrokesLayout(width,y-2,cells);
    }
    public static final class Cell {
        public final int keyIndex,x,y,width,height;
        private Cell(int keyIndex,int x,int y,int width,int height) {
            this.keyIndex=keyIndex;this.x=x;this.y=y;this.width=width;this.height=height;
        }
    }
}
