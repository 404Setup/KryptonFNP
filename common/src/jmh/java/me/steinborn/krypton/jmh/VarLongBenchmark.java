package me.steinborn.krypton.jmh;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.steinborn.krypton.mod.shared.network.util.VarLongUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Threads(1)
@Fork(2)
@State(Scope.Thread)
@SuppressWarnings("unused")
public class VarLongBenchmark {
    private static final long MASK_21_BITS = -1L << 21;
    private static final long MASK_28_BITS = -1L << 28;

    private static final long[] SIZE_MASKS = {
            0L,
            -1L << 7,
            -1L << 14,
            -1L << 21,
            -1L << 28,
            -1L << 35,
            -1L << 42,
            -1L << 49,
            -1L << 56,
            -1L << 63
    };

    private long[] testValues;
    private long[] smallValues;      // 1-2
    private long[] mediumValues;     // 3-5

    // ========== getByteSize test ==========
    private long[] largeValues;      // 6-10
    private ByteBuf buffer;

    // ========== method ==========
    private static int getByteSizeMinecraft(long data) {
        for (int i = 1; i < 10; ++i) {
            if ((data & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    private static int getByteSize0212(long data) {
        for (int i = 1; i < 10; i++) {
            if ((data & SIZE_MASKS[i]) == 0L) {
                return i;
            }
        }
        return 10;
    }

    // ========== write test ==========

    private static int getByteSize0210(long data) {
        if (data == 0) return 1;
        int significantBits = 64 - Long.numberOfLeadingZeros(data);
        return (significantBits + 6) / 7;
    }

    private static void writeMinecraft(ByteBuf buffer, long value) {
        while ((value & VarLongUtil.MASK_7_BITS) != 0L) {
            buffer.writeByte((int) (value & 0x7FL) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte((int) value);
    }

    private static void write0213(ByteBuf buffer, long value) {
        if ((value & VarLongUtil.MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & VarLongUtil.MASK_14_BITS) == 0L) {
            VarLongUtil.writeTwoBytes(buffer, value);
        } else if ((value & MASK_21_BITS) == 0L) {
            VarLongUtil.writeThreeBytes(buffer, value);
        } else if ((value & MASK_28_BITS) == 0L) {
            VarLongUtil.writeFourBytes(buffer, value);
        } else {
            writeMinecraft(buffer, value);
        }
    }

    // ========== size write test ==========

    private static void write0214(ByteBuf buffer, long value) {
        if ((value & VarLongUtil.MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & VarLongUtil.MASK_14_BITS) == 0L) {
            VarLongUtil.writeTwoBytes(buffer, value);
        } else {
            VarLongUtil.writeVarLongFull(buffer, value);
        }
    }

    @Setup
    public void setup() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        testValues = new long[10000];
        for (int i = 0; i < testValues.length; i++) {
            switch (i % 10) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    // 50% small values (1-2 bytes)
                    testValues[i] = random.nextLong(16384); // 0 to 2^14-1
                    break;
                case 5:
                case 6:
                case 7:
                    // 30% medium values (3-5 bytes)
                    testValues[i] = random.nextLong(268435456L); // 0 to 2^28-1
                    break;
                case 8:
                case 9:
                    // 20% large values (6-10 bytes)
                    testValues[i] = random.nextLong();
                    break;
            }
            if (testValues[i] < 0) testValues[i] = -testValues[i];
        }

        smallValues = new long[1000];
        mediumValues = new long[1000];
        largeValues = new long[1000];

        for (int i = 0; i < 1000; i++) {
            smallValues[i] = random.nextLong(16384);
            mediumValues[i] = random.nextLong(268435456L);
            largeValues[i] = Math.abs(random.nextLong());
        }

        buffer = PooledByteBufAllocator.DEFAULT.directBuffer(64 * 1024);
    }

    @TearDown
    public void tearDown() {
        if (buffer != null) {
            buffer.release();
        }
    }

    @Benchmark
    @Group("GetByteSize")
    public void benchmarkGetByteSizeMinecraft(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSizeMinecraft(value));
        }
    }

    @Benchmark
    @Group("GetByteSize")
    public void benchmarkGetByteSize0210(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSize0210(value));
        }
    }

    @Benchmark
    @Group("GetByteSize")
    public void benchmarkGetByteSize0212(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSize0212(value));
        }
    }

    @Benchmark
    @Group("GetByteSize")
    public void benchmarkGetByteSize0214(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(VarLongUtil.getVarLongLength(value));
        }
    }

    @Benchmark
    @Group("Write")
    public void benchmarkWriteMinecraft(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("Write")
    public void benchmarkWrite0213(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            write0213(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("Write")
    public void benchmarkWrite0214(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            write0214(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    // ========== size-specific write benchmarks ==========

    @Benchmark
    @Group("WriteSmallValues")
    public void benchmarkWriteSmallValuesMinecraft(Blackhole bh) {
        buffer.clear();
        for (long value : smallValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteSmallValues")
    public void benchmarkWriteSmallValues0213(Blackhole bh) {
        buffer.clear();
        for (long value : smallValues) {
            write0213(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteSmallValues")
    public void benchmarkWriteSmallValues0214(Blackhole bh) {
        buffer.clear();
        for (long value : smallValues) {
            write0214(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteMediumValues")
    public void benchmarkWriteMediumValuesMinecraft(Blackhole bh) {
        buffer.clear();
        for (long value : mediumValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteMediumValues")
    public void benchmarkWriteMediumValues0213(Blackhole bh) {
        buffer.clear();
        for (long value : mediumValues) {
            write0213(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteMediumValues")
    public void benchmarkWriteMediumValues0214(Blackhole bh) {
        buffer.clear();
        for (long value : mediumValues) {
            write0214(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteLargeValues")
    public void benchmarkWriteLargeValuesMinecraft(Blackhole bh) {
        buffer.clear();
        for (long value : largeValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteLargeValues")
    public void benchmarkWriteLargeValues0213(Blackhole bh) {
        buffer.clear();
        for (long value : largeValues) {
            write0213(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    @Group("WriteLargeValues")
    public void benchmarkWriteLargeValues0214(Blackhole bh) {
        buffer.clear();
        for (long value : largeValues) {
            write0214(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }
}