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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

        return i << 24 | r << 16 | g << 8 | b;
    }
    //?}

    /**
     * @author Starlev
     * @reason Eliminate second modulo: branch on sign is ~5x cheaper than float remainder.
     */
    @Overwrite
    public static float positiveModulo(float input, float mod) {
        float r = input % mod;
        if (r < 0.0F) return r + mod;
        if (r == 0.0F) return 0.0F;
        return r;
    }

    /**
     * @author Starlev
     * @reason Eliminate second modulo: branch on sign is ~5x cheaper than double remainder.
     */
    @Overwrite
    public static double positiveModulo(double input, double mod) {
        double r = input % mod;
        if (r < 0.0D) return r + mod;
        if (r == 0.0D) return 0.0D;
        return r;
    }

    //? if >=1.21.4 {
    /**
     * @author Starlev
     * @reason Replace the unbounded O(n) normalization loop (which can iterate millions
     *         of times and cost multiple milliseconds if an unnormalized angle difference
     *         ever reaches this function) with an O(1) single-remainder reduction.
     */
    @Overwrite
    public static float rotLerpRad(float f, float f1, float f2) {
        float diff = f2 - f1;
        float r = diff % 6.2831855F;
        if (r < -3.1415927F) {
            r += 6.2831855F;
        } else if (r >= 3.1415927F) {
            r -= 6.2831855F;
        }
        return f1 + f * r;
    }
    //?}

    /**
     * @author Starlev
     * @reason Optimize ray-AABB intersection with cheap early-out checks to bypass heavy math.
     */
    @Overwrite
    public static boolean rayIntersectsAABB(Vec3 vec3, Vec3 vec31, AABB aabb) {
        if (vec3.x > aabb.maxX) {
            if (vec31.x >= 0.0D) return false;
        } else if (vec3.x < aabb.minX) {
            if (vec31.x <= 0.0D) return false;
        }
        if (vec3.y > aabb.maxY) {
            if (vec31.y >= 0.0D) return false;
        } else if (vec3.y < aabb.minY) {
            if (vec31.y <= 0.0D) return false;
        }
        if (vec3.z > aabb.maxZ) {
            if (vec31.z >= 0.0D) return false;
        } else if (vec3.z < aabb.minZ) {
            if (vec31.z <= 0.0D) return false;
        }

        double cx = (aabb.minX + aabb.maxX) * 0.5D;
        double ex = (aabb.maxX - aabb.minX) * 0.5D;
        double dx = vec3.x - cx;
        double cy = (aabb.minY + aabb.maxY) * 0.5D;
        double ey = (aabb.maxY - aabb.minY) * 0.5D;
        double dy = vec3.y - cy;
        double cz = (aabb.minZ + aabb.maxZ) * 0.5D;
        double ez = (aabb.maxZ - aabb.minZ) * 0.5D;
        double dz = vec3.z - cz;

        double ax = Math.abs(vec31.x);
        double ay = Math.abs(vec31.y);
        double az = Math.abs(vec31.z);

        double cross;
        cross = vec31.y * dz - vec31.z * dy;
        if (Math.abs(cross) > ey * az + ez * ay) return false;
        cross = vec31.z * dx - vec31.x * dz;
        if (Math.abs(cross) > ex * az + ez * ax) return false;
        cross = vec31.x * dy - vec31.y * dx;
        return Math.abs(cross) < ex * ay + ey * ax;
    }
}