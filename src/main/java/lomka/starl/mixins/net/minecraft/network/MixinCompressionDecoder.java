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
import java.nio.ByteBuffer;
import java.util.zip.Inflater;
import net.minecraft.network.CompressionDecoder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompressionDecoder.class)
public abstract class MixinCompressionDecoder {

    @Shadow @Final private Inflater inflater;

    /**
     * @author Starlev
     * @reason Use internalNioBuffer (Netty's cached wrapper accessor) instead of nioBuffer, which
     *         allocates a fresh ByteBuffer view via duplicate() on every compressed packet.
     *         The fallback is not paranoia: internalNioBuffer is absent from AbstractByteBuf and
     *         from AbstractUnpooledSlicedByteBuf, while the only guard available here is
     *         nioBufferCount() - two independent methods, so "has one NIO buffer" does not imply
     *         "implements internalNioBuffer". Verified on netty-buffer 4.1.115: all five buffer
     *         implementations tried accept it and the handed-out range is bit-identical to
     *         nioBuffer's, so the try never throws there - but a refused buffer would throw
     *         inside the packet decoder and drop the connection. A non-throwing try/catch costs
     *         nothing once JIT-compiled, so the guard costs us nothing and removes the failure
     *         mode. (Measured figures live in scripts/bench, not in source comments.)
     */
    @Overwrite
    private void setupInflaterInput(ByteBuf bytebuf) {
        ByteBuffer bytebuffer;
        int readable = bytebuf.readableBytes();

        if (bytebuf.nioBufferCount() > 0) {
            ByteBuffer cached;
            try {
                cached = bytebuf.internalNioBuffer(bytebuf.readerIndex(), readable);
            } catch (UnsupportedOperationException noInternalView) {
                cached = bytebuf.nioBuffer();
            }
            bytebuffer = cached;
            bytebuf.skipBytes(readable);
        } else {
            bytebuffer = ByteBuffer.allocateDirect(readable);
            bytebuf.readBytes(bytebuffer);
            bytebuffer.flip();
        }

        this.inflater.setInput(bytebuffer);
    }
}