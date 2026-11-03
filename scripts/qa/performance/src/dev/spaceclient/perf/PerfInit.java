package dev.spaceclient.perf;
import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.opengl.Display;
public final class PerfInit implements ClientModInitializer {
    private static java.awt.Frame frame;
    private static java.awt.Canvas canvas;
    public static void resize(int w,int h) { canvas.setPreferredSize(new java.awt.Dimension(w,h));frame.pack(); }
    public void onInitializeClient() {
        frame=new java.awt.Frame();frame.setUndecorated(true);
        canvas=new java.awt.Canvas();canvas.setPreferredSize(new java.awt.Dimension(1280,720));
        frame.add(canvas);frame.pack();frame.addNotify();frame.setVisible(false);
        try { Display.setParent(canvas); } catch(Exception e) { throw new IllegalStateException("Hidden Canvas unavailable",e); }
    }
}
