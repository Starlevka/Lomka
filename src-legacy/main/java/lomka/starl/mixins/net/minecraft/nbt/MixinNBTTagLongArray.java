//$ if >=1.12

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

package lomka.starl.mixins.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import lomka.starl.utils.nbt.NbtBulkReader;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagLongArray;

/**
 * Port of the bulk {@code load} patch from the modern build. This is the shape that actually
 * pays on a server: {@code Heightmaps} are long arrays, so a single chunk carries hundreds of
 * kilobytes read eight bytes at a time.
 */
@Mixin(NBTTagLongArray.class)
public abstract class MixinNBTTagLongArray {

	@Shadow private long[] data;

	@Overwrite
	void read(DataInput dataInput, int depth, NBTSizeTracker sizeTracker) throws IOException {
		sizeTracker.read(192L);
		int length = dataInput.readInt();
		sizeTracker.read(64L * length);
		long[] target = new long[length];

		if (NbtBulkReader.worthBulking(length) && dataInput instanceof DataInputStream) {
			NbtBulkReader.readLongs((DataInputStream) dataInput, target, length);
		} else {
			for (int i = 0; i < length; ++i) {
				target[i] = dataInput.readLong();
			}
		}

		this.data = target;
	}
}
//$ end
