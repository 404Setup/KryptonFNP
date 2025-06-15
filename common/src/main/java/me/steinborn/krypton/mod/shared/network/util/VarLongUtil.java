package me.steinborn.krypton.mod.shared.network.util;

import io.netty.buffer.ByteBuf;

public class VarLongUtil {
    public static final long MASK_7_BITS = -1L << 7;
    public static final long MASK_14_BITS = -1L << 14;
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

    public static void writeTwoBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 8 | (int) (value >>> 7);
        buffer.writeShort(encoded);
    }

    public static void writeVarLongFull(ByteBuf buffer, long value) {
        int length = getVarLongLength(value);

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

    public static void writeThreeBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 14);
        buffer.writeMedium(encoded);
    }

    public static void writeFourBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 21);
        buffer.writeInt(encoded);
    }

    public static void writeFiveBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        buffer.writeInt(first4);
        buffer.writeByte((int) (value >>> 28));
    }

    public static void writeSixBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        int last2 = (int) (((value >>> 28) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 35);
        buffer.writeInt(first4);
        buffer.writeShort(last2);
    }

    public static void writeSevenBytes(ByteBuf buffer, long value) {
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

    public static void writeEightBytes(ByteBuf buffer, long value) {
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

    public static void writeNineBytes(ByteBuf buffer, long value) {
        long first8 = getFirst8(value);
        buffer.writeLong(first8);
        buffer.writeByte((int) (value >>> 56));
    }

    public static void writeTenBytes(ByteBuf buffer, long value) {
        long first8 = getFirst8(value);
        int last2 = (int) (((value >>> 56) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 63);
        buffer.writeLong(first8);
        buffer.writeShort(last2);
    }

    public static long getFirst8(long value) {
        return ((value & 0x7FL) | 0x80L) << 56
                | (((value >>> 7) & 0x7FL) | 0x80L) << 48
                | (((value >>> 14) & 0x7FL) | 0x80L) << 40
                | (((value >>> 21) & 0x7FL) | 0x80L) << 32
                | (((value >>> 28) & 0x7FL) | 0x80L) << 24
                | (((value >>> 35) & 0x7FL) | 0x80L) << 16
                | (((value >>> 42) & 0x7FL) | 0x80L) << 8
                | (((value >>> 49) & 0x7FL) | 0x80L);
    }
}
