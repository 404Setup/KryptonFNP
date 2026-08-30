package one.pkg.kreno.shared.culling;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.tinyutils.map.WeakConcurrentHashMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ServerCullingManager {
    public static final double NEAR_DISTANCE_SQ = 64.0;
    private static final float DEG_TO_RAD = (float) Math.PI / 180F;
    private static final Map<ServerPlayer, Map<Integer, CullingState>> VISIBILITY_CACHE = new WeakConcurrentHashMap<>();
    private static final Object ACTIVE_MAPS_LOCK = new Object();
    private static final long CHECK_INTERVAL_TICKS = 10;
    private static final long HIDE_DELAY_TICKS = 20;
    private static final int MAX_ENTITY_RAYTRACES_PER_TICK = 16;
    private static final int MAX_PARTICLE_RAYTRACES_PER_TICK = 64;
    private static final int MAX_PARTICLE_RAYTRACES_PER_PLAYER_TICK = 8;
    private static final Map<ServerPlayer, ParticleCullCache> PARTICLE_CACHE = new WeakConcurrentHashMap<>();
    private static final Map<ServerPlayer, EntityCullCache> ENTITY_CACHE = new WeakConcurrentHashMap<>();
    private static final ThreadLocal<double[]> PROBE_COORDS = ThreadLocal.withInitial(() -> new double[30]);
    private static long entityRaytraceBudgetTick = Long.MIN_VALUE;
    private static int entityRaytracesThisTick;
    private static long particleRaytraceBudgetTick = Long.MIN_VALUE;
    private static int particleRaytracesThisTick;
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
        entityRaytraceBudgetTick = Long.MIN_VALUE;
        entityRaytracesThisTick = 0;
        particleRaytraceBudgetTick = Long.MIN_VALUE;
        particleRaytracesThisTick = 0;
    }

    public static void onConfigReload() {
        PARTICLE_CACHE.clear();
        ENTITY_CACHE.clear();
        if (!ModConfig.Culling.isEntityEnabled()) {
            restoreHiddenEntities();
            VISIBILITY_CACHE.clear();
            updateActiveVisibilityMaps();
        }
    }

    private static void restoreHiddenEntities() {
        for (Map<Integer, CullingState> map : activeVisibilityMaps) {
            for (CullingState state : map.values()) {
                Runnable restoreAction = state.restoreAction;
                if (!state.lastSentVisible && restoreAction != null) {
                    state.lastSentVisible = true;
                    state.restoreAction = null;
                    restoreAction.run();
                }
            }
        }
    }


    /**
     * Only entities whose purpose is visual decoration are safe to remove from a vanilla client.
     * Gameplay entities and unknown modded entities must remain in the vanilla tracking set.
     */
    public static boolean shouldCullEntity(Entity entity) {
        return ModConfig.Culling.isEntityEnabled()
                && (entity instanceof Display || entity instanceof HangingEntity);
    }

    public static boolean isEntityVisible(ServerPlayer player, Entity entity, long currentTick) {
        if (!shouldCullEntity(entity) || !player.level().getServer().isDedicatedServer()) return true;

        CullingState state = getEntityCullingState(player, entity);

        float cx = (float) entity.getX();
        float cy = (float) (entity.getY() + entity.getBbHeight() / 2.0);
        float cz = (float) entity.getZ();

        float distanceSq = (float) player.distanceToSqr(entity);
        if (distanceSq < NEAR_DISTANCE_SQ) {
            state.lastRaytraceResult = true;
            state.isCurrentlyVisible = true;
            state.hiddenSinceTick = -1;
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
            state.hiddenSinceTick = -1;
            return true;
        }

        float dx = cx - (float) player.getX();
        float dy = cy - (float) player.getEyeY();
        float dz = cz - (float) player.getZ();

        boolean inFOV = isInFOVCached(state, dx, dy, dz, player.getXRot(), player.getYRot(), distanceSq);

        if (inFOV && (state.lastCheckTick == Long.MIN_VALUE
                || currentTick - state.lastCheckTick >= CHECK_INTERVAL_TICKS)) {
            if (tryAcquireEntityRaytrace(player.level())) {
                state.lastCheckTick = currentTick;
                state.lastRaytraceResult = checkAABBVisible(
                        player.level(), player.getEyePosition(), entity.getBoundingBox().inflate(0.5));
            } else {
                // A saturated culling budget must fail open instead of hiding an unverified entity.
                state.lastRaytraceResult = true;
            }
        }

        boolean isVisible = inFOV && state.lastRaytraceResult;
        updateVisibilityState(state, isVisible, currentTick);

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

    public static void setLastSentVisible(ServerPlayer player, Entity entity) {
        CullingState state = getEntityCullingState(player, entity);
        state.lastSentVisible = true;
        state.restoreAction = null;
    }

    public static void setLastSentHidden(ServerPlayer player, Entity entity, Runnable restoreAction) {
        CullingState state = getEntityCullingState(player, entity);
        state.lastSentVisible = false;
        state.restoreAction = restoreAction;
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

    private static boolean tryAcquireEntityRaytrace(Level level) {
        long tick = level.getServer().getTickCount();
        if (entityRaytraceBudgetTick != tick) {
            entityRaytraceBudgetTick = tick;
            entityRaytracesThisTick = 0;
        }
        if (entityRaytracesThisTick >= MAX_ENTITY_RAYTRACES_PER_TICK) {
            return false;
        }
        entityRaytracesThisTick++;
        return true;
    }

    private static boolean tryAcquireParticleRaytrace(Level level, ParticleCullCache cache) {
        long tick = level.getServer().getTickCount();
        if (particleRaytraceBudgetTick != tick) {
            particleRaytraceBudgetTick = tick;
            particleRaytracesThisTick = 0;
        }
        if (particleRaytracesThisTick >= MAX_PARTICLE_RAYTRACES_PER_TICK
                || cache.raytraces >= MAX_PARTICLE_RAYTRACES_PER_PLAYER_TICK) {
            return false;
        }
        particleRaytracesThisTick++;
        cache.raytraces++;
        return true;
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

    private static void updateVisibilityState(CullingState state, boolean isVisible, long currentTick) {
        if (!isVisible) {
            if (state.hiddenSinceTick < 0) {
                state.hiddenSinceTick = currentTick;
            } else if (currentTick - state.hiddenSinceTick >= HIDE_DELAY_TICKS) {
                state.isCurrentlyVisible = false;
            }
        } else {
            state.hiddenSinceTick = -1;
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
        p[7] = aabb.minY;
        p[8] = aabb.minZ;
        p[9] = aabb.minX;
        p[10] = aabb.minY;
        p[11] = aabb.maxZ;
        p[12] = aabb.minX;
        p[13] = aabb.maxY;
        p[14] = aabb.minZ;
        p[15] = aabb.minX;
        p[16] = aabb.maxY;
        p[17] = aabb.maxZ;
        p[18] = aabb.maxX;
        p[19] = aabb.minY;
        p[20] = aabb.minZ;
        p[21] = aabb.maxX;
        p[22] = aabb.minY;
        p[23] = aabb.maxZ;
        p[24] = aabb.maxX;
        p[25] = aabb.maxY;
        p[26] = aabb.minZ;
        p[27] = aabb.maxX;
        p[28] = aabb.maxY;
        p[29] = aabb.maxZ;

        for (int i = 0; i < 30; i += 3) {
            if (isLineOfSightClear(level, eye, new Vec3(p[i], p[i + 1], p[i + 2]))) return true;
        }
        return false;
    }

    public static boolean isLineOfSightClear(Level level, Vec3 start, Vec3 end) {
        try {
            int minX = (int) Math.floor(Math.min(start.x, end.x)) >> 4;
            int minZ = (int) Math.floor(Math.min(start.z, end.z)) >> 4;
            int maxX = (int) Math.floor(Math.max(start.x, end.x)) >> 4;
            int maxZ = (int) Math.floor(Math.max(start.z, end.z)) >> 4;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!level.hasChunk(x, z)) return true;
                }
            }

            ClipContext ctx = new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty());
            return level.clip(ctx).getType() == HitResult.Type.MISS;
        } catch (Exception e) {
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
        return packGridKey(gridX, gridY, gridZ);
    }

    private static long toParticleGridKey(double x, double y, double z) {
        return packGridKey((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    private static long packGridKey(int gridX, int gridY, int gridZ) {
        return ((long) (gridX & 0x3FFFFF) << 42) | ((long) (gridY & 0xFFFFF) << 22) | (gridZ & 0x3FFFFF);
    }

    public static boolean isParticleVisible(ServerPlayer player, double x, double y, double z) {
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

        ParticleCullCache cache = getOrCreate(PARTICLE_CACHE, player, ParticleCullCache::new);
        Level level = player.level();
        long tick = level.getServer().getTickCount();
        if (cache.level != level || cache.lastTick != tick) {
            cache.level = level;
            cache.lastTick = tick;
            cache.raytraces = 0;
            cache.visibleCells.clear();
        }

        long gridKey = toParticleGridKey(x, y, z);
        if (cache.visibleCells.contains(gridKey)) {
            return true;
        }
        if (!tryAcquireParticleRaytrace(level, cache)) {
            return true;
        }

        boolean result = isLineOfSightClear(level, eyePos, new Vec3(x, y, z));
        if (result) {
            cache.visibleCells.add(gridKey);
        }

        return result;
    }

    private static class ParticleCullCache {
        final LongArraySet visibleCells = new LongArraySet();
        Level level;
        long lastTick = Long.MIN_VALUE;
        int raytraces;
    }

    private static class EntityCullCache {
        LongArraySet grid = new LongArraySet();
        long lastTick = -1;
    }

    private static class CullingState {
        boolean lastRaytraceResult = true;
        boolean isCurrentlyVisible = true;
        long lastCheckTick = Long.MIN_VALUE;
        long hiddenSinceTick = -1;
        boolean lastSentVisible = true;
        Runnable restoreAction;
        float cachedRotX = Float.MAX_VALUE;
        float cachedRotY = Float.MAX_VALUE;
        float sinF = 0f;
        float cosF = 1f;
        float sinG = 0f;
        float cosG = 1f;
    }
}
