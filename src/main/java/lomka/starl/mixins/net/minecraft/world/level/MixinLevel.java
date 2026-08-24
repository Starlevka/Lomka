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

import net.minecraft.world.level.Level;
import lomka.starl.duck.ILevelProfiler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author Starlev
 * @reason Marks Level as implementing ILevelProfiler so subclasses-of-Level mixins
 *         (ClientLevel) can reach the inherited getProfiler() via an interface cast -
 *         @Shadow cannot locate members declared in the target's superclass. Intentionally
 *         empty: Level's own public getProfiler() satisfies the interface contract after
 *         the merge, so only the implements clause is added.
 */
@Mixin(Level.class)
public abstract class MixinLevel implements ILevelProfiler {
}
