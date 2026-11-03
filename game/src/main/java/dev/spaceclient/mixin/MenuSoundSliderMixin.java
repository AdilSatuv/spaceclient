package dev.spaceclient.mixin;

import dev.spaceclient.menu.MenuSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.screen.SoundsScreen$SoundButtonWidget")
public abstract class MenuSoundSliderMixin implements MenuSlider {
    @Shadow public float volume;
    @Override public float spaceclient$progress() { return volume; }
}
