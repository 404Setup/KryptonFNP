package one.pkg.kreno.shared.network.quic;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicClientCodecBuilder;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.codec.quic.QuicStreamType;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.debugchart.LocalSampleLogger;
import one.pkg.kreno.mixin.accessor.ConnectionAccessor;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class QuicConnect {

    public static final String APPLICATION_NAME = "minecraft";

    public static Connection connectToServer(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, LocalSampleLogger bandwidthLogger) {
        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        if (bandwidthLogger != null) {
            connection.setBandwidthLogger(bandwidthLogger);
        }

        try {
            var context = QuicSslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocols(APPLICATION_NAME);

            var codec = new QuicClientCodecBuilder()
                    .sslContext(context.build())
                    .maxIdleTimeout(30, TimeUnit.SECONDS)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .build();

            EventLoopGroup group = eventLoopGroupHolder.eventLoopGroup();

            var channel = new Bootstrap()
                    .group(group)
                    .channel(eventLoopGroupHolder.channelCls())
                    .handler(codec)
                    .bind(0)
                    .sync()
                    .channel();

            QuicChannel.newBootstrap(channel)
                    .streamHandler(new ChannelInboundHandlerAdapter())
                    .remoteAddress(address)
                    .connect()
                    .get()
                    .createStream(QuicStreamType.BIDIRECTIONAL, new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ((ConnectionAccessor) connection).setEncrypted(true);
                            var pipeline = ch.pipeline();
                            Connection.configureSerialization(
                                    pipeline,
                                    PacketFlow.CLIENTBOUND,
                                    false,
                                    ((ConnectionAccessor) connection).getBandwidthDebugMonitor()
                            );
                            connection.configurePacketHandler(pipeline);
                        }
                    })
                    .get();

            return connection;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to establish QUIC connection", e);
        }
    }
}
