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

package lomka.starl.duck;

import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Exposes {@link net.minecraft.world.level.Level#getProfiler()} to mixins applied to
 * subclasses of Level (ClientLevel), where {@code @Shadow} cannot reach members declared
 * in the superclass. Only injected on <1.21.4 by MixinLevel; the method intentionally
 * mirrors the existing public {@code Level#getProfiler()} name/descriptor, so the parent
 * implementation satisfies the interface without any generated body. Compiled on all
 * versions - ProfilerFiller exists across the whole supported range.
 */
public interface ILevelProfiler {

    ProfilerFiller getProfiler();
}
