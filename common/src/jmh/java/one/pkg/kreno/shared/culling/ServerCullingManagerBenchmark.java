package one.pkg.kreno.shared.culling;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ServerCullingManagerBenchmark {

    private Map<Integer, Object>[] activeVisibilityMaps;
    private AtomicInteger counter = new AtomicInteger(100000);
    private int entityId;
    private Integer boxedId;

    @SuppressWarnings("unchecked")
    @Setup(Level.Iteration)
    public void setup() {
        int numPlayers = 100;
        int numEntities = 1000;

        activeVisibilityMaps = new Map[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            Map<Integer, Object> playerMap = new ConcurrentHashMap<>();
            activeVisibilityMaps[i] = playerMap;
        }
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        entityId = counter.incrementAndGet();
        boxedId = entityId;

        for (int i = 0; i < activeVisibilityMaps.length; i++) {
            if (i % 10 == 0) {
                activeVisibilityMaps[i].put(boxedId, new Object());
            } else {
                activeVisibilityMaps[i].remove(boxedId);
            }
        }
    }

    @Benchmark
    public void testRemoveCurrent() {
        for (Map<Integer, Object> map : activeVisibilityMaps) {
            map.remove(entityId);
        }
    }

    @Benchmark
    public void testRemoveOptimized() {
        Integer boxed = entityId;
        Map<Integer, Object>[] maps = activeVisibilityMaps;
        for (int i = 0; i < maps.length; i++) {
            Map<Integer, Object> map = maps[i];
            if (map.remove(boxed) != null) {
                // do nothing
            }
        }
    }
}
