package one.pkg.mod.krypton_fnp.mixin.network.experimental;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarLong;
import one.pkg.mod.krypton_fnp.shared.network.util.VarLongUtil;
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
        VarLongUtil.writeVarLongFull(buffer, value);
        return buffer;
    }
}