package one.pkg.kreno.mixin.network;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import net.minecraft.server.network.EventLoopGroupHolder;
import one.pkg.kreno.shared.network.util.SystemInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EventLoopGroupHolder.class)
public abstract class EventLoopGroupHolderMixin {

    @Shadow
    @Final
    private static EventLoopGroupHolder NIO;

    @Shadow
    @Final
    private static EventLoopGroupHolder EPOLL;

    @Shadow
    @Final
    private static EventLoopGroupHolder KQUEUE;

    /**
     * @author 404
     * @reason Fix KQueue crash on non-OSX/BSD
     */
    @Inject(method = "remote(Z)Lnet/minecraft/server/network/EventLoopGroupHolder;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onRemote(boolean allowNativeTransport, CallbackInfoReturnable<EventLoopGroupHolder> cir) {
        if (!allowNativeTransport) {
            cir.setReturnValue(NIO);
            return;
        }

        if (SystemInfo.IS_MAC) {
            if (KQueue.isAvailable()) {
                cir.setReturnValue(KQUEUE);
                return;
            }
        } else if (SystemInfo.IS_LINUX) {
            if (Epoll.isAvailable()) {
                cir.setReturnValue(EPOLL);
                return;
            }
        }
        cir.setReturnValue(NIO);
    }
}
