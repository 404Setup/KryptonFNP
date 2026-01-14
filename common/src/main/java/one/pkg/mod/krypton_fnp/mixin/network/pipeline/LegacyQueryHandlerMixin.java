package one.pkg.mod.krypton_fnp.mixin.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.network.LegacyQueryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into {@link LegacyQueryHandler} to fix a security issue.
 */
@Mixin(LegacyQueryHandler.class)
public abstract class LegacyQueryHandlerMixin {
    @Inject(method = "channelRead", at = @At(value = "HEAD"), cancellable = true)
    public void channelRead(ChannelHandlerContext context, Object message, CallbackInfo ci) throws Exception {
        if (!context.channel().isActive()) {
            ((ByteBuf) message).clear();
            ci.cancel();
        }
    }
}
