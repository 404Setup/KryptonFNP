package one.pkg.kfnp.shared.network.he;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class HEConnectHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HEConnectHandler.class);

    private final AtomicBoolean winnerChosen;
    private final CompletableFuture<Channel> winnerFuture;
    private ChannelFuture otherFuture;
    private ScheduledFuture<?> timer;

    public HEConnectHandler(AtomicBoolean winnerChosen, CompletableFuture<Channel> winnerFuture) {
        this.winnerChosen = winnerChosen;
        this.winnerFuture = winnerFuture;
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
            if (otherFuture != null && otherFuture.channel() != null) {
                otherFuture.channel().close();
            }

            ctx.pipeline().remove(this);
            winnerFuture.complete(ctx.channel());

            ctx.fireChannelActive();
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
        ctx.channel().close();
    }
}