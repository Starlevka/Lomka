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

package lomka.starl.utils.cache;

import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * Immutable {@code (Data, value)} pair for the uniform-section cache.
 *
 * <p>The pair has to be published atomically: a reader that observes a cached {@code data}
 * reference must observe the value that belongs to it. Splitting the two across a volatile
 * reference and a plain field is unsound, because two concurrent fillers can interleave as
 *
 * <pre>
 *   W1: value = V1
 *   W2: value = V2   (V2 belongs to D2)
 *   W2: data  = D2
 *   W1: data  = D1
 * </pre>
 *
 * leaving {@code data == D1} paired with {@code value == V2}, so a subsequent read returns another
 * section's value. One immutable holder behind a single volatile reference removes the
 * interleaving entirely: the record's final fields carry the JMM final-field guarantee, so any
 * thread that sees the reference also sees a fully initialised pair.
 *
 * <p>Storing the value on {@code PalettedContainer.Data} itself would be allocation-free and one
 * load shorter, but {@code Data} is a {@code private static record} in every supported version, so
 * that needs a second mixin on a final nested type plus access widening on all ten variants for a
 * single dependent load. Not worth it.
 */
public record UniformPaletteCache<T>(PalettedContainer.Data<T> data, T value) {
}
