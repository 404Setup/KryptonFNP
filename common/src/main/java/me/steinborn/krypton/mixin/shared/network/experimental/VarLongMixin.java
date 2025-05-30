package me.steinborn.krypton.mixin.shared.network.experimental;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VarLong.class)
public class VarLongMixin {
    @Unique
    private static final int MAX_VARLONG_SIZE = 10;
    @Unique
    private static final int DATA_BITS_MASK = 0x7F;
    @Unique
    private static final int CONTINUATION_BIT_MASK = 0x80;
    @Unique
    private static final int DATA_BITS_PER_BYTE = 7;

    @Unique
    private static boolean krypton_Multi$hasContinuationBit(byte data) {
        return (data & CONTINUATION_BIT_MASK) != 0;
    }

    /**
     * @author 404
     * @reason test
     */
    @Overwrite
    public static int getByteSize(long data) {
        if (data == 0) return 1;

        int significantBits = 64 - Long.numberOfLeadingZeros(data);
        return (significantBits + DATA_BITS_PER_BYTE - 1) / DATA_BITS_PER_BYTE;
    }


    /**
     * @author 404
     * @reason test
     */
    @Overwrite
    public static long read(ByteBuf buffer) {
        long result = 0L;
        int bytesRead = 0;

        byte currentByte;
        do {
            if (bytesRead >= MAX_VARLONG_SIZE) {
                throw new RuntimeException("VarLong too big");
            }

            currentByte = buffer.readByte();
            result |= (long) (currentByte & DATA_BITS_MASK) << (bytesRead * DATA_BITS_PER_BYTE);
            bytesRead++;
        } while (krypton_Multi$hasContinuationBit(currentByte));

        return result;
    }
}
