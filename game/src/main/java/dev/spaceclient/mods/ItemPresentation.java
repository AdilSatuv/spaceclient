package dev.spaceclient.mods;
import dev.spaceclient.SpaceClient;
public final class ItemPresentation {
 private ItemPresentation() { }
 public static float heldScale(){return SpaceClient.mods!=null&&SpaceClient.mods.itemSize()?SpaceClient.mods.heldItemScale()/100f:1;}
 public static float droppedScale(){return SpaceClient.mods!=null&&SpaceClient.mods.itemSize()?SpaceClient.mods.droppedItemScale()/100f:1;}
 public static float spin(float age,int speed,int id){return age*speed*5+Math.floorMod(id*31,360);}
}
