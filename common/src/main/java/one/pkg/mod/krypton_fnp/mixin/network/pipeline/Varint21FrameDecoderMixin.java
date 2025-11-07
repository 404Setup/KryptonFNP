package one.pkg.mod.krypton_fnp.mixin.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Varint21FrameDecoder;
import one.pkg.mod.krypton_fnp.shared.network.VarIntByteDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

import static one.pkg.mod.krypton_fnp.shared.network.util.WellKnownExceptions.BAD_LENGTH_CACHED;
import static one.pkg.mod.krypton_fnp.shared.network.util.WellKnownExceptions.VARINT_BIG_CACHED;

/**
 * Overrides the Varint21FrameDecoder to use optimized packet splitting from Velocity 1.1.0. In addition this applies a
 * security fix to stop "nullping" attacks.
 */
@Mixin(Varint21FrameDecoder.class)
public class Varint21FrameDecoderMixin {
    @Unique
    private final VarIntByteDecoder krypton_FNP$reader = new VarIntByteDecoder();

    /**
     * @author Andrew Steinborn
     * @reason Use optimized Velocity varint decoder that reduces bounds checking
     */
    @Overwrite
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!ctx.channel().isActive()) {
            in.clear();
            return;
        }


        krypton_FNP$reader.reset();

        int varintEnd = in.forEachByte(krypton_FNP$reader);
        if (varintEnd == -1) {
            // We tried to go beyond the end of the buffer. This is probably a good sign that the
            // buffer was too short to hold a proper varint.
            if (krypton_FNP$reader.getResult() == VarIntByteDecoder.DecodeResult.RUN_OF_ZEROES) {
                // Special case where the entire packet is just a run of zeroes. We ignore them all.
                in.clear();
            }
            return;
        }

        if (krypton_FNP$reader.getResult() == VarIntByteDecoder.DecodeResult.RUN_OF_ZEROES) {
            // this will return to the point where the next varint starts
            in.readerIndex(varintEnd);
        } else if (krypton_FNP$reader.getResult() == VarIntByteDecoder.DecodeResult.SUCCESS) {
            int readVarint = krypton_FNP$reader.getReadVarint();
            int bytesRead = krypton_FNP$reader.getBytesRead();
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
        } else if (krypton_FNP$reader.getResult() == VarIntByteDecoder.DecodeResult.TOO_BIG) {
            in.clear();
            throw VARINT_BIG_CACHED;
        }
    }
}