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

package lomka.starl.mixins.net.minecraft.client.sounds;

import java.nio.ByteBuffer;
import net.minecraft.client.sounds.ChunkedSampleByteBuf;
import net.minecraft.util.Mth;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkedSampleByteBuf.class)
public abstract class MixinChunkedSampleByteBuf {

    @Shadow private ByteBuffer currentBuffer;
    @Shadow private int byteCount;

    /**
     * @author Starlevka.
     * @reason Removes per-chunk native allocations.
     */
    @Overwrite
    public void accept(float f) {
        if (this.currentBuffer.remaining() < 2) {
            this.grow();
        }

        int i = Mth.clamp((int) (f * 32767.5F - 0.5F), -32768, 32767);

        this.currentBuffer.putShort((short) i);
        this.byteCount += 2;
    }

    /**
     * @author Starlevka.
     * @reason Single growing buffer.
     */
    @Overwrite
    public ByteBuffer get() {
        this.currentBuffer.flip();
        return this.currentBuffer;
    }

    private void grow() {
        ByteBuffer bytebuffer = BufferUtils.createByteBuffer(this.currentBuffer.capacity() * 2);
        this.currentBuffer.flip();
        bytebuffer.put(this.currentBuffer);
        this.currentBuffer = bytebuffer;
    }
}
