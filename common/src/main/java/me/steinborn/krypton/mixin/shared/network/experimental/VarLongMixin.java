package me.steinborn.krypton.mixin.shared.network.experimental;

import io.netty.buffer.ByteBuf;
import me.steinborn.krypton.mod.shared.network.util.VarLongUtil;
import net.minecraft.network.VarLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = VarLong.class, priority = 900)
public class VarLongMixin {
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
        if ((value & VarLongUtil.MASK_7_BITS) == 0L) {
            buffer.writeByte((int) value);
        } else if ((value & VarLongUtil.MASK_14_BITS) == 0L) {
            buffer.writeShort((int) ((value & 0x7FL) | 0x80L) << 8 | (int) (value >>> 7));
        } else {
            VarLongUtil.writeVarLongFull(buffer, value);
        }
        return buffer;
    }
}