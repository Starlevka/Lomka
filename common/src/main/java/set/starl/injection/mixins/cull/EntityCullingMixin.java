package set.starl.injection.mixins.cull;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import set.starl.injection.access.EntityCullingAccessor;

@Mixin(Entity.class)
public abstract class EntityCullingMixin implements EntityCullingAccessor {
    @Unique
    private int lomka$lastRenderTick = Integer.MIN_VALUE;

    @Unique
    private int lomka$lastRenderCamHash = 0;

    @Unique
    private int lomka$lastRenderMotionHash = 0;

    @Unique
    private boolean lomka$lastRenderVisible = false;

    @Override
    public int lomka$getLastRenderTick() {
        return this.lomka$lastRenderTick;
    }

    @Override
    public void lomka$setLastRenderTick(int tick) {
        this.lomka$lastRenderTick = tick;
    }

    @Override
    public int lomka$getLastRenderCamHash() {
        return this.lomka$lastRenderCamHash;
    }

    @Override
    public void lomka$setLastRenderCamHash(int hash) {
        this.lomka$lastRenderCamHash = hash;
    }

    @Override
    public int lomka$getLastRenderMotionHash() {
        return this.lomka$lastRenderMotionHash;
    }

    @Override
    public void lomka$setLastRenderMotionHash(int hash) {
        this.lomka$lastRenderMotionHash = hash;
    }

    @Override
    public boolean lomka$isRenderVisible() {
        return this.lomka$lastRenderVisible;
    }

    @Override
    public void lomka$setRenderVisible(boolean visible) {
        this.lomka$lastRenderVisible = visible;
    }
}