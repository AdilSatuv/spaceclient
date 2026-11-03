package dev.spaceclient.qa.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccess {
    @Invoker("mouseClicked") void qa$click(int x,int y,int button);
    @Invoker("mouseDragged") void qa$drag(int x,int y,int button,long elapsed);
    @Invoker("mouseReleased") void qa$release(int x,int y,int button);
    @Invoker("keyPressed") void qa$key(char c,int key);
}
