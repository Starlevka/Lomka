package lomka.starl.mixins.net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.world.level.lighting.LeveledPriorityQueue;
import net.minecraft.world.level.lighting.SpatialLongSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeveledPriorityQueue.class)
public class MixinLeveledPriorityQueue {

    @Shadow @Final private LongLinkedOpenHashSet[] queues;

        /**
     * @author Starlev
     * @reason Replaces the vanilla LongLinkedOpenHashSet queues with SpatialLongSet,
     *         which packs up to 64 light positions sharing a 4×4×4 key into a single
     *         Long2LongLinkedOpenHashMap entry. This shrinks the per-queue starting
     *         size from 512+ slots to minSize/64, cutting hash-table count, rehash
     *         churn and allocations in the hot light queues (DynamicGraphMinFixedPoint).
     *         dequeue() calls remove(long), but SpatialLongSet renames removal to
     *         rem(long); the override below reroutes remove -> rem so pending nodes are
     *         still deleted correctly. Actually, Mojang don't have uses of SpatialLongSet
     *         at all. So this fixes that.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$spatialQueues(int i, int j, CallbackInfo ci) {
        for (int k = 0; k < this.queues.length; ++k) {
            this.queues[k] = new SpatialLongSet(j * 64, 0.5F) {
                @Override
                public boolean remove(long key) {
                    return this.rem(key);
                }
            };
        }
    }
}