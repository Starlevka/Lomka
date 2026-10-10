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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.math.MathHelper;

/**
 * Replaces the bit-spread and the De Bruijn table lookup with a single leading-zero intrinsic.
 *
 * <p>Both methods exist unchanged on every version in range and are selected by NAME rather than
 * by a full descriptor. {@code @Overwrite} fails to locate them on the oldest version with
 * "method smallestEncompassingPowerOfTwo(I)I was not located in the target class", even though the
 * target declares both as {@code public static (I)I} - verified with {@code javap -v} on the mapped
 * jar. A cancellable HEAD {@code @Inject} resolves through a different path and applies.
 *
 * <p>That also means one {@link CallbackInfoReturnable} per call instead of a straight static
 * dispatch. Both methods are on the palette and block-power paths, so this is a real cost and the
 * reason the {@code @Overwrite} form is preferred wherever it resolves.
 */
@Mixin(MathHelper.class)
public abstract class MixinMathHelper {

	/**
	 * Vanilla answers 0 for every {@code n <= 0} except {@code MIN_VALUE}, where the OR-cone
	 * saturates to {@code 0x7FFFFFFF} and the {@code +1} overflows back to {@code MIN_VALUE}.
	 * {@code n == 1} needs no case of its own: the shift amount is {@code 32 - nlz(0) = 0}.
	 */
	@Inject(method = "smallestEncompassingPowerOfTwo", at = @At("HEAD"), cancellable = true)
	private static void lomka$smallestEncompassingPowerOfTwo(int n, CallbackInfoReturnable<Integer> cir) {
		if (n <= 0) {
			cir.setReturnValue(n == Integer.MIN_VALUE ? n : 0);
		} else {
			cir.setReturnValue(1 << (32 - Integer.numberOfLeadingZeros(n - 1)));
		}
	}

	/**
	 * Vanilla answers {@code -1} for every {@code n <= 0} - the table entry for zero minus the
	 * non-power-of-two correction - which a bare {@code 31 - numberOfLeadingZeros(n)} would not do.
	 * {@code MIN_VALUE} is the one exception, because vanilla's {@code isPowerOfTwo} accepts it (the
	 * sign bit makes {@code n & (n - 1)} zero) and therefore skips the correction, answering 31.
	 */
	@Inject(method = "log2", at = @At("HEAD"), cancellable = true)
	private static void lomka$log2(int n, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(n > 0 ? 31 - Integer.numberOfLeadingZeros(n)
				: n == Integer.MIN_VALUE ? 31 : -1);
	}
}