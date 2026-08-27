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

package lomka.starl.mixins.net.minecraft.client.renderer.culling;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the four Math.floor/Math.ceil library calls in
 * {@code offsetToFullyIncludeCameraCube} with branchless integer-cast
 * variants. The method runs once per frame on the critical section-graph
 * setup path; the intrinsic-eligible cast form keeps the JIT from
 * emitting the slower library path.
 */
@Mixin(Frustum.class)
public class MixinFrustum {

    /**
     * Branchless floor replacement avoiding Math.floor's library path on the per-frame cube setup.
     */
    @Redirect(
            method = "offsetToFullyIncludeCameraCube",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;floor(D)D"
            )
    )
    private double lomka$fastFloor(double v) {
        long i = (long) v;
        return v < (double) i ? (double) (i - 1L) : (double) i;
    }

    /**
     * Branchless ceil replacement avoiding Math.ceil's library path on the per-frame cube setup.
     */
    @Redirect(
            method = "offsetToFullyIncludeCameraCube",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;ceil(D)D"
            )
    )
    private double lomka$fastCeil(double v) {
        long i = (long) v;
        return v > (double) i ? (double) (i + 1L) : (double) i;
    }
}
