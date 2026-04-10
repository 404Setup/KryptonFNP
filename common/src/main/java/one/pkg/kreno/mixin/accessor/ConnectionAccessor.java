package one.pkg.kreno.mixin.accessor;

import io.netty.channel.Channel;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

@Mixin(Connection.class)
public interface ConnectionAccessor {
    @Accessor("bandwidthDebugMonitor")
    BandwidthDebugMonitor getBandwidthDebugMonitor();

    @Accessor("encrypted")
    void setEncrypted(boolean encrypted);

    @Accessor("channel")
    Channel getChannel();

    @Accessor("address")
    void setAddress(SocketAddress address);
}
