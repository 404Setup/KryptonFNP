package one.pkg.kreno.shared.culling;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import one.pkg.kreno.shared.ModConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerCullingManager {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
    private static final Map<Integer, Map<Integer, CullingState>> VISIBILITY_CACHE = new ConcurrentHashMap<>();

    private static final long CHECK_INTERVAL_MS = 500;
    private static final long HIDE_DELAY_MS = 1000;
    private static final Map<Integer, Cache<BlockPos, CullingState>> BLOCK_VISIBILITY_CACHE = new ConcurrentHashMap<>();

    public static void onEnd() {
        BLOCK_VISIBILITY_CACHE.values().forEach(Cache::invalidateAll);
        BLOCK_VISIBILITY_CACHE.clear();
        VISIBILITY_CACHE.clear();
        EXECUTOR.close();
    }

    public static boolean isBlockVisible(ServerPlayer player, BlockPos pos) {
        if (!ModConfig.Culling.isBlockEnabled() || !player.level().getServer().isDedicatedServer()) return true;

        Cache<BlockPos, CullingState> cache = BLOCK_VISIBILITY_CACHE.computeIfAbsent(player.getId(), k ->
                CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(1, TimeUnit.MINUTES).build()
        );
        CullingState cachedState = cache.getIfPresent(pos);
        if (cachedState == null) {
            cachedState = new CullingState();
            cache.put(pos, cachedState);
        }
        final CullingState state = cachedState;

        long now = System.currentTimeMillis();

        Vec3 blockCenter = Vec3.atCenterOf(pos);
        state.lastDistanceSq = player.getEyePosition().distanceToSqr(blockCenter);

        if (now - state.lastCheckTime > CHECK_INTERVAL_MS) {
            if (!state.isChecking) {
                state.isChecking = true;
                state.lastCheckTime = now;
                AABB aabb = new AABB(pos).inflate(0.1);
                Level level = player.level();
                Vec3 eyePos = player.getEyePosition();

                EXECUTOR.submit(() -> {
                    try {
                        state.lastResult = checkAABBVisible(level, eyePos, aabb);
                    } catch (Exception e) {
                        state.lastResult = true;
                    } finally {
                        state.isChecking = false;
                    }
                });
            }
        }

        if (!state.lastResult) {
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

    public static boolean isEntityVisible(ServerPlayer player, Entity entity) {
        if (!ModConfig.Culling.isEntityEnabled() || !player.level().getServer().isDedicatedServer()) return true;

        Map<Integer, CullingState> map = VISIBILITY_CACHE.computeIfAbsent(player.getId(), _ -> new ConcurrentHashMap<>());
        CullingState state = map.computeIfAbsent(entity.getId(), k -> new CullingState());

        long now = System.currentTimeMillis();
        state.lastDistanceSq = player.distanceToSqr(entity);

        if (now - state.lastCheckTime > CHECK_INTERVAL_MS) {
            if (!state.isChecking) {
                state.isChecking = true;
                state.lastCheckTime = now;
                queueEntityCheck(player, entity, state);
            }
        }

        if (!state.lastResult) {
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
        Map<Integer, CullingState> map = VISIBILITY_CACHE.computeIfAbsent(player.getId(), k -> new ConcurrentHashMap<>());
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
                state.lastResult = checkAABBVisible(level, eyePos, aabb);
            } catch (Exception e) {
                state.lastResult = true;
            } finally {
                state.isChecking = false;
            }
        });
    }

    public static boolean checkAABBVisible(Level level, Vec3 eye, AABB aabb) {
        Vec3 center = aabb.getCenter();
        if (isLineOfSightClear(level, eye, center)) return true;

        if (isLineOfSightClear(level, eye, new Vec3(aabb.minX, aabb.minY, aabb.minZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.minX, aabb.minY, aabb.maxZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.minX, aabb.maxY, aabb.minZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.minX, aabb.maxY, aabb.maxZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.maxX, aabb.minY, aabb.minZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.maxX, aabb.minY, aabb.maxZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.maxX, aabb.maxY, aabb.minZ))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ))) return true;

        if (isLineOfSightClear(level, eye, new Vec3(aabb.minX, center.y, center.z))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(aabb.maxX, center.y, center.z))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(center.x, aabb.minY, center.z))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(center.x, aabb.maxY, center.z))) return true;
        if (isLineOfSightClear(level, eye, new Vec3(center.x, center.y, aabb.minZ))) return true;
        return isLineOfSightClear(level, eye, new Vec3(center.x, center.y, aabb.maxZ));
    }

    private static boolean isLineOfSightClear(Level level, Vec3 start, Vec3 end) {
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

    public static void removePlayer(ServerPlayer player) {
        VISIBILITY_CACHE.remove(player.getId());
        BLOCK_VISIBILITY_CACHE.remove(player.getId());
    }

    public static void removeEntity(Entity entity) {
        int id = entity.getId();
        for (Map<Integer, CullingState> map : VISIBILITY_CACHE.values()) {
            map.remove(id);
        }
    }

    private static class CullingState {
        volatile boolean isCurrentlyVisible = true;
        volatile boolean lastResult = true;
        volatile long lastCheckTime = 0;
        volatile long hiddenSince = 0;
        volatile boolean isChecking = false;
        volatile double lastDistanceSq = 0;
        volatile boolean lastSentVisible = true;
    }
}
