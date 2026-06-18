package one.pkg.kreno.shared.network.he;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Deprecated
public class HEConnectHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HEConnectHandler.class);

    private final AtomicBoolean winnerChosen;
    private final CompletableFuture<Channel> winnerFuture;
    private final Consumer<Channel> pipelineConfigurator;
    private ChannelFuture otherFuture;
    private ScheduledFuture<?> timer;

    public HEConnectHandler(AtomicBoolean winnerChosen, CompletableFuture<Channel> winnerFuture, Consumer<Channel> pipelineConfigurator) {
        this.winnerChosen = winnerChosen;
        this.winnerFuture = winnerFuture;
        this.pipelineConfigurator = pipelineConfigurator;
    }

    public void setOtherFuture(ChannelFuture otherFuture) {
        this.otherFuture = otherFuture;
    }

    public void setTimer(ScheduledFuture<?> timer) {
        this.timer = timer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (winnerChosen.compareAndSet(false, true)) {
            if (ctx.channel().remoteAddress() instanceof InetSocketAddress inetAddr) {
                LOGGER.debug("HE: Connection succeeded on {}:{}", inetAddr.getHostString(), inetAddr.getPort());
            } else {
                LOGGER.debug("HE: Connection succeeded on {}", ctx.channel().remoteAddress());
            }
            if (timer != null) timer.cancel(false);
            if (otherFuture != null) {
                otherFuture.cancel(false);
                if (otherFuture.isDone() && otherFuture.isSuccess()) {
                    otherFuture.channel().close();
                }
            }

            pipelineConfigurator.accept(ctx.channel());
            ctx.fireChannelActive();
            ctx.pipeline().remove(this);

            winnerFuture.complete(ctx.channel());
        } else {
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!winnerChosen.get()) {
            if (otherFuture != null && otherFuture.isDone() && !otherFuture.isSuccess()) {
                winnerFuture.completeExceptionally(new RuntimeException("Both IPv6 and IPv4 connections failed."));
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!winnerChosen.get()) {
            if (otherFuture != null && otherFuture.isDone() && !otherFuture.isSuccess()) {
                winnerFuture.completeExceptionally(cause);
            }
        }

        if (cause instanceof IOException &&
                (cause.getMessage() != null && (cause.getMessage().contains("Connection reset") || cause.getMessage().contains("Broken pipe")))) {
            LOGGER.trace("HE: Suppressed expected connection reset during race: {}", cause.getMessage());
        } else {
            LOGGER.debug("HE: Exception caught during connection race", cause);
        }

        ctx.close();
    }
}