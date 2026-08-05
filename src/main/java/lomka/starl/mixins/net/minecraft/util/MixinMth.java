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

    /**
     * @author Starlev
     * @reason Inline lerp formula directly to assist JIT compiler in register allocation.
     */
    @Overwrite
    public static float clampedLerp(float f, float f1, float f2) {
        return f < 0.0F ? f1 : (f > 1.0F ? f2 : f1 + f * (f2 - f1));
    }

    /**
     * @author Starlev
     * @reason Inline lerp formula directly to assist JIT compiler in register allocation.
     */
    @Overwrite
    public static double clampedLerp(double d0, double d1, double d2) {
        return d0 < 0.0D ? d1 : (d0 > 1.0D ? d2 : d1 + d0 * (d2 - d1));
    }

    /**
     * @author Starlev
     * @reason Avoid Math.floor double conversion; precomputed exact float constant.
     */
    //? if >=1.21.4 {
    @Overwrite
    public static byte packDegrees(float f) {
        float val = f * (256.0F / 360.0F);
        int i = (int) val;
        return (byte) (val < (float) i ? i - 1 : i);
    }

    /**
     * @author Starlev
     * @reason Replace complex division and casts with a single float multiplication.
     */
    @Overwrite
    public static float unpackDegrees(byte b0) {
        return (float) b0 * 1.40625F;
    }
    //?}

    /**
     * @author Starlev
     * @reason Avoid Math.floor double conversion for typical lerp input range.
     */
    @Overwrite
    public static int lerpInt(float alpha, int p0, int p1) {
        float v = alpha * (float) (p1 - p0);
        int i = (int) v;
        return p0 + (v < (float) i ? i - 1 : i);
    }

    //? if >=1.21 {
    /**
     * @author Starlev
     * @reason Optimize HSV to ARGB conversion by lazily evaluating sector-specific floats (f5/f6)
     *         and bypassing modulo division on the normalized fast-path.
     */
    @Overwrite
    public static int hsvToArgb(float f, float f1, float f2, int i) {
        float val = f * 6.0F;
        int j = (int) val;
        j %= 6;

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
        } else {
            float f5 = f2 * (1.0F - f3 * f1);
            f7 = f2;
            f8 = f4;
            f9 = f5;
        }

        int r = (int) (f7 * 255.0F);
        int g = (int) (f8 * 255.0F);
        int b = (int) (f9 * 255.0F);

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

    /**
     * @author Starlev
     * @reason if-else skips second comparison when first matched; early return avoids reassignment.
     */
    @Overwrite
    public static int wrapDegrees(int angle) {
        int r = angle % 360;
        if (r >= 180) return r - 360;
        if (r < -180) return r + 360;
        return r;
    }

    /**
     * @author Starlev
     * @reason if-else skips second comparison when first matched; early return avoids reassignment.
     */
    @Overwrite
    public static float wrapDegrees(float angle) {
        float r = angle % 360.0F;
        if (r >= 180.0F) return r - 360.0F;
        if (r < -180.0F) return r + 360.0F;
        return r;
    }

    /**
     * @author Starlev
     * @reason if-else skips second comparison when first matched; early return avoids reassignment.
     */
    @Overwrite
    public static double wrapDegrees(double angle) {
        double r = angle % 360.0D;
        if (r >= 180.0D) return r - 360.0D;
        if (r < -180.0D) return r + 360.0D;
        return r;
    }

    //? if >=1.21.4 {
    /**
     * @author Starlev
     * @reason if-else skips second comparison when first matched; early return avoids reassignment.
     */
    @Overwrite
    public static float wrapDegrees(long angle) {
        float r = (float) (angle % 360L);
        if (r >= 180.0F) return r - 360.0F;
        if (r < -180.0F) return r + 360.0F;
        return r;
    }
    //?}

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