package dev.spaceclient.mixin;

import dev.spaceclient.mods.BlockOverlayRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public abstract class BlockOverlayMixin {
    @Redirect(method="drawBlockOutline(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;IF)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/util/math/Box;)V"))
    private void spaceclient$blockStyle(Box box){BlockOverlayRenderer.draw(box);}
}
