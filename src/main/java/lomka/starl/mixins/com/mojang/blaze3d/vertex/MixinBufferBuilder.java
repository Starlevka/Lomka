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

package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BufferBuilder.class, priority = 500) // Mojang overwrite first
public abstract class MixinBufferBuilder implements VertexConsumer {

    @Shadow @Final private static boolean IS_LITTLE_ENDIAN;

    //? if >=26.2 {
    /*@Shadow
    private long beginElement(int semanticID) {
        return 0;
    }
    *///?} else {
    @Shadow
    private long beginElement(VertexFormatElement element) {
        return 0;
    }
    //?}

    /**
     * Converts a 32-bit ARGB color value to ABGR format using hardware-accelerated instructions.
     * <p>
     * On Little-Endian architectures (x86_64, aarch64), GPU vertex memory expects 4 sequential
     * bytes ordered as [R, G, B, A]. When writing via a single 32-bit integer store, the least
     * significant byte is stored at offset 0. Therefore, the integer must be packed as ABGR
     * ((A << 24) | (B << 16) | (G << 8) | R) so that R lands at byte 0, G at byte 1, B at byte 2,
     * and A at byte 3 in native memory.
     * <p>
     * Replaces 7 bitwise operations with BSWAP (reverseBytes) and ROR (rotateRight 8), executing
     * in ~2 CPU clock cycles. The {@code IS_LITTLE_ENDIAN} check is constant-folded by JIT C2.
     */
    private static int lomka$packAbgr(int argb) {
        int abgr = Integer.rotateRight(Integer.reverseBytes(argb), 8);
        return IS_LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr);
    }

    /**
     * @author Starlev
     * @reason Replace 7 bitwise operations with 2 native hardware instructions (BSWAP and ROR).
     *         Converts ARGB to ABGR in 2 CPU cycles instead of standard masking.
     */
    @Overwrite
    private static void putRgba(long pointer, int argb) {
        MemoryUtil.memPutInt(pointer, lomka$packAbgr(argb));
    }

    /**
     * @author Starlev
     * @reason Batches 4 individual 8-bit byte writes into a single 32-bit integer store instruction.
     *         Directly constructs the native Little-Endian ABGR memory layout to prevent L1D cache
     *         port saturation and eliminate store-forwarding stalls during chunk mesh generation.
     */
    @Overwrite
    public VertexConsumer setColor(int r, int g, int b, int a) {
        //? if >=26.2 {
        /*long pointer = this.beginElement(1);
        *///?} else {
        long pointer = this.beginElement(VertexFormatElement.COLOR);
        //?}

        if (pointer != -1L) {
            // Direct ABGR packing: on Little-Endian, bits 0..7 (R) land at byte +0,
            // bits 8..15 (G) at byte +1, bits 16..23 (B) at byte +2, bits 24..31 (A) at byte +3.
            int abgr = (a << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
            MemoryUtil.memPutInt(pointer, IS_LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr));
        }

        return this;
    }

    /**
     * @author Starlev
     * @reason Batches 2 32-bit float writes into a single 64-bit long native write on Little Endian architectures.
     */
    @Overwrite
    public VertexConsumer setUv(float f, float f1) {
        //? if >=26.2 {
        /*long pointer = this.beginElement(2);
        *///?} else {
        long pointer = this.beginElement(VertexFormatElement.UV0);
        //?}

        if (pointer != -1L) {
            long uv = ((long) Float.floatToRawIntBits(f1) << 32) | (Float.floatToRawIntBits(f) & 0xFFFFFFFFL);
            MemoryUtil.memPutLong(pointer, uv);
        }
        return this;
    }

    /**
     * @author Starlev
     * @reason Batches 2 16-bit short writes into a single 32-bit int write to eliminate store forwarding stalls.
     */
    @Overwrite
    //? if >=26.2 {
    /*private VertexConsumer uvShort(short short0, short short1, int semanticID) {
        long pointer = this.beginElement(semanticID);
    *///?} else {
    private VertexConsumer uvShort(short short0, short short1, VertexFormatElement vertexformatelement) {
        long pointer = this.beginElement(vertexformatelement);
    //?}
        if (pointer != -1L) {
            int packed = ((short1 & 0xFFFF) << 16) | (short0 & 0xFFFF);
            MemoryUtil.memPutInt(pointer, IS_LITTLE_ENDIAN ? packed : Integer.reverseBytes(packed));
        }
        return this;
    }
}