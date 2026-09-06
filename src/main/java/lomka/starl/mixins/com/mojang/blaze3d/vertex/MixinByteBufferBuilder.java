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

package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ByteBufferBuilder.class, priority = 999) // VulkanMod compability
public abstract class MixinByteBufferBuilder {

    //? if >=1.21.6 {
    @Shadow @Final private long maxCapacity;
    @Shadow long pointer;
    @Shadow private long capacity;
    @Shadow private long writeOffset;

    @Shadow protected abstract void resize(long newCapacity);

    /**
     * @author Starlev
     * @reason Inlines fast-path capacity check and replaces method call to ensureCapacity
     *         with direct branch; drops Math.addExact on both offsets since maxCapacity is
     *         a long but real buffer sizes are kilobytes — 64-bit overflow is physically impossible.
     */
    @Overwrite
    public long reserve(int size) {
        long currentOffset = this.writeOffset;
        long newOffset     = currentOffset + (long) size;

        if (newOffset > this.capacity) {
            this.lomka$grow(newOffset);
        }

        this.writeOffset = newOffset;
        return this.pointer + currentOffset;
    }

    /**
     * Expands maximum single-step growth from 2 MB to 8 MB to prevent
     * repeated native reallocations during large chunk/batch surges.
     */
    private void lomka$grow(long requiredCapacity) {
        if (requiredCapacity > this.maxCapacity) {
            throw new IllegalArgumentException("Maximum capacity of ByteBufferBuilder (" + this.maxCapacity + ") exceeded, required " + requiredCapacity);
        }

        long growth         = Math.min(this.capacity, 8388608L);
        long targetCapacity = Mth.clamp(this.capacity + growth, requiredCapacity, this.maxCapacity);

        this.resize(targetCapacity);
    }
    //?} else {
    /*@Shadow long pointer;
    @Shadow private int capacity;
    @Shadow private int writeOffset;

    @Shadow protected abstract void resize(int i);

    @Overwrite
    public long reserve(int i) {
        int j = this.writeOffset;
        int k = j + i;
        if (k > this.capacity) {
            this.lomka$grow(k);
        }
        this.writeOffset = k;
        return this.pointer + (long) j;
    }

    private void lomka$grow(int requiredCapacity) {
        int growth = Math.min(this.capacity, 8388608);
        int targetCapacity = Math.max(this.capacity + growth, requiredCapacity);
        this.resize(targetCapacity);
    }
    *///?}
}
