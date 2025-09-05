package one.pkg.mod.krypton_fnp.mixin.network.pipeline.prepender;

import io.netty.channel.ChannelOutboundHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.NoOpFrameEncoder;
import one.pkg.mod.krypton_fnp.shared.network.pipeline.MinecraftVarintPrepender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Connection.class)
public class ConnectionMixin {
    /**
     * @author Andrew Steinborn
     * @reason replace Mojang prepender with a more efficient one
     */
    @Overwrite
    private static ChannelOutboundHandler createFrameEncoder(boolean local) {
        if (local) {
            return new NoOpFrameEncoder();
        } else {
            return MinecraftVarintPrepender.INSTANCE;
        }
    }
}
