package one.pkg.kfnp.shared.network.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

/**
 * Maps VarInt byte sizes to a lookup table corresponding to the number of bits in the integer,
 * from zero to 32.
 */
public class VarIntUtil {
    public static final int MASK_7_BITS = 0xFFFFFFFF << 7;
    public static final int MASK_14_BITS = 0xFFFFFFFF << 14;
    public static final int MASK_21_BITS = 0xFFFFFFFF << 21;
    public static final int MASK_28_BITS = 0xFFFFFFFF << 28;
    private static final int[] VARINT_EXACT_BYTE_LENGTHS = new int[33];

    static {
        for (int i = 0; i <= 32; ++i) {
            VARINT_EXACT_BYTE_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
        }
        VARINT_EXACT_BYTE_LENGTHS[32] = 1; // Special case for 0.
    }

    public static int getVarIntLength(int value) {
        return VARINT_EXACT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)];
    }

    public static int readVarInt(ByteBuf buf) {
        int i = 0;
        int max = 5;
        int result = 0;
        int shift = 0;
        int b;

        do {
            if (i >= max) {
                throw new DecoderException("VarInt is too big");
            }
            b = buf.readByte();
            result |= (b & 127) << shift;
            shift += 7;
            i++;
        } while ((b & 128) != 0);
        return result;
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        if ((value & MASK_7_BITS) == 0) {
            buf.writeByte(value);
        } else if ((value & MASK_14_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
        } else if ((value & MASK_21_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & MASK_28_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }

    public static void writeVarIntFull0210(ByteBuf buf, int value) {
        // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((value & MASK_21_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & MASK_28_BITS) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }
}
