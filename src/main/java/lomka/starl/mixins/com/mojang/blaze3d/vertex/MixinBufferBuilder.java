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

/**
 * Speeds up per-vertex writes on the element-wise BufferBuilder paths
 * (particles, glyphs, per-element geometry) and the packed-color path used
 * by every chunk/entity vertex (putRgba, reached via the untouched 11-arg
 * addVertex fast path):
 *
 * - putRgba: ARGB.toABGR's 7-op mask chain becomes BSWAP + ROR, both HotSpot
 *   intrinsics (~2 ALU uops). The JIT cannot derive this from the masks.
 * - setColor(IIII) / setUv(FF) / uvShort: N byte/short/float stores become a
 *   single int/long store. floatToRawIntBits (not floatToIntBits) keeps NaN
 *   payloads bit-identical to memPutFloat.
 *
 * The 11-argument addVertex is deliberately untouched: hottest method in the
 * class and a magnet for foreign patches, and vanilla already keeps it at one
 * store per element.
 */
@Mixin(value = BufferBuilder.class, priority = 999) // levl
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
     * Packs ARGB into the int whose native store yields [R, G, B, A] bytes.
     * Little-endian (all supported platforms): ABGR via BSWAP + ROR.
     * Big-endian: vanilla's own expression, verbatim - byte parity by construction.
     */
    private static int lomka$packColor(int argb) {
        return IS_LITTLE_ENDIAN
            ? Integer.rotateRight(Integer.reverseBytes(argb), 8)
            : Integer.reverseBytes(argb & -16711936 
                                | (argb & 16711680) >> 16
                                | (argb & 255) << 16);
    }

    /**
     * @author Starlev
     * @reason BSWAP + ROR (2 intrinsic uops) instead of toABGR's 7-op mask chain.
     *         Fires once per colored vertex on the chunk/entity mesh fast path.
     */
    @Overwrite
    private static void putRgba(long pointer, int argb) {
        MemoryUtil.memPutInt(pointer, lomka$packColor(argb));
    }

    /**
     * @author Starlev
     * @reason 4 byte stores -> 1 int store. Hot on per-element vertex paths
     *         (particles, misc geometry); chunk meshes use the 11-arg fast path.
     */
    @Overwrite
    public VertexConsumer setColor(int r, int g, int b, int a) {
        //? if >=26.2 {
        /*long pointer = this.beginElement(1);
        *///?} else {
        long pointer = this.beginElement(VertexFormatElement.COLOR);
        //?}

        if (pointer != -1L) {
            int packed = IS_LITTLE_ENDIAN
                ? (a << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF)
                : (r << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | a;
            MemoryUtil.memPutInt(pointer, packed);
        }

        return this;
    }

    /**
     * @author Starlev
     * @reason 2 float stores -> 1 long store (hot on the glyph/particle vertex
     *         path). Each endianness branch orders the halves so u lands at +0
     *         and v at +4; floatToRawIntBits preserves NaN payloads exactly.
     */
    @Overwrite
    public VertexConsumer setUv(float u, float v) {
        //? if >=26.2 {
        /*long pointer = this.beginElement(2);
        *///?} else {
        long pointer = this.beginElement(VertexFormatElement.UV0);
        //?}

        if (pointer != -1L) {
            long packed = IS_LITTLE_ENDIAN
                ? ((long) Float.floatToRawIntBits(v) << 32) | (Float.floatToRawIntBits(u) & 0xFFFFFFFFL)
                : ((long) Float.floatToRawIntBits(u) << 32) | (Float.floatToRawIntBits(v) & 0xFFFFFFFFL);
            MemoryUtil.memPutLong(pointer, packed);
        }
        return this;
    }

    /**
     * @author Starlev
     * @reason 2 short stores -> 1 int store; both endianness branches reproduce
     *         vanilla's exact byte order ([s0hi, s0lo, s1hi, s1lo] on BE).
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
            int packed = IS_LITTLE_ENDIAN
                ? ((short1 & 0xFFFF) << 16) | (short0 & 0xFFFF)
                : (short0 << 16)            | (short1 & 0xFFFF);
            MemoryUtil.memPutInt(pointer, packed);
        }
        return this;
    }
}