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

package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GpuBuffer.class)
public abstract class MixinGpuBuffer {

    //? if >=1.21.11 {
    @Shadow public abstract long size();
    //?} else {
    /*@Shadow public abstract int size();
    *///?}

    @Unique private GpuBufferSlice lomka$fullSlice;

    /**
     * @author Starlev
     * @reason Lazy-cache the full-buffer GpuBufferSlice instead of allocating
     *         a new slice object on every call. Config-gated out on 26.2+,
     *         where vanilla precomputes the same slice as a final
     *         {@code defaultSlice} field in the constructor — there is
     *         nothing left to optimize there.
     */
    @Overwrite
    public GpuBufferSlice slice() {
        if (this.lomka$fullSlice == null) {
            //? if >=1.21.11 {
            this.lomka$fullSlice = new GpuBufferSlice((GpuBuffer) (Object) this, 0L, this.size());
            //?} else {
            /*this.lomka$fullSlice = new GpuBufferSlice((GpuBuffer) (Object) this, 0, (int) this.size());
            *///?}
        }
        return this.lomka$fullSlice;
    }
}