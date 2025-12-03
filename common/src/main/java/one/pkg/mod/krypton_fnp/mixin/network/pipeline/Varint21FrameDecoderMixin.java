package one.pkg.mod.krypton_fnp.mixin.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import one.pkg.mod.krypton_fnp.shared.ModConfig;
import one.pkg.mod.krypton_fnp.shared.network.VarIntByteDecoder;
import one.pkg.mod.krypton_fnp.shared.network.util.QuietDecoderException;
import net.minecraft.network.Varint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static io.netty.util.ByteProcessor.FIND_NON_NUL;
import static one.pkg.mod.krypton_fnp.shared.network.util.WellKnownExceptions.BAD_LENGTH_CACHED;
import static one.pkg.mod.krypton_fnp.shared.network.util.WellKnownExceptions.VARINT_BIG_CACHED;

/**
 * Overrides the Varint21FrameDecoder to use optimized packet splitting from Velocity 1.1.0. In addition this applies a
 * security fix to stop "nullping" attacks.
 */
@Mixin(value = Varint21FrameDecoder.class)
public class Varint21FrameDecoderMixin {
    @Unique
    private final VarIntByteDecoder reader = new VarIntByteDecoder();

    /**
     * @author Andrew Steinborn
     * @reason Use optimized Velocity varint decoder that reduces bounds checking
     */
    @Inject(method = "decode", at = @At(value = "HEAD"), cancellable = true)
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, CallbackInfo ci) throws Exception {
        if (!ctx.channel().isActive()) {
            in.clear();
            ci.cancel();
            return;
        }


        reader.reset();

        int varintEnd = in.forEachByte(reader);
        if (varintEnd == -1) {
            // We tried to go beyond the end of the buffer. This is probably a good sign that the
            // buffer was too short to hold a proper varint.
            if (reader.getResult() == VarIntByteDecoder.DecodeResult.RUN_OF_ZEROES) {
                // Special case where the entire packet is just a run of zeroes. We ignore them all.
                in.clear();
            }
            ci.cancel();
            return;
        }

        if (reader.getResult() == VarIntByteDecoder.DecodeResult.RUN_OF_ZEROES) {
            // this will return to the point where the next varint starts
            in.readerIndex(varintEnd);
        } else if (reader.getResult() == VarIntByteDecoder.DecodeResult.SUCCESS) {
            int readVarint = reader.getReadVarint();
            int bytesRead = reader.getBytesRead();
            if (readVarint < 0) {
                in.clear();
                throw BAD_LENGTH_CACHED;
            } else if (readVarint == 0) {
                // skip over the empty packet(s) and ignore it
                in.readerIndex(varintEnd + 1);
            } else {
                int minimumRead = bytesRead + readVarint;
                if (in.isReadable(minimumRead)) {
                    out.add(in.retainedSlice(varintEnd + 1, readVarint));
                    in.skipBytes(minimumRead);
                }
            }
        } else if (reader.getResult() == VarIntByteDecoder.DecodeResult.TOO_BIG) {
            in.clear();
            throw VARINT_BIG_CACHED;
        }
        ci.cancel();
    }
}
