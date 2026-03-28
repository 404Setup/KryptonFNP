package one.pkg.kreno.mixin.network.pipeline;

import io.netty.util.concurrent.AbstractEventExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AbstractEventExecutor.class, remap = false)
public abstract class AbstractEventExecutorMixin {
    @Redirect(method = "safeExecute", at = @At(value = "INVOKE", target = "Lio/netty/util/concurrent/AbstractEventExecutor;runTask(Ljava/lang/Runnable;)V"))
    private static void onRunTask(Runnable task) {
        try {
            task.run();
        } catch (NullPointerException e) {
            StackTraceElement[] stack = e.getStackTrace();
            if (stack.length > 0 && "removeReadOp".equals(stack[0].getMethodName()) && stack[0].getClassName().contains("AbstractNioChannel")) {
                // Suppress Netty NullPointerException in clearReadPending0
                return;
            }
            throw e;
        }
    }
}
