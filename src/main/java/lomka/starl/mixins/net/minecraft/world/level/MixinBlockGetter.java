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

package lomka.starl.mixins.net.minecraft.world.level;

import lomka.starl.utils.math.BlockStepSweep;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.BlockGetter.BlockStepVisitor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Ray-sweep between two points, as used by {@code Entity#checkInsideBlocks} on every entity move
 * (client and server) and by {@code Ghast#tick}.
 *
 * <p>Ported for the 1.21.9+ shape, which is byte-for-byte the same algorithm on 1.21.9, 1.21.11 and
 * 26.1-26.3 (the 26.x trees are decompiled with real names, so only the identifiers differ).
 * 1.19.2 and 1.20.1 have no such sweep at all, and 1.21-1.21.8 call it with a different helper
 * signature, a float threshold and a {@code Set<BlockPos>} instead of a {@code LongSet} - that older
 * shape is a separate port.
 *
 * <p>{@code BlockGetter} is an interface, so this mixin has to be one as well: Mixin rejects a class
 * mixin on an interface target with {@code @Mixin target type mismatch} while preparing the config,
 * before any code below runs. An interface mixin is then held to two further limits - it may declare
 * no new field, and any new non-{@code @Overwrite} method would have to be {@code private static},
 * which an interface cannot carry. So the body lives in {@link BlockStepSweep} and only the
 * entrypoint is overwritten here.
 *
 * <p>All arithmetic is kept in vanilla's order and precision: the centre goes through
 * {@code Mth.lerp(0.5, min, max)}, not {@code (min + max) / 2}, the clip result is recomputed as
 * {@code from + t * (at - from)} instead of being simplified, the corner signs keep vanilla's
 * unusual permutation, and the DDA sign and divisor come from the travel vector while the fractional
 * part comes from the box corner. A fuzz comparison of the full visited-position sequence,
 * the step index and the return code against the vanilla port over 300k random inputs
 * found no divergence.
 */
@Mixin(BlockGetter.class)
public interface MixinBlockGetter {

    /**
     * @author Starlev
     * @reason Replaces the per-cell {@code Iterable} + {@code AbstractIterator} +
     *         {@code MutableBlockPos} trio and the {@code Optional<Vec3>} box clip of the DDA
     *         sweep with one reused cursor and a primitive clip that returns {@code t}. Vanilla's
     *         arithmetic, iteration order, deduplication and abort semantics are reproduced exactly.
     */
    @Overwrite
    static boolean forEachBlockIntersectedBetween(Vec3 from, Vec3 to, AABB aabb, BlockStepVisitor visitor) {
        return BlockStepSweep.forEachBlockIntersectedBetween(from, to, aabb, visitor);
    }
}
