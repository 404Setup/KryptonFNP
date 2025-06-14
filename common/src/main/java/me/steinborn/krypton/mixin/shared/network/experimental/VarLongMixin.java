package me.steinborn.krypton.mixin.shared.network.experimental;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = VarLong.class, priority = 900)
public class VarLongMixin {
    @Unique
    private static final long[] SIZE_MASKS = {
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

    @Unique
    private static final long MASK_7_BITS = -1L << 7;
    @Unique
    private static final long MASK_14_BITS = -1L << 14;
    @Unique
    private static final long MASK_21_BITS = -1L << 21;
    @Unique
    private static final long MASK_28_BITS = -1L << 28;

    /**
     * @author 404
     * @reason optimized version for VarLong
     */
    @Overwrite
    public static int getByteSize(long data) {
        for (int i = 1; i < 10; i++) {
            if ((data & SIZE_MASKS[i]) == 0L) {
                return i;
            }
        }
        return 10;
    }

    /**
     * @author 404
     * @reason optimized version for VarLong (test)
     */
    @Overwrite
    public static ByteBuf write(ByteBuf buffer, long value) {
        if ((value & MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & MASK_14_BITS) == 0L) {
            krypton_Multi$writeTwoBytes(buffer, value);
        } else if ((value & MASK_21_BITS) == 0L) {
            krypton_Multi$writeThreeBytes(buffer, value);
        } else if ((value & MASK_28_BITS) == 0L) {
            krypton_Multi$writeFourBytes(buffer, value);
        } else {
            krypton_Multi$writeVarLongFull(buffer, value);
        }
        return buffer;
    }

    @Unique
    private static void krypton_Multi$writeTwoBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 8 | (int) (value >>> 7);
        buffer.writeShort(encoded);
    }

    @Unique
    private static void krypton_Multi$writeThreeBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 14);
        buffer.writeMedium(encoded);
    }

    @Unique
    private static void krypton_Multi$writeFourBytes(ByteBuf buffer, long value) {
        int encoded = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 21);
        buffer.writeInt(encoded);
    }

    @Unique
    private static void krypton_Multi$writeVarLongFull(ByteBuf buffer, long value) {
        while ((value & MASK_7_BITS) != 0L) {
            buffer.writeByte((int) (value & 0x7FL) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte((int) value);
    }

}