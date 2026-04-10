package one.pkg.kreno.mixin.network.quic.client;

import net.minecraft.network.Connection;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.debugchart.LocalSampleLogger;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.network.quic.QuicConnect;
import one.pkg.kreno.shared.network.quic.QuicSocketAddress;
import one.pkg.kreno.shared.network.quic.ServerAddressProperties;
import one.pkg.libsl.loader.JavaLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;

@Mixin(Connection.class)
public class ConnectionQuicMixin {

    @Inject(method = "connectToServer", at = @At("HEAD"), cancellable = true)
    private static void connectToServerQuic(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder,
                                            LocalSampleLogger bandwidthLogger, CallbackInfoReturnable<Connection> cir) {
        if (!ModConfig.Quic.isDisableQuic() && JavaLoader.INSTANCE.loaded("kreno_addons_quic")) {
            if (address instanceof QuicSocketAddress qsa) {
                if (((ServerAddressProperties) (Object) qsa.getOrigin()).getUseQuic()) {
                    Connection connection = QuicConnect.connectToServer(address, eventLoopGroupHolder, bandwidthLogger);
                    if (connection != null) {
                        cir.setReturnValue(connection);
                    }
                }
            }
        }
    }
}
