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

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DynamicGpuDataStorageMapped.class)
public abstract class MixinDynamicGpuDataStorageMapped<T extends DynamicGpuDataStorage.DynamicGpuData> {

    @Shadow private @Nullable T lastData;
    @Shadow private MappableRingBuffer ringBuffer;
    @Shadow private int nextBlock;
    @Shadow private int capacity;
    @Shadow @Final private int blockSize;
    @Shadow @Final private String label;
    @Shadow @Final private static Logger LOGGER;

    @Unique private GpuBufferSlice lomka$lastSlice;

    /**
     * Returns a {@link GpuBufferSlice} for {@code (buffer, offset, blockSize)},
     * re-emitting the previously returned instance when the buffer and offset match.
     * GpuBufferSlice is an immutable record, so aliasing a repeated request is
     * observably identical to handing out a fresh copy; ring rotation and
     * resizeBuffers invalidate the cache implicitly through buffer identity.
     *
     * <p>The length is deliberately not compared. Every slice this class ever stores
     * in the cache is built with {@code blockSize} as its length - here, and by the
     * tail element of the batch write - so the component cannot differ and the test
     * could never fail. Do not add a third write path to the cache without
     * restoring it.
     */
    @Unique
    private GpuBufferSlice lomka$sliceFor(GpuBuffer gpubuffer, int i) {
        GpuBufferSlice cached = this.lomka$lastSlice;

        if (cached != null && cached.buffer() == gpubuffer && cached.offset() == (long) i) {
            return cached;
        } else {
            GpuBufferSlice fresh = gpubuffer.slice((long) i, (long) this.blockSize);
            this.lomka$lastSlice = fresh;
            return fresh;
        }
    }

    @Shadow
    private void resizeBuffers(int i) {
        throw new UnsupportedOperationException();
    }

    /**
     * @author Starlev
     * @reason 26.3 successor of the DynamicUniformStorage patch: adds a
     *         reference-equality short-circuit before the value-based equals()
     *         check and caches ringBuffer.currentBuffer() (vanilla calls the
     *         getter twice per writeData even though rotation only happens in
     *         endFrame(), never mid-write), then deduplicates the returned slice
     *         through lomka$lastSlice so repeated fast-path hits re-emit the same
     *         immutable record instead of allocating a fresh one per call. The
     *         cache is read strictly AFTER the resize check, since resizeBuffers()
     *         reassigns the ringBuffer field.
     *
     *         The slow path builds its slice directly instead of routing it
     *         through lomka$sliceFor: the cached slice belongs to the previous
     *         block, so its offset is always (nextBlock - 1) * blockSize and can
     *         never equal the offset being written now, and after a ring rotation
     *         or a resize the buffer differs as well. The probe could only ever
     *         miss, so it was three compares spent on a guaranteed miss; the fresh
     *         slice is still stored, which is what primes the next fast-path hit.
     *
     *         The returned slice is the one the mapping already built -
     *         GpuBufferSlice.MappedView is a record over (slice, data, onClose)
     *         and the backend fills that slice with GpuBuffer#slice(offset, length)
     *         from the very arguments it was handed, so it is component-for-
     *         component the slice vanilla allocates here. Returning it drops the
     *         second record per write without changing what the caller receives.
     */
    @Overwrite
    public GpuBufferSlice writeData(T gpuData) {
        if (this.lastData != null && (gpuData == this.lastData || this.lastData.equals(gpuData))) {
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
        GpuBufferSlice written;

        try (GpuBufferSlice.MappedView mappedView = currentBuffer.slice((long) offset, (long) this.blockSize).map(false, true)) {
            gpuData.write(mappedView.data());
            written = mappedView.slice();
        }

        ++this.nextBlock;
        this.lastData = gpuData;
        this.lomka$lastSlice = written;
        return written;
    }

    /**
     * @author Starlev
     * @reason Same currentBuffer() caching as writeData, scaled to matter much
     *         more here: vanilla re-fetches currentBuffer() once per element in
     *         the batch (N calls for N data entries) purely to build each slice,
     *         even though every one of those calls returns the identical object
     *         for the whole duration of this method. Cached once, after the
     *         resize check. The tail element's slice primes lomka$lastSlice so an
     *         immediately following writeData() fast-path hit re-emits it
     *         allocation-free.
     */
    @Overwrite
    public GpuBufferSlice[] writeData(T[] dataArray) {
        if (dataArray.length == 0) {
            return new GpuBufferSlice[0];
        }

        if (this.nextBlock + dataArray.length > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, dataArray.length));
            LOGGER.info(
                "Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.",
                this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int baseOffset          = this.nextBlock * this.blockSize;
        GpuBuffer currentBuffer = this.ringBuffer.currentBuffer();
        GpuBufferSlice[] slices = new GpuBufferSlice[dataArray.length];

        try (GpuBufferSlice.MappedView mappedView = currentBuffer.slice((long) baseOffset, (long) (dataArray.length * this.blockSize)).map(false, true)) {
            ByteBuffer byteBuffer = mappedView.data();
            for (int j = 0; j < dataArray.length; ++j) {
                T data = dataArray[j];
                int elementOffset = j * this.blockSize;
                slices[j] = currentBuffer.slice((long) (baseOffset + elementOffset), (long) this.blockSize);
                byteBuffer.position(elementOffset);
                data.write(byteBuffer);
            }
        }

        this.nextBlock      += dataArray.length;
        this.lastData        = dataArray[dataArray.length - 1];
        this.lomka$lastSlice = slices[dataArray.length - 1];
        return slices;
    }
}
