package set.starl.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import org.joml.FrustumIntersection;

public final class SectionVisibilityCache {
    // Generation-based flat array cache — no clear() needed
    // Each entry: generation (bits 0-30) + visibility flag (bit 31)
    private static final int CACHE_CAPACITY = 1 << 16; // 65536 entries, power-of-2
    private static final int CACHE_MASK = CACHE_CAPACITY - 1;
    private static final int[] CACHE = new int[CACHE_CAPACITY];
    private static int currentGeneration = 1;

    private static int lastLevelId = Integer.MIN_VALUE;
    private static long lastGameTime = -1L;
    private static double camX;
    private static double camY;
    private static double camZ;
    private static int camChunkX;
    private static int camChunkZ;
    private static net.minecraft.client.renderer.culling.Frustum currentFrustum;
    private static FrustumIntersection currentIntersection;

    // Distance threshold: sections within this many chunks are always visible
    private static final int ALWAYS_VISIBLE_CHUNK_DIST = 4;
    private static final int ALWAYS_VISIBLE_CHUNK_DIST_SQ = ALWAYS_VISIBLE_CHUNK_DIST * ALWAYS_VISIBLE_CHUNK_DIST;

    // Pre-computed camera section coords for fast-path (avoid SectionPos extraction)
    private static int camSectionX;
    private static int camSectionY;
    private static int camSectionZ;

    private SectionVisibilityCache() {
    }

    public static void checkLevelChange(int levelId) {
        if (levelId != lastLevelId) {
            lastLevelId = levelId;
            currentGeneration += 2;
            if (currentGeneration < 0) {
                java.util.Arrays.fill(CACHE, 0);
                currentGeneration = 1;
            }
        }
    }

    public static boolean ensureCameraCache(long gameTime) {
        if (gameTime == lastGameTime && currentFrustum != null) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return false;
        }

        lastGameTime = gameTime;
        var pos = camera.position();
        camX = pos.x();
        camY = pos.y();
        camZ = pos.z();
        camChunkX = SectionPos.blockToSectionCoord((int) camX);
        camChunkZ = SectionPos.blockToSectionCoord((int) camZ);
        camSectionX = camChunkX;
        camSectionY = SectionPos.blockToSectionCoord((int) camY);
        camSectionZ = camChunkZ;

        net.minecraft.client.renderer.culling.Frustum newFrustum =
            ((set.starl.injection.access.LevelRendererAccess) mc.levelRenderer).lomka$getLastCullFrustum();
        currentFrustum = newFrustum;

        if (newFrustum instanceof set.starl.injection.access.FrustumIntersectionAccess fia) {
            currentIntersection = fia.lomka$getIntersection();
        } else {
            currentIntersection = null;
        }

        currentGeneration++;
        if (currentGeneration < 0) {
            java.util.Arrays.fill(CACHE, 0);
            currentGeneration = 1;
        }

        return currentFrustum != null;
    }

    public static double getCamX() { return camX; }
    public static double getCamY() { return camY; }
    public static double getCamZ() { return camZ; }
    public static int getCamChunkX() { return camChunkX; }
    public static int getCamChunkZ() { return camChunkZ; }

    public static boolean isSectionVisible(long sectionKey) {
        int sx = SectionPos.x(sectionKey);
        int sy = SectionPos.y(sectionKey);
        int sz = SectionPos.z(sectionKey);

        // Fast path: nearby sections are always visible (within render distance)
        int dx = sx - camChunkX;
        int dz = sz - camChunkZ;
        if (dx * dx + dz * dz <= ALWAYS_VISIBLE_CHUNK_DIST_SQ) {
            return true;
        }

        // Hash with avalanche for uniform distribution
        int idx = set.starl.LomkaCore.MathHelper.hash3(sx, sy, sz) & CACHE_MASK;

        int entry = CACHE[idx];
        int entryGen = entry & 0x7FFFFFFF;
        int entryVis = entry & 0x80000000;

        // Cache hit: same generation = valid entry
        if (entryGen == currentGeneration) {
            return entryVis != 0;
        }

        // Cache miss: compute visibility
        if (currentFrustum == null) {
            CACHE[idx] = currentGeneration | 0x80000000;
            return true;
        }

        int x = sx << 4;
        int y = sy << 4;
        int z = sz << 4;

        // Use FrustumIntersection directly — no AABB allocation, O(1)
        FrustumIntersection fi = currentIntersection;
        boolean visible;
        if (fi != null) {
            visible = fi.intersectAab(
                (float) x, (float) y, (float) z,
                (float) (x + 16), (float) (y + 16), (float) (z + 16)
            ) != FrustumIntersection.OUTSIDE;
        } else {
            visible = currentFrustum.isVisible(
                new net.minecraft.world.phys.AABB(x, y, z, x + 16, y + 16, z + 16)
            );
        }

        CACHE[idx] = currentGeneration | (visible ? 0x80000000 : 0);
        return visible;
    }

    // Batch visibility check — check multiple sections at once, reduce loop overhead
    // Returns true if ANY section is visible
    public static boolean isAnySectionVisible(long[] keys, int count) {
        for (int i = 0; i < count; i++) {
            if (isSectionVisible(keys[i])) return true;
        }
        return false;
    }

    // Pre-compute visibility for a set of sections (call once per frame)
    // Reduces per-entity overhead by amortizing cache miss cost
    public static void prefetchSections(long[] keys, int count) {
        for (int i = 0; i < count; i++) {
            int sx = SectionPos.x(keys[i]);
            int sy = SectionPos.y(keys[i]);
            int sz = SectionPos.z(keys[i]);
            int dx = sx - camChunkX;
            int dz = sz - camChunkZ;
            if (dx * dx + dz * dz <= ALWAYS_VISIBLE_CHUNK_DIST_SQ) continue;

            int idx = set.starl.LomkaCore.MathHelper.hash3(sx, sy, sz) & CACHE_MASK;
            int entry = CACHE[idx];
            if ((entry & 0x7FFFFFFF) == currentGeneration) continue;

            if (currentFrustum == null) {
                CACHE[idx] = currentGeneration | 0x80000000;
                continue;
            }

            int x = sx << 4;
            int y = sy << 4;
            int z = sz << 4;

            FrustumIntersection fi = currentIntersection;
            boolean visible;
            if (fi != null) {
                visible = fi.intersectAab(
                    (float) x, (float) y, (float) z,
                    (float) (x + 16), (float) (y + 16), (float) (z + 16)
                ) != FrustumIntersection.OUTSIDE;
            } else {
                visible = currentFrustum.isVisible(
                    new net.minecraft.world.phys.AABB(x, y, z, x + 16, y + 16, z + 16)
                );
            }

            CACHE[idx] = currentGeneration | (visible ? 0x80000000 : 0);
        }
    }
}