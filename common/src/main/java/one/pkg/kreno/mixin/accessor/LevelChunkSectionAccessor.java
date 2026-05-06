package one.pkg.kreno.mixin.accessor;

import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelChunkSection.class)
public interface LevelChunkSectionAccessor {
    @Accessor("nonEmptyBlockCount")
    short getNonEmptyBlockCount();

    @Accessor("fluidCount")
    short getFluidCount();
}
