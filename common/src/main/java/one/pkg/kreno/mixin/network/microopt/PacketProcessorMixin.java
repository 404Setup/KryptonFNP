package one.pkg.kreno.mixin.network.microopt;

import net.minecraft.network.PacketProcessor;
import one.pkg.kreno.shared.ModConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Queue;

/**
 * Optimizes {@link PacketProcessor#processQueuedPackets()}.
 * <p>
 * Vanilla drains the pending packet queue with {@code while (!queue.isEmpty()) queue.poll().handle();},
 * performing two traversals of the ConcurrentLinkedQueue head per packet. Rewriting the loop to the
 * canonical {@code while ((e = queue.poll()) != null)} pattern halves the queue operations on the
 * main-thread packet processing hot path.
 * <p>
 * Implemented as a pair of redirects with a carrier field instead of a full method replacement, so it
 * stays independent of the queue's element type (which differs between vanilla and loader-patched
 * runtimes) and remains toggleable at runtime.
 */
@Mixin(PacketProcessor.class)
public class PacketProcessorMixin {
    /**
     * Carries the element polled in {@link #kreno$pollInsteadOfIsEmpty} over to {@link #kreno$reusePolled}.
     * PacketProcessor only ever drains the queue from its owning thread, so no synchronization is needed.
     */
    @Unique
    @Nullable
    private Object kreno$polled;

    @Redirect(method = "processQueuedPackets", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
    private boolean kreno$pollInsteadOfIsEmpty(Queue<Object> queue) {
        if (ModConfig.Mixin.isPacketProcessorOpt()) {
            Object polled = queue.poll();
            this.kreno$polled = polled;
            return polled == null;
        }
        return queue.isEmpty();
    }

    @Redirect(method = "processQueuedPackets", at = @At(value = "INVOKE", target = "Ljava/util/Queue;poll()Ljava/lang/Object;"))
    private Object kreno$reusePolled(Queue<Object> queue) {
        Object polled = this.kreno$polled;
        if (polled != null) {
            this.kreno$polled = null;
            return polled;
        }
        return queue.poll();
    }
}
