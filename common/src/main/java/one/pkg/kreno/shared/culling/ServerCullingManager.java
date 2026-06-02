package one.pkg.kreno.shared.culling;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.network.TrafficMonitor;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ServerCullingManager {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
    private static final Map<Integer, Map<Integer, CullingState>> VISIBILITY_CACHE = new ConcurrentHashMap<>();

    private static final Object ACTIVE_MAPS_LOCK = new Object();
    @SuppressWarnings("unchecked")
    private static volatile Map<Integer, CullingState>[] activeVisibilityMaps = new Map[0];

    private static final long CHECK_INTERVAL_MS = 500;
    private static final long HIDE_DELAY_MS = 1000;
    private static final long REFRESH_SWEEP_INTERVAL_MS = 250;
    private static final int MAX_REFRESHES_PER_SWEEP = 64;
    private static final int MAX_DROPPED_TRACKED = 4096;
    public static final double NEAR_DISTANCE_SQ = 64.0;

    private static final Map<Integer, Cache<BlockPos, CullingState>> BLOCK_VISIBILITY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<BlockPos>> DROPPED_BLOCK_UPDATES = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> LAST_SWEEP_TIME = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static void updateActiveVisibilityMaps() {
        synchronized (ACTIVE_MAPS_LOCK) {
            activeVisibilityMaps = VISIBILITY_CACHE.values().toArray(new Map[0]);
        }
    }

    public static void onEnd() {
        BLOCK_VISIBILITY_CACHE.values().forEach(Cache::invalidateAll);
        BLOCK_VISIBILITY_CACHE.clear();
        VISIBILITY_CACHE.clear();
        updateActiveVisibilityMaps();
        DROPPED_BLOCK_UPDATES.clear();
        LAST_SWEEP_TIME.clear();
        EXECUTOR.close();
    }

    public static boolean isBlockVisible(ServerPlayer player, BlockPos pos) {
        if (!ModConfig.Culling.isBlockEnabled()) return true;

        Cache<BlockPos, CullingState> cache = BLOCK_VISIBILITY_CACHE.computeIfAbsent(player.getId(), k ->
                CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(1, TimeUnit.MINUTES).build()
        );
        CullingState cachedState = cache.getIfPresent(pos);
        if (cachedState == null) {
            cachedState = new CullingState();
            cache.put(pos.immutable(), cachedState);
        }
        final CullingState state = cachedState;

        long now = System.currentTimeMillis();

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        double ex = player.getX();
        double ey = player.getEyeY();
        double ez = player.getZ();
        double dx = cx - ex, dy = cy - ey, dz = cz - ez;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        state.lastDistanceSq = distanceSq;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            state.lastRaytraceResult = true;
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        Vec3 lookVec = player.getLookAngle();
        double dot = lookVec.x * dx + lookVec.y * dy + lookVec.z * dz;
        boolean inFOV;
        if (dot >= 0) {
            inFOV = true;
        } else {
            inFOV = (dot * dot <= 0.0225 * distanceSq);
        }

        if (inFOV) {
            if (now - state.lastCheckTime > CHECK_INTERVAL_MS) {
                if (!state.isChecking) {
                    state.isChecking = true;
                    state.lastCheckTime = now;
                    AABB aabb = new AABB(pos).inflate(0.1);
                    Level level = player.level();
                    Vec3 eyePos = new Vec3(ex, ey, ez);

                    EXECUTOR.submit(() -> {
                        try {
                            state.lastRaytraceResult = checkAABBVisible(level, eyePos, aabb);
                        } catch (Exception e) {
                            state.lastRaytraceResult = true;
                        } finally {
                            state.isChecking = false;
                        }
                    });
                }
            }
        }

        boolean isVisible = inFOV && state.lastRaytraceResult;

        if (!isVisible) {
            if (state.hiddenSince == 0) {
                state.hiddenSince = now;
            } else if (now - state.hiddenSince > HIDE_DELAY_MS) {
                state.isCurrentlyVisible = false;
            }
        } else {
            state.hiddenSince = 0;
            state.isCurrentlyVisible = true;
        }

        return state.isCurrentlyVisible;
    }

    /**
     * Records that a block update packet was dropped due to culling so that it can be re-sent later
     * once the block becomes visible to the player again.
     *
     * @return {@code true} if the position was recorded (or already tracked) and the caller may safely
     * skip sending the packet; {@code false} if the tracking set is full, in which case the
     * caller MUST send the original packet to the client to avoid losing the update.
     */
    public static boolean recordDroppedBlock(ServerPlayer player, BlockPos pos) {
        Set<BlockPos> set = DROPPED_BLOCK_UPDATES.computeIfAbsent(player.getId(), k -> ConcurrentHashMap.newKeySet());
        BlockPos immutable = pos.immutable();
        if (set.contains(immutable)) return true;
        if (set.size() >= MAX_DROPPED_TRACKED) return false;
        set.add(immutable);
        return true;
    }

    /**
     * Periodically sweeps the dropped-block set for the given player and re-sends a fresh
     * {@link ClientboundBlockUpdatePacket} for any positions that have become visible again.
     * The sweep is rate-limited per player by {@link #REFRESH_SWEEP_INTERVAL_MS}.
     *
     * @param player       the player whose dropped updates to sweep
     * @param directSender a sender that sends the packet directly, bypassing the culling mixin
     */
    public static void maybeProcessPendingRefreshes(ServerPlayer player, Consumer<Packet<?>> directSender) {
        if (!ModConfig.Culling.isBlockEnabled()) return;
        int pid = player.getId();
        Set<BlockPos> set = DROPPED_BLOCK_UPDATES.get(pid);
        if (set == null || set.isEmpty()) return;

        long now = System.currentTimeMillis();
        Long last = LAST_SWEEP_TIME.get(pid);
        if (last != null && now - last < REFRESH_SWEEP_INTERVAL_MS) return;
        LAST_SWEEP_TIME.put(pid, now);

        Level level = player.level();
        int processed = 0;
        Iterator<BlockPos> it = set.iterator();
        while (it.hasNext() && processed < MAX_REFRESHES_PER_SWEEP) {
            BlockPos pos = it.next();
            if (isBlockVisible(player, pos)) {
                BlockState state = level.getBlockState(pos);
                directSender.accept(new ClientboundBlockUpdatePacket(pos, state));
                it.remove();
                processed++;
            }
        }
    }

    /**
     * Re-sends the actual block state of the 6 neighbors of the given position via the supplied
     * direct sender. This is intended to recover from chunk-level block culling that may have
     * replaced surrounded blocks with stone in the original chunk packet.
     */
    public static void refreshAdjacentBlocks(ServerPlayer player, BlockPos pos, Consumer<Packet<?>> directSender) {
        if (!ModConfig.Culling.isChunkBlockCullingEnabled()) return;
        Level level = player.level();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            cursor.setWithOffset(pos, dir);
            BlockState neighborState = level.getBlockState(cursor);
            if (!neighborState.isAir() && neighborState.isSolidRender()) {
                BlockPos immutable = cursor.immutable();
                directSender.accept(new ClientboundBlockUpdatePacket(immutable, neighborState));
                Set<BlockPos> dropped = DROPPED_BLOCK_UPDATES.get(player.getId());
                if (dropped != null) dropped.remove(immutable);
            }
        }
    }

    public static boolean isEntityVisible(ServerPlayer player, Entity entity) {
        if (!ModConfig.Culling.isEntityEnabled() || !player.level().getServer().isDedicatedServer()) return true;

        int playerId = player.getId();
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(playerId);
        if (map == null) {
            Map<Integer, CullingState> newMap = new ConcurrentHashMap<>();
            map = VISIBILITY_CACHE.putIfAbsent(playerId, newMap);
            if (map == null) {
                map = newMap;
                updateActiveVisibilityMaps();
            }
        }
        CullingState state = map.computeIfAbsent(entity.getId(), k -> new CullingState());

        long now = System.currentTimeMillis();
        double distanceSq = player.distanceToSqr(entity);
        state.lastDistanceSq = distanceSq;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            state.lastRaytraceResult = true;
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 entityCenter = entity.getBoundingBox().getCenter();
        double dx = entityCenter.x - eyePos.x;
        double dy = entityCenter.y - eyePos.y;
        double dz = entityCenter.z - eyePos.z;
        Vec3 lookVec = player.getLookAngle();
        double dot = lookVec.x * dx + lookVec.y * dy + lookVec.z * dz;
        boolean inFOV;
        if (dot >= 0) {
            inFOV = true;
        } else {
            inFOV = (dot * dot <= 0.0225 * distanceSq);
        }

        if (inFOV) {
            if (now - state.lastCheckTime > CHECK_INTERVAL_MS) {
                if (!state.isChecking) {
                    state.isChecking = true;
                    state.lastCheckTime = now;
                    queueEntityCheck(player, entity, state);
                }
            }
        }

        boolean isVisible = inFOV && state.lastRaytraceResult;

        if (!isVisible) {
            if (state.hiddenSince == 0) {
                state.hiddenSince = now;
            } else if (now - state.hiddenSince > HIDE_DELAY_MS) {
                state.isCurrentlyVisible = false;
            }
        } else {
            state.hiddenSince = 0;
            state.isCurrentlyVisible = true;
        }

        return state.isCurrentlyVisible;
    }

    public static boolean getLastSentVisible(ServerPlayer player, Entity entity) {
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(player.getId());
        if (map == null) return true;
        CullingState state = map.get(entity.getId());
        if (state == null) return true;
        return state.lastSentVisible;
    }

    public static void setLastSentVisible(ServerPlayer player, Entity entity, boolean visible) {
        int playerId = player.getId();
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(playerId);
        if (map == null) {
            Map<Integer, CullingState> newMap = new ConcurrentHashMap<>();
            map = VISIBILITY_CACHE.putIfAbsent(playerId, newMap);
            if (map == null) {
                map = newMap;
                updateActiveVisibilityMaps();
            }
        }
        CullingState state = map.computeIfAbsent(entity.getId(), k -> new CullingState());
        state.lastSentVisible = visible;
    }

    public static void removePlayerEntityState(ServerPlayer player, Entity entity) {
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(player.getId());
        if (map != null) {
            map.remove(entity.getId());
        }
    }

    private static void queueEntityCheck(ServerPlayer player, Entity entity, CullingState state) {
        Vec3 eyePos = player.getEyePosition();
        AABB aabb = entity.getBoundingBox().inflate(0.5);
        Level level = player.level();

        EXECUTOR.submit(() -> {
            try {
                state.lastRaytraceResult = checkAABBVisible(level, eyePos, aabb);
            } catch (Exception e) {
                state.lastRaytraceResult = true;
            } finally {
                state.isChecking = false;
            }
        });
    }

    public static boolean checkAABBVisible(Level level, Vec3 eye, AABB aabb) {
        Vec3 center = aabb.getCenter();
        if (isLineOfSightClear(level, eye, center)) return true;

        if (isLineOfSightClear(level, eye, new Vec3(center.x, aabb.maxY, center.z))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(center.x, aabb.minY, center.z))) return true;

        if (isLineOfSightClear(level, eye, new Vec3(aabb.minX, aabb.maxY, aabb.minZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.minX, aabb.minY, aabb.maxZ))) return true;
        return isLineOfSightClear(level, eye, new Vec3(aabb.maxX, aabb.minY, aabb.minZ));
    }

    public static boolean isLineOfSightClear(Level level, Vec3 start, Vec3 end) {
        try {
            int chunkX = (int) end.x >> 4;
            int chunkZ = (int) end.z >> 4;
            ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk == null) return true;

            ClipContext ctx = new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty());
            return level.clip(ctx).getType() == HitResult.Type.MISS;
        } catch (Throwable t) {
            return true;
        }
    }

    public static void estimateParticlePacketOptSavings(Entity entity, int estimatedBytes) {
        if (entity.level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(entity) < 4096) {
                    TrafficMonitor.onDroppedPacket(p.getUUID(), "particlePacketOpt", estimatedBytes);
                }
            }
        }
    }

    public static void removePlayer(ServerPlayer player) {
        int pid = player.getId();
        if (VISIBILITY_CACHE.remove(pid) != null) {
            updateActiveVisibilityMaps();
        }
        BLOCK_VISIBILITY_CACHE.remove(pid);
        DROPPED_BLOCK_UPDATES.remove(pid);
        LAST_SWEEP_TIME.remove(pid);
    }

    public static void removeEntity(Entity entity) {
        Integer id = entity.getId();
        Map<Integer, CullingState>[] maps = activeVisibilityMaps;
        for (int i = 0; i < maps.length; i++) {
            maps[i].remove(id);
        }
    }

    private static class CullingState {
        volatile boolean isCurrentlyVisible = true;
        volatile boolean lastRaytraceResult = true;
        volatile long lastCheckTime = 0;
        volatile long hiddenSince = 0;
        volatile boolean isChecking = false;
        volatile double lastDistanceSq = 0;
        volatile boolean lastSentVisible = true;
    }
}
