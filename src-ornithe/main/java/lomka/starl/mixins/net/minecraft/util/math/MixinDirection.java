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

package lomka.starl.mixins.net.minecraft.util.math;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import lomka.starl.utils.constants.DirectionTables;
import net.minecraft.util.math.Direction;

/**
 * Flattens {@code Direction#clockwise(Axis)} into one array load per axis.
 *
 * <p><b>Anchored on the three private helpers, not on {@code clockwise} itself.</b> {@code Direction}
 * is first loaded <em>through</em> {@code Direction$Axis} - {@code AxisBlock.<clinit>} calls
 * {@code EnumProperty.of(Direction.Axis.class)}, which calls {@code Class#getEnumConstants}, which
 * loads {@code Direction} - and every attempt to patch the outer method fails there. Both
 * {@code @Overwrite} and a cancellable HEAD {@code @Inject}, by descriptor and by bare name, with
 * the parameter typed {@code Direction.Axis} and coerced to {@code Object}, all end in
 * {@code could not find any targets matching '...clockwise(Lnet/minecraft/util/math/Direction$Axis;)
 * Lnet/minecraft/util/math/Direction;'}. Resolving that parameter type re-enters a class that is
 * still being defined. The helpers take no parameters, so they resolve cleanly, and the semantics
 * are identical: {@code clockwise} returns the receiver unchanged for the axis it is parallel to
 * before dispatching, which is exactly the identity row this table carries.
 *
 * <p>These are cancellable HEAD {@code @Inject}s rather than {@code @Overwrite}s. On this class
 * {@code @Overwrite} cannot resolve a target at all - the outer method fails with
 * {@code @Overwrite method clockwise(L...Direction$Axis;)L...Direction; ... was not located in the
 * target class}, and so do the private helpers - while the identical bodies expressed as
 * cancellable HEAD injectors apply cleanly. That is the same split {@code MixinMathHelper} hit on
 * 1.7.10, so it is treated as the property of this Mixin build rather than a one-off. The price is
 * one {@link CallbackInfoReturnable} per call; the method is on the block/model rotation path
 * ({@code BlockRotation}, {@code ModelRotation}, {@code FaceBakery}), so it is hot enough to care
 * about, but the flat table still beats a switch chain plus an allocation.
 *
 * <p>Overwriting the helpers is safe against foreign anchors because nothing outside the enum can
 * reach them - they are private - and the public entry point's body and its early-out stay
 * untouched, so every injector targeting {@code clockwise} still resolves.
 *
 * <p>Gated {@code >=1.8}: 1.7.10's {@code Direction} exposes no rotation methods at all, only
 * {@code getOffsetX/Y/Z} and {@code byId}, so there is nothing to flatten.
 */
@Mixin(Direction.class)
public abstract class MixinDirection {

	@Inject(method = "clockwiseX", at = @At("HEAD"), cancellable = true)
	private void lomka$clockwiseX(CallbackInfoReturnable<Direction> cir) {
		cir.setReturnValue(DirectionTables.CLOCKWISE[((Direction) (Object) this).ordinal()]);
	}

	@Inject(method = "clockwiseY", at = @At("HEAD"), cancellable = true)
	private void lomka$clockwiseY(CallbackInfoReturnable<Direction> cir) {
		cir.setReturnValue(DirectionTables.CLOCKWISE[6 + ((Direction) (Object) this).ordinal()]);
	}

	@Inject(method = "clockwiseZ", at = @At("HEAD"), cancellable = true)
	private void lomka$clockwiseZ(CallbackInfoReturnable<Direction> cir) {
		cir.setReturnValue(DirectionTables.CLOCKWISE[12 + ((Direction) (Object) this).ordinal()]);
	}
}
//$ end