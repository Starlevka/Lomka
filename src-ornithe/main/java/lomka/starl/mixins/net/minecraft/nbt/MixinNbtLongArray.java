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
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtReadLimiter;

/**
 * Long-array half of the bulk {@code read} patch; see {@code MixinNbtIntArray}
 * for why the bulk branch is taken only for a {@link DataInputStream}.
 */
@Mixin(NbtLongArray.class)
public abstract class MixinNbtLongArray {

	@Shadow private long[] value;

	// Public for the same reason as the int-array half: Mixin resolves the target as PUBLIC and
	// rejects an @Overwrite that narrows visibility.
	@Overwrite
	public void read(DataInput dataInput, int depth, NbtReadLimiter limiter) throws IOException {
		limiter.read(192L);
		int length = dataInput.readInt();
		limiter.read(64L * length);
		long[] target = new long[length];

		if (NbtBulkReader.worthBulking(length) && dataInput instanceof DataInputStream) {
			NbtBulkReader.readLongs((DataInputStream) dataInput, target, length);
		} else {
			for (int i = 0; i < length; ++i) {
				target[i] = dataInput.readLong();
			}
		}

		this.value = target;
	}
}
//$ end
