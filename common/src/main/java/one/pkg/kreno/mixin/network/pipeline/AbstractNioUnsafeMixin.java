package one.pkg.kreno.mixin.network.pipeline;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe", remap = false)
public abstract class AbstractNioUnsafeMixin {
    @Shadow(aliases = "registration")
    private Object registration;

    @Inject(method = "removeReadOp", at = @At("HEAD"), cancellable = true)
    private void onRemoveReadOp(CallbackInfo ci) {
        if (registration == null) {
            ci.cancel();
        }
    }
}
