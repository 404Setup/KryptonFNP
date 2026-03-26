package one.pkg.kfnp.mixin.accessor;

import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Connection.class)
public interface ConnectionAccessor {
    @Accessor("bandwidthDebugMonitor")
    BandwidthDebugMonitor getBandwidthDebugMonitor();
}
