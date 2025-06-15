package me.steinborn.krypton.mod.shared.network.util;

public class VarLongUtil {
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
}
