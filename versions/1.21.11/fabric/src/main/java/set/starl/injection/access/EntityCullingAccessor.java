package set.starl.injection.access;

public interface EntityCullingAccessor {
	// TICK CULLING
	long lomka$getMotionSampleTick();
	void lomka$setMotionSampleTick(final long packed);

	// RENDER CULLING
	int lomka$getLastRenderTick();
	void lomka$setLastRenderTick(final int tick);

	int lomka$getLastRenderCamHash();
	void lomka$setLastRenderCamHash(final int hash);

	int lomka$getLastRenderPosHash();
	void lomka$setLastRenderPosHash(final int hash);

	int lomka$getLastRenderMotionHash();
	void lomka$setLastRenderMotionHash(final int hash);

	boolean lomka$isRenderVisible();
	void lomka$setRenderVisible(final boolean visible);

	// OCCLUSION CACHE
	long lomka$getOcclusionState();
	void lomka$setOcclusionState(final long state);
}
