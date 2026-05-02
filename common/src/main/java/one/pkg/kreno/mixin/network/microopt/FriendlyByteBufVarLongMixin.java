package one.pkg.kreno.mixin.network.microopt;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import one.pkg.kreno.shared.network.util.VarLongUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FriendlyByteBuf.class, priority = 900)
public abstract class FriendlyByteBufVarLongMixin extends ByteBuf {
    @Shadow
    @Final
    private ByteBuf source;

    /**
     * @author 404
     * @reason optimized version for VarLong
     */
    @Overwrite
    public static int getVarLongSize(long data) {
        return VarLongUtil.getVarLongLength(data);
    }

    /**
     * @author 404
     * @reason optimized version for VarLong
     */
    @Overwrite
    public FriendlyByteBuf writeVarLong(long value) {
        VarLongUtil.writeVarLongFull(this, value);
        return (FriendlyByteBuf) (Object) this;
    }
}
