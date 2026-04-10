package one.pkg.kreno.mixin.network.quic.server;

import io.netty.channel.ChannelFuture;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnectionListener;
import one.pkg.kreno.shared.network.quic.QuicServerConnectionListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.util.List;

@Mixin(ServerConnectionListener.class)
public abstract class ServerConnectionListenerQuicMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private List<ChannelFuture> channels;

    @Shadow
    @Final
    private List<Connection> connections;

    @Inject(method = "startTcpServerListener", at = @At("TAIL"))
    private void onStartTcpServerListener(InetAddress address, int port, CallbackInfo ci) {
        QuicServerConnectionListener.startQuicServerListener(server, channels, connections, address, port);
    }
}
