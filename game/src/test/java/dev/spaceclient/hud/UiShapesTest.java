package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiShapesTest {
    @Test public void roundedRectangleFanClosesAtTheSameCornerForWideAndTallShapes() {
        for(int[] size:new int[][]{{600,330},{224,27},{24,12},{10,10},{20,100}}){
            float r=Math.min(5,Math.min(size[0],size[1])/2f);
            assertEquals(UiShapes.edgeX(0,size[0],r,r,0),UiShapes.edgeX(0,size[0],r,r,64),0);
            assertEquals(UiShapes.edgeY(0,size[1],r,r,0),UiShapes.edgeY(0,size[1],r,r,64),0);
            for(int i=0;i<=64;i++){
                double x=UiShapes.edgeX(0,size[0],r,r,i),y=UiShapes.edgeY(0,size[1],r,r,i);
                assertTrue(x>=-.001&&x<=size[0]+.001);assertTrue(y>=-.001&&y<=size[1]+.001);
            }
        }
    }
}
