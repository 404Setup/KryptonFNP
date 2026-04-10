package one.pkg.kreno.mixin.network.quic.client;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import net.minecraft.network.Connection;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.SocketAddress;

@Mixin(Connection.class)
public class ConnectionQuicLifecycleMixin {

    @Shadow(remap = false)
    private Channel channel;

    @Shadow(remap = false)
    private SocketAddress address;

    @Redirect(method = "channelActive(Lio/netty/channel/ChannelHandlerContext;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;address:Ljava/net/SocketAddress;", opcode = Opcodes.PUTFIELD), remap = false)
    private void channelActiveSetAddress(Connection instance, SocketAddress value) {
        if (channel instanceof QuicStreamChannel quicStreamChannel) {
            address = quicStreamChannel.parent().remoteAddress();
        } else {
            address = value;
        }
    }

    @Redirect(
            method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/network/Connection;channel:Lio/netty/channel/Channel;",
                    ordinal = 1,
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Channel disconnectGetChannel(Connection instance) {
        if (channel instanceof QuicStreamChannel) {
            return channel.parent();
        } else {
            return channel;
        }
    }
}
