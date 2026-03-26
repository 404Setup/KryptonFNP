package one.pkg.kreno.jmh.varint;

import one.pkg.kreno.shared.network.util.VarIntUtil;
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
public class VarIntGetByteSize extends VarIntBase {
    private static int getByteSizeMinecraft(int data) {
        for (int i = 1; i < 5; ++i) {
            if ((data & -1 << i * 7) == 0) {
                return i;
            }
        }
        return 5;
    }

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
        for (int value : testValues) {
            bh.consume(getByteSizeMinecraft(value));
        }
    }

    @Benchmark
    public void V0209(Blackhole bh) {
        for (int value : testValues) {
            bh.consume(VarIntUtil.getVarIntLength(value));
        }
    }
}
