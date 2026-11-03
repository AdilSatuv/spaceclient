package dev.spaceclient.qa.mixin;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GameRenderer.class)
public interface RendererPorts {@Invoker("getFov") float qa$fov(float delta,boolean world);}
