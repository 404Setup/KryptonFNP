package one.pkg.kfnp.mixin.network.microopt;


import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import one.pkg.kfnp.shared.network.util.VarIntUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FriendlyByteBuf.class, priority = 900)
public abstract class FriendlyByteBufVarIntMixin extends ByteBuf {
    @Shadow
    @Final
    private ByteBuf source;

    /**
     * @author Andrew
     * @reason Use optimized VarInt byte size lookup table
     */
    @Overwrite
    public static int getVarIntSize(int v) {
        return VarIntUtil.getVarIntLength(v);
    }

    /**
     * @author Andrew
     * @reason optimized VarInt writing
     */
    @Overwrite
    public FriendlyByteBuf writeVarInt(int value) {
        // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
        // that the server will send, to improve inlining.
        if ((value & VarIntUtil.MASK_7_BITS) == 0) {
            this.source.writeByte(value);
        } else if ((value & VarIntUtil.MASK_14_BITS) == 0) {
            this.source.writeShort((value & 0x7F | 0x80) << 8 | (value >>> 7));
        } else if ((value & VarIntUtil.MASK_21_BITS) == 0) {
            this.source.writeMedium((value & 0x7F | 0x80) << 16
                    | ((value >>> 7) & 0x7F | 0x80) << 8
                    | (value >>> 14));
        } else if ((value & VarIntUtil.MASK_28_BITS) == 0) {
            this.source.writeInt((value & 0x7F | 0x80) << 24
                    | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8
                    | (value >>> 21));
        } else {
            this.source.writeInt((value & 0x7F | 0x80) << 24
                    | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8
                    | ((value >>> 21) & 0x7F | 0x80));
            this.source.writeByte(value >>> 28);
        }
        return (FriendlyByteBuf) (Object) this;
    }

}
