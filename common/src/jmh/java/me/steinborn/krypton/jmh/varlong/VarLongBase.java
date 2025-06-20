package me.steinborn.krypton.jmh.varlong;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.steinborn.krypton.mod.shared.network.util.VarLongUtil;

import java.util.concurrent.ThreadLocalRandom;

public class VarLongBase {
    static final long MASK_21_BITS = -1L << 21;
    static final long MASK_28_BITS = -1L << 28;

    static final long[] SIZE_MASKS = {
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

    long[] testValues;
    long[] smallValues;      // 1-2
    long[] mediumValues;     // 3-5

    long[] largeValues;      // 6-10
    ByteBuf buffer;

    // ========== method ==========
    static int getByteSizeMinecraft(long data) {
        for (int i = 1; i < 10; ++i) {
            if ((data & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    static int getByteSize0212(long data) {
        for (int i = 1; i < 10; i++) {
            if ((data & SIZE_MASKS[i]) == 0L) {
                return i;
            }
        }
        return 10;
    }

    static int getByteSize0210(long data) {
        if (data == 0) return 1;
        int significantBits = 64 - Long.numberOfLeadingZeros(data);
        return (significantBits + 6) / 7;
    }

    static void writeMinecraft(ByteBuf buffer, long value) {
        while ((value & VarLongUtil.MASK_7_BITS) != 0L) {
            buffer.writeByte((int) (value & 0x7FL) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte((int) value);
    }

    static void write0213(ByteBuf buffer, long value) {
        if ((value & VarLongUtil.MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & VarLongUtil.MASK_14_BITS) == 0L) {
            buffer.writeShort((int) ((value & 0x7FL) | 0x80L) << 8 | (int) (value >>> 7));
        } else if ((value & MASK_21_BITS) == 0L) {
            VarLongUtil.writeThreeBytes(buffer, value);
        } else if ((value & MASK_28_BITS) == 0L) {
            VarLongUtil.writeFourBytes(buffer, value);
        } else {
            writeMinecraft(buffer, value);
        }
    }

    static void write0214(ByteBuf buffer, long value) {
        if ((value & VarLongUtil.MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & VarLongUtil.MASK_14_BITS) == 0L) {
            buffer.writeShort((int) ((value & 0x7FL) | 0x80L) << 8 | (int) (value >>> 7));
        } else {
            VarLongUtil.writeVarLongFull(buffer, value);
        }
    }

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

    public void tearDown() {
        if (buffer != null) {
            buffer.release();
        }
    }
}
