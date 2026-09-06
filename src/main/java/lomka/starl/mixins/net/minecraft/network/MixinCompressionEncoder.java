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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.zip.Deflater;
import net.minecraft.network.CompressionEncoder;
//? if >=1.21 {
import net.minecraft.network.VarInt;
//?} else {
/*import net.minecraft.network.FriendlyByteBuf;
*///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CompressionEncoder.class)
public abstract class MixinCompressionEncoder {

	@Shadow @Final private byte[] encodeBuf;
	@Shadow @Final private Deflater deflater;
	@Shadow private int threshold;

	@Unique private byte[] lomka$inputBuf = new byte[8192];

	/**
	 * @author Starlev
	 * @reason Reuses a growing scratch byte[] instead of allocating `new byte[n]` for every
	 *         compressed packet. The output side already reuses encodeBuf; only the input
	 *         copy was missing reuse. Saves one heap array per chunk/map/entity packet on
	 *         any server with network compression enabled. Monotonic growth is capped:
	 *         buffers >64KB that are 4x larger than the current packet are shrunk to
	 *         avoid holding an 8MB array from a single huge packet forever (ChannelHandler
	 *         lives per connection on one EventLoop).
	 */
	@Overwrite
	protected void encode(ChannelHandlerContext channelhandlercontext, ByteBuf bytebuf, ByteBuf bytebuf1) {
		int i = bytebuf.readableBytes();

		//? if >=1.21 {
		if (i > 8388608) {
			throw new IllegalArgumentException("Packet too big (is " + i + ", should be less than 8388608)");
		}
		//?}

		//? if >=1.21 {
		if (i < this.threshold) {
			VarInt.write(bytebuf1, 0);
			bytebuf1.writeBytes(bytebuf);
		} else {
			byte[] input = this.lomka$inputBuf;
			if (input.length < i) {
				input = this.lomka$inputBuf = new byte[Math.max(i, 8192)];
			} else if (input.length > 65536 && input.length > i * 4) {
				// Shrink runaway buffer (e.g. 8MB after one huge chunk packet) back to max(8KB, i*2)
				int target = Math.max(8192, i * 2);
				// Round to power-of-two to keep future grows amortized
				target = Integer.highestOneBit(target - 1) << 1;
				input = this.lomka$inputBuf = new byte[target];
			}
			bytebuf.readBytes(input, 0, i);
			VarInt.write(bytebuf1, i);
			this.deflater.setInput(input, 0, i);
			this.deflater.finish();

			while (!this.deflater.finished()) {
				int j = this.deflater.deflate(this.encodeBuf);

				bytebuf1.writeBytes(this.encodeBuf, 0, j);
			}

			this.deflater.reset();
		}
		//?} else {
		/*FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(bytebuf1);
		if (i < this.threshold) {
			friendlybytebuf.writeVarInt(0);
			friendlybytebuf.writeBytes(bytebuf);
		} else {
			byte[] input = this.lomka$inputBuf;
			if (input.length < i) {
				input = this.lomka$inputBuf = new byte[Math.max(i, 8192)];
			} else if (input.length > 65536 && input.length > i * 4) {
				int target = Math.max(8192, i * 2);
				target = Integer.highestOneBit(target - 1) << 1;
				input = this.lomka$inputBuf = new byte[target];
			}
			bytebuf.readBytes(input, 0, i);
			friendlybytebuf.writeVarInt(i);
			this.deflater.setInput(input, 0, i);
			this.deflater.finish();

			while (!this.deflater.finished()) {
				int j = this.deflater.deflate(this.encodeBuf);

				friendlybytebuf.writeBytes(this.encodeBuf, 0, j);
			}

			this.deflater.reset();
		}
		*///?}
	}
}