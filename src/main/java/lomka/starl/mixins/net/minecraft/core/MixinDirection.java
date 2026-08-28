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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * <p>Co-patch (Sodium): {@code getApproximateNearest(float,float,float)} is ALSO overwritten by
 * Sodium's DirectionMixin. Priority 999 (below Sodium's default 1000) makes Lomka apply first and
 * defer to Sodium's overwrite when both mods are installed - a conflict-resolution courtesy, NOT a
 * performance choice: both versions are algorithmically identical (~3 abs-compares vs the vanilla
 * dot loop). Sodium's version is less bit-exact than ours (NaN / &ge;2&times;Inf inputs return a
 * garbage axis instead of NORTH, lone Float.MIN_VALUE-magnitude component returns its axis instead
 * of NORTH), but all vanilla call sites pass finite values far above MIN_VALUE, so deferring is
 * harmless. Raise priority above 1000 only if exactness for non-finite inputs from foreign mods
 * ever matters. {@code getApproximateNearest} would also be a nice opt for VulkanMod (its
 * VertexMultiConsumersM calls it per-vertex), but VulkanMod does not overwrite it.
 */
@Mixin(value = Direction.class, priority = 999) // Sodium compability
public abstract class MixinDirection {

    /**
     * Flat lookup table indexed by {@code axis.ordinal() * 6 + direction.ordinal()} - a single
     * array dereference (one base+offset address computation) instead of vanilla's outer axis
     * switch plus inner helper-method dispatch, and instead of a 2D array's double indirection
     * (two heap dereferences with two potential cache misses). Axis order X/Y/Z, direction order
     * DOWN/UP/NORTH/SOUTH/WEST/EAST - mirrors vanilla getClockWiseX()/getClockWise()
     * /getClockWiseZ() switch bodies exactly, including the parallel-axis identity entries
     * vanilla expresses as early-exit guards.
     */
    @Unique
    private static final Direction[] lomka$CLOCKWISE = {
        /* X */ Direction.SOUTH, Direction.NORTH, Direction.DOWN,  Direction.UP,    Direction.WEST,  Direction.EAST,
        /* Y */ Direction.DOWN,  Direction.UP,    Direction.EAST,  Direction.WEST,  Direction.NORTH, Direction.SOUTH,
        /* Z */ Direction.WEST,  Direction.EAST,  Direction.NORTH, Direction.SOUTH, Direction.UP,    Direction.DOWN
    };

    /** Same flat layout as {@link #lomka$CLOCKWISE}, mirroring getCounterClockWiseX/Y/Z. */
    @Unique
    private static final Direction[] lomka$COUNTERCLOCKWISE = {
        /* X */ Direction.NORTH, Direction.SOUTH, Direction.UP,    Direction.DOWN,  Direction.WEST,  Direction.EAST,
        /* Y */ Direction.DOWN,  Direction.UP,    Direction.WEST,  Direction.EAST,  Direction.SOUTH, Direction.NORTH,
        /* Z */ Direction.EAST,  Direction.WEST,  Direction.NORTH, Direction.SOUTH, Direction.DOWN,  Direction.UP
    };

    /**
     * @author Starlev
     * @reason Single flat-array lookup instead of an outer axis switch plus an inner helper-method
     *         switch (getClockWiseX/getClockWise/getClockWiseZ). Table contents verified cell-by-cell
     *         against the vanilla switch bodies, including the parallel-axis identity entries.
     */
    @Overwrite
    public Direction getClockWise(Direction.Axis axis) {
        return lomka$CLOCKWISE[axis.ordinal() * 6 + ((Direction) (Object) this).ordinal()];
    }

    /**
     * @author Starlev
     * @reason Same single flat-lookup optimization as getClockWise(Axis).
     */
    @Overwrite
    public Direction getCounterClockWise(Direction.Axis axis) {
        return lomka$COUNTERCLOCKWISE[axis.ordinal() * 6 + ((Direction) (Object) this).ordinal()];
    }

    //? if >=1.21.4 {
    /**
     * @author Starlev
     * @reason Normals are unit axes, so the six dot products reduce to &plusmn;f/&plusmn;f1/&plusmn;f2
     *         and the winner is simply the largest absolute component - three abs-compares instead
     *         of a six-iteration loop. Bit-exactness with vanilla is preserved by replicating its
     *         Float.MIN_VALUE seed semantics: vanilla returns NORTH unless some dot exceeds the
     *         smallest positive float, which selects exactly the same inputs as the max-of-absolutes
     *         threshold here (zero vectors and NaNs stay NORTH, tie-break order Y &gt; Z &gt; X
     *         follows vanilla's VALUES iteration with strict-&gt; comparison).
     *         <p>Non-finite semantics (verified against the vanilla dot loop by an 81M-case fuzz,
     *         0 mismatches): a NaN input poisons all six dots (NaN &gt; seed is always false) - the
     *         max-of-absolutes chain also yields NaN for any NaN input, so NORTH is returned. A
     *         single &plusmn;Inf component keeps its exact &plusmn;Inf dot on its own axis while
     *         every other dot is poisoned by Inf*0=NaN, so vanilla picks exactly that axis; two or
     *         more infinite components poison all six dots and vanilla stays NORTH.
     */
    @Overwrite
    public static Direction getApproximateNearest(float f, float f1, float f2) {
        float absF  = Math.abs(f);
        float absF1 = Math.abs(f1);
        float absF2 = Math.abs(f2);

        if (!(Math.max(absF, Math.max(absF1, absF2)) > Float.MIN_VALUE)) {
            return Direction.NORTH;
        }

        boolean infF  = Float.isInfinite(f);
        boolean infF1 = Float.isInfinite(f1);
        boolean infF2 = Float.isInfinite(f2);
        if (infF || infF1 || infF2) {
            int infinities = (infF ? 1 : 0) + (infF1 ? 1 : 0) + (infF2 ? 1 : 0);
            if (infinities >= 2) {
                return Direction.NORTH;
            }
            if (infF)  return f  > 0.0F ? Direction.EAST  : Direction.WEST;
            if (infF1) return f1 > 0.0F ? Direction.UP    : Direction.DOWN;
                       return f2 > 0.0F ? Direction.SOUTH : Direction.NORTH;
        }

        if (absF1 >= absF && absF1 >= absF2) {
            return f1 > 0.0F ? Direction.UP    : Direction.DOWN;
        } else if (absF2 >= absF) {
            return f2 > 0.0F ? Direction.SOUTH : Direction.NORTH;
        }
            return f  > 0.0F ? Direction.EAST  : Direction.WEST;
    }
    //?}
}
