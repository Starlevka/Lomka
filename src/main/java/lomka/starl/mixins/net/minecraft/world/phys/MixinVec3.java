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

package lomka.starl.mixins.net.minecraft.world.phys;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3.class)
public class MixinVec3 {

    @Shadow @Final public double x;
    @Shadow @Final public double y;
    @Shadow @Final public double z;

    /**
     * @author Starlev
     * @reason One division plus three multiplications instead of three divisions; the
     *         near-zero guard is preserved exactly. The product form can differ from
     *         vanilla by 1 ulp on some inputs (double rounding instead of single);
     *         accepted: direction use sites only consume the unit direction.
     */
    @Overwrite
    public Vec3 normalize() {
        double d0 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (d0 < 9.999999747378752E-6D) {
            return Vec3.ZERO;
        }
        double inv = 1.0D / d0;
        return new Vec3(this.x * inv, this.y * inv, this.z * inv);
    }
}
