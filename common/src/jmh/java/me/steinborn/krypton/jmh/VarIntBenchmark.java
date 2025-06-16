package me.steinborn.krypton.jmh;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.steinborn.krypton.mod.shared.network.util.VarIntUtil;
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
public class VarIntBenchmark {
    private static final int MASK_21_BITS = -1 << 21;
    private static final int MASK_28_BITS = -1 << 28;

    private int[] testValues;
    private int[] smallValues;      // 1-2 bytes
    private int[] mediumValues;     // 3-4 bytes
    private int[] largeValues;      // 5 bytes
    private ByteBuf buffer;

    // ========== getByteSize methods ==========

    private static int getByteSizeMinecraft(int data) {
        for (int i = 1; i < 5; ++i) {
            if ((data & -1 << i * 7) == 0) {
                return i;
            }
        }
        return 5;
    }

    // ========== write methods ==========

    private static void writeMinecraft(ByteBuf buffer, int value) {
        while ((value & -128) != 0) {
            buffer.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buffer.writeByte(value);
    }

    private static void write0209(ByteBuf buffer, int value) {
        if ((value & VarIntUtil.MASK_7_BITS) == 0) {
            buffer.writeByte(value);
        } else if ((value & VarIntUtil.MASK_14_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buffer.writeShort(w);
        } else if ((value & MASK_21_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buffer.writeMedium(w);
        } else if ((value & MASK_28_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buffer.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buffer.writeInt(w);
            buffer.writeByte(value >>> 28);
        }
    }

    private static void write0210(ByteBuf buffer, int value) {
        if ((value & VarIntUtil.MASK_7_BITS) == 0) {
            buffer.writeByte(value);
        } else if ((value & VarIntUtil.MASK_14_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buffer.writeShort(w);
        } else {
            VarIntUtil.writeVarIntFull0210(buffer, value);
        }
    }

    private static void write0216(ByteBuf buf, int value) {
        if ((value & VarIntUtil.MASK_7_BITS) == 0) {
            buf.writeByte(value);
        } else if ((value & VarIntUtil.MASK_14_BITS) == 0) {
            buf.writeShort((value & 0x7F | 0x80) << 8 | (value >>> 7));
        } else if ((value & VarIntUtil.MASK_21_BITS) == 0) {
            buf.writeMedium((value & 0x7F | 0x80) << 16
                    | ((value >>> 7) & 0x7F | 0x80) << 8
                    | (value >>> 14));
        } else if ((value & VarIntUtil.MASK_28_BITS) == 0) {
            buf.writeInt((value & 0x7F | 0x80) << 24
                    | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8
                    | (value >>> 21));
        } else {
            buf.writeInt((value & 0x7F | 0x80) << 24
                    | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8
                    | ((value >>> 21) & 0x7F | 0x80));
            buf.writeByte(value >>> 28);
        }
    }

    @Setup
    public void setup() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        testValues = new int[10000];
        for (int i = 0; i < testValues.length; i++) {
            switch (i % 10) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    // 50% small values (1-2 bytes)
                    testValues[i] = random.nextInt(16384); // 0 to 2^14-1
                    break;
                case 5:
                case 6:
                case 7:
                    // 30% medium values (3-4 bytes)
                    testValues[i] = random.nextInt(268435456); // 0 to 2^28-1
                    break;
                case 8:
                case 9:
                    // 20% large values (5 bytes)
                    testValues[i] = Math.abs(random.nextInt());
                    break;
            }
        }

        smallValues = new int[1000];
        mediumValues = new int[1000];
        largeValues = new int[1000];

        for (int i = 0; i < 1000; i++) {
            smallValues[i] = random.nextInt(16384);
            mediumValues[i] = random.nextInt(268435456);
            largeValues[i] = Math.abs(random.nextInt());
        }

        buffer = PooledByteBufAllocator.DEFAULT.directBuffer(32 * 1024);
    }

    @TearDown
    public void tearDown() {
        if (buffer != null) {
            buffer.release();
        }
    }

    // ========== getByteSize benchmarks ==========

    @Benchmark
    public void benchmarkGetByteSizeMinecraft(Blackhole bh) {
        for (int value : testValues) {
            bh.consume(getByteSizeMinecraft(value));
        }
    }

    @Benchmark
    public void benchmarkGetByteSize0209(Blackhole bh) {
        for (int value : testValues) {
            bh.consume(VarIntUtil.getVarIntLength(value));
        }
    }

    // ========== write benchmarks ==========

    @Benchmark
    public void benchmarkWriteMinecraft(Blackhole bh) {
        buffer.clear();
        for (int value : testValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWrite0209(Blackhole bh) {
        buffer.clear();
        for (int value : testValues) {
            write0209(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWrite0210(Blackhole bh) {
        buffer.clear();
        for (int value : testValues) {
            write0210(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWrite0216(Blackhole bh) {
        buffer.clear();
        for (int value : testValues) {
            write0216(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    // ========== size-specific write benchmarks ==========

    @Benchmark
    public void benchmarkWriteSmallValuesMinecraft(Blackhole bh) {
        buffer.clear();
        for (int value : smallValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteSmallValues0209(Blackhole bh) {
        buffer.clear();
        for (int value : smallValues) {
            write0209(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteSmallValues0210(Blackhole bh) {
        buffer.clear();
        for (int value : smallValues) {
            write0210(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteSmallValues0216(Blackhole bh) {
        buffer.clear();
        for (int value : smallValues) {
            write0216(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteMediumValuesMinecraft(Blackhole bh) {
        buffer.clear();
        for (int value : mediumValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteMediumValues0209(Blackhole bh) {
        buffer.clear();
        for (int value : mediumValues) {
            write0209(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteMediumValues0210(Blackhole bh) {
        buffer.clear();
        for (int value : mediumValues) {
            write0210(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteMediumValues0216(Blackhole bh) {
        buffer.clear();
        for (int value : mediumValues) {
            write0216(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteLargeValuesMinecraft(Blackhole bh) {
        buffer.clear();
        for (int value : largeValues) {
            writeMinecraft(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteLargeValues0209(Blackhole bh) {
        buffer.clear();
        for (int value : largeValues) {
            write0209(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteLargeValues0210(Blackhole bh) {
        buffer.clear();
        for (int value : largeValues) {
            write0210(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteLargeValues0216(Blackhole bh) {
        buffer.clear();
        for (int value : largeValues) {
            write0216(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }
}