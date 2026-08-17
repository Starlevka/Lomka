package lomka.starl.mixins.net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.StagedVertexBuffer$GpuBufferPool")
public abstract class MixinGpuBufferPool {

    /**
     * @author Starlev
     * @reason Vanilla's GpuBufferPool tracks three lists per pool:
     *         `available` (recycled buffers idle and waiting for reuse),
     *         `usedThisFrame` (acquired this frame, still riding the GPU
     *         pipeline), and `pendingRecycle` (the used-this-frame batch,
     *         snapshot-fenced via GpuFence in endFrame() and held until the GPU
     *         finishes with it). endFrame() fences the whole `usedThisFrame`
     *         as one PendingRecycle, then unconditionally closes EVERY buffer
     *         left in `available` (the 2nd isEmpty() check, ordinal 1) - nuking
     *         the warm cache even though the next frame usually wants the same
     *         size-bucketed buffers again. When GPU-bound the GPU lags by
     *         ~MAX_SUBMITS_IN_FLIGHT(2) frames, so recycling is delayed and any
     *         buffer that completes its fence but isn't grabbed in the same
     *         frame's acquire() survives into endFrame's wipe -> destroyed ->
     *         re-allocated by device.createBuffer() (driver stall) next time
     *         that size is needed = reallocation stutter. Keeping up to 6 alive
     *         preserves that cache; 6 is the steady-state ceiling of
     *         not-yet-reused buffers: (MAX_SUBMITS_IN_FLIGHT 2 + 1 current
     *         frame) x 2, the x2 absorbing recycling-latency slide across
     *         size-bucket batches. GpuBuffer::close thus essentially never runs
     *         in steady state.
     */
    @Unique
    private static final int lomka$threshold = 6;

    /**
     * @author Starlev
     * @reason endFrame()'s 2nd isEmpty() (ordinal 1) gates the all-or-nothing
     *         `available.forEach(GpuBuffer::close)`. This redirect swaps that
     *         check for `available.size() <= threshold` (6): it reports the pool
     *         "empty" while the cache is within bounds, so `!isEmpty` is false and
     *         the wipe is skipped - recycled buffers survive endFrame() and get
     *         re-picked by acquire()'s takeBestAvailable() next frame instead of
     *         being re-allocated. Only once available balloons past 6 (an
     *         abnormal spike where one rebuild outruns recycling) does vanilla's
     *         full drain run - capping VRAM at the cost of a single one-off
     *         reallocation. usedThisFrame (ordinal 0 gate) and pendingRecycle
     *         are untouched; buffers are still always freed via
     *         GpuBufferPool.close() / StagedVertexBuffer.close().
     */
    @Redirect(
        method = "endFrame",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z",
            ordinal = 1
        )
    )
    private boolean lomka$keepBuffers(List<GpuBuffer> available) {
        return available.size() <= lomka$threshold;
    }
}