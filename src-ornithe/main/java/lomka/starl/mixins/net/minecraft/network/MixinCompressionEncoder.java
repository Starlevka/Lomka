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

import java.util.zip.Deflater;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.PacketByteBuf;

/**
 * Reuses the scratch buffer vanilla's {@code CompressionEncoder#encode} allocates per compressed
 * packet. The cache is capped because a chunk packet can be large and holding an oversized buffer
 * forever turns a one-shot allocation into a permanent high-water mark.
 */
@Mixin(CompressionEncoder.class)
public abstract class MixinCompressionEncoder {

	// Both fields are final on this version (they are not in the MCP 1.12.2 tree), so they need
	// the @Final annotation rather than the keyword - a blank final field is a javac error.
	@Shadow @Final private int threshold;
	@Shadow @Final private byte[] buffer;
	@Shadow @Final private java.util.zip.Deflater deflator;

	@Unique private static final int LOMKA_MAX_CACHED = 65536;
	@Unique private byte[] lomka$scratch = new byte[0];

	@Overwrite
	protected void encode(ChannelHandlerContext context, ByteBuf in, ByteBuf out) throws Exception {
		int readable = in.readableBytes();
		PacketByteBuf buffer = new PacketByteBuf(out);
		if (readable < this.threshold) {
			buffer.writeVarInt(0);
			buffer.writeBytes(in);
			return;
		}

		byte[] scratch = this.lomka$scratch;
		if (scratch.length < readable) {
			scratch = new byte[readable];
			if (readable <= LOMKA_MAX_CACHED) {
				this.lomka$scratch = scratch;
			}
		}
		in.readBytes(scratch, 0, readable);

		buffer.writeVarInt(readable);
		this.deflator.setInput(scratch, 0, readable);
		this.deflator.finish();
		while (!this.deflator.finished()) {
			int written = this.deflator.deflate(this.buffer);
			buffer.writeBytes(this.buffer, 0, written);
		}
		this.deflator.reset();
	}
}
//$ end
