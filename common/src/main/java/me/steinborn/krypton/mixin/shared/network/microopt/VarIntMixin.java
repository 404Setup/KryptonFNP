package me.steinborn.krypton.mixin.shared.network.microopt;

import io.netty.buffer.ByteBuf;
import me.steinborn.krypton.mod.shared.network.util.VarIntUtil;
import net.minecraft.network.VarInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VarInt.class)
public class VarIntMixin {

    /**
     * @author Andrew Steinborn
     * @reason optimized version
     */
    @Overwrite
    public static int getByteSize(int v) {
        return VarIntUtil.getVarIntLength(v);
    }

    /**
     * @author Andrew Steinborn
     * @reason optimized version
     */
    @Overwrite
    public static ByteBuf write(ByteBuf buf, int value) {
        // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
        // that the server will send, to improve inlining.
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
        return buf;
    }
}