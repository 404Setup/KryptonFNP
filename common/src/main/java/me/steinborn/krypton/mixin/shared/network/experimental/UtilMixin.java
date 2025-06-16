package me.steinborn.krypton.mixin.shared.network.experimental;

import com.mojang.jtracy.TracyClient;
import net.minecraft.TracingExecutor;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Util.class)
public abstract class UtilMixin {
    @Invoker("onThreadException")
    static void onThreadException(Thread thread, Throwable throwable) {
    }

    /**
     * @author 404
     * @reason test
     */
    @Overwrite
    private static TracingExecutor makeIoExecutor(String name, boolean daemon) {
        AtomicInteger atomicinteger = new AtomicInteger(1);
        return new TracingExecutor(Executors.newCachedThreadPool((r) -> {
            Thread thread = daemon ? Thread.ofVirtual().unstarted(r) : new Thread(r);
            String s = name + atomicinteger.getAndIncrement();
            TracyClient.setThreadName(s, name.hashCode());
            thread.setName(s);
            thread.setDaemon(daemon);
            thread.setUncaughtExceptionHandler(UtilMixin::onThreadException);
            return thread;
        }));
    }
}
