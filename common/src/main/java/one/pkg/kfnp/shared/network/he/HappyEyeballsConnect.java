package one.pkg.kfnp.shared.network.he;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.debugchart.LocalSampleLogger;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HappyEyeballsConnect {
    private static final Logger LOGGER = LoggerFactory.getLogger(HappyEyeballsConnect.class);
    private static final long CONNECTION_ATTEMPT_DELAY_MS = 250;

    public static Connection connectToServer(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, LocalSampleLogger bandwidthLogger) {
        String host = address.getHostString();
        int port = address.getPort();

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return null; // Let the original method handle the error
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
            return null; // Fallback to Minecraft's standard connection logic if not dual-stack
        }

        InetAddress ipv6 = ipv6Addresses.get(0);
        InetAddress ipv4 = ipv4Addresses.get(0);

        LOGGER.debug("HappyEyeballs: Racing IPv6 {} and IPv4 {} for {}", ipv6, ipv4, host);

        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        if (bandwidthLogger != null) {
            connection.setBandwidthLogger(bandwidthLogger);
        }

        EventLoopGroup group = eventLoopGroupHolder.eventLoopGroup();
        Class<? extends Channel> channelClass = eventLoopGroupHolder.channelCls();

        Bootstrap baseBootstrap = new Bootstrap()
                .group(group)
                .channel(channelClass)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        // Base placeholder
                    }
                });

        AtomicBoolean winnerChosen = new AtomicBoolean(false);
        CompletableFuture<Channel> winnerFuture = new CompletableFuture<>();

        Bootstrap ipv6Bootstrap = baseBootstrap.clone();
        Bootstrap ipv4Bootstrap = baseBootstrap.clone();

        HEConnectHandler ipv6Handler = new HEConnectHandler(winnerChosen, winnerFuture, connection);
        ipv6Bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().addLast("he_handler", ipv6Handler);
            }
        });

        HEConnectHandler ipv4Handler = new HEConnectHandler(winnerChosen, winnerFuture, connection);
        ipv4Bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().addLast("he_handler", ipv4Handler);
            }
        });

        ChannelFuture ipv6Future = ipv6Bootstrap.connect(ipv6, port);
        EventLoop eventLoop = ipv6Future.channel().eventLoop();

        ScheduledFuture<?> timer = eventLoop.schedule(() -> {
            if (!winnerChosen.get()) {
                LOGGER.debug("HappyEyeballs: IPv6 connection delayed, launching IPv4 fallback for {}", ipv4);
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
                    LOGGER.debug("HappyEyeballs: IPv6 connection failed, launching IPv4 fallback for {}", ipv4);
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
            Connection.configureSerialization(winningChannel.pipeline(), PacketFlow.CLIENTBOUND, false, null);
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