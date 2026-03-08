package set.starl.injection.mixins.cull;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import set.starl.Lomka;
import set.starl.util.SectionVisibilityCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelEntityTickCullMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = Lomka.CONFIG.entities.enable && Lomka.CONFIG.entities.clientTickCull.enable;

	@Unique
	private static final double LOMKA$MIN_DISTANCE = Lomka.CONFIG.entities.clientTickCull.minDistance;

	@Unique
	private static final double LOMKA$MIN_DIST_SQ = LOMKA$MIN_DISTANCE <= 0.0 ? -1.0 : LOMKA$MIN_DISTANCE * LOMKA$MIN_DISTANCE;

	@Unique
	private static final int LOMKA$MIN_CHUNK_DISTANCE = 4;

	@Unique
	private static final int LOMKA$INTERVAL_TICKS = 20;

	@Unique
	private static final long LOMKA$INTERVAL_L = (long)LOMKA$INTERVAL_TICKS;

	@Unique
	private static final boolean LOMKA$INTERVAL_POW2 = LOMKA$INTERVAL_TICKS > 0 && (LOMKA$INTERVAL_TICKS & (LOMKA$INTERVAL_TICKS - 1)) == 0;

	@Unique
	private static final long LOMKA$INTERVAL_MASK = LOMKA$INTERVAL_POW2 ? (LOMKA$INTERVAL_L - 1L) : 0L;

	@Unique
	private static final int LOMKA$WARMUP_TICKS = 5;

	@Unique
	private static final int LOMKA$MOTION_SAMPLE_TICKS = 10;

	@Unique
	private static final boolean LOMKA$INCLUDE_DISPLAY = false;

	@Unique
	private static final boolean LOMKA$SECTION_OCCLUSION_CULL = Lomka.CONFIG.entities.sectionOcclusionCull;

	@Unique
	private static final Int2LongOpenHashMap LOMKA$MOTION_SAMPLE_BY_ID = new Int2LongOpenHashMap(512);

	@Unique
	private static int LOMKA$LAST_LEVEL_ID = Integer.MIN_VALUE;

	static {
		LOMKA$MOTION_SAMPLE_BY_ID.defaultReturnValue(Long.MIN_VALUE);
	}

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void lomka$throttleTickNonPassenger(final Entity entity, final CallbackInfo ci) {
		if (!LOMKA$ENABLE) {
			return;
		}

		if (LOMKA$MIN_DIST_SQ <= 0.0) {
			return;
		}

		if (LOMKA$INTERVAL_TICKS <= 1) {
			return;
		}

		if (!lomka$isCandidate(entity)) {
			return;
		}

		ClientLevel level = (ClientLevel)(Object)this;
		int levelId = System.identityHashCode(level);
		if (levelId != LOMKA$LAST_LEVEL_ID) {
			LOMKA$LAST_LEVEL_ID = levelId;
			LOMKA$MOTION_SAMPLE_BY_ID.clear();
		}
		SectionVisibilityCache.checkLevelChange(levelId);

		int warmup = LOMKA$WARMUP_TICKS;
		if (warmup > 0 && entity.tickCount < warmup) {
			return;
		}

		if (!entity.getPassengers().isEmpty()) {
			return;
		}

		int motionSampleTicks = LOMKA$MOTION_SAMPLE_TICKS;
		if (motionSampleTicks > 0) {
			Int2LongOpenHashMap m = LOMKA$MOTION_SAMPLE_BY_ID;
			long packed = m.get(entity.getId());
			int lastSampleTick = (int)packed;
			boolean lastStatic = ((packed >>> 32) & 1L) != 0L;
			int now = entity.tickCount;
			if (lastSampleTick == Integer.MIN_VALUE || now - lastSampleTick >= motionSampleTicks) {
				Vec3 v = entity.getDeltaMovement();
				lastStatic = (v.x * v.x + v.y * v.y + v.z * v.z) <= 1.0E-8;
				// Prevent unbounded growth
				if (m.size() < 4096) {
					m.put(entity.getId(), ((long)now) | (lastStatic ? (1L << 32) : 0L));
				} else {
					// Clear oldest entries when limit reached
					m.clear();
					m.put(entity.getId(), ((long)now) | (lastStatic ? (1L << 32) : 0L));
				}
			}
			if (!lastStatic) return;
		}

		long gameTime = level.getGameTime();

		if (!SectionVisibilityCache.ensureCameraCache(gameTime)) {
			return;
		}

		int minChunk = LOMKA$MIN_CHUNK_DISTANCE;
		double dist2 = -1.0;
		if (minChunk > 0) {
			int ex = entity.blockPosition().getX() >> 4;
			int ez = entity.blockPosition().getZ() >> 4;
			int dx = ex - SectionVisibilityCache.getCamChunkX();
			int dz = ez - SectionVisibilityCache.getCamChunkZ();
			if (dx < 0) {
				dx = -dx;
			}
			if (dz < 0) {
				dz = -dz;
			}
			if (dx < minChunk && dz < minChunk) {
				double ddx = entity.getX() - SectionVisibilityCache.getCamX();
				double ddy = entity.getY() - SectionVisibilityCache.getCamY();
				double ddz = entity.getZ() - SectionVisibilityCache.getCamZ();
				dist2 = ddx * ddx + ddy * ddy + ddz * ddz;
				if (dist2 < LOMKA$MIN_DIST_SQ) {
					return;
				}
			}
		} else {
			double ddx = entity.getX() - SectionVisibilityCache.getCamX();
			double ddy = entity.getY() - SectionVisibilityCache.getCamY();
			double ddz = entity.getZ() - SectionVisibilityCache.getCamZ();
			dist2 = ddx * ddx + ddy * ddy + ddz * ddz;
			if (dist2 < LOMKA$MIN_DIST_SQ) {
				return;
			}
		}

		if (LOMKA$SECTION_OCCLUSION_CULL && !(entity instanceof Player)) {
			double minDistSq = LOMKA$MIN_DIST_SQ;
			if (minDistSq <= 0.0 || dist2 >= minDistSq) {
				int ex = Mth.floor(entity.getX());
				int ey = Mth.floor(entity.getY());
				int ez = Mth.floor(entity.getZ());
				long cellKey = SectionPos.asLong(ex >> 4, ey >> 4, ez >> 4);
				if (!SectionVisibilityCache.isSectionVisible(cellKey)) {
					entity.setOldPosAndRot();
					++entity.tickCount;
					ci.cancel();
					return;
				}
			}
		}

		int id = entity.getId();
		long phase = gameTime + (long)id;
		boolean shouldFullTick = LOMKA$INTERVAL_POW2
			? (phase & LOMKA$INTERVAL_MASK) == 0L
			: (phase % LOMKA$INTERVAL_L) == 0L;
		if (!shouldFullTick) {
			entity.setOldPosAndRot();
			++entity.tickCount;
			ci.cancel();
			return;
		}
	}

	@Unique
	private static boolean lomka$isCandidate(final Entity entity) {
		return (LOMKA$INCLUDE_DISPLAY && entity instanceof Display)
			|| entity instanceof ArmorStand
			|| entity instanceof ItemFrame
			|| entity instanceof Painting;
	}
}
