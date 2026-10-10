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

import net.minecraft.util.math.Direction;

/**
 * Flat lookup table for {@code Direction#clockwise(Axis)}, indexed by
 * {@code axis.ordinal() * 6 + direction.ordinal()}.
 *
 * <p>Kept outside {@code lomka.starl.mixins} on purpose: a class that lives in a configured mixin
 * package cannot be loaded by the loader's mixin plugin, so a nested holder inside the mixin itself
 * would abort the launch. The table is also built here rather than in a static field on the mixin,
 * because a static field with an array initializer is merged into the enum's own static initialiser,
 * where it would read {@code Direction.SOUTH} before the constants are constructed. First touch is
 * from an instance method, i.e. after {@code Direction}'s class initialiser has run to completion.
 *
 * <p>The rows were read out of the mapped jars rather than transcribed from the bytecode: {@code
 * clockwise} dispatches through three private helpers and a synthetic switch map, so hand-decoding
 * it is a silent-corruption risk. Vanilla returns the receiver unchanged for the axis it is parallel
 * to, which is why WEST and EAST are the identity in the X row, UP and DOWN in the Y row and NORTH
 * and SOUTH in the Z row.
 *
 * <p>Identical on 1.8.9 and 1.12.2 - both verified - and identical to the modern build's table.
 * Only 1.7.10 differs, and there {@code Direction} has no rotation methods at all, so nothing reads
 * it.
 */
public final class DirectionTables {

	/** Axis order X/Y/Z, direction order DOWN/UP/NORTH/SOUTH/WEST/EAST. */
	public static final Direction[] CLOCKWISE = {
			/* X */ Direction.SOUTH, Direction.NORTH, Direction.DOWN,  Direction.UP,    Direction.WEST,  Direction.EAST,
			/* Y */ Direction.DOWN,  Direction.UP,    Direction.EAST,  Direction.WEST,  Direction.NORTH, Direction.SOUTH,
			/* Z */ Direction.WEST,  Direction.EAST,  Direction.NORTH, Direction.SOUTH, Direction.UP,    Direction.DOWN
	};

	private DirectionTables() {
	}
}