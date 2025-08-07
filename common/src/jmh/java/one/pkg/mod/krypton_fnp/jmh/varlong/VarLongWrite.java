package one.pkg.mod.krypton_fnp.jmh.varlong;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
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
public class VarLongWrite extends VarLongBase {
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
        buffer.clear();
        for (long value : testValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void V0213(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            write0213(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void V0214(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            write0214(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }
}
