package one.pkg.kreno.shared.culling;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import one.pkg.kreno.mixin.accessor.LevelChunkSectionAccessor;

public class BlockCullingUtil {
    private static final ThreadLocal<FriendlyByteBuf> CACHED_BUFFER = ThreadLocal.withInitial(() -> new FriendlyByteBuf(Unpooled.buffer(2048)));
    private static final ThreadLocal<Boolean> HAS_CACHED_DATA = ThreadLocal.withInitial(() -> false);
    private static final BlockState REPLACEMENT = Blocks.STONE.defaultBlockState();

    public static int calculateAndCacheChunkSize(LevelChunk chunk) {
        FriendlyByteBuf buffer = CACHED_BUFFER.get();
        buffer.clear();

        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) {
                section.write(buffer);
                continue;
            }

            PalettedContainer<BlockState> culled = getCulledContainer(chunk, sections, i, minY, maxY);
            if (culled == null) {
                section.write(buffer);
            } else {
                LevelChunkSectionAccessor accessor = (LevelChunkSectionAccessor) section;
                buffer.writeShort(accessor.getNonEmptyBlockCount());
                buffer.writeShort(accessor.getFluidCount());
                culled.write(buffer);
                section.getBiomes().write(buffer);
            }
        }

        HAS_CACHED_DATA.set(true);
        return buffer.writerIndex();
    }

    public static boolean hasCachedData() {
        return HAS_CACHED_DATA.get();
    }

    public static void writeCachedData(FriendlyByteBuf outputBuffer) {
        FriendlyByteBuf cached = CACHED_BUFFER.get();
        cached.readerIndex(0);
        outputBuffer.writeBytes(cached);
        HAS_CACHED_DATA.set(false);
    }

    private static PalettedContainer<BlockState> getCulledContainer(LevelChunk chunk, LevelChunkSection[] sections, int sectionYIndex, int minY, int maxY) {
        LevelChunkSection section = sections[sectionYIndex];
        PalettedContainerRO<BlockState> roContainer = section.getStates();

        if (!(roContainer instanceof PalettedContainer<BlockState> container)) {
            return null;
        }

        int sectionYOffset = chunk.getSectionYFromSectionIndex(sectionYIndex) * 16;

        PalettedContainer<BlockState> culled = null;

        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                for (int y = 0; y < 16; y++) {
                    int worldY = sectionYOffset + y;

                    if (worldY <= minY || worldY >= maxY) {
                        continue;
                    }

                    BlockState state = container.get(x, y, z);
                    if (state.isAir() || !state.isSolidRender()) {
                        continue;
                    }

                    if (isOpaque(sections, minY, x + 1, worldY, z) &&
                        isOpaque(sections, minY, x - 1, worldY, z) &&
                        isOpaque(sections, minY, x, worldY + 1, z) &&
                        isOpaque(sections, minY, x, worldY - 1, z) &&
                        isOpaque(sections, minY, x, worldY, z + 1) &&
                        isOpaque(sections, minY, x, worldY, z - 1)) {

                        if (culled == null) {
                            culled = container.copy();
                        }
                        culled.set(x, y, z, REPLACEMENT);
                    }
                }
            }
        }

        return culled;
    }

    private static boolean isOpaque(LevelChunkSection[] sections, int minY, int localX, int worldY, int localZ) {
        int sectionYIndex = (worldY - minY) >> 4;
        if (sectionYIndex >= 0 && sectionYIndex < sections.length) {
            LevelChunkSection section = sections[sectionYIndex];
            if (!section.hasOnlyAir()) {
                BlockState state = section.getBlockState(localX, worldY & 15, localZ);
                return state.isSolidRender();
            }
        }
        return false;
    }
}
