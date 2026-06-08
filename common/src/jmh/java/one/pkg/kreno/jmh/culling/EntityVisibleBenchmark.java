package one.pkg.kreno.jmh.culling;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class EntityVisibleBenchmark {

    private long now;

    @Setup(Level.Iteration)
    public void setup() {
        now = System.currentTimeMillis();
    }

    @Benchmark
    public long baselineCurrentTimeMillis() {
        long sum = 0;
        for (int i = 0; i < 100; i++) {
            sum += System.currentTimeMillis();
        }
        return sum;
    }

    @Benchmark
    public long optimizedCurrentTimeMillis() {
        long sum = 0;
        for (int i = 0; i < 100; i++) {
            sum += now;
        }
        return sum;
    }
}
