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
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtReadLimiter;

/**
 * Port of the bulk {@code read} patch from the modern build. 1.12.2 accounts 192 bits per tag
 * exactly like the modern {@code NbtAccounter} branch, so the body keeps the same shape; Feather
 * only renames the tag to {@code NbtIntArray}, the size tracker to {@code NbtReadLimiter} and the
 * payload field to {@code value}.
 *
 * The bulk branch is only taken for a {@link DataInputStream}: that is the disk path - region
 * files, player data, level.dat - where vanilla pays four {@code read()} calls per element.
 * Network and buffer-backed inputs keep the vanilla loop; the modern build measured the bulked
 * network path as slower and nothing about 1.12.2 changes that.
 */
@Mixin(NbtIntArray.class)
public abstract class MixinNbtIntArray {

	@Shadow private int[] value;

	// Declared public because Mixin resolves the target as PUBLIC and rejects an @Overwrite that
	// narrows visibility ("cannot reduce visibility of PUBLIC target method"). Widening the
	// injected method over the package-private declaration on this version is legal bytecode.
	// No @Override: the target is not visible from this package, so Mixin resolves it by name and
	// descriptor instead.
	@Overwrite
	public void read(DataInput dataInput, int depth, NbtReadLimiter limiter) throws IOException {
		limiter.read(192L);
		int length = dataInput.readInt();
		limiter.read(32L * length);
		int[] target = new int[length];

		if (NbtBulkReader.worthBulking(length) && dataInput instanceof DataInputStream) {
			NbtBulkReader.readInts((DataInputStream) dataInput, target, length);
		} else {
			for (int i = 0; i < length; ++i) {
				target[i] = dataInput.readInt();
			}
		}

		this.value = target;
	}
}
