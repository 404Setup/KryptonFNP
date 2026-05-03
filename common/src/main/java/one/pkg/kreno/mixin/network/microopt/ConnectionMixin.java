package one.pkg.kreno.mixin.network.microopt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import one.pkg.kreno.shared.ModConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.function.Consumer;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow
    private Channel channel;

    @Shadow
    private int sentPackets;

    @Shadow
    @Final
    private Queue<Consumer<Connection>> pendingActions;

    @Shadow
    protected abstract void doSendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush);

    /**
     * @author KryptonReno
     * @reason Avoid allocating a capturing lambda when sending packets from outside the event loop.
     * Netty has a highly optimized, pooled task for writes that handles off-thread submissions with zero allocations.
     */
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void kreno$optimizedSendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (ModConfig.Mixin.isConnectionMicroOpt()) {
            ++this.sentPackets;

            if (listener != null) {
                ChannelFuture future = flush ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
                future.addListener(listener);
            } else if (flush) {
                this.channel.writeAndFlush(packet, this.channel.voidPromise());
            } else {
                this.channel.write(packet, this.channel.voidPromise());
            }
            ci.cancel();
        }
    }

    /**
     * @author KryptonReno
     * @reason The pendingActions queue is already thread-safe (ConcurrentLinkedQueue).
     * The synchronized block adds an unnecessary monitor lock acquisition to every single packet sent,
     * which hurts scalability under heavy load.
     */
    @Inject(method = "flushQueue", at = @At("HEAD"), cancellable = true)
    private void kreno$optimizedFlushQueue(CallbackInfo ci) {
        if (ModConfig.Mixin.isConnectionMicroOpt()) {
            if (this.channel != null && this.channel.isOpen()) {
                Consumer<Connection> pendingAction;
                while ((pendingAction = this.pendingActions.poll()) != null) {
                    pendingAction.accept((Connection) (Object) this);
                }
            }
            ci.cancel();
        }
    }

    /**
     * @author KryptonReno
     * @reason Avoid allocating a lambda when flushing from outside the event loop.
     * Netty caches the flush task internally and handles off-thread flushes with zero allocations.
     */
    @Inject(method = "flush", at = @At("HEAD"), cancellable = true)
    private void kreno$optimizedFlush(CallbackInfo ci) {
        if (ModConfig.Mixin.isConnectionMicroOpt()) {
            this.channel.flush();
            ci.cancel();
        }
    }
}
