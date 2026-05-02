package one.pkg.kreno.mixin.network.microopt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Mixin(value = FriendlyByteBuf.class, priority = 900)
public abstract class FriendlyByteBufUtfMixin extends ByteBuf {
    @Shadow
    @Final
    private ByteBuf source;

    @Shadow
    public FriendlyByteBuf writeVarInt(int input) {
        throw new UnsupportedOperationException("Mixin method not implemented");
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
}
