package set.starl.util;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import set.starl.injection.access.LevelRendererAccess;

public class SectionVisibilityCache {
	// Use a larger initial capacity to avoid rehashing
	private static final Long2ByteOpenHashMap CACHE = new Long2ByteOpenHashMap(4096);
	private static int lastLevelId = Integer.MIN_VALUE;
	private static long lastGameTime = -1L;
	private static double camX, camY, camZ;
	private static int camChunkX, camChunkZ;
	private static Frustum currentFrustum;
	// Track if we've already cached for this camera position
	private static boolean cacheValid = false;

	static {
		CACHE.defaultReturnValue((byte)-1);
	}

	public static void checkLevelChange(int levelId) {
		if (levelId != lastLevelId) {
			lastLevelId = levelId;
			// Clear less frequently - only on dimension change
			if (!CACHE.isEmpty()) {
				CACHE.clear();
			}
			cacheValid = false;
		}
	}

	public static boolean ensureCameraCache(long gameTime) {
		if (gameTime == lastGameTime && cacheValid && currentFrustum != null) {
			return true;
		}
		var mc = Minecraft.getInstance();
		if (mc == null || mc.gameRenderer == null) {
			return false;
		}
		var cam = mc.gameRenderer.getMainCamera();
		if (!cam.isInitialized()) {
			return false;
		}
		var entity = cam.entity();
		if (entity == null) {
			return false;
		}

		lastGameTime = gameTime;
		var pos = cam.position();
		camX = pos.x;
		camY = pos.y;
		camZ = pos.z;
		camChunkX = Mth.floor(camX) >> 4;
		camChunkZ = Mth.floor(camZ) >> 4;

		currentFrustum = ((LevelRendererAccess)mc.levelRenderer).lomka$getLastCullFrustum();
		// Don't clear on every call - only when frustum actually changes
		if (currentFrustum != null) {
			cacheValid = true;
		}
		return currentFrustum != null;
	}

	public static double getCamX() { return camX; }
	public static double getCamY() { return camY; }
	public static double getCamZ() { return camZ; }
	public static int getCamChunkX() { return camChunkX; }
	public static int getCamChunkZ() { return camChunkZ; }

	public static boolean isSectionVisible(long sectionKey) {
		// Fast path: check cache first without allocation
		byte cached = CACHE.get(sectionKey);
		if (cached != -1) {
			return cached != 0;
		}

		if (!cacheValid || currentFrustum == null) {
			return true;
		}

		// Avoid AABB allocation by using direct coordinate check
		int x = SectionPos.x(sectionKey) << 4;
		int y = SectionPos.y(sectionKey) << 4;
		int z = SectionPos.z(sectionKey) << 4;

		boolean visible = currentFrustum.isVisible(new AABB(x, y, z, x + 16, y + 16, z + 16));
		// Only cache if we have room - prevents unbounded growth
		if (CACHE.size() < 8192) {
			CACHE.put(sectionKey, (byte)(visible ? 1 : 0));
		}
		return visible;
	}
}
