package me.steinborn.krypton.mixin.shared.network.experimental;

import io.netty.buffer.ByteBuf;
import me.steinborn.krypton.mod.shared.network.util.VarLongUtil;
import net.minecraft.network.VarLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = VarLong.class, priority = 900)
public class VarLongMixin {
    @Unique
    private static final long MASK_7_BITS = -1L << 7;
    @Unique
    private static final long MASK_14_BITS = -1L << 14;

    /**
     * @author 404
     * @reason optimized version for VarLong
     */
    @Overwrite
    public static int getByteSize(long data) {
        return VarLongUtil.getVarLongLength(data);
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
    private static void krypton_Multi$writeVarLongFull(ByteBuf buffer, long value) {
        int length = VarLongUtil.getVarLongLength(value);

        switch (length) {
            case 3:
                krypton_Multi$writeThreeBytes(buffer, value);
                break;
            case 4:
                krypton_Multi$writeFourBytes(buffer, value);
                break;
            case 5:
                krypton_Multi$writeFiveBytes(buffer, value);
                break;
            case 6:
                krypton_Multi$writeSixBytes(buffer, value);
                break;
            case 7:
                krypton_Multi$writeSevenBytes(buffer, value);
                break;
            case 8:
                krypton_Multi$writeEightBytes(buffer, value);
                break;
            case 9:
                krypton_Multi$writeNineBytes(buffer, value);
                break;
            case 10:
                krypton_Multi$writeTenBytes(buffer, value);
                break;
            default:
                throw new IllegalArgumentException("Invalid VarLong length: " + length);
        }
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
    private static void krypton_Multi$writeFiveBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        buffer.writeInt(first4);
        buffer.writeByte((int) (value >>> 28));
    }

    @Unique
    private static void krypton_Multi$writeSixBytes(ByteBuf buffer, long value) {
        int first4 = (int) ((value & 0x7FL) | 0x80L) << 24
                | (int) (((value >>> 7) & 0x7FL) | 0x80L) << 16
                | (int) (((value >>> 14) & 0x7FL) | 0x80L) << 8
                | (int) (((value >>> 21) & 0x7FL) | 0x80L);
        int last2 = (int) (((value >>> 28) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 35);
        buffer.writeInt(first4);
        buffer.writeShort(last2);
    }

    @Unique
    private static void krypton_Multi$writeSevenBytes(ByteBuf buffer, long value) {
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

    @Unique
    private static void krypton_Multi$writeEightBytes(ByteBuf buffer, long value) {
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

    @Unique
    private static void krypton_Multi$writeNineBytes(ByteBuf buffer, long value) {
        long first8 = krypton_Multi$getFirst8(value);
        buffer.writeLong(first8);
        buffer.writeByte((int) (value >>> 56));
    }

    @Unique
    private static void krypton_Multi$writeTenBytes(ByteBuf buffer, long value) {
        long first8 = krypton_Multi$getFirst8(value);
        int last2 = (int) (((value >>> 56) & 0x7FL) | 0x80L) << 8
                | (int) (value >>> 63);
        buffer.writeLong(first8);
        buffer.writeShort(last2);
    }

    @Unique
    private static long krypton_Multi$getFirst8(long value) {
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