package set.starl.injection.mixins.cull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starl.LomkaCore;
import set.starl.util.SectionVisibilityCache;

@Mixin(ClientLevel.class)
public abstract class ClientLevelEntityTickCullMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = LomkaCore.CONFIG.entities.enable && LomkaCore.CONFIG.entities.clientTickCull;
	@Unique
	private static final double LOMKA$MIN_DIST_SQ;
	@Unique
	private static final int LOMKA$MIN_CHUNK_DISTANCE = 4;
	@Unique
    private static final int LOMKA$INTERVAL_TICKS = 20;
	@Unique
	private static final boolean LOMKA$SECTION_OCCLUSION_CULL = LomkaCore.CONFIG.culling.enable && LomkaCore.CONFIG.culling.sectionOcclusionCull;
	@Unique
    private static final boolean LOMKA$INCLUDE_DISPLAY = LomkaCore.CONFIG.entities.clientTickCullIncludeDisplay;

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void lomka$throttleTickNonPassenger(Entity entity, CallbackInfo ci) {
		if (!LOMKA$ENABLE) return;
		if (LOMKA$MIN_DIST_SQ <= 0.0) return;
		if (!lomka$isCandidate(entity)) return;

        // Cache level identity — avoid repeated cast
        ClientLevel level = (ClientLevel) (Object) this;
        SectionVisibilityCache.checkLevelChange(System.identityHashCode(level));

		// Warmup: let entities tick normally for first few ticks after spawn
		if (entity.tickCount < 5) return;

		// Don't throttle entities with passengers (vehicles carrying players)
		if (!entity.getPassengers().isEmpty()) return;

		long gameTime = level.getGameTime();
		if (!SectionVisibilityCache.ensureCameraCache(gameTime)) return;

        // Distance check — chunk distance first (cheap), then exact distance² if needed
        int ecx = SectionPos.blockToSectionCoord(entity.getBlockX());
        int ecz = SectionPos.blockToSectionCoord(entity.getBlockZ());
        int dx = Math.abs(ecx - SectionVisibilityCache.getCamChunkX());
        int dz = Math.abs(ecz - SectionVisibilityCache.getCamChunkZ());
        // Chunk fast reject: if both chunk deltas are small, do precise distance check
        if (dx < LOMKA$MIN_CHUNK_DISTANCE && dz < LOMKA$MIN_CHUNK_DISTANCE) {
            double dist2 = LomkaCore.MathHelper.distSq(
                entity.getX(), entity.getY(), entity.getZ(),
                SectionVisibilityCache.getCamX(), SectionVisibilityCache.getCamY(), SectionVisibilityCache.getCamZ()
            );
            if (dist2 < LOMKA$MIN_DIST_SQ) return;
        }
        // If chunk distance is >= LOMKA$MIN_CHUNK_DISTANCE, entity is far enough to throttle

        // Section occlusion cull — skip entities in invisible sections
        if (LOMKA$SECTION_OCCLUSION_CULL && !(entity instanceof net.minecraft.world.entity.player.Player)) {
            long cellKey = SectionPos.asLong(ecx,
                SectionPos.blockToSectionCoord(entity.getBlockY()), ecz);
            if (!SectionVisibilityCache.isSectionVisible(cellKey)) {
                entity.setOldPosAndRot();
                ++entity.tickCount;
                ci.cancel();
                return;
            }
        }
		// Phase-based throttle: tick every N ticks based on entity ID
        long phase = gameTime + (long) entity.getId();
        if (phase % LOMKA$INTERVAL_TICKS != 0L) {
            entity.setOldPosAndRot();
            ++entity.tickCount;
            ci.cancel();
        }
	}

	@Unique
	private static boolean lomka$isCandidate(Entity entity) {
		return entity instanceof ItemFrame
			|| entity instanceof Painting
			|| entity instanceof GlowItemFrame
			|| (LOMKA$INCLUDE_DISPLAY && entity instanceof net.minecraft.world.entity.Display);
	}

    static {
		double minDist = LomkaCore.CONFIG.entities.clientTickCullMinDistance;
		LOMKA$MIN_DIST_SQ = minDist <= 0.0 ? -1.0 : minDist * minDist;
	}
}