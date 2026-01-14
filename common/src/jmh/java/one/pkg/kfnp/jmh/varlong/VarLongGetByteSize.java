package one.pkg.kfnp.jmh.varlong;

import one.pkg.kfnp.shared.network.util.VarLongUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Threads(1)
@Fork(2)
@State(Scope.Thread)
@SuppressWarnings("unused")
public class VarLongGetByteSize extends VarLongBase {
    @Setup
    public void setup() {
        super.setup();
    }

    @TearDown
    public void tearDown() {
        super.tearDown();
    }

    @Benchmark
    public void Minecraft(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSizeMinecraft(value));
        }
    }

    @Benchmark
    public void V0210(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSize0210(value));
        }
    }

    @Benchmark
    public void V0212(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSize0212(value));
        }
    }

    @Benchmark
    public void V0214(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(VarLongUtil.getVarLongLength(value));
        }
    }
}
