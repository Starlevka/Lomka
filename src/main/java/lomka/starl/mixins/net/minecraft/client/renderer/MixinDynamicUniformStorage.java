/*
 * This file is part of Lomka (https://github.com/Starlevka/Lomka)
 * Copyright (C) 2026 Starlev (a.k.a. Starlevka) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 */

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
import org.spongepowered.asm.mixin.Unique;

@Mixin(DynamicUniformStorage.class)
public abstract class MixinDynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> {

    @Shadow private @Nullable T lastUniform;
    @Shadow private MappableRingBuffer ringBuffer;
    @Shadow private int nextBlock;
    @Shadow private int capacity;
    @Shadow @Final private int blockSize;
    @Shadow @Final private String label;
    @Shadow @Final private static Logger LOGGER;

    @Unique private GpuBufferSlice lomka$lastSlice;

    /**
     * Returns a {@link GpuBufferSlice} for {@code (buffer, offset, blockSize)},
     * re-emitting the previously returned instance when every component matches.
     * GpuBufferSlice is an immutable record, so aliasing a repeated request is
     * observably identical to handing out a fresh copy; ring rotation and
     * resizeBuffers invalidate the cache implicitly through buffer identity.
     */
    @Unique
    private GpuBufferSlice lomka$sliceFor(GpuBuffer gpubuffer, int i) {
        GpuBufferSlice gpubufferslice = this.lomka$lastSlice;

        if (gpubufferslice != null && gpubufferslice.buffer() == gpubuffer
                && gpubufferslice.offset() == (long) i && gpubufferslice.length() == (long) this.blockSize) {
            return gpubufferslice;
        } else {
            //? if >=1.21.11 {
            gpubufferslice = gpubuffer.slice((long) i, (long) this.blockSize);
            //?} else {
            /*gpubufferslice = gpubuffer.slice(i, this.blockSize);
            *///?}
            this.lomka$lastSlice = gpubufferslice;
            return gpubufferslice;
        }
    }

    @Shadow
    private void resizeBuffers(int i) {
        throw new UnsupportedOperationException();
    }

    /**
     * @author Starlev
     * @reason Adds a reference-equality short-circuit before the value-based
     *         equals() check. Note: if T is a record, its generated equals() already
     *         performs this exact check internally for free (verified empirically:
     *         a record's auto-generated equals() invokes zero field-level equals()
     *         calls when compared against itself), so this mainly guards the case
     *         where T is a hand-written class without that same fast path, at
     *         negligible cost either way.
     *
     *         The actual win here is caching ringBuffer.currentBuffer(): vanilla
     *         calls this getter twice per writeUniform() invocation even though
     *         nothing between those calls can change which buffer is "current"
     *         (rotation only happens in endFrame(), never mid-write). Cached once,
     *         strictly AFTER the resize check, since resizeBuffers() reassigns the
     *         ringBuffer field — caching before that point would return a slice
     *         into a buffer that's about to be replaced.
     *
     *         Returned slices are additionally deduplicated through lomka$lastSlice:
     *         repeated fast-path hits re-emit the same immutable record instead of
     *         allocating a fresh one per call.
     */
    @Overwrite
    public GpuBufferSlice writeUniform(T t0) {
        if (this.lastUniform != null && (t0 == this.lastUniform || this.lastUniform.equals(t0))) {
            return this.lomka$sliceFor(this.ringBuffer.currentBuffer(), (this.nextBlock - 1) * this.blockSize);
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
                .mapBuffer(currentBuffer.slice(offset, this.blockSize), false, true)) {
        *///?}
            t0.write(mappedView.data());
        }

        ++this.nextBlock;
        this.lastUniform = t0;
        return this.lomka$sliceFor(currentBuffer, offset);
    }

    /**
     * @author Starlev
     * @reason Same currentBuffer() caching as writeUniform, scaled to matter
     *         much more here: vanilla re-fetches currentBuffer() once per element in
     *         the batch (N calls for N uniforms) purely to build each slice, even
     *         though every one of those calls returns the identical object for the
     *         whole duration of this method. Cached once, after the resize check.
     *         The tail element's slice primes lomka$lastSlice so an immediately
     *         following writeUniform() fast-path hit re-emits it allocation-free.
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

        int baseOffset          = this.nextBlock * this.blockSize;
        GpuBuffer currentBuffer = this.ringBuffer.currentBuffer();
        GpuBufferSlice[] slices = new GpuBufferSlice[at.length];

        //? if >=26.2 {
        /*try (GpuBufferSlice.MappedView mappedView = currentBuffer.slice((long) baseOffset, (long) (at.length * this.blockSize)).map(false, true)) {*/
        //?} else if >=1.21.11 {
        try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(currentBuffer.slice((long) baseOffset, (long) (at.length * this.blockSize)), false, true)) {
        //?} else {
        /*try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(currentBuffer.slice(baseOffset, at.length * this.blockSize), false, true)) {
        *///?}
            ByteBuffer byteBuffer = mappedView.data();
            for (int j = 0; j < at.length; ++j) {
                T uniform = at[j];
                int elementOffset = j * this.blockSize;
                //? if >=1.21.11 {
                slices[j] = currentBuffer.slice((long) (baseOffset + elementOffset), (long) this.blockSize);
                //?} else {
                /*slices[j] = currentBuffer.slice(baseOffset + elementOffset, this.blockSize);
                *///?}
                byteBuffer.position(elementOffset);
                uniform.write(byteBuffer);
            }
        }

        this.nextBlock += at.length;
        this.lastUniform = at[at.length - 1];
        this.lomka$lastSlice = slices[at.length - 1];
        return slices;
    }
}