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

package lomka.starl.mixins.net.minecraft.network;

import java.util.List;
import java.util.zip.Inflater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.PacketByteBuf;

/**
 * Reuses the input buffer vanilla's {@code CompressionDecoder#decode} allocates for the compressed
 * payload of every packet. That array only lives between {@code Inflater#setInput} and the
 * {@code inflate} call - {@code reset()} runs before the method returns, so nothing keeps a
 * reference - which makes it a per-packet allocation of up to the protocol maximum.
 *
 * Two details keep this a transcription rather than a rewrite:
 *
 * <ul>
 * <li>The cached array is almost always larger than the payload, so the inflater is fed with
 * {@code setInput(buffer, 0, length)}. Passing the bare array would hand the inflater the
 * whole buffer and inflate garbage past the end of the packet.</li>
 * <li>The <em>output</em> array stays a fresh allocation. It is handed to
 * {@code Unpooled.wrappedBuffer} and escapes into the pipeline, so reusing it would corrupt
 * packets still queued behind this one.</li>
 * </ul>
 *
 * The cache is capped for the same reason as the encoder's: one oversized packet must not
 * pin memory for the lifetime of the connection.
 */
@Mixin(CompressionDecoder.class)
public abstract class MixinCompressionDecoder extends ByteToMessageDecoder {

	/** Protocol maximum for a decompressed packet, per vanilla's own check. */
	@Unique private static final int PROTOCOL_MAXIMUM = 2097152;
	@Unique private static final int LOMKA_MAX_CACHED = 65536;
	@Unique private byte[] lomka$scratch = new byte[0];

	@Shadow private int threshold;
	@Shadow private Inflater inflater;

	@Overwrite
	protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() == 0) {
			return;
		}

		PacketByteBuf buffer = new PacketByteBuf(in);
		int decompressed = buffer.readVarInt();
		if (decompressed == 0) {
			// Uncompressed packet: this slice is the payload the pipeline wants, nothing to reuse.
			out.add(buffer.readBytes(buffer.readableBytes()));
			return;
		}

		if (decompressed < this.threshold) {
			throw new DecoderException("Badly compressed packet - size of " + decompressed
					+ " is below server threshold of " + this.threshold);
		}
		if (decompressed > PROTOCOL_MAXIMUM) {
			throw new DecoderException("Badly compressed packet - size of " + decompressed
					+ " is larger than protocol maximum of " + PROTOCOL_MAXIMUM);
		}

		int readable = buffer.readableBytes();
		byte[] scratch = this.lomka$scratch;
		if (scratch.length < readable) {
			scratch = new byte[readable];
			if (readable <= LOMKA_MAX_CACHED) {
				this.lomka$scratch = scratch;
			}
		}
		buffer.readBytes(scratch, 0, readable);
		this.inflater.setInput(scratch, 0, readable);

		// Fresh on purpose: this one escapes into the pipeline through wrappedBuffer.
		byte[] inflated = new byte[decompressed];
		this.inflater.inflate(inflated);
		out.add(Unpooled.wrappedBuffer(inflated));
		this.inflater.reset();
	}
}
//$ end
