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

package lomka.starl.mixins.net.minecraft.core;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3i.class)
public abstract class MixinVec3i {

    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int z;

    /**
     * @author Starlev
     * @reason Exhaustive enum switch instead of the virtual Axis.choose dispatch through three
     *         enum constants. Unreachable default is impossible: Direction.Axis has exactly three
     *         constants, so the switch expression stays exhaustive without a fallback branch.
     */
    @Overwrite
    public int get(Direction.Axis axis) {
        return switch (axis) {
            case X -> this.x;
            case Y -> this.y;
            case Z -> this.z;
        };
    }

    /**
     * @author Starlev
     * @reason Vanilla routes the integer deltas through float: three casts to float, an fadd sum,
     *         then a truncating cast back. Floats represent integers exactly only up to 2^24, so
     *         Manhattan distances beyond ~16.7M lose precision (world border range reaches ±30M per
     *         axis, sums up to ~90M) and the float ALU round-trip costs extra cycles. Pure int
     *         arithmetic is faster and exact for every possible input.
     */
    @Overwrite
    public int distManhattan(Vec3i vec3i) {
        return Math.abs(vec3i.getX() - this.x)
             + Math.abs(vec3i.getY() - this.y)
             + Math.abs(vec3i.getZ() - this.z);
    }
}
