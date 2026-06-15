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
    public static final double NEAR_DISTANCE_SQ = 64.0;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final ExecutorService EXECUTOR = Executors.newWorkStealingPool();
    private static final Map<Integer, Map<Integer, CullingState>> VISIBILITY_CACHE = new ConcurrentHashMap<>();
    private static final Object ACTIVE_MAPS_LOCK = new Object();
    private static final long CHECK_INTERVAL_MS = 500;
    private static final long HIDE_DELAY_MS = 1000;
    private static final long REFRESH_SWEEP_INTERVAL_MS = 250;
    private static final int MAX_REFRESHES_PER_SWEEP = 64;
    private static final int MAX_DROPPED_TRACKED = 4096;
    private static final Map<Integer, Cache<Long, CullingState>> BLOCK_VISIBILITY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<BlockPos>> DROPPED_BLOCK_UPDATES = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> LAST_SWEEP_TIME = new ConcurrentHashMap<>();
    private static final Map<Integer, ParticleCullCache> PARTICLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, EntityCullCache> ENTITY_CACHE = new ConcurrentHashMap<>();
    @SuppressWarnings("unchecked")
    private static volatile Map<Integer, CullingState>[] activeVisibilityMaps = new Map[0];

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
        PARTICLE_CACHE.clear();
        ENTITY_CACHE.clear();
        EXECUTOR.close();
    }

    public static boolean isBlockVisible(ServerPlayer player, BlockPos pos) {
        if (!ModConfig.Culling.isBlockEnabled()) return true;

        Integer playerId = player.getId();
        Cache<Long, CullingState> cache = BLOCK_VISIBILITY_CACHE.get(playerId);
        if (cache == null) {
            cache = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(1, TimeUnit.MINUTES).build();
            Cache<Long, CullingState> existing = BLOCK_VISIBILITY_CACHE.putIfAbsent(playerId, cache);
            if (existing != null) {
                cache = existing;
            }
        }

        long posLong = pos.asLong();
        CullingState cachedState = cache.getIfPresent(posLong);
        if (cachedState == null) {
            cachedState = new CullingState();
            cache.put(posLong, cachedState);
        }
        final CullingState state = cachedState;

        long now = System.currentTimeMillis();

        float cx = pos.getX() + 0.5f;
        float cy = pos.getY() + 0.5f;
        float cz = pos.getZ() + 0.5f;
        float ex = (float) player.getX();
        float ey = (float) player.getEyeY();
        float ez = (float) player.getZ();
        float rotX = player.getXRot();
        float rotY = player.getYRot();

        if (Math.abs(state.lastPx - ex) < 0.1f && Math.abs(state.lastPy - ey) < 0.1f && Math.abs(state.lastPz - ez) < 0.1f &&
                Math.abs(state.lastTx - cx) < 0.1f && Math.abs(state.lastTy - cy) < 0.1f && Math.abs(state.lastTz - cz) < 0.1f &&
                Math.abs(state.lastRotX - rotX) < 1.0f && Math.abs(state.lastRotY - rotY) < 1.0f) {
            return state.isCurrentlyVisible;
        }

        state.lastPx = ex;
        state.lastPy = ey;
        state.lastPz = ez;
        state.lastTx = cx;
        state.lastTy = cy;
        state.lastTz = cz;
        state.lastRotX = rotX;
        state.lastRotY = rotY;

        float dx = cx - ex, dy = cy - ey, dz = cz - ez;
        float distanceSq = dx * dx + dy * dy + dz * dz;
        state.lastDistanceSq = distanceSq;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            state.lastRaytraceResult = true;
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        float f = rotX * ((float) Math.PI / 180F);
        float g = -rotY * ((float) Math.PI / 180F);
        float h = (float) Math.cos(g);
        float i = (float) Math.sin(g);
        float j = (float) Math.cos(f);
        float k = (float) Math.sin(f);
        double lVx = (i * j);
        double lVy = (-k);
        double lVz = (h * j);

        double dot = lVx * dx + lVy * dy + lVz * dz;
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
                    Vec3 rayEyePos = new Vec3(ex, ey, ez);

                    if (ModConfig.Culling.isAsyncMode()) {
                        EXECUTOR.submit(() -> {
                            try {
                                state.lastRaytraceResult = checkAABBVisible(level, rayEyePos, aabb);
                            } catch (Exception e) {
                                state.lastRaytraceResult = true;
                            } finally {
                                state.isChecking = false;
                            }
                        });
                    } else {
                        try {
                            state.lastRaytraceResult = checkAABBVisible(level, rayEyePos, aabb);
                        } catch (Exception e) {
                            state.lastRaytraceResult = true;
                        } finally {
                            state.isChecking = false;
                        }
                    }
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

        if (state.isCurrentlyVisible) {
            EntityCullCache hashCache = ENTITY_CACHE.get(playerId);
            if (hashCache != null) {
                int gridX = (int) Math.floor(cx / 8.0);
                int gridY = (int) Math.floor(cy / 8.0);
                int gridZ = (int) Math.floor(cz / 8.0);
                long gridKey = ((long) (gridX & 0x3FFFFF) << 42) | ((long) (gridY & 0xFFFFF) << 22) | (gridZ & 0x3FFFFF);
                hashCache.grid.put(gridKey, true);
            }
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
        for (Direction dir : DIRECTIONS) {
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
        return isEntityVisible(player, entity, System.currentTimeMillis());
    }

    public static boolean isEntityVisible(ServerPlayer player, Entity entity, long now) {
        if (!ModConfig.Culling.isEntityEnabled() || !player.level().getServer().isDedicatedServer()) return true;

        Integer playerId = player.getId();
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(playerId);
        if (map == null) {
            Map<Integer, CullingState> newMap = new ConcurrentHashMap<>();
            map = VISIBILITY_CACHE.putIfAbsent(playerId, newMap);
            if (map == null) {
                map = newMap;
                updateActiveVisibilityMaps();
            }
        }
        Integer entityId = entity.getId();
        CullingState state = map.get(entityId);
        if (state == null) {
            state = new CullingState();
            map.put(entityId, state);
        }

        float ex = (float) player.getX();
        float ey = (float) player.getEyeY();
        float ez = (float) player.getZ();
        float cx = (float) entity.getX();
        float cy = (float) (entity.getY() + entity.getBbHeight() / 2.0);
        float cz = (float) entity.getZ();
        float rotX = player.getXRot();
        float rotY = player.getYRot();

        if (Math.abs(state.lastPx - ex) < 0.1f && Math.abs(state.lastPy - ey) < 0.1f && Math.abs(state.lastPz - ez) < 0.1f &&
                Math.abs(state.lastTx - cx) < 0.1f && Math.abs(state.lastTy - cy) < 0.1f && Math.abs(state.lastTz - cz) < 0.1f &&
                Math.abs(state.lastRotX - rotX) < 1.0f && Math.abs(state.lastRotY - rotY) < 1.0f) {
            return state.isCurrentlyVisible;
        }

        state.lastPx = ex;
        state.lastPy = ey;
        state.lastPz = ez;
        state.lastTx = cx;
        state.lastTy = cy;
        state.lastTz = cz;
        state.lastRotX = rotX;
        state.lastRotY = rotY;

        float distanceSq = (float) player.distanceToSqr(entity);
        state.lastDistanceSq = distanceSq;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            state.lastRaytraceResult = true;
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        EntityCullCache hashCache = ENTITY_CACHE.computeIfAbsent(playerId, k -> new EntityCullCache());
        long tickCount = player.level().getServer().getTickCount();
        if (hashCache.lastTick != tickCount) {
            hashCache.grid.clear();
            hashCache.lastTick = tickCount;
        }

        int gridX = (int) Math.floor(cx / 8.0);
        int gridY = (int) Math.floor(cy / 8.0);
        int gridZ = (int) Math.floor(cz / 8.0);
        long gridKey = ((long) (gridX & 0x3FFFFF) << 42) | ((long) (gridY & 0xFFFFF) << 22) | (gridZ & 0x3FFFFF);

        Boolean cellVisible = hashCache.grid.get(gridKey);
        if (cellVisible != null && cellVisible) {
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        float dx = cx - ex;
        float dy = cy - ey;
        float dz = cz - ez;

        float f = rotX * ((float) Math.PI / 180F);
        float g = -rotY * ((float) Math.PI / 180F);
        float h = (float) Math.cos(g);
        float i = (float) Math.sin(g);
        float j = (float) Math.cos(f);
        float k = (float) Math.sin(f);
        double lVx = (i * j);
        double lVy = (-k);
        double lVz = (h * j);

        double dot = lVx * dx + lVy * dy + lVz * dz;
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

        if (state.isCurrentlyVisible) {
            hashCache.grid.put(gridKey, true);
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
        Integer playerId = player.getId();
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(playerId);
        if (map == null) {
            Map<Integer, CullingState> newMap = new ConcurrentHashMap<>();
            map = VISIBILITY_CACHE.putIfAbsent(playerId, newMap);
            if (map == null) {
                map = newMap;
                updateActiveVisibilityMaps();
            }
        }
        Integer entityId = entity.getId();
        CullingState state = map.get(entityId);
        if (state == null) {
            state = new CullingState();
            map.put(entityId, state);
        }
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

        if (ModConfig.Culling.isAsyncMode()) {
            EXECUTOR.submit(() -> {
                try {
                    state.lastRaytraceResult = checkAABBVisible(level, eyePos, aabb);
                } catch (Exception e) {
                    state.lastRaytraceResult = true;
                } finally {
                    state.isChecking = false;
                }
            });
        } else {
            try {
                state.lastRaytraceResult = checkAABBVisible(level, eyePos, aabb);
            } catch (Exception e) {
                state.lastRaytraceResult = true;
            } finally {
                state.isChecking = false;
            }
        }
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
            for (ServerPlayer p : sl.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false)) {
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
        PARTICLE_CACHE.remove(pid);
    }

    public static void removeEntity(Entity entity) {
        Integer id = entity.getId();
        for (int i = 0; i < activeVisibilityMaps.length; i++) {
            activeVisibilityMaps[i].remove(id);
        }
    }

    public static boolean isParticleVisible(ServerPlayer player, double x, double y, double z) {
        Integer playerId = player.getId();
        ParticleCullCache cache = PARTICLE_CACHE.get(playerId);
        if (cache == null) {
            cache = new ParticleCullCache();
            PARTICLE_CACHE.put(playerId, cache);
        }

        Vec3 eyePos = player.getEyePosition();
        double dx = x - eyePos.x;
        double dy = y - eyePos.y;
        double dz = z - eyePos.z;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        if (distanceSq < NEAR_DISTANCE_SQ) {
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

        if (!inFOV) return false;

        float fx = (float) x;
        float fy = (float) y;
        float fz = (float) z;

        if (Math.abs(cache.lastX - fx) < 1.0f && Math.abs(cache.lastY - fy) < 1.0f && Math.abs(cache.lastZ - fz) < 1.0f) {
            return cache.lastResult;
        }

        boolean result = isLineOfSightClear(player.level(), eyePos, new Vec3(x, y, z));

        cache.lastX = fx;
        cache.lastY = fy;
        cache.lastZ = fz;
        cache.lastResult = result;

        return result;
    }

    private static class ParticleCullCache {
        float lastX = Float.MAX_VALUE;
        float lastY = Float.MAX_VALUE;
        float lastZ = Float.MAX_VALUE;
        boolean lastResult = true;
    }

    private static class EntityCullCache {
        final Map<Long, Boolean> grid = new ConcurrentHashMap<>();
        long lastTick = -1;
    }

    private static class CullingState {
        boolean isCurrentlyVisible = true;
        volatile boolean lastRaytraceResult = true;
        long lastCheckTime = 0;
        long hiddenSince = 0;
        volatile boolean isChecking = false;
        float lastDistanceSq = 0;
        boolean lastSentVisible = true;

        float lastPx = Float.MAX_VALUE;
        float lastPy = Float.MAX_VALUE;
        float lastPz = Float.MAX_VALUE;
        float lastTx = Float.MAX_VALUE;
        float lastTy = Float.MAX_VALUE;
        float lastTz = Float.MAX_VALUE;
        float lastRotX = Float.MAX_VALUE;
        float lastRotY = Float.MAX_VALUE;
    }
}
