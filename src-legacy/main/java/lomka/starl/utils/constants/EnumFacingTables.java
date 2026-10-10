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

import net.minecraft.util.EnumFacing;

/**
 * Flat lookup table for {@code EnumFacing#rotateAround(Axis)}, indexed by
 * {@code axis.ordinal() * 6 + facing.ordinal()}.
 *
 * <p>Held outside {@code lomka.starl.mixins} and built here rather than in a static field on the
 * mixin, for two reasons. A class that lives in a configured mixin package cannot be loaded by the
 * loader's mixin plugin, so a nested holder inside the mixin would abort the launch. And a static
 * field with an array initializer is merged into the enum's own static initialiser, where it would
 * read {@code EnumFacing.SOUTH} before the constants are constructed. First touch is from an
 * instance method, i.e. after {@code EnumFacing}'s class initialiser has run to completion.
 *
 * <p>Row contents are transcribed from the three private helpers vanilla dispatches to - X from
 * {@code func_176744_n}, Y from {@code func_176746_e}, Z from {@code func_176738_p} - with the
 * parallel-axis entries filled in from the identity checks the public method performs before
 * calling them. Verified cell-by-cell against those switch bodies, plus the four-cycle and
 * bijection invariants, by
 * {@code scripts/bench/src/main/java/lomka/bench/legacy/EnumFacingClockwiseCrossCheck.java}.
 */
public final class EnumFacingTables {

	/** Axis order X/Y/Z, facing order DOWN/UP/NORTH/SOUTH/WEST/EAST. */
	public static final EnumFacing[] CLOCKWISE = {
			/* X */ EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.DOWN,  EnumFacing.UP,    EnumFacing.WEST,  EnumFacing.EAST,
			/* Y */ EnumFacing.DOWN,  EnumFacing.UP,    EnumFacing.EAST,  EnumFacing.WEST,  EnumFacing.NORTH, EnumFacing.SOUTH,
			/* Z */ EnumFacing.WEST,  EnumFacing.EAST,  EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP,    EnumFacing.DOWN
	};

	private EnumFacingTables() {
	}
}
