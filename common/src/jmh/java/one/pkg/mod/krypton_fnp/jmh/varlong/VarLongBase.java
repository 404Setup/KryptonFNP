package one.pkg.mod.krypton_fnp.jmh.varlong;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import one.pkg.mod.krypton_fnp.shared.network.util.VarLongUtil;

import java.util.concurrent.ThreadLocalRandom;

public class VarLongBase {
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
    long[] smallValues;
    long[] mediumValues;

    long[] largeValues;
    ByteBuf buffer;

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
                    testValues[i] = random.nextLong(16384);
                    break;
                case 5:
                case 6:
                case 7:
                    testValues[i] = random.nextLong(268435456L);
                    break;
                case 8:
                case 9:
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
