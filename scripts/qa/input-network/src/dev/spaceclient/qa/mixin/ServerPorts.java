package dev.spaceclient.qa.mixin;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(IntegratedServer.class)
public interface ServerPorts {@Accessor("published") void qa$published(boolean value);}
