package one.pkg.kfnp.shared.network.util;

import io.netty.buffer.ByteBuf;

public class VarLongUtil {
    public static final long MASK_7_BITS = -1L << 7;
    public static final long MASK_14_BITS = -1L << 14;
    public static final long MASK_21_BITS = -1L << 21;
    public static final long MASK_28_BITS = -1L << 28;
    public static final long MASK_35_BITS = -1L << 35;
    public static final long MASK_42_BITS = -1L << 42;
    private static final long CONTINUATION_BIT = 0x80L;
    private static final long DATA_MASK = 0x7FL;
    private static final int[] VARLONG_EXACT_BYTE_LENGTHS = new int[65];

    static {
        for (int i = 0; i < 64; i++) {
            int s = 64 - i;
            VARLONG_EXACT_BYTE_LENGTHS[i] = (s + 6) / 7;
        }
        VARLONG_EXACT_BYTE_LENGTHS[64] = 1;
    }

    public static int getVarLongLength(long data) {
        return VARLONG_EXACT_BYTE_LENGTHS[Long.numberOfLeadingZeros(data)];
    }

    public static void writeVarLongFull(ByteBuf buffer, long value) {
        if ((value & MASK_7_BITS) == 0) {
            buffer.writeByte((int) value);
        } else if ((value & MASK_14_BITS) == 0) {
            buffer.writeShort((int) ((value & DATA_MASK) | CONTINUATION_BIT) << 8 | (int) (value >>> 7));
        } else if ((value & MASK_21_BITS) == 0) {
            int encoded3 = (int) ((value & DATA_MASK) | CONTINUATION_BIT) << 16
                    | (int) (((value >>> 7) & DATA_MASK) | CONTINUATION_BIT) << 8
                    | (int) (value >>> 14);
            buffer.writeMedium(encoded3);
        } else if ((value & MASK_28_BITS) == 0) {
            int encoded4 = (int) ((value & DATA_MASK) | CONTINUATION_BIT) << 24
                    | (int) (((value >>> 7) & DATA_MASK) | CONTINUATION_BIT) << 16
                    | (int) (((value >>> 14) & DATA_MASK) | CONTINUATION_BIT) << 8
                    | (int) (value >>> 21);
            buffer.writeInt(encoded4);
        } else if ((value & MASK_35_BITS) == 0) {
            int first45 = (int) ((value & DATA_MASK) | CONTINUATION_BIT) << 24
                    | (int) (((value >>> 7) & DATA_MASK) | CONTINUATION_BIT) << 16
                    | (int) (((value >>> 14) & DATA_MASK) | CONTINUATION_BIT) << 8
                    | (int) (((value >>> 21) & DATA_MASK) | CONTINUATION_BIT);
            buffer.writeInt(first45);
            buffer.writeByte((int) (value >>> 28));
        } else if ((value & MASK_42_BITS) == 0) {
            int first46 = (int) ((value & DATA_MASK) | CONTINUATION_BIT) << 24
                    | (int) (((value >>> 7) & DATA_MASK) | CONTINUATION_BIT) << 16
                    | (int) (((value >>> 14) & DATA_MASK) | CONTINUATION_BIT) << 8
                    | (int) (((value >>> 21) & DATA_MASK) | CONTINUATION_BIT);
            int last26 = (int) (((value >>> 28) & DATA_MASK) | CONTINUATION_BIT) << 8
                    | (int) (value >>> 35);
            buffer.writeInt(first46);
            buffer.writeShort(last26);
        } else {
            int length = getVarLongLength(value);

            switch (length) {
                case 7:
                    int first47 = (int) ((value & DATA_MASK) | CONTINUATION_BIT) << 24
                            | (int) (((value >>> 7) & DATA_MASK) | CONTINUATION_BIT) << 16
                            | (int) (((value >>> 14) & DATA_MASK) | CONTINUATION_BIT) << 8
                            | (int) (((value >>> 21) & DATA_MASK) | CONTINUATION_BIT);
                    int last37 = (int) (((value >>> 28) & DATA_MASK) | CONTINUATION_BIT) << 16
                            | (int) (((value >>> 35) & DATA_MASK) | CONTINUATION_BIT) << 8
                            | (int) (value >>> 42);
                    buffer.writeInt(first47);
                    buffer.writeMedium(last37);
                    break;
                case 8:
                    int first48 = (int) ((value & DATA_MASK) | CONTINUATION_BIT) << 24
                            | (int) (((value >>> 7) & DATA_MASK) | CONTINUATION_BIT) << 16
                            | (int) (((value >>> 14) & DATA_MASK) | CONTINUATION_BIT) << 8
                            | (int) (((value >>> 21) & DATA_MASK) | CONTINUATION_BIT);
                    int last48 = (int) (((value >>> 28) & DATA_MASK) | CONTINUATION_BIT) << 24
                            | (int) (((value >>> 35) & DATA_MASK) | CONTINUATION_BIT) << 16
                            | (int) (((value >>> 42) & DATA_MASK) | CONTINUATION_BIT) << 8
                            | (int) (value >>> 49);
                    buffer.writeInt(first48);
                    buffer.writeInt(last48);
                    break;
                case 9:
                    buffer.writeLong(getFirst8(value));
                    buffer.writeByte((int) (value >>> 56));
                    break;
                case 10:
                    int last2 = (int) (((value >>> 56) & DATA_MASK) | CONTINUATION_BIT) << 8
                            | (int) (value >>> 63);
                    buffer.writeLong(getFirst8(value));
                    buffer.writeShort(last2);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid VarLong length: " + length);
            }
        }
    }

    public static long getFirst8(long value) {
        return ((value & DATA_MASK) | CONTINUATION_BIT) << 56
                | (((value >>> 7) & DATA_MASK) | CONTINUATION_BIT) << 48
                | (((value >>> 14) & DATA_MASK) | CONTINUATION_BIT) << 40
                | (((value >>> 21) & DATA_MASK) | CONTINUATION_BIT) << 32
                | (((value >>> 28) & DATA_MASK) | CONTINUATION_BIT) << 24
                | (((value >>> 35) & DATA_MASK) | CONTINUATION_BIT) << 16
                | (((value >>> 42) & DATA_MASK) | CONTINUATION_BIT) << 8
                | (((value >>> 49) & DATA_MASK) | CONTINUATION_BIT);
    }
}
