package dev.spaceclient.mods;
import dev.spaceclient.SpaceClient;
public final class GuiScaleRuntime {
 private GuiScaleRuntime() { }
 public static int requested(int vanilla){return SpaceClient.mods!=null&&SpaceClient.mods.gui()?SpaceClient.mods.guiScale():vanilla;}
}
