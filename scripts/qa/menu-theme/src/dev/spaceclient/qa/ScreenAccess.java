package dev.spaceclient.qa.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import net.minecraft.client.gui.widget.ButtonWidget;

@Mixin(Screen.class)
public interface ScreenAccess {
    @Accessor("buttons") List<ButtonWidget> qa$buttons();
    @Invoker("mouseClicked") void qa$click(int x,int y,int button);
    @Invoker("mouseDragged") void qa$drag(int x,int y,int button,long elapsed);
    @Invoker("mouseReleased") void qa$release(int x,int y,int button);
    @Invoker("keyPressed") void qa$key(char c,int key);
}
