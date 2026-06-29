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
     * @reason Use numberOfLeadingZeros instead of manual bit-shift chain.
     */
    @Overwrite
    public static int smallestEncompassingPowerOfTwo(int i) {
        return i > 1 ? 1 << (32 - Integer.numberOfLeadingZeros(i - 1)) : i;
    }

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
        return i > 0 ? 31 - Integer.numberOfLeadingZeros(i) : 0;
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
     * @reason Avoid Math.floor double conversion; precomputed exact float constant.
     */
    //? if >=1.21.9 {
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

    /**
     * @author Starlev
     * @reason Eliminate second modulo: branch on sign is ~5x cheaper than float remainder.
     */
    @Overwrite
    public static float positiveModulo(float input, float mod) {
        float r = input % mod;
        return r < 0.0F ? r + mod : r;
    }

    /**
     * @author Starlev
     * @reason Eliminate second modulo: branch on sign is ~5x cheaper than double remainder.
     */
    @Overwrite
    public static double positiveModulo(double input, double mod) {
        double r = input % mod;
        return r < 0.0D ? r + mod : r;
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