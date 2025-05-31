package me.steinborn.krypton.mixin.shared.network.microopt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import me.steinborn.krypton.mod.shared.network.util.VarIntUtil;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin extends ByteBuf {
    @Unique
    private static final int DATA_BITS_PER_BYTE = 7;
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
     * @reason test
     */
    @Overwrite
    public static int getVarLongSize(long data) {
        if (data == 0) return 1;

        int significantBits = 64 - Long.numberOfLeadingZeros(data);
        return (significantBits + DATA_BITS_PER_BYTE - 1) / DATA_BITS_PER_BYTE;
    }

    @Unique
    private static void krypton_Multi$writeVarIntFull(ByteBuf buf, int value) {
        // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((value & (0xFFFFFFFF << 21)) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }

    @Invoker("writeCharSequence")
    public abstract int writeCharSequence(CharSequence charSequence, Charset charset);

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
            return new FriendlyByteBuf(source);
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
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            source.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            source.writeShort(w);
        } else {
            krypton_Multi$writeVarIntFull(source, value);
        }
        return new FriendlyByteBuf(source);
    }
}
