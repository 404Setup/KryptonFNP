package one.pkg.mod.krypton_fnp.mixin.network.experimental;

import net.minecraft.server.network.ServerTextFilter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerTextFilter.class)
public class ServerTextFilterMixin {
    @Shadow
    @Final
    private static AtomicInteger WORKER_COUNT;
    @Final
    @Shadow
    private static ThreadFactory THREAD_FACTORY = r -> Thread.ofVirtual().name("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement()).unstarted(r);;
}
