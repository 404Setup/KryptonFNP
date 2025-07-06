package me.steinborn.krypton.mod.shared.network.util;

import net.minecraft.util.Mth;

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
            VARINT_EXACT_BYTE_LENGTHS[i] = Mth.ceil((31d - (i - 1)) / 7d);
        }
        VARINT_EXACT_BYTE_LENGTHS[32] = 1; // Special case for 0.
    }

    public static int getVarIntLength(int value) {
        return VARINT_EXACT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)];
    }
}
