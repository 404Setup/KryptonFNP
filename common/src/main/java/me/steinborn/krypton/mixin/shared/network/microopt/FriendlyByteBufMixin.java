package me.steinborn.krypton.mixin.shared.network.microopt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import me.steinborn.krypton.mod.shared.network.util.VarIntUtil;
import me.steinborn.krypton.mod.shared.network.util.VarLongUtil;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Mixin(value = FriendlyByteBuf.class, priority = 900)
public abstract class FriendlyByteBufMixin extends ByteBuf {
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
     * @author 404
     * @reason optimized version for VarLong
     */
    @Overwrite
    public static int getVarLongSize(long data) {
        return VarLongUtil.getVarLongLength(data);
    }

    /**
     * @author Andrew
     * @reason Use {@link ByteBuf#writeCharSequence(CharSequence, Charset)} instead for improved performance along with
     * computing the byte size ahead of time with {@link ByteBufUtil#utf8Bytes(CharSequence)}
     */
    @Overwrite
    public FriendlyByteBuf writeUtf(String string, int maxLength) {
        if (string.length() > maxLength) {
            throw new EncoderException("String too big (was " + string.length() + " characters, max " + maxLength + ")");
        }
        int utf8Bytes = ByteBufUtil.utf8Bytes(string);
        if (utf8Bytes > maxLength * 3) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + (maxLength * 3) + ")");
        } else {
            this.writeVarInt(utf8Bytes);
            this.writeCharSequence(string, StandardCharsets.UTF_8);
            return (FriendlyByteBuf) (Object) this;
        }
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

    /**
     * @author 404
     * @reason optimized version for VarLong
     */
    @Overwrite
    public FriendlyByteBuf writeVarLong(long value) {
        if ((value & VarLongUtil.MASK_7_BITS) == 0L) {
            this.source.writeByte((int) value);
        } else if ((value & VarLongUtil.MASK_14_BITS) == 0L) {
            this.source.writeShort((int) ((value & 0x7FL) | 0x80L) << 8 | (int) (value >>> 7));
        } else {
            VarLongUtil.writeVarLongFull(this, value);
        }
        return (FriendlyByteBuf) (Object) this;
    }
}
