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

package lomka.starl.mixins.com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(RenderSystem.AutoStorageIndexBuffer.class)
public abstract class MixinAutoStorageIndexBuffer {

    /**
     * Minimum index count requested from {@code ensureStorage} by the warmup
     * injection below. Combined with vanilla's doubling this grows the shared
     * buffer to at least 8192 indices (~16 KB SHORT) on first use instead of a
     * few dozen, skipping the first five growth reallocations that otherwise
     * hitch mid-frame during early rendering.
     */
    private static final int WARMUP_INDEX_COUNT = 4096;

    /**
     * @author Starlev
     * @reason Vanilla grows the shared sequential index buffer by doubling,
     *         and every growth reallocates the GPU buffer mid-frame (full
     *         index regeneration plus GpuBuffer close/create, or a GL data
     *         upload on <=1.21.4). Each regen stalls the frame that triggers
     *         it — typically chunk rendering after entering new areas, complex
     *         GUI batching, or cloud quad spikes. Growing x4 instead of x2
     *         halves the number of regens on the log scale and doubles the
     *         time between them; buffer contents stay byte-identical to
     *         vanilla's, only the allocation schedule changes.
     */
    @ModifyConstant(method = "ensureStorage", constant = @Constant(intValue = 2))
    private int lomka$widenIndexBufferGrowth(int original) {
        return 4;
    }

    /**
     * @author Starlev
     * @reason Warm up the shared index buffer to WARMUP_INDEX_COUNT on the
     *         first request so early small draws (crosshair, sky, GUI) do not
     *         walk through several grow-regen cycles in the first frames.
     *         Once hasStorage() covers the floor this is just an int compare
     *         on every call. The host method wrapping ensureStorage() changed
     *         from bind(int) to getBuffer(int) in 1.21.6.
     */
    //? if <1.21.6 {
    /*@ModifyArg(method = "bind", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/systems/RenderSystem$AutoStorageIndexBuffer;ensureStorage(I)V"))
     *///?} else {
    @ModifyArg(method = "getBuffer", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/systems/RenderSystem$AutoStorageIndexBuffer;ensureStorage(I)V"))
    //?}
    private int lomka$warmupIndexBuffer(int requested) {
        return Math.max(requested, WARMUP_INDEX_COUNT);
    }
}
