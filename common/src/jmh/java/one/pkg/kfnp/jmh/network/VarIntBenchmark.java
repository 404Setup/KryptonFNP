package one.pkg.kfnp.jmh.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import one.pkg.kfnp.shared.network.util.VarIntUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class VarIntBenchmark {

    private ByteBuf smallBuffer;
    private ByteBuf mediumBuffer;
    private ByteBuf largeBuffer;
    private ByteBuf mixedBuffer;

    @Setup
    public void setup() {
        smallBuffer = Unpooled.buffer();
        mediumBuffer = Unpooled.buffer();
        largeBuffer = Unpooled.buffer();
        mixedBuffer = Unpooled.buffer();

        Random random = new Random(12345);

        // Small VarInts (1 byte)
        for (int i = 0; i < 1000; i++) {
            VarIntUtil.writeVarInt(smallBuffer, random.nextInt(128));
        }

        // Medium VarInts (2-3 bytes)
        for (int i = 0; i < 1000; i++) {
            // 2 bytes: 128 to 16383
            // 3 bytes: 16384 to 2097151
            int val = 128 + random.nextInt(2097151 - 128);
            VarIntUtil.writeVarInt(mediumBuffer, val);
        }

        // Large VarInts (4-5 bytes)
        for (int i = 0; i < 1000; i++) {
            // 4 bytes: 2097152 to 268435455
            // 5 bytes: 268435456 to MAX_VALUE
            int val = 2097152 + random.nextInt(Integer.MAX_VALUE - 2097152);
            VarIntUtil.writeVarInt(largeBuffer, val);
        }

        // Mixed VarInts
        for (int i = 0; i < 3000; i++) {
             int val = random.nextInt(Integer.MAX_VALUE);
             VarIntUtil.writeVarInt(mixedBuffer, val);
        }
    }

    @TearDown
    public void tearDown() {
        smallBuffer.release();
        mediumBuffer.release();
        largeBuffer.release();
        mixedBuffer.release();
    }

    @Benchmark
    public void testSmall(Blackhole bh) {
        smallBuffer.readerIndex(0);
        while (smallBuffer.isReadable()) {
            bh.consume(VarIntUtil.readVarInt(smallBuffer));
        }
    }

    @Benchmark
    public void testMedium(Blackhole bh) {
        mediumBuffer.readerIndex(0);
        while (mediumBuffer.isReadable()) {
            bh.consume(VarIntUtil.readVarInt(mediumBuffer));
        }
    }

    @Benchmark
    public void testLarge(Blackhole bh) {
        largeBuffer.readerIndex(0);
        while (largeBuffer.isReadable()) {
            bh.consume(VarIntUtil.readVarInt(largeBuffer));
        }
    }

    @Benchmark
    public void testMixed(Blackhole bh) {
        mixedBuffer.readerIndex(0);
        while (mixedBuffer.isReadable()) {
            bh.consume(VarIntUtil.readVarInt(mixedBuffer));
        }
    }
}
