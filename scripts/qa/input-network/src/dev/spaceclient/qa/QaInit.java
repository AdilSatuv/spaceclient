package dev.spaceclient.qa;
import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.opengl.Display;
public final class QaInit implements ClientModInitializer {
    private static java.awt.Frame frame;
    public void onInitializeClient() {
        frame=new java.awt.Frame();frame.setUndecorated(true);
        java.awt.Canvas canvas=new java.awt.Canvas();canvas.setPreferredSize(new java.awt.Dimension(1280,720));
        frame.add(canvas);frame.pack();frame.addNotify();frame.setVisible(false);
        try { Display.setParent(canvas); } catch(Exception e) { throw new IllegalStateException("Hidden Canvas unavailable",e); }
    }
}
