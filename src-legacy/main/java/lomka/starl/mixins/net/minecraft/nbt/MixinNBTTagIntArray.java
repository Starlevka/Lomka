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
import net.minecraft.nbt.NBTTagIntArray;

/**
 * Port of the bulk {@code load} patch from the modern build. 1.12.2 calls it
 * {@code read(DataInput, int, NBTSizeTracker)} and accounts 192 bits per tag exactly like the
 * modern {@code NbtAccounter} branch, so the body keeps the same shape.
 *
 * The bulk branch is only taken for a {@link DataInputStream}: that is the disk path - region
 * files, player data, level.dat - where vanilla pays four {@code read()} calls per element.
 * Network and buffer-backed inputs keep the vanilla loop; the modern build measured the bulked
 * network path as slower and nothing about 1.12.2 changes that.
 */
@Mixin(NBTTagIntArray.class)
public abstract class MixinNBTTagIntArray {

	@Shadow private int[] intArray;

	// No @Override: the target method is package-private in net.minecraft.nbt, so javac cannot
	// see it from this package. Mixin resolves it by name and descriptor instead.
	@Overwrite
	void read(DataInput dataInput, int depth, NBTSizeTracker sizeTracker) throws IOException {
		sizeTracker.read(192L);
		int length = dataInput.readInt();
		sizeTracker.read(32L * length);
		int[] target = new int[length];

		if (NbtBulkReader.worthBulking(length) && dataInput instanceof DataInputStream) {
			NbtBulkReader.readInts((DataInputStream) dataInput, target, length);
		} else {
			for (int i = 0; i < length; ++i) {
				target[i] = dataInput.readInt();
			}
		}

		this.intArray = target;
	}
}
