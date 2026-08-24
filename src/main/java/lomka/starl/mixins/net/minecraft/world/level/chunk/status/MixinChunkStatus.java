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

package lomka.starl.mixins.net.minecraft.world.level.chunk.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkStatus.class)
public abstract class MixinChunkStatus {

	@Unique private static List<ChunkStatus> lomka$cachedStatusList;

	/**
	 * @author Starlev
	 * @reason Caches the FULL->EMPTY status chain. Since 1.21 ChunkGenerationTask.scheduleNextLayer
	 *         rebuilds a fresh ArrayList on every layer of every chunk generation task (8-10 lists
	 *         per chunk during loading bursts), the allocation churn shows up on the worldgen threads.
	 *         The chain is static after registration, so a single cached list is returned instead.
	 */
	@Overwrite
	public static List<ChunkStatus> getStatusList() {
		List<ChunkStatus> list = lomka$cachedStatusList;
		if (list == null) {
			list = new ArrayList<>();
			ChunkStatus chunkstatus = ChunkStatus.FULL;
			for (; chunkstatus.getParent() != chunkstatus; chunkstatus = chunkstatus.getParent()) {
				list.add(chunkstatus);
			}
			list.add(chunkstatus);
			Collections.reverse(list);
			lomka$cachedStatusList = list;
		}
		return list;
	}
}