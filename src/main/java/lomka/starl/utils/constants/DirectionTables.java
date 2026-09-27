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

package lomka.starl.utils.constants;

import net.minecraft.core.Direction;

/**
 * Flat lookup tables for {@code Direction#getClockWise(Axis)} / {@code getCounterClockWise(Axis)},
 * indexed by {@code axis.ordinal() * 6 + direction.ordinal()}.
 *
 * <p>Kept outside {@code lomka.starl.mixins} on purpose: ModLauncher's MixinLaunchPluginLegacy
 * refuses to load any class that lives in a configured mixin package, so a nested holder inside
 * the mixin itself aborts every Forge and NeoForge launch with
 * {@code IllegalClassLoadError: ... is in a defined mixin package}. The tables are also built
 * here rather than in a static field on the mixin, because a static field with an array
 * initializer is merged into the enum's own static initialiser, where it would read
 * {@code Direction.SOUTH} before the constants are constructed. This class is first touched from
 * an instance method, i.e. after the enum is fully initialised.
 */
public final class DirectionTables {

    /** Axis order X/Y/Z, direction order DOWN/UP/NORTH/SOUTH/WEST/EAST. */
    public static final Direction[] CLOCKWISE = {
        /* X */ Direction.SOUTH, Direction.NORTH, Direction.DOWN,  Direction.UP,    Direction.WEST,  Direction.EAST,
        /* Y */ Direction.DOWN,  Direction.UP,    Direction.EAST,  Direction.WEST,  Direction.NORTH, Direction.SOUTH,
        /* Z */ Direction.WEST,  Direction.EAST,  Direction.NORTH, Direction.SOUTH, Direction.UP,    Direction.DOWN
    };

    /** Same flat layout as {@link #CLOCKWISE}, mirroring getCounterClockWiseX/Y/Z. */
    public static final Direction[] COUNTERCLOCKWISE = {
        /* X */ Direction.NORTH, Direction.SOUTH, Direction.UP,    Direction.DOWN,  Direction.WEST,  Direction.EAST,
        /* Y */ Direction.DOWN,  Direction.UP,    Direction.WEST,  Direction.EAST,  Direction.SOUTH, Direction.NORTH,
        /* Z */ Direction.EAST,  Direction.WEST,  Direction.NORTH, Direction.SOUTH, Direction.DOWN,  Direction.UP
    };

    private DirectionTables() {
    }
}
