package one.pkg.kreno.shared.culling;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import one.pkg.kreno.mixin.accessor.LevelChunkSectionAccessor;

public class BlockCullingUtil {
    private static final ThreadLocal<FriendlyByteBuf> CACHED_BUFFER = ThreadLocal.withInitial(() -> new FriendlyByteBuf(Unpooled.buffer(2048)));
    private static final ThreadLocal<Boolean> HAS_CACHED_DATA = ThreadLocal.withInitial(() -> false);
    private static final BlockState DEFAULT_REPLACEMENT = Blocks.STONE.defaultBlockState();
    private static final BlockState NETHERRACK = Blocks.NETHERRACK.defaultBlockState();
    private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();

    public static int calculateAndCacheChunkSize(LevelChunk chunk) {
        FriendlyByteBuf buffer = CACHED_BUFFER.get();
        buffer.clear();

        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();

        NeighborChunks neighbors = new NeighborChunks(chunk);

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) {
                section.write(buffer);
                continue;
            }

            PalettedContainer<BlockState> culled = getCulledContainer(chunk, sections, neighbors, i, minY, maxY);
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

    private static BlockState pickAdaptiveReplacement(PalettedContainer<BlockState> container) {
        for (int i = 0; i < 8; i++) {
            BlockState state = container.get((i & 1) * 15, (i & 2) == 0 ? 0 : 15, (i & 4) == 0 ? 0 : 15);
            if (!state.isAir() && state.isSolidRender()) {
                return state;
            }
        }
        BlockState center = container.get(7, 7, 7);
        if (!center.isAir() && center.isSolidRender()) return center;
        return null;
    }

    private static PalettedContainer<BlockState> getCulledContainer(
            LevelChunk chunk, LevelChunkSection[] sections,
            NeighborChunks neighbors,
            int sectionYIndex, int minY, int maxY
    ) {
        LevelChunkSection section = sections[sectionYIndex];
        PalettedContainerRO<BlockState> roContainer = section.getStates();

        if (!(roContainer instanceof PalettedContainer<BlockState> container)) {
            return null;
        }

        int sectionYOffset = chunk.getSectionYFromSectionIndex(sectionYIndex) * 16;

        BlockState adaptive = pickAdaptiveReplacement(container);
        BlockState replacement = adaptive != null ? adaptive : pickReplacement(chunk.getLevel(), sectionYOffset);

        PalettedContainer<BlockState> culled = null;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    int worldY = sectionYOffset + y;

                    if (worldY <= minY || worldY >= maxY - 1) {
                        continue;
                    }

                    BlockState state = container.get(x, y, z);
                    if (state.isAir() || !state.isSolidRender()) {
                        continue;
                    }

                    if (isOpaque(sections, neighbors, minY, x + 1, worldY, z) &&
                            isOpaque(sections, neighbors, minY, x - 1, worldY, z) &&
                            isOpaque(sections, neighbors, minY, x, worldY + 1, z) &&
                            isOpaque(sections, neighbors, minY, x, worldY - 1, z) &&
                            isOpaque(sections, neighbors, minY, x, worldY, z + 1) &&
                            isOpaque(sections, neighbors, minY, x, worldY, z - 1)) {

                        if (culled == null) {
                            culled = container.copy();
                        }

                        if (state != replacement) {
                            culled.set(x, y, z, replacement);
                        }
                    }
                }
            }
        }

        return culled;
    }

    /**
     * Returns true if the block at (localX, worldY, localZ) — possibly outside of the source chunk in
     * the X or Z axis (but never both at once for our use-case) — is fully solid-render and therefore
     * blocks line of sight from outside.
     */
    private static boolean isOpaque(LevelChunkSection[] sections, NeighborChunks neighbors,
                                    int minY, int localX, int worldY, int localZ) {
        int sectionYIndex = (worldY - minY) >> 4;
        if (sectionYIndex < 0) return false;

        LevelChunkSection[] targetSections = sections;
        int lx = localX;
        int lz = localZ;
        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) {
            LevelChunk neighbor = neighbors.get(lx, lz);
            if (neighbor == null) {
                // Neighbor chunk not loaded — be conservative and don't cull this edge block.
                return false;
            }
            targetSections = neighbor.getSections();
            lx = lx & 15;
            lz = lz & 15;
        }

        if (sectionYIndex >= targetSections.length) return false;
        LevelChunkSection section = targetSections[sectionYIndex];
        if (section.hasOnlyAir()) return false;

        BlockState state = section.getBlockState(lx, worldY & 15, lz);
        return state.isSolidRender();
    }

    private static BlockState pickReplacement(Level level, int sectionYOffset) {
        if (level == null) return DEFAULT_REPLACEMENT;
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.NETHER) return NETHERRACK;
        if (dim == Level.END) return END_STONE;

        return sectionYOffset < 0 ? DEEPSLATE : DEFAULT_REPLACEMENT;
    }

    /**
     * Lazy holder of cardinal neighbor chunks used for boundary opacity look-ups.
     * Only the four cardinal directions are needed because neighbor sampling never
     * goes diagonally outside the source chunk simultaneously in X and Z.
     */
    private static final class NeighborChunks {
        private final LevelChunk source;
        private final LevelChunk[] cache = new LevelChunk[4];
        private final boolean[] resolved = new boolean[4];

        NeighborChunks(LevelChunk source) {
            this.source = source;
        }

        LevelChunk get(int localX, int localZ) {
            int idx;
            if (localX < 0) idx = 0;
            else if (localX >= 16) idx = 1;
            else if (localZ < 0) idx = 2;
            else idx = 3;
            if (!resolved[idx]) {
                resolved[idx] = true;
                int dx = idx == 0 ? -1 : idx == 1 ? 1 : 0;
                int dz = idx == 2 ? -1 : idx == 3 ? 1 : 0;
                Level level = source.getLevel();
                if (level != null) {
                    try {
                        int cx = (source.getPos().getMinBlockX() >> 4) + dx;
                        int cz = (source.getPos().getMinBlockZ() >> 4) + dz;
                        ChunkAccess ca = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                        if (ca instanceof LevelChunk lc) {
                            cache[idx] = lc;
                        }
                    } catch (Throwable ignored) {
                        // chunk lookup may be unsafe off-thread; treat as unloaded.
                    }
                }
            }
            return cache[idx];
        }
    }
}
