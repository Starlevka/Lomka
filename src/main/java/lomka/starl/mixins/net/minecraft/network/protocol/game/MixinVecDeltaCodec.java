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

package lomka.starl.mixins.net.minecraft.network.protocol.game;

import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * <p>Origin: zero-delta fast path ported from Collections Of Optimizations by Misanthropy (LGPLv3).
 * Reimplemented here as a direct {@code @Overwrite} without the MixinExtras/config
 * wrapper so the hot tracker-delta path stays branch-lean on every version.
 */
@Mixin(value = VecDeltaCodec.class, priority = 999) // Lower priority to apply compatibility with CoO
public abstract class MixinVecDeltaCodec {

    @Shadow private Vec3 base;

    /**
     * @author Misanthropy (original idea, Collections Of Optimizations), Starlev (Lomka port)
     * @reason Skip the subtract+Vec3 allocation when the delta is exactly zero;
     *         vanilla always returns {@code new Vec3(...subtract...)} even when
     *         the result would be {@link Vec3#ZERO}. Saves one heap allocation per
     *         unchanged tracker delta on the network codec path.
     */
    @Overwrite
    public Vec3 delta(Vec3 vec3) {
        Vec3 base = this.base;
        return vec3.x == base.x
            && vec3.y == base.y
            && vec3.z == base.z
             ? Vec3.ZERO
             : vec3.subtract(base);
    }
}
