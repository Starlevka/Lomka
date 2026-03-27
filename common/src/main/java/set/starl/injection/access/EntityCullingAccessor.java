package set.starl.injection.access;

public interface EntityCullingAccessor {
    int lomka$getLastRenderTick();
    void lomka$setLastRenderTick(int tick);

    int lomka$getLastRenderCamHash();
    void lomka$setLastRenderCamHash(int hash);

    int lomka$getLastRenderMotionHash();
    void lomka$setLastRenderMotionHash(int hash);

    boolean lomka$isRenderVisible();
    void lomka$setRenderVisible(boolean visible);
}