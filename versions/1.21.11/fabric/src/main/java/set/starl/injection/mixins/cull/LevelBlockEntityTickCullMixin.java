package set.starl.injection.mixins.cull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import set.starl.Lomka;
import set.starl.util.SectionVisibilityCache;

@Mixin(Level.class)
public class LevelBlockEntityTickCullMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = Lomka.CONFIG.entities.enable && Lomka.CONFIG.entities.clientTickCull.enable;

	@Unique
	private static final double LOMKA$MIN_DIST_SQ = Lomka.CONFIG.entities.clientTickCull.minDistance * Lomka.CONFIG.entities.clientTickCull.minDistance;

	@Unique
	private static final boolean LOMKA$SECTION_OCCLUSION_CULL = Lomka.CONFIG.entities.sectionOcclusionCull;

	@Unique
	private static int LOMKA$LAST_LEVEL_ID = Integer.MIN_VALUE;

	@Redirect(
		method = "tickBlockEntities",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"
		)
	)
	private void lomka$maybeTickBlockEntity(final TickingBlockEntity ticker) {
		Level level = (Level)(Object)this;
		if (!LOMKA$ENABLE || !LOMKA$SECTION_OCCLUSION_CULL || !level.isClientSide()) {
			ticker.tick();
			return;
		}
		int levelId = System.identityHashCode(level);
		if (levelId != LOMKA$LAST_LEVEL_ID) {
			LOMKA$LAST_LEVEL_ID = levelId;
		}
		SectionVisibilityCache.checkLevelChange(levelId);
		if (!SectionVisibilityCache.ensureCameraCache(level.getGameTime())) {
			ticker.tick();
			return;
		}
		BlockPos pos = ticker.getPos();
		if (pos == null) {
			ticker.tick();
			return;
		}
		if (lomka$shouldTickBlockEntity(pos)) {
			ticker.tick();
		}
	}

	@Unique
	private static boolean lomka$shouldTickBlockEntity(final BlockPos pos) {
		double minDistSq = LOMKA$MIN_DIST_SQ;
		if (minDistSq > 0.0) {
			double px = (double)pos.getX() + 0.5;
			double py = (double)pos.getY() + 0.5;
			double pz = (double)pos.getZ() + 0.5;
			double dx = px - SectionVisibilityCache.getCamX();
			double dy = py - SectionVisibilityCache.getCamY();
			double dz = pz - SectionVisibilityCache.getCamZ();
			if ((dx * dx + dy * dy + dz * dz) < minDistSq) {
				return true;
			}
		}
		long cellKey = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
		return SectionVisibilityCache.isSectionVisible(cellKey);
	}
}
