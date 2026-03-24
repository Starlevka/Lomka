package set.starl.injection.access;

public interface EntityCullingAccessor {
	long lomka$getMotionSampleTick();
	void lomka$setMotionSampleTick(long tick);

	int lomka$getLastRenderTick();
	void lomka$setLastRenderTick(int tick);

	int lomka$getLastRenderCamHash();
	void lomka$setLastRenderCamHash(int hash);

	int lomka$getLastRenderPosHash();
	void lomka$setLastRenderPosHash(int hash);

	int lomka$getLastRenderMotionHash();
	void lomka$setLastRenderMotionHash(int hash);

	boolean lomka$isRenderVisible();
	void lomka$setRenderVisible(boolean visible);

	long lomka$getOcclusionState();
	void lomka$setOcclusionState(long state);
}