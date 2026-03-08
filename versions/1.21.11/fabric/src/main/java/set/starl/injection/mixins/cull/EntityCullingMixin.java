package set.starl.injection.mixins.cull;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import set.starl.injection.access.EntityCullingAccessor;

@Mixin(Entity.class)
public abstract class EntityCullingMixin implements EntityCullingAccessor {
	@Unique
	private long lomka$motionSampleTick = Long.MIN_VALUE;

	@Unique
	private int lomka$lastRenderTick = Integer.MIN_VALUE;

	@Unique
	private int lomka$lastRenderCamHash = 0;

	@Unique
	private int lomka$lastRenderPosHash = 0;

	@Unique
	private int lomka$lastRenderMotionHash = 0;

	@Unique
	private boolean lomka$renderVisible = true;

	@Unique
	private long lomka$occlusionState = Long.MIN_VALUE;

	@Unique
	@Override
	public long lomka$getMotionSampleTick() { return this.lomka$motionSampleTick; }

	@Unique
	@Override
	public void lomka$setMotionSampleTick(final long packed) { this.lomka$motionSampleTick = packed; }

	@Unique
	@Override
	public int lomka$getLastRenderTick() { return this.lomka$lastRenderTick; }

	@Unique
	@Override
	public void lomka$setLastRenderTick(final int tick) { this.lomka$lastRenderTick = tick; }

	@Unique
	@Override
	public int lomka$getLastRenderCamHash() { return this.lomka$lastRenderCamHash; }

	@Unique
	@Override
	public void lomka$setLastRenderCamHash(final int hash) { this.lomka$lastRenderCamHash = hash; }

	@Unique
	@Override
	public int lomka$getLastRenderPosHash() { return this.lomka$lastRenderPosHash; }

	@Unique
	@Override
	public void lomka$setLastRenderPosHash(final int hash) { this.lomka$lastRenderPosHash = hash; }

	@Unique
	@Override
	public int lomka$getLastRenderMotionHash() { return this.lomka$lastRenderMotionHash; }

	@Unique
	@Override
	public void lomka$setLastRenderMotionHash(final int hash) { this.lomka$lastRenderMotionHash = hash; }

	@Unique
	@Override
	public boolean lomka$isRenderVisible() { return this.lomka$renderVisible; }

	@Unique
	@Override
	public void lomka$setRenderVisible(final boolean visible) { this.lomka$renderVisible = visible; }

	@Unique
	@Override
	public long lomka$getOcclusionState() { return this.lomka$occlusionState; }

	@Unique
	@Override
	public void lomka$setOcclusionState(final long state) { this.lomka$occlusionState = state; }
}
