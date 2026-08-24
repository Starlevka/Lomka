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

package lomka.starl.mixins.net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LayerLightSectionStorage.class)
public abstract class MixinLayerLightSectionStorage<M extends DataLayerStorageMap<M>> {

	@Shadow protected abstract DataLayer getDataLayer(long i, boolean flag);
	@Shadow protected M       updatingSectionData;
	@Shadow protected LongSet changedSections;
	@Shadow protected LongSet sectionsAffectedByLightUpdates;

	@Unique private LongConsumer lomka$affectedSectionsCollector;

	/**
	 * @author Starlev
	 * @reason Replaces the per-block capturing method reference (`longset::add`, which
	 *         allocates a lambda object for every block level change) with a single
	 *         lazily-built LongConsumer field. setStoredLevel runs once per block per
	 *         light-level change, so this removes one allocation from the hottest
	 *         per-block path of light propagation. The collector is initialized lazily
	 *         to avoid any dependency on target-field initialization order.
	 */
	@Overwrite
	protected void setStoredLevel(long i, int j) {
		long k = SectionPos.blockToSection(i);
		DataLayer datalayer;

		if (this.changedSections.add(k)) {
			datalayer = this.updatingSectionData.copyDataLayer(k);
		} else {
			datalayer = this.getDataLayer(k, true);
		}

		datalayer.set(SectionPos.sectionRelative(BlockPos.getX(i)), SectionPos.sectionRelative(BlockPos.getY(i)), SectionPos.sectionRelative(BlockPos.getZ(i)), j);
		LongConsumer collector = this.lomka$affectedSectionsCollector;
		if (collector == null) {
			collector = this.lomka$affectedSectionsCollector = this.sectionsAffectedByLightUpdates::add;
		}
		SectionPos.aroundAndAtBlockPos(i, collector);
	}
}