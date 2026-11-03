package dev.spaceclient.mixin;

import dev.spaceclient.menu.MenuSlider;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OptionSliderWidget.class)
public abstract class MenuOptionSliderMixin implements MenuSlider {
    @Shadow private float value;
    @Override public float spaceclient$progress() { return value; }
}
