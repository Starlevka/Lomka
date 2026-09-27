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

import lomka.starl.utils.constants.DirectionTables;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Direction.class)
public abstract class MixinDirection {

    /**
     * @author Starlev
     * @reason Single flat-array lookup instead of an outer axis switch plus an inner helper-method
     *         switch (getClockWiseX/getClockWise/getClockWiseZ). Table contents verified cell-by-cell
     *         against the vanilla switch bodies, including the parallel-axis identity entries.
     */
    @Overwrite
    public Direction getClockWise(Direction.Axis axis) {
        return DirectionTables.CLOCKWISE[axis.ordinal() * 6 + ((Direction) (Object) this).ordinal()];
    }

    /**
     * @author Starlev
     * @reason Same single flat-lookup optimization as getClockWise(Axis).
     */
    @Overwrite
    public Direction getCounterClockWise(Direction.Axis axis) {
        return DirectionTables.COUNTERCLOCKWISE[axis.ordinal() * 6 + ((Direction) (Object) this).ordinal()];
    }
}
