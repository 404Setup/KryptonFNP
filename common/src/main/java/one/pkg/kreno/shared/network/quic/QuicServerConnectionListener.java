package one.pkg.kreno.shared.network.quic;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicPathEvent;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.util.Util;
import one.pkg.kreno.mixin.accessor.ConnectionAccessor;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.network.netty.NettyUtil;
import one.pkg.kreno.shared.network.quic.token.KeyedConnectionIdGenerator;
import one.pkg.kreno.shared.network.quic.token.KeyedTokenHandler;
import one.pkg.libsl.loader.JavaLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public class QuicServerConnectionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuicServerConnectionListener.class);

    public static void startQuicServerListener(MinecraftServer server, List<ChannelFuture> channels,
                                               List<Connection> connections, @Nullable InetAddress address,
                                               int targetTcpPort) {
        if (ModConfig.Quic.isDisableQuic() || !JavaLoader.INSTANCE.loaded("kreno_addons_quic")) {
            return;
        }

        int quicPort = ModConfig.Quic.getQuicPort();
        boolean useFallback = false;
        if (quicPort <= 0 || quicPort > 65535) {
            quicPort = targetTcpPort;
            useFallback = true;
        }

        var useNativeTransport = server.usesAuthentication(); // approximate check

        Path config = JavaLoader.INSTANCE.getConfigPath().resolve("quic");
        Path keyFile = config.resolve("key.pem");
        Path certificateFile = config.resolve("certificate.pem");

        if (!Files.exists(keyFile) || !Files.exists(certificateFile)) {
            LOGGER.error("QUIC TLS key or certificate not found in {}; skipping QUIC bind", config);
            return;
        }

        try {
            var context = QuicSslContextBuilder.forServer(keyFile.toFile(), null, certificateFile.toFile())
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocols(QuicConnect.APPLICATION_NAME);

            var inheritAddresses = new WeakHashMap<Channel, SocketAddress>();

            var codec = new QuicServerCodecBuilder()
                    .sslContext(context.build())
                    .maxIdleTimeout(30, TimeUnit.SECONDS)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .initialMaxStreamDataBidirectionalRemote(1000000)
                    .initialMaxStreamsBidirectional(100)
                    .initialMaxStreamsUnidirectional(100)
                    .tokenHandler(new KeyedTokenHandler(Util.make(new byte[32], new SecureRandom()::nextBytes)))
                    .connectionIdAddressGenerator(
                            new KeyedConnectionIdGenerator(Util.make(new byte[32], new SecureRandom()::nextBytes)))
                    .handler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                            if (evt instanceof QuicPathEvent.PeerMigrated event) {
                                var newAddress = event.remote();
                                inheritAddresses.put(ctx.channel(), newAddress);

                                for (var connection : connections) {
                                    var currentAddress = connection.getRemoteAddress();
                                    var accessor = (ConnectionAccessor) connection;

                                    if (!newAddress.equals(currentAddress)
                                            && accessor.getChannel() instanceof QuicStreamChannel
                                            && accessor.getChannel().parent() == ctx.channel()) {
                                        accessor.setAddress(newAddress);
                                        LOGGER.info("{}[{}] was migrated to {}", connection,
                                                currentAddress, newAddress);
                                    }
                                }
                            }
                            ctx.fireUserEventTriggered(evt);
                        }

                        @Override
                        public void channelInactive(@NotNull ChannelHandlerContext ctx) {
                            inheritAddresses.remove(ctx.channel());
                            ctx.fireChannelInactive();
                        }

                        @Override
                        public boolean isSharable() {
                            return true;
                        }
                    })
                    .streamHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(@NotNull Channel channel) {
                            var pipeline = channel.pipeline();
                            var connection = new Connection(PacketFlow.SERVERBOUND);
                            ((ConnectionAccessor) connection).setEncrypted(true);
                            connections.add(connection);
                            Connection.configureSerialization(pipeline, PacketFlow.SERVERBOUND,
                                    false, ((ConnectionAccessor) connection).getBandwidthDebugMonitor());
                            connection.configurePacketHandler(pipeline);
                            connection.setListenerForServerboundHandshake(new ServerHandshakePacketListenerImpl(
                                    server, connection));

                            var addr = inheritAddresses.get(channel.parent());
                            if (addr != null) {
                                ((ConnectionAccessor) connection).setAddress(addr);
                            }
                        }
                    })
                    .build();

            Bootstrap bootstrap = new Bootstrap()
                    .group(EventLoopGroupHolder.remote(useNativeTransport).eventLoopGroup())
                    .channel(NettyUtil.getChannelClass(useNativeTransport))
                    .handler(codec);

            int attempts = 0;
            boolean bound = false;
            while (attempts <= 10 && !bound) {
                try {
                    ChannelFuture future = bootstrap.bind(new InetSocketAddress(address,
                            quicPort + attempts)).syncUninterruptibly();
                    channels.add(future);
                    LOGGER.info("Successfully bound QUIC on UDP port {}", quicPort + attempts);
                    bound = true;
                } catch (Exception e) {
                    if (useFallback && attempts < 10) {
                        LOGGER.warn("Failed to bind QUIC on port {}. Trying next port...", quicPort + attempts);
                        attempts++;
                    } else {
                        LOGGER.error("Failed to bind QUIC on port {} after {} attempts. Crashing server.", quicPort,
                                attempts);
                        throw new RuntimeException("QUIC bind failed", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("QUIC setup failed", e);
            throw new RuntimeException(e);
        }
    }
}
