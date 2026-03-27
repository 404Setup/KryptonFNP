package one.pkg.kreno.mixin.network.pipeline.he;

import net.minecraft.network.Connection;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.debugchart.LocalSampleLogger;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.network.he.HEConnect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;

// TODO: I didn't implement Ping compatibility
@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "connectToServer", at = @At("HEAD"), cancellable = true)
    private static void happyEyeballsConnectToServer(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, LocalSampleLogger bandwidthLogger, CallbackInfoReturnable<Connection> cir) {
        if (ModConfig.Netty.isHappyEyeballs() && !address.isUnresolved() && address.getHostString() != null) {
            Connection connection = HEConnect.connectToServer(address, eventLoopGroupHolder, bandwidthLogger);
            if (connection != null) {
                cir.setReturnValue(connection);
            }
        }
    }
}