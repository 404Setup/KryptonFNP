package me.steinborn.krypton.mixin.shared.network.experimental;

import net.minecraft.network.VarLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = VarLong.class, priority = 900)
public class VarLongMixin {
    @Unique
    private static final long[] SIZE_MASKS = {
            0L,
            -1L << 7,
            -1L << 14,
            -1L << 21,
            -1L << 28,
            -1L << 35,
            -1L << 42,
            -1L << 49,
            -1L << 56,
            -1L << 63
    };

    /**
     * @author 404
     * @reason test
     */
    @Overwrite
    public static int getByteSize(long data) {
        for (int i = 1; i < 10; i++) {
            if ((data & SIZE_MASKS[i]) == 0L) {
                return i;
            }
        }
        return 10;
    }
}