package me.steinborn.krypton.jmh;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.steinborn.krypton.mod.shared.network.util.VarLongUtil;
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
public class VarLongBenchmark {
    private static final long MASK_7_BITS = -1L << 7;
    private static final long MASK_14_BITS = -1L << 14;
    private static final long MASK_21_BITS = -1L << 21;
    private static final long MASK_28_BITS = -1L << 28;
    private long[] testValues;
    private long[] smallValues;      // 1-2
    private long[] mediumValues;     // 3-5

    // ========== getByteSize test ==========
    private long[] largeValues;      // 6-10
    private ByteBuf buffer;

    // ========== method ==========
    private static int getByteSizeOriginal(long data) {
        for (int i = 1; i < 10; ++i) {
            if ((data & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    // ========== write test ==========

    private static int getByteSizeMath(long data) {
        if (data == 0) return 1;
        int significantBits = 64 - Long.numberOfLeadingZeros(data);
        return (significantBits + 6) / 7;
    }

    private static void writeOriginalLoop(ByteBuf buffer, long value) {
        while ((value & MASK_7_BITS) != 0L) {
            buffer.writeByte((int) (value & 0x7FL) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte((int) value);
    }

    private static void writeOptimizedBranches(ByteBuf buffer, long value) {
        if ((value & MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & MASK_14_BITS) == 0L) {
            writeTwoBytes(buffer, value);
        } else if ((value & MASK_21_BITS) == 0L) {
            writeThreeBytes(buffer, value);
        } else if ((value & MASK_28_BITS) == 0L) {
            writeFourBytes(buffer, value);
        } else {
            writeOriginalLoop(buffer, value);
        }
    }

    // ========== size write test ==========

    private static void writeOptimizedSwitch(ByteBuf buffer, long value) {
        if ((value & MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & MASK_14_BITS) == 0L) {
            writeTwoBytes(buffer, value);
        } else {
            writeVarLongFull(buffer, value);
        }
    }

    private static void writeVarLongFull(ByteBuf buffer, long value) {
        int length = VarLongUtil.getVarLongLength(value);

        switch (length) {
            case 3:
                writeThreeBytes(buffer, value);
                break;
            case 4:
                writeFourBytes(buffer, value);
                break;
            case 5:
                writeFiveBytes(buffer, value);
                break;
            case 6:
                writeSixBytes(buffer, value);
                break;
            case 7:
                writeSevenBytes(buffer, value);
                break;
            case 8:
                writeEightBytes(buffer, value);
                break;
            case 9:
                writeNineBytes(buffer, value);
                break;
            case 10:
                writeTenBytes(buffer, value);
                break;
            default:
                throw new IllegalArgumentException("Invalid VarLong length: " + length);
        }
    }

    private static void writeTwoBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 8 | (int) (value >>> 7);
        buffer.writeShort(encoded);
    }

    private static void writeThreeBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 14);
        buffer.writeMedium(encoded);
    }

    private static void writeFourBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 21);
        buffer.writeInt(encoded);
    }

    private static void writeFiveBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        buffer.writeInt(first4);
        buffer.writeByte((int) (value >>> 28));
    }

    private static void writeSixBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        int last2 = (int) (((value >>> 28) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 35);
        buffer.writeInt(first4);
        buffer.writeShort(last2);
    }

    private static void writeSevenBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        int last3 = (int) (((value >>> 28) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 35) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 42);
        buffer.writeInt(first4);
        buffer.writeMedium(last3);
    }

    private static void writeEightBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        int last4 = (int) (((value >>> 28) & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 35) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 42) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 49);
        buffer.writeInt(first4);
        buffer.writeInt(last4);
    }

    private static void writeNineBytes(ByteBuf buffer, long value) {
        long first8 = getFirst8(value);
        buffer.writeLong(first8);
        buffer.writeByte((int) (value >>> 56));
    }

    private static void writeTenBytes(ByteBuf buffer, long value) {
        long first8 = getFirst8(value);
        int last2 = (int) (((value >>> 56) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 63);
        buffer.writeLong(first8);
        buffer.writeShort(last2);
    }

    private static long getFirst8(long value) {
        return ((value & 0x7FL) | 0x80L) << 56
                | (((value >>> 7) & 0x7FL) | 0x80L) << 48
                | (((value >>> 14) & 0x7FL) | 0x80L) << 40
                | (((value >>> 21) & 0x7FL) | 0x80L) << 32
                | (((value >>> 28) & 0x7FL) | 0x80L) << 24
                | (((value >>> 35) & 0x7FL) | 0x80L) << 16
                | (((value >>> 42) & 0x7FL) | 0x80L) << 8
                | (((value >>> 49) & 0x7FL) | 0x80L);
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
    public void benchmarkGetByteSizeOriginal(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSizeOriginal(value));
        }
    }

    @Benchmark
    public void benchmarkGetByteSizeMath(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(getByteSizeMath(value));
        }
    }

    @Benchmark
    public void benchmarkGetByteSizeLookupTable(Blackhole bh) {
        for (long value : testValues) {
            bh.consume(VarLongUtil.getVarLongLength(value));
        }
    }

    @Benchmark
    public void benchmarkWriteOriginalLoop(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            writeOriginalLoop(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteOptimizedBranches(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            writeOptimizedBranches(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteOptimizedSwitch(Blackhole bh) {
        buffer.clear();
        for (long value : testValues) {
            writeOptimizedSwitch(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteSmallValues(Blackhole bh) {
        buffer.clear();
        for (long value : smallValues) {
            writeOptimizedSwitch(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteMediumValues(Blackhole bh) {
        buffer.clear();
        for (long value : mediumValues) {
            writeOptimizedSwitch(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }

    @Benchmark
    public void benchmarkWriteLargeValues(Blackhole bh) {
        buffer.clear();
        for (long value : largeValues) {
            writeOptimizedSwitch(buffer, value);
        }
        bh.consume(buffer.writerIndex());
    }
}
