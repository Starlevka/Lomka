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
package lomka.starl.mixins.net.minecraft.util.math;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
//$ if >=1.12
import net.minecraft.util.math.MathHelper;
//$ else
import net.minecraft.util.MathHelper;
//$ end

/**
 * Port of {@code MixinMth} to 1.12.2, where the class is {@code MathHelper}. Only the members
 * that exist on this version come across: {@code ceillog2}, {@code binarySearch},
 * {@code unpackDegrees} and the {@code hsvToArgb} split are all 1.19+ or later.
 */
@Mixin(MathHelper.class)
public abstract class MixinMathHelper {

/**
	 * @author Starlev
	 * @reason Replaces the five-step bit-spread with a single leading-zero intrinsic. The
	 * 		   non-positive range is spelled out because vanilla returns 0 for every {@code n <= 0}
	 * 		   except {@code MIN_VALUE}, where the OR-cone saturates to {@code 0x7FFFFFFF} and the
	 * 		   {@code +1} overflows back to {@code MIN_VALUE}. {@code n == 1} needs no case of its
	 * 		   own: the shift amount is {@code 32 - nlz(0) = 0}, so the general form answers 1.
	 */
	@Overwrite
	public static int smallestEncompassingPowerOfTwo(int n) {
		if (n <= 0) {
			return n == Integer.MIN_VALUE ? n : 0;
		}
		return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
	}

	/**
	 * @author Starlev
	 * @reason Replaces the De Bruijn table lookup plus the {@code isPowerOfTwo} branch with one
	 * 		   leading-zero intrinsic. Vanilla answers {@code -1} for every {@code n <= 0} - the table
	 * 		   entry for zero minus the non-power-of-two correction - and that is reproduced exactly,
	 * 		   which a bare {@code 31 - numberOfLeadingZeros(n)} would not do. {@code MIN_VALUE} is the
	 * 		   one exception, because vanilla's {@code isPowerOfTwo} accepts it (the sign bit makes
	 * 		   {@code n & (n - 1)} zero) and therefore skips the correction, answering 31.
	 */
	@Overwrite
	public static int log2(int n) {
		return n > 0 ? 31 - Integer.numberOfLeadingZeros(n) : n == Integer.MIN_VALUE ? 31 : -1;
	}
}
