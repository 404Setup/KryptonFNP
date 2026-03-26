package one.pkg.kreno.shared.network.he;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.ScheduledFuture;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.debugchart.LocalSampleLogger;
import one.pkg.kreno.mixin.accessor.ConnectionAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HEConnect {
    private static final Logger LOGGER = LoggerFactory.getLogger(HEConnect.class);
    private static final long CONNECTION_ATTEMPT_DELAY_MS = 250;

    public static Connection connectToServer(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, LocalSampleLogger bandwidthLogger) {
        String host = address.getHostString();

        if (address.getAddress() != null && host.equals(address.getAddress().getHostAddress())) {
            return null;
        }

        int port = address.getPort();

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return null;
        }

        List<InetAddress> ipv6Addresses = new ArrayList<>();
        List<InetAddress> ipv4Addresses = new ArrayList<>();

        for (InetAddress addr : addresses) {
            if (addr instanceof Inet6Address) {
                ipv6Addresses.add(addr);
            } else if (addr instanceof Inet4Address) {
                ipv4Addresses.add(addr);
            }
        }

        if (ipv6Addresses.isEmpty() || ipv4Addresses.isEmpty()) {
            return null;
        }

        InetAddress ipv6 = ipv6Addresses.getFirst();
        InetAddress ipv4 = ipv4Addresses.getFirst();

        LOGGER.debug("HE: Racing IPv6 {} and IPv4 {} for {}", ipv6.getHostAddress(), ipv4.getHostAddress(), host);


        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        if (bandwidthLogger != null) {
            connection.setBandwidthLogger(bandwidthLogger);
        }

        EventLoopGroup group = eventLoopGroupHolder.eventLoopGroup();
        Class<? extends Channel> channelClass = eventLoopGroupHolder.channelCls();

        Bootstrap baseBootstrap = new Bootstrap()
                .group(group)
                .channel(channelClass)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        // Base placeholder
                    }
                });

        AtomicBoolean winnerChosen = new AtomicBoolean(false);
        CompletableFuture<Channel> winnerFuture = new CompletableFuture<>();

        Bootstrap ipv6Bootstrap = baseBootstrap.clone();
        Bootstrap ipv4Bootstrap = baseBootstrap.clone();

        HEConnectHandler ipv6Handler = new HEConnectHandler(winnerChosen, winnerFuture);
        ipv6Bootstrap.handler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                try {
                    ch.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException _) {
                }

                ch.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                ch.pipeline().addLast("he_handler", ipv6Handler);
            }
        });

        HEConnectHandler ipv4Handler = new HEConnectHandler(winnerChosen, winnerFuture);
        ipv4Bootstrap.handler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                try {
                    ch.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException _) {
                }

                ch.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                ch.pipeline().addLast("he_handler", ipv4Handler);
            }
        });

        ChannelFuture ipv6Future = ipv6Bootstrap.connect(ipv6, port);
        EventLoop eventLoop = ipv6Future.channel().eventLoop();

        ScheduledFuture<?> timer = eventLoop.schedule(() -> {
            if (!winnerChosen.get()) {
                LOGGER.debug("HE: IPv6 connection delayed, launching IPv4 fallback for {}", ipv4.getHostAddress());
                ChannelFuture ipv4Future = ipv4Bootstrap.connect(ipv4, port);
                ipv6Handler.setOtherFuture(ipv4Future);
                ipv4Handler.setOtherFuture(ipv6Future);

                ipv4Future.addListener(future -> {
                    if (!future.isSuccess() && !winnerChosen.get()) {
                        if (ipv6Future.isDone() && !ipv6Future.isSuccess()) {
                            winnerFuture.completeExceptionally(future.cause());
                        }
                    }
                });
            }
        }, CONNECTION_ATTEMPT_DELAY_MS, TimeUnit.MILLISECONDS);

        ipv6Handler.setTimer(timer);

        ipv6Future.addListener(future -> {
            if (!future.isSuccess() && !winnerChosen.get()) {
                if (!timer.isDone() && timer.cancel(false)) {
                    LOGGER.debug("HE: IPv6 connection failed, launching IPv4 fallback for {}", ipv4.getHostAddress());
                    ChannelFuture ipv4Future = ipv4Bootstrap.connect(ipv4, port);
                    ipv6Handler.setOtherFuture(ipv4Future);
                    ipv4Handler.setOtherFuture(ipv6Future);

                    ipv4Future.addListener(f4 -> {
                        if (!f4.isSuccess() && !winnerChosen.get()) {
                            winnerFuture.completeExceptionally(f4.cause());
                        }
                    });
                }
            }
        });

        try {
            Channel winningChannel = winnerFuture.get();

            Connection.configureSerialization(
                    winningChannel.pipeline(),
                    PacketFlow.CLIENTBOUND,
                    false,
                    ((ConnectionAccessor) connection).getBandwidthDebugMonitor()
            );

            connection.configurePacketHandler(winningChannel.pipeline());
            return connection;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}