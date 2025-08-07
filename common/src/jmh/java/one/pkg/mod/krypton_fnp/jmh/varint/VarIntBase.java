package one.pkg.mod.krypton_fnp.jmh.varint;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import one.pkg.mod.krypton_fnp.shared.network.util.VarIntUtil;

import java.util.concurrent.ThreadLocalRandom;

public class VarIntBase {
    static final int MASK_21_BITS = -1 << 21;
    static final int MASK_28_BITS = -1 << 28;

    int[] testValues;
    int[] smallValues;      // 1-2 bytes
    int[] mediumValues;     // 3-4 bytes
    int[] largeValues;      // 5 bytes
    ByteBuf buffer;

    // ========== write methods ==========

    static void writeMinecraft(ByteBuf buffer, int value) {
        while ((value & -128) != 0) {
            buffer.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buffer.writeByte(value);
    }

    static void write0209(ByteBuf buffer, int value) {
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

    static void write0210(ByteBuf buffer, int value) {
        if ((value & VarIntUtil.MASK_7_BITS) == 0) {
            buffer.writeByte(value);
        } else if ((value & VarIntUtil.MASK_14_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buffer.writeShort(w);
        } else {
            VarIntUtil.writeVarIntFull0210(buffer, value);
        }
    }

    static void write0216(ByteBuf buf, int value) {
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

    public void tearDown() {
        if (buffer != null) {
            buffer.release();
        }
    }
}
