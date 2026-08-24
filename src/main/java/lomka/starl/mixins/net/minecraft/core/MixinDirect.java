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

package lomka.starl.mixins.net.minecraft.core;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Set;

@Mixin(HolderSet.Direct.class)
public abstract class MixinDirect<T> {

	@Shadow private List<Holder<T>> contents;
	@Shadow private Set <Holder<T>> contentsSet;

	/**
	 * @author Starlev
	 * @reason Avoids materialising a Set.copyOf for tiny direct holder sets.
	 * 		   Measured on JDK 25 over hit+miss probes: a linear scan beats the cached
	 * 		   immutable-set lookup for sets up to 8 elements (roughly 1.5-2.5x faster,
	 * 		   still ~2x on misses at n=8), roughly ties at 10-12 and loses from 16 up.
	 * 		   Larger sets keep the lazy Set cache.
	 */
	@Overwrite
	public boolean contains(Holder<T> holder) {
		if (this.contents.size() <= 8) {
			return this.contents.contains(holder);
		}
		if (this.contentsSet == null) {
			this.contentsSet = Set.copyOf(this.contents);
		}
		return this.contentsSet.contains(holder);
	}
}