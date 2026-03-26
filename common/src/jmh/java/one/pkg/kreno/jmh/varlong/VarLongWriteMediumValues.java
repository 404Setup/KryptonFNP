package one.pkg.kreno.jmh.varlong;

import one.pkg.kreno.shared.network.util.VarLongUtil;
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
public class VarLongWriteMediumValues extends VarLongBase {
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
        for (long value : mediumValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void VLatest(Blackhole bh) {
        buffer.clear();
        for (long value : mediumValues) {
            VarLongUtil.writeVarLongFull(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }
}
