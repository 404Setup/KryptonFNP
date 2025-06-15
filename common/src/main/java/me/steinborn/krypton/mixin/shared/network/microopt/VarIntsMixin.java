package me.steinborn.krypton.mixin.shared.network.microopt;

import io.netty.buffer.ByteBuf;
import me.steinborn.krypton.mod.shared.network.util.VarIntUtil;
import net.minecraft.network.VarInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VarInt.class)
public class VarIntsMixin {

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
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
        } else {
            VarIntUtil.writeVarIntFull(buf, value);
        }

        return buf;
    }
}