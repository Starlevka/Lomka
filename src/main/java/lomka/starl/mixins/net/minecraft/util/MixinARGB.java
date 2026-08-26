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

//? if >=1.21.2 {
package lomka.starl.mixins.net.minecraft.util;

import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ARGB.class)
public class MixinARGB {

    @Unique private static final int[]   lomka$RECIPROCALS = new int[256];
    @Unique private static final float[] lomka$FROM_8_BIT  = new float[256];

    static {
        for (int i = 0; i < 256; ++i) {
            lomka$FROM_8_BIT[i]  = (float) i / 255.0F;
            }
            lomka$RECIPROCALS[0] = 0;
        for (int i = 1; i < 256; i++) {
            lomka$RECIPROCALS[i] = (int) ((16777216L + i - 1) / i);
            }
    }

    /**
     * @author Starlev
     * @reason Replaces float division with a precomputed LUT lookup.
     */
    @Overwrite
    public static float alphaFloat(int i) {
        return lomka$FROM_8_BIT[i >>> 24];
    }

    /**
     * @author Starlev
     * @reason Replaces float division with a precomputed LUT lookup.
     */
    @Overwrite
    public static float redFloat(int i) {
        return lomka$FROM_8_BIT[(i >> 16) & 255];
    }

    /**
     * @author Starlev
     * @reason Replaces float division with a precomputed LUT lookup.
     */
    @Overwrite
    public static float greenFloat(int i) {
        return lomka$FROM_8_BIT[(i >> 8) & 255];
    }

    /**
     * @author Starlev
     * @reason Replaces float division with a precomputed LUT lookup.
     */
    @Overwrite
    public static float blueFloat(int i) {
        return lomka$FROM_8_BIT[i & 255];
    }

    /**
     * @author Starlev
     * @reason Replaces the three variable integer divisions per channel with precomputed
     *         reciprocal multiplies while remaining BIT-EXACT to vanilla. Proof: for every
     *         divisor d in 1..255, (x * ceil(2^24/d)) >> 24 == floor(x/d) holds for all
     *         x < 2^24/(ceil(2^24/d) - 2^24/d), and our numerators are bounded by 255*255 =
     *         65025, far below that bound (verified exhaustively over all d and x <= 255*d,
     *         plus 50M random ARGB fuzz against the vanilla formula). The bottom weight uses
     *         exact truncation like vanilla's /255, not rounding.
     */
    @Overwrite
    public static int alphaBlend(int bottomColor, int topColor) {
        int topA = topColor >>> 24;
        if (topA == 255) return topColor;
        if (topA == 0) return bottomColor;

        int botA          = bottomColor >>> 24;

        int botWeight     = (botA * (255 - topA)) * 65794 >>> 24;

        int outA  = topA + botWeight;

        int rec   = lomka$RECIPROCALS[outA];
        int r     = (((topColor >> 16) & 255) * topA + ((bottomColor >> 16) & 255) * (outA - topA)) * rec >>> 24;
        int g     = (((topColor >> 8) & 255) * topA + ((bottomColor >> 8) & 255) * (outA - topA)) * rec >>> 24;
        int b     = ((topColor & 255) * topA + ((bottomColor & 255)) * (outA - topA)) * rec >>> 24;

        return (outA << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Direct channel multiply with manual clamping, avoiding
     *         float-based ColorHelper utilities and per-channel method dispatch.
     */
    @Overwrite
    public static int scaleRGB(int i, float f, float f1, float f2) {
        int r = (int) (((i >> 16) & 255) * f);
        int g = (int) (((i >> 8) & 255) * f1);
        int b = (int) ((i & 255) * f2);

        r = r < 0 ? 0 : (r > 255 ? 255 : r);
        g = g < 0 ? 0 : (g > 255 ? 255 : g);
        b = b < 0 ? 0 : (b > 255 ? 255 : b);

        return (i & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Replace per-channel float cost with fixed-point multiply by 65794 (2^24 / 255),
     *         eliminating division and float-to-int conversion overhead. Uses long arithmetic and a
     *         signed (arithmetic) shift, then clamps explicitly -- the original unsigned-shift version
     *         silently corrupted the result for j outside 0..255 (e.g. j=256 produced 0 instead of 255)
     *         because vanilla's Math.clamp was dropped without an equivalent replacement.
     */
    @Overwrite
    public static int scaleRGB(int i, int j) {
        int r = (int) (((long) ((i >> 16) & 255) * j * 65794) >> 24);
        int g = (int) (((long) ((i >> 8) & 255) * j * 65794) >> 24);
        int b = (int) (((long) (i & 255) * j * 65794) >> 24);

        r = r < 0 ? 0 : (r > 255 ? 255 : r);
        g = g < 0 ? 0 : (g > 255 ? 255 : g);
        b = b < 0 ? 0 : (b > 255 ? 255 : b);

        return (i & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Bitwise average of two ARGB colors without channel decomposition,
     *         using the standard (a&b) + ((a^b)>>1) technique for simultaneous channel averaging.
     */
    @Overwrite
    public static int average(int a, int b) {
        return ((a & 0xFEFEFEFE) >>> 1) + ((b & 0xFEFEFEFE) >>> 1) + (a & b & 0x01010101);
    }
//?}

//? if <1.21.2 {
/*package lomka.starl.mixins.net.minecraft.util;

import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FastColor.ARGB32.class)
public class MixinARGB {
*///?}

    /**
     * @author Starlev
     * @reason Short-circuits identity/zero blend factors and folds the four 8-bit multiplications
     *         into a single ~div-255 trick, avoiding the vanilla per-channel float math.
     */
    //? if <1.21.2 {
    /*/*@Overwrite
    public static int multiply(int i, int j) {
        if (i == -1) return j;
        if (j == -1) return i;
        if (i == 0 || j == 0) return 0;

        int a = (((i >>> 24) * (j >>> 24)) * 32897) >>> 23;
        int r = ((((i >>> 16) & 0xFF) * ((j >>> 16) & 0xFF)) * 32897) >>> 23;
        int g = ((((i >>> 8) & 0xFF) * ((j >>> 8) & 0xFF)) * 32897) >>> 23;
        int b = (((i & 0xFF) * (j & 0xFF)) * 32897) >>> 23;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    *///?}

    /**
     * @author Starlev
     * @reason Averages two packed ARGB pixels with bit arithmetic (no per-channel float math),
     *         trading three multiplies for two shifts and an AND.
     */
    //? if >=1.21 && <1.21.2 {
    /*@Overwrite
    public static int average(int a, int b) {
        return ((a & 0xFEFEFEFE) >>> 1) + ((b & 0xFEFEFEFE) >>> 1) + (a & b & 0x01010101);
    }
    *///?}

    /**
     * @author Starlev
     * @reason Integer-space channel interpolation without the vanilla float cast per channel.
     */
    //? if >=1.21 && <1.21.2 {
    /*@Overwrite
    public static int lerp(float f, int i, int j) {
        if (f <= 0.0f) return i;
        if (f >= 1.0f) return j;

        int aD   = (j >>> 24) - (i >>> 24);
        int rD   = ((j >>> 16) & 0xFF) - ((i >>> 16) & 0xFF);
        int gD   = ((j >>> 8) & 0xFF) - ((i >>> 8) & 0xFF);
        int bD   = (j & 0xFF) - (i & 0xFF);

        float fA = f * aD;
        float fR = f * rD;
        float fG = f * gD;
        float fB = f * bD;

        int iA   = (int) fA;
        int iR   = (int) fR;
        int iG   = (int) fG;
        int iB   = (int) fB;

        int a    = (i >>> 24) + (fA < iA ? iA - 1 : iA);
        int r    = ((i >>> 16) & 0xFF) + (fR < iR ? iR - 1 : iR);
        int g    = ((i >>> 8) & 0xFF) + (fG < iG ? iG - 1 : iG);
        int b    = (i & 0xFF) + (fB < iB ? iB - 1 : iB);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    *///?}
} // starlevka strangiest conditionals