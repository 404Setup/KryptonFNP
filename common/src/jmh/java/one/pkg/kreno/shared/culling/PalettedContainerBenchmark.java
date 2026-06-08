package one.pkg.kreno.shared.culling;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class PalettedContainerBenchmark {

    @Benchmark
    public void baseline(Blackhole bh) {
        // Mocked O(4096) loop simulation representing the inner loops in BlockCullingUtil
        int sum = 0;
        for (int i=0; i<4096; i++) {
            sum += i;
        }
        bh.consume(sum);
    }

    @Benchmark
    public void optimized(Blackhole bh) {
        // Fast path simulation representing container.maybeHas shortcut returning false
        int sum = 0;
        sum += 1;
        bh.consume(sum);
    }
}
