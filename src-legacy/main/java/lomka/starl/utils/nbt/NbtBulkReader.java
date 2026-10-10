//$ if >=1.8

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

package lomka.starl.utils.nbt;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Shared scratch for the bulk NBT array reads. Vanilla reads these payloads element by element,
 * so every int costs four {@code read()} calls and every long costs eight; funneling the bytes
 * through one big-endian buffer turns that into block reads.
 *
 * <p>Gated {@code >=1.8} after the lowest-gated of its consumers, {@code MixinNBTTagIntArray}
 * (the other one, {@code MixinNBTTagLongArray}, is {@code >=1.12}). A helper is gated to the widest
 * set of mixins that reference it, never wider.
 *
 * <p>The scratch is per-thread because chunk NBT is read on the server's IO workers, and it is
 * thread-local rather than a static single buffer for that reason. Both fields are lazily
 * created, so a client that never reads a chunk pays nothing.
 *
 * <p>Byte order is pinned to {@link ByteOrder#BIG_ENDIAN} because that is what
 * {@code DataInputStream} produces; the buffer is byte-order agnostic otherwise.
 */
public final class NbtBulkReader {

	/** 8 KiB of payload per iteration. */
	private static final int BLOCK_BYTES = 8192;
	private static final int INTS_PER_BLOCK  = BLOCK_BYTES >> 2;
	private static final int LONGS_PER_BLOCK = BLOCK_BYTES >> 3;

	private static final ThreadLocal<byte[]> BLOCK 		= new ThreadLocal<byte[]>();
	private static final ThreadLocal<ByteBuffer> BUFFER = new ThreadLocal<ByteBuffer>();

	/**
	 * Below this length the plain loop wins: a block read plus a view object costs more than the
	 * handful of {@code readInt()} calls it replaces.
	 */
	public static boolean worthBulking(int elements) {
		return elements > INTS_PER_BLOCK;
	}

	public static void readInts(DataInputStream stream, int[] target, int length) throws IOException {
		byte[] block = block();
		ByteBuffer buffer = buffer(block);
		int index = 0;
		while (index < length) {
			int chunk = Math.min(INTS_PER_BLOCK, length - index);
			stream.readFully(block, 0, chunk << 2);
			buffer.clear();
			buffer.limit(chunk << 2);
			buffer.asIntBuffer().get(target, index, chunk);
			index += chunk;
		}
	}

	public static void readLongs(DataInputStream stream, long[] target, int length) throws IOException {
		byte[] block = block();
		ByteBuffer buffer = buffer(block);
		int index = 0;
		while (index < length) {
			int chunk = Math.min(LONGS_PER_BLOCK, length - index);
			stream.readFully(block, 0, chunk << 3);
			buffer.clear();
			buffer.limit(chunk << 3);
			buffer.asLongBuffer().get(target, index, chunk);
			index += chunk;
		}
	}

	private static byte[] block() {
		byte[] block = BLOCK.get();
		if (block == null) {
			block = new byte[BLOCK_BYTES];
			BLOCK.set(block);
		}
		return block;
	}

	private static ByteBuffer buffer(byte[] block) {
		ByteBuffer buffer = BUFFER.get();
		if (buffer == null) {
			buffer = ByteBuffer.wrap(block).order(ByteOrder.BIG_ENDIAN);
			BUFFER.set(buffer);
		}
		return buffer;
	}

	private NbtBulkReader() {
	}
}
//$ end
