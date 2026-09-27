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

package lomka.starl.mixins.net.minecraft.util;

import java.util.function.IntPredicate;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Mth.class)
public class MixinMth {

    /**
     * @author Starlev
     * @reason Use numberOfLeadingZeros instead of De Bruijn lookup table.
     */
    @Overwrite
    public static int ceillog2(int i) {
        return i > 1 ? 32 - Integer.numberOfLeadingZeros(i - 1) : 0;
    }

    /**
     * @author Starlev
     * @reason Use numberOfLeadingZeros instead of ceillog2 delegation.
     */
    @Overwrite
    public static int log2(int i) {
        return i > 0 ? 31 - Integer.numberOfLeadingZeros(i) : -1;
    }

    /**
     * @author Starlev
     * @reason Single LZCNT instruction replaces the five-step OR-cone. Guarded to match
     *         vanilla exactly: f(0)=0, f(negative)=0 except f(MIN_VALUE)=MIN_VALUE (vanilla's
     *         OR-cone saturates to -1 whose +1 overflows back), f(1)=1; without the guard
     *         value=0 would yield 1<<(32-nlz(-1))=1<<32=1.
     */
    @Overwrite
    public static int smallestEncompassingPowerOfTwo(int i) {
        if (i == Integer.MIN_VALUE) return i;
        return i <= 0 ? 0 : i == 1 ? 1 : 1 << (32 - Integer.numberOfLeadingZeros(i - 1));
    }

    /**
     * @author Starlev
     * @reason Use bitwise unsigned right shift instead of integer division.
     */
    @Overwrite
    public static int binarySearch(int i, int j, IntPredicate intpredicate) {
        if (j <= i) {
            return i;
        }
        int k = j - i;
        while (k > 0) {
            int l = k >>> 1;
            int i1 = i + l;
            if (intpredicate.test(i1)) {
                k = l;
            } else {
                i = i1 + 1;
                k -= l + 1;
            }
        }
        return i;
    }

    //? if >=1.21.4 {
    /**
     * @author Starlev
     * @reason Replace complex division and casts with a single float multiplication.
     */
    @Overwrite
    public static float unpackDegrees(byte b0) {
        return (float) b0 * 1.40625F;
    }
    //?}

    //? if >=1.21 {
    /**
     * @author Starlev
     * @reason Optimize HSV to ARGB conversion by lazily evaluating sector-specific floats (f5/f6)
     *         and bypassing modulo division on the normalized fast-path. Channel clamping and the
     *         default-branch exception replicate vanilla exactly (negative hues reach default just
     *         like vanilla's switch and must fail loudly, not fall through to sector 5).
     */
    @Overwrite
    public static int hsvToArgb(float f, float f1, float f2, int i) {
        int j = (int) (f * 6.0F) % 6;
        float val = f * 6.0F;
        float f3 = val - (float) j;
        float f4 = f2 * (1.0F - f1);
        float f7;
        float f8;
        float f9;

        if (j == 0) {
            float f6 = f2 * (1.0F - (1.0F - f3) * f1);
            f7 = f2;
            f8 = f6;
            f9 = f4;
        } else if (j == 1) {
            float f5 = f2 * (1.0F - f3 * f1);
            f7 = f5;
            f8 = f2;
            f9 = f4;
        } else if (j == 2) {
            float f6 = f2 * (1.0F - (1.0F - f3) * f1);
            f7 = f4;
            f8 = f2;
            f9 = f6;
        } else if (j == 3) {
            float f5 = f2 * (1.0F - f3 * f1);
            f7 = f4;
            f8 = f5;
            f9 = f2;
        } else if (j == 4) {
            float f6 = f2 * (1.0F - (1.0F - f3) * f1);
            f7 = f6;
            f8 = f4;
            f9 = f2;
        } else if (j == 5) {
            float f5 = f2 * (1.0F - f3 * f1);
            f7 = f2;
            f8 = f4;
            f9 = f5;
        } else {
            throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + f + ", " + f1 + ", " + f2);
        }

        int r = Math.min(Math.max((int) (f7 * 255.0F), 0), 255);
        int g = Math.min(Math.max((int) (f8 * 255.0F), 0), 255);
        int b = Math.min(Math.max((int) (f9 * 255.0F), 0), 255);

        return (i & 255) << 24 | r << 16 | g << 8 | b;
    }
    //?}
}