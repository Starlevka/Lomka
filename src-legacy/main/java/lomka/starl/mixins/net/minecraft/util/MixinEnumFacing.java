//$ if >=1.8

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

package lomka.starl.mixins.net.minecraft.util;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import lomka.starl.utils.constants.EnumFacingTables;
import net.minecraft.util.EnumFacing;

/**
 * Port of {@code MixinDirection}'s clockwise rotation to 1.12.2, where the class is
 * {@code EnumFacing}, the method is {@code rotateAround} rather than {@code getClockWise(Axis)},
 * and the counter-clockwise sibling takes no axis argument.
 */
@Mixin(EnumFacing.class)
public abstract class MixinEnumFacing {

	/**
	 * @author Starlev
	 * @reason Replaces an outer axis switch, two identity compares and a nested helper-method
	 * 		   switch with one flat array load. The trailing IllegalStateException is dropped
	 * 		   because {@code Axis} holds exactly X, Y and Z, so every branch already returned;
	 * 		   a null axis still fails the same way, on the ordinal() call.
	 */
	@Overwrite
	public EnumFacing rotateAround(EnumFacing.Axis axis) {
		return EnumFacingTables.CLOCKWISE[axis.ordinal() * 6 + ((EnumFacing) (Object) this).ordinal()];
	}
}
//$ end
