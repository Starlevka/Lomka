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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.nbt.LongArrayTag$1")
public class MixinLongArrayTag {

    /**
     * @author Starlev
     * @reason Vanilla reads the array element by element, so every long costs eight
     *         read() calls on a DataInputStream. Bulk reading through a bounded
     *         scratch buffer reduces stream calls for chunk, player and saved-data
     *         loading. The network path hands
     *         over a ByteBuf-backed DataInput where those single reads are already
     *         cheap, so it keeps the vanilla loop.
     */
    //? if >=1.21 {
    @Overwrite
    public LongArrayTag load(DataInput datainput, NbtAccounter nbtaccounter) throws IOException {
        nbtaccounter.accountBytes(24L);
        int count = datainput.readInt();

        nbtaccounter.accountBytes(8L, (long) count);
        return new LongArrayTag(lomka$readLongs(datainput, count));
    }
    //? } else if >=1.20 {
    /*@Overwrite
    public LongArrayTag load(DataInput datainput, int i, NbtAccounter nbtaccounter) throws IOException {
        nbtaccounter.accountBytes(24L);
        int count = datainput.readInt();

        nbtaccounter.accountBytes(8L * (long) count);
        return new LongArrayTag(lomka$readLongs(datainput, count));
    }
    *///? } else {
    /*@Overwrite
    public LongArrayTag load(DataInput datainput, int i, NbtAccounter nbtaccounter) throws IOException {
        nbtaccounter.accountBits(192L);
        int count = datainput.readInt();

        nbtaccounter.accountBits(64L * (long) count);
        return new LongArrayTag(lomka$readLongs(datainput, count));
    }
    *///?}

    @Unique
    private long[] lomka$readLongs(DataInput datainput, int count) throws IOException {
        long[] result = new long[count];

        if (!(datainput instanceof DataInputStream)) {
            for (int i = 0; i < count; i++) {
                result[i] = datainput.readLong();
            }

            return result;
        }

        int perBlock = 1024;
        byte[] scratch = new byte[Math.min(count, perBlock) * 8];
        ByteBuffer buffer = ByteBuffer.wrap(scratch).order(ByteOrder.BIG_ENDIAN);
        int done = 0;

        while (done < count) {
            int block = Math.min(perBlock, count - done);
            datainput.readFully(scratch, 0, block * 8);
            buffer.clear();

            for (int i = 0; i < block; i++) {
                result[done + i] = buffer.getLong();
            }

            done += block;
        }

        return result;
    }
}
