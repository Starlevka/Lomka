package lomka.starl.mixins.net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//? if <26.2 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DynamicUniformStorage.class)
public abstract class MixinDynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> {

    /*
    * Actual work without Sodium or Embeddium mods. 
    * No causes any conflicts with them, just a must-have.
    */

    @Shadow private @Nullable T lastUniform;
    @Shadow private MappableRingBuffer ringBuffer;
    @Shadow private int nextBlock;
    @Shadow private int capacity;
    @Shadow @Final private int blockSize;
    @Shadow @Final private String label;
    @Shadow @Final private static Logger LOGGER;

    @Shadow
    private void resizeBuffers(int i) {
        throw new UnsupportedOperationException();
    }

    /**
     * @author Starlev
     * @reason Adds a reference-equality short-circuit before the value-based
     * equals() check. Note: if T is a record, its generated equals() already
     * performs this exact check internally for free (verified empirically:
     * a record's auto-generated equals() invokes zero field-level equals()
     * calls when compared against itself), so this mainly guards the case
     * where T is a hand-written class without that same fast path, at
     * negligible cost either way.
     *
     * The actual win here is caching ringBuffer.currentBuffer(): vanilla
     * calls this getter twice per writeUniform() invocation even though
     * nothing between those calls can change which buffer is "current"
     * (rotation only happens in endFrame(), never mid-write). Cached once,
     * strictly AFTER the resize check, since resizeBuffers() reassigns the
     * ringBuffer field — caching before that point would return a slice
     * into a buffer that's about to be replaced.
     */
    @Overwrite
    public GpuBufferSlice writeUniform(T t0) {
        if (this.lastUniform != null && (t0 == this.lastUniform || this.lastUniform.equals(t0))) {
            //? if >=1.21.11 {
            return this.ringBuffer.currentBuffer()
                .slice((long) ((this.nextBlock - 1) * this.blockSize), (long) this.blockSize);
            //?} else {
            /*return this.ringBuffer.currentBuffer()
                .slice((this.nextBlock - 1) * this.blockSize, this.blockSize);*/
            //?}
        }

        if (this.nextBlock >= this.capacity) {
            int newCapacity = this.capacity * 2;
            LOGGER.info(
                "Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.",
                this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int offset = this.nextBlock * this.blockSize;
        GpuBuffer currentBuffer = this.ringBuffer.currentBuffer();

        //? if >=26.2 {
        /*try (GpuBufferSlice.MappedView mappedView = currentBuffer.slice((long) offset, (long) this.blockSize).map(false, true)) {*/
        //?} else if >=1.21.11 {
        try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(currentBuffer.slice((long) offset, (long) this.blockSize), false, true)) {
        //?} else {
        /*try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(currentBuffer.slice(offset, this.blockSize), false, true)) {*/
        //?}
            t0.write(mappedView.data());
        }

        ++this.nextBlock;
        this.lastUniform = t0;
        //? if >=1.21.11 {
        return currentBuffer.slice((long) offset, (long) this.blockSize);
        //?} else {
        /*return currentBuffer.slice(offset, this.blockSize);*/
        //?}
    }

    /**
     * @author Starlev
     * @reason Same currentBuffer() caching as writeUniform, scaled to matter
     * much more here: vanilla re-fetches currentBuffer() once per element in
     * the batch (N calls for N uniforms) purely to build each slice, even
     * though every one of those calls returns the identical object for the
     * whole duration of this method. Cached once, after the resize check.
     */
    @Overwrite
    public GpuBufferSlice[] writeUniforms(T[] at) {
        if (at.length == 0) {
            return new GpuBufferSlice[0];
        }

        if (this.nextBlock + at.length > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, at.length));
            LOGGER.info(
                "Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.",
                this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int baseOffset = this.nextBlock * this.blockSize;
        GpuBuffer currentBuffer = this.ringBuffer.currentBuffer();
        GpuBufferSlice[] slices = new GpuBufferSlice[at.length];

        //? if >=26.2 {
        /*try (GpuBufferSlice.MappedView mappedView = currentBuffer.slice((long) baseOffset, (long) (at.length * this.blockSize)).map(false, true)) {*/
        //?} else if >=1.21.11 {
        try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(currentBuffer.slice((long) baseOffset, (long) (at.length * this.blockSize)), false, true)) {
        //?} else {
        /*try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(currentBuffer.slice(baseOffset, at.length * this.blockSize), false, true)) {*/
        //?}
            ByteBuffer byteBuffer = mappedView.data();
            for (int j = 0; j < at.length; ++j) {
                T uniform = at[j];
                int elementOffset = j * this.blockSize;
                //? if >=1.21.11 {
                slices[j] = currentBuffer.slice((long) (baseOffset + elementOffset), (long) this.blockSize);
                //?} else {
                /*slices[j] = currentBuffer.slice(baseOffset + elementOffset, this.blockSize);*/
                //?}
                byteBuffer.position(elementOffset);
                uniform.write(byteBuffer);
            }
        }

        this.nextBlock += at.length;
        this.lastUniform = at[at.length - 1];
        return slices;
    }
}