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
import org.spongepowered.asm.mixin.Overwrite;
//$ if >=1.12
import net.minecraft.util.math.Vec3i;
//$ else
import net.minecraft.util.Vec3i;
//$ end

/**
 * Port of the phi-mix {@code hashCode} from the modern build. 1.12.2 carries the identical weak
 * degree-2 polynomial, so the body is a transcription rather than a re-derivation.
 *
 * <p>Vanilla's {@code (y + z * 31) * 31 + x} is a weak degree-2 polynomial: across a dense
 * 64^3 block region it collapses a large share of the 262144 cells onto far fewer distinct values,
 * and its low bits degenerate to {@code (x - y + z)} for small coordinates, so neighbouring
 * positions land on the same bucket. The phi-mix chain is bijective and, per the modern javadoc,
 * keeps z additive so z-runs spread evenly across power-of-two tables.
 *
 * <p>Caching the result would be unsound here and is deliberately not attempted:
 * {@code BlockPos.MutableBlockPos} mutates x/y/z in place, so a cached value would go stale. This
 * mixin replaces the function and recomputes it from the current coordinates every call.
 *
 * <p>{@code priority = 999} matches the modern mixin. Mixin applies higher priorities first, so at
 * 999 this overwrite is written before a normally-prioritised foreign one and the foreign body ends
 * up on top - the same deferral as {@code MixinVecDeltaCodec} and CoO. That matters here in
 * particular: the mods that actually compete with this patch (EfficientHashing and its forks,
 * from which the phi-mix constants are taken) are 1.12-era and are not covered by any check
 * performed here, so a collision is expected in the wild rather than ruled out.
 */
@Mixin(value = Vec3i.class, priority = 999)
public abstract class MixinVec3i {

	// 2^32 * phi, phi=(sqrt(5)-1)/2 from zank.mods.efficient_hashing.PhiMix by ZZZank (LGPL-3.0-only)
	private static final int HASH = 0x9E3779B9;

	/**
	 * @author ZZZank (original idea, EfficientHashing), Starlev (Lomka port)
	 * @reason Replaces the weak degree-2 polynomial with a bijective phi-mix chain, which spreads
	 *         block-position keys across hash tables instead of colliding in dense regions. The
	 *         value is not equal to vanilla's and does not need to be: hashCode has no cross-run
	 *         contract, only equals/hashCode consistency, which is untouched.
	 */
	@Overwrite
	public int hashCode() {
		Vec3i self = (Vec3i) (Object) this;
		int mixed  = self.getX() * HASH;
		int h1     = mixed ^ (mixed >>> 16);
		int mixed2 = (h1 + self.getY()) * HASH;
		return (mixed2 ^ (mixed2 >>> 16)) + self.getZ();
	}
}
//$ end
