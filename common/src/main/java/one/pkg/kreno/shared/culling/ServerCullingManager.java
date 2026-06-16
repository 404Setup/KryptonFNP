package one.pkg.kreno.shared.culling;

import it.unimi.dsi.fastutil.longs.LongArraySet;
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
import one.pkg.tinyutils.map.WeakConcurrentHashMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ServerCullingManager {
    public static final double NEAR_DISTANCE_SQ = 64.0;
    private static final float DEG_TO_RAD = (float) Math.PI / 180F;
    private static final Map<ServerPlayer, Map<Integer, CullingState>> VISIBILITY_CACHE = new WeakConcurrentHashMap<>();
    private static final Object ACTIVE_MAPS_LOCK = new Object();
    private static final long CHECK_INTERVAL_MS = 500;
    private static final long HIDE_DELAY_MS = 1000;
    private static final Map<ServerPlayer, ParticleCullCache> PARTICLE_CACHE = new WeakConcurrentHashMap<>();
    private static final Map<ServerPlayer, EntityCullCache> ENTITY_CACHE = new WeakConcurrentHashMap<>();
    private static final ThreadLocal<double[]> PROBE_COORDS = ThreadLocal.withInitial(() -> new double[18]);
    private static volatile ExecutorService EXECUTOR = Executors.newWorkStealingPool();
    @SuppressWarnings("unchecked")
    private static volatile Map<Integer, CullingState>[] activeVisibilityMaps = new Map[0];

    @SuppressWarnings("unchecked")
    private static void updateActiveVisibilityMaps() {
        synchronized (ACTIVE_MAPS_LOCK) {
            activeVisibilityMaps = VISIBILITY_CACHE.values().toArray(new Map[0]);
        }
    }

    public static void onEnd() {
        VISIBILITY_CACHE.clear();
        updateActiveVisibilityMaps();
        PARTICLE_CACHE.clear();
        ENTITY_CACHE.clear();
        if (EXECUTOR != null && !EXECUTOR.isShutdown())
            EXECUTOR.shutdown();
        EXECUTOR = Executors.newWorkStealingPool();
    }


    public static boolean isEntityVisible(ServerPlayer player, Entity entity, long now) {
        if (!ModConfig.Culling.isEntityEnabled() || !player.level().getServer().isDedicatedServer()) return true;

        CullingState state = getEntityCullingState(player, entity);

        float ex = (float) player.getX();
        float ey = (float) player.getEyeY();
        float ez = (float) player.getZ();
        float cx = (float) entity.getX();
        float cy = (float) (entity.getY() + entity.getBbHeight() / 2.0);
        float cz = (float) entity.getZ();
        float rotX = player.getXRot();
        float rotY = player.getYRot();

        if (checkAndUpdateStatePosition(state, ex, ey, ez, cx, cy, cz, rotX, rotY)) {
            return state.isCurrentlyVisible;
        }

        float distanceSq = (float) player.distanceToSqr(entity);
        state.lastDistanceSq = distanceSq;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            state.lastRaytraceResult = true;
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        EntityCullCache hashCache = getOrCreate(ENTITY_CACHE, player, EntityCullCache::new);
        long tickCount = player.level().getServer().getTickCount();
        if (hashCache.lastTick != tickCount) {
            hashCache.grid.clear();
            hashCache.lastTick = tickCount;
        }

        long gridKey = toGridKey(cx, cy, cz);

        if (hashCache.grid.contains(gridKey)) {
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        float dx = cx - ex;
        float dy = cy - ey;
        float dz = cz - ez;

        boolean inFOV = isInFOVCached(state, dx, dy, dz, rotX, rotY, distanceSq);

        if (inFOV) {
            if (now - state.lastCheckTime > CHECK_INTERVAL_MS) {
                if (!state.isChecking) {
                    state.isChecking = true;
                    state.lastCheckTime = now;
                    queueRaytraceCheck(state, player.level(), player.getEyePosition(), entity.getBoundingBox().inflate(0.5));
                }
            }
        }

        boolean isVisible = inFOV && state.lastRaytraceResult;
        updateVisibilityState(state, isVisible, now);

        if (state.isCurrentlyVisible) {
            hashCache.grid.add(gridKey);
        }

        return state.isCurrentlyVisible;
    }

    public static boolean getLastSentVisible(ServerPlayer player, Entity entity) {
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(player);
        if (map == null) return true;
        CullingState state = map.get(entity.getId());
        if (state == null) return true;
        return state.lastSentVisible;
    }

    public static void setLastSentVisible(ServerPlayer player, Entity entity, boolean visible) {
        CullingState state = getEntityCullingState(player, entity);
        state.lastSentVisible = visible;
    }

    public static void removePlayerEntityState(ServerPlayer player, Entity entity) {
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(player);
        if (map != null) {
            map.remove(entity.getId());
        }
    }

    private static CullingState getEntityCullingState(ServerPlayer player, Entity entity) {
        Map<Integer, CullingState> map = VISIBILITY_CACHE.get(player);
        if (map == null) {
            Map<Integer, CullingState> newMap = new ConcurrentHashMap<>();
            Map<Integer, CullingState> existing = VISIBILITY_CACHE.putIfAbsent(player, newMap);
            if (existing == null) {
                map = newMap;
                updateActiveVisibilityMaps();
            } else {
                map = existing;
            }
        }
        Integer entityId = entity.getId();
        CullingState state = map.get(entityId);
        if (state == null) {
            state = new CullingState();
            CullingState prev = map.putIfAbsent(entityId, state);
            if (prev != null) state = prev;
        }
        return state;
    }

    private static void queueRaytraceCheck(CullingState state, Level level, Vec3 eyePos, AABB aabb) {
        if (ModConfig.Culling.isAsyncMode()) {
            if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
                EXECUTOR.submit(new AsyncAABBCheckTask(state, level, eyePos, aabb));
                return;
            }
        }
        try {
            state.lastRaytraceResult = checkAABBVisible(level, eyePos, aabb);
        } catch (Exception e) {
            state.lastRaytraceResult = true;
        } finally {
            state.isChecking = false;
        }
    }

    private static boolean checkAndUpdateStatePosition(CullingState state, float ex, float ey, float ez, float cx, float cy, float cz, float rotX, float rotY) {
        if (Math.abs(state.lastPx - ex) < 0.1f && Math.abs(state.lastPy - ey) < 0.1f && Math.abs(state.lastPz - ez) < 0.1f &&
                Math.abs(state.lastTx - cx) < 0.1f && Math.abs(state.lastTy - cy) < 0.1f && Math.abs(state.lastTz - cz) < 0.1f &&
                Math.abs(state.lastRotX - rotX) < 1.0f && Math.abs(state.lastRotY - rotY) < 1.0f) {
            return true;
        }

        state.lastPx = ex;
        state.lastPy = ey;
        state.lastPz = ez;
        state.lastTx = cx;
        state.lastTy = cy;
        state.lastTz = cz;
        state.lastRotX = rotX;
        state.lastRotY = rotY;
        return false;
    }

    private static boolean isInFOV(float dx, float dy, float dz, float rotX, float rotY, float distanceSq) {
        float f = rotX * DEG_TO_RAD;
        float g = -rotY * DEG_TO_RAD;
        float cosG = (float) Math.cos(g);
        float sinG = (float) Math.sin(g);
        float cosF = (float) Math.cos(f);
        float sinF = (float) Math.sin(f);
        double lVx = sinG * cosF;
        double lVy = -sinF;
        double lVz = cosG * cosF;

        double dot = lVx * dx + lVy * dy + lVz * dz;
        return dot >= 0 || (dot * dot <= 0.0225 * distanceSq);
    }

    /**
     * FOV check that reuses cached sin/cos values from {@code state} when the player's rotation
     * has not changed significantly, avoiding expensive transcendental math calls every tick.
     */
    private static boolean isInFOVCached(CullingState state, float dx, float dy, float dz, float rotX, float rotY, float distanceSq) {
        if (Math.abs(state.cachedRotX - rotX) >= 0.01f || Math.abs(state.cachedRotY - rotY) >= 0.01f) {
            float f = rotX * DEG_TO_RAD;
            float g = -rotY * DEG_TO_RAD;
            state.cosF = (float) Math.cos(f);
            state.sinF = (float) Math.sin(f);
            state.cosG = (float) Math.cos(g);
            state.sinG = (float) Math.sin(g);
            state.cachedRotX = rotX;
            state.cachedRotY = rotY;
        }
        double lVx = state.sinG * state.cosF;
        double lVy = -state.sinF;
        double lVz = state.cosG * state.cosF;
        double dot = lVx * dx + lVy * dy + lVz * dz;
        return dot >= 0 || (dot * dot <= 0.0225 * distanceSq);
    }

    private static void updateVisibilityState(CullingState state, boolean isVisible, long now) {
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
    }

    public static boolean checkAABBVisible(Level level, Vec3 eye, AABB aabb) {
        Vec3 center = aabb.getCenter();
        if (isLineOfSightClear(level, eye, center)) return true;

        double[] p = PROBE_COORDS.get();
        double cx = center.x, cz = center.z;
        p[0] = cx;
        p[1] = aabb.maxY;
        p[2] = cz;
        p[3] = cx;
        p[4] = aabb.minY;
        p[5] = cz;
        p[6] = aabb.minX;
        p[7] = aabb.maxY;
        p[8] = aabb.minZ;
        p[9] = aabb.maxX;
        p[10] = aabb.maxY;
        p[11] = aabb.maxZ;
        p[12] = aabb.minX;
        p[13] = aabb.minY;
        p[14] = aabb.maxZ;
        p[15] = aabb.maxX;
        p[16] = aabb.minY;
        p[17] = aabb.minZ;

        for (int i = 0; i < 18; i += 3) {
            if (isLineOfSightClear(level, eye, new Vec3(p[i], p[i + 1], p[i + 2]))) return true;
        }
        return false;
    }

    public static boolean isLineOfSightClear(Level level, Vec3 start, Vec3 end) {
        try {
            int chunkX = (int) Math.floor(end.x) >> 4;
            int chunkZ = (int) Math.floor(end.z) >> 4;
            ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk == null) return true;

            ClipContext ctx = new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty());
            return level.clip(ctx).getType() == HitResult.Type.MISS;
        } catch (Throwable t) {
            return true;
        }
    }

    public static void removePlayer(ServerPlayer player) {
        if (VISIBILITY_CACHE.remove(player) != null) {
            updateActiveVisibilityMaps();
        }
        PARTICLE_CACHE.remove(player);
        ENTITY_CACHE.remove(player);
    }

    public static void removeEntity(Entity entity) {
        int id = entity.getId();
        for (Map<Integer, CullingState> map : activeVisibilityMaps) {
            map.remove(id);
        }
    }

    private static <K, V> V getOrCreate(Map<K, V> map, K key, Supplier<V> factory) {
        V value = map.get(key);
        if (value == null) {
            V created = factory.get();
            V existing = map.putIfAbsent(key, created);
            value = existing != null ? existing : created;
        }
        return value;
    }

    private static long toGridKey(float cx, float cy, float cz) {
        int gridX = (int) Math.floor(cx / 8.0);
        int gridY = (int) Math.floor(cy / 8.0);
        int gridZ = (int) Math.floor(cz / 8.0);
        return ((long) (gridX & 0x3FFFFF) << 42) | ((long) (gridY & 0xFFFFF) << 22) | (gridZ & 0x3FFFFF);
    }

    public static boolean isParticleVisible(ServerPlayer player, double x, double y, double z) {
        ParticleCullCache cache = getOrCreate(PARTICLE_CACHE, player, ParticleCullCache::new);

        Vec3 eyePos = player.getEyePosition();
        double dx = x - eyePos.x;
        double dy = y - eyePos.y;
        double dz = z - eyePos.z;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            return true;
        }

        boolean inFOV = isInFOV((float) dx, (float) dy, (float) dz, player.getXRot(), player.getYRot(), (float) distanceSq);

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
        LongArraySet grid = new LongArraySet();
        long lastTick = -1;
    }

    private record AsyncAABBCheckTask(CullingState state, Level level, Vec3 eyePos, AABB aabb) implements Runnable {
        @Override
        public void run() {
            try {
                state.lastRaytraceResult = checkAABBVisible(level, eyePos, aabb);
            } catch (Exception e) {
                state.lastRaytraceResult = true;
            } finally {
                state.isChecking = false;
            }
        }
    }

    private static class CullingState {
        volatile boolean lastRaytraceResult = true;
        volatile boolean isChecking = false;
        boolean isCurrentlyVisible = true;
        long lastCheckTime = 0;
        long hiddenSince = 0;
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
        float cachedRotX = Float.MAX_VALUE;
        float cachedRotY = Float.MAX_VALUE;
        float sinF = 0f;
        float cosF = 1f;
        float sinG = 0f;
        float cosG = 1f;
    }
}
