//? if >=1.21.2 {
package lomka.starl.mixins.net.minecraft.util;

import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ARGB.class)
public class MixinARGB {

    @Shadow
    private static short[] SRGB_TO_LINEAR;
    @Shadow
    private static byte[] LINEAR_TO_SRGB;

    @Unique
    private static final int[] lomka$RECIPROCALS = new int[256];
    @Unique
    private static final float[] lomka$FROM_8_BIT = new float[256];

    static {
        for (int i = 0; i < 256; ++i) {
            lomka$FROM_8_BIT[i] = (float) i / 255.0F;
        }
        lomka$RECIPROCALS[0] = 0;
        for (int i = 1; i < 256; i++) {
            int min_m = 0;
            for (int rSum = 1; rSum <= 65025; rSum++) {
                int target = rSum / i;
                int lower_bound = ((target << 24) + rSum - 1) / rSum;
                min_m = Math.max(min_m, lower_bound);
            }
            lomka$RECIPROCALS[i] = min_m;
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
     * @reason Use a single multiplication by 32897 (2^23 / 255) with right-shift
     * instead of division by 255 for each channel, eliminating 4 integer divisions.
     */
    @Overwrite
    public static int multiply(int i, int j) {
        if (i == -1) return j;
        if (j == -1) return i;

        int a = (((i >>> 24) * (j >>> 24)) * 32897) >>> 23;
        int r = ((((i >> 16) & 255) * ((j >> 16) & 255)) * 32897) >>> 23;
        int g = ((((i >> 8) & 255) * ((j >> 8) & 255)) * 32897) >>> 23;
        int b = (((i & 255) * (j & 255)) * 32897) >>> 23;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Optimize the blend with early-outs for fully opaque/transparent,
     * integer-only arithmetic, and a precomputed reciprocal LUT for non-opaque results.
     */
    @Overwrite
    public static int alphaBlend(int bottomColor, int topColor) {
        int topA = topColor >>> 24;

        if (topA == 255) return topColor;
        if (topA == 0) return bottomColor;

        int botA = bottomColor >>> 24;

        int botWeightBase = botA * (255 - topA);
        int botWeight = (botWeightBase + 1 + (botWeightBase >> 8)) >> 8;

        int outA = topA + botWeight;

        int rSum = (((topColor >> 16) & 255) * topA) + (((bottomColor >> 16) & 255) * botWeight);
        int gSum = (((topColor >> 8)  & 255) * topA) + (((bottomColor >> 8)  & 255) * botWeight);
        int bSum = ((topColor & 255)         * topA) + ((bottomColor & 255)         * botWeight);

        if (outA == 255) {
            int r = (rSum + 1 + (rSum >> 8)) >> 8;
            int g = (gSum + 1 + (gSum >> 8)) >> 8;
            int b = (bSum + 1 + (bSum >> 8)) >> 8;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        int reciprocal = lomka$RECIPROCALS[outA];
        int r = (rSum * reciprocal) >>> 24;
        int g = (gSum * reciprocal) >>> 24;
        int b = (bSum * reciprocal) >>> 24;

        return (outA << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Use fixed-point luminance weights instead of floating-point math,
     * avoiding float-to-int conversions and rounding overhead.
     */
    @Overwrite
    public static int greyscale(int i) {
        int r = (i >> 16) & 0xFF;
        int g = (i >> 8) & 0xFF;
        int b = i & 0xFF;

        int gray = (r * 19661 + g * 38666 + b * 7209) >>> 16;

        return (i & 0xFF000000) | (gray << 16) | (gray << 8) | gray;
    }

    /**
     * @author Starlev
     * @reason Per-channel float lerp with truncation-based rounding,
     * avoiding Math.floor() and Objects.hash() overhead.
     */
    @Overwrite
    public static int srgbLerp(float f, int i, int j) {
        int aI = i >>> 24;
        int rI = (i >> 16) & 255;
        int gI = (i >> 8) & 255;
        int bI = i & 255;

        int aJ = j >>> 24;
        int rJ = (j >> 16) & 255;
        int gJ = (j >> 8) & 255;
        int bJ = j & 255;

        float fA = f * (aJ - aI);
        float fR = f * (rJ - rI);
        float fG = f * (gJ - gI);
        float fB = f * (bJ - bI);

        int iA = (int) fA;
        int iR = (int) fR;
        int iG = (int) fG;
        int iB = (int) fB;

        int a = aI + (fA < iA ? iA - 1 : iA);
        int r = rI + (fR < iR ? iR - 1 : iR);
        int g = gI + (fG < iG ? iG - 1 : iG);
        int b = bI + (fB < iB ? iB - 1 : iB);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Unrolled channel averaging in linear space using SRGB_TO_LINEAR/LINEAR_TO_SRGB tables,
     * eliminating per-channel method calls and temporary objects.
     */
    @Overwrite
    public static int meanLinear(int i, int j, int k, int l) {
        int a = ((i >>> 24) + (j >>> 24) + (k >>> 24) + (l >>> 24)) >> 2;

        int rLin = (SRGB_TO_LINEAR[(i >> 16) & 0xFF] + SRGB_TO_LINEAR[(j >> 16) & 0xFF] +
                    SRGB_TO_LINEAR[(k >> 16) & 0xFF] + SRGB_TO_LINEAR[(l >> 16) & 0xFF]) >> 2;

        int gLin = (SRGB_TO_LINEAR[(i >> 8)  & 0xFF] + SRGB_TO_LINEAR[(j >> 8)  & 0xFF] +
                    SRGB_TO_LINEAR[(k >> 8)  & 0xFF] + SRGB_TO_LINEAR[(l >> 8)  & 0xFF]) >> 2;

        int bLin = (SRGB_TO_LINEAR[i         & 0xFF] + SRGB_TO_LINEAR[j         & 0xFF] +
                    SRGB_TO_LINEAR[k         & 0xFF] + SRGB_TO_LINEAR[l         & 0xFF]) >> 2;

        int r = LINEAR_TO_SRGB[rLin] & 0xFF;
        int g = LINEAR_TO_SRGB[gLin] & 0xFF;
        int b = LINEAR_TO_SRGB[bLin] & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Per-channel linear-space interpolation with early SRGB_TO_LINEAR lookup,
     * avoiding ColorHelper method call overhead and temporary int/float arrays.
     */
    @Overwrite
    public static int linearLerp(float f, int i, int j) {
        int aI = i >>> 24;
        int rI = (i >> 16) & 255;
        int gI = (i >> 8) & 255;
        int bI = i & 255;

        int aJ = j >>> 24;
        int rJ = (j >> 16) & 255;
        int gJ = (j >> 8) & 255;
        int bJ = j & 255;

        float fA = f * (aJ - aI);
        int iA = (int) fA;
        int a = aI + (fA < iA ? iA - 1 : iA);

        int rStart = SRGB_TO_LINEAR[rI];
        int rEnd = SRGB_TO_LINEAR[rJ];
        float fR = f * (rEnd - rStart);
        int iR = (int) fR;
        int r = LINEAR_TO_SRGB[rStart + (fR < iR ? iR - 1 : iR)] & 255;

        int gStart = SRGB_TO_LINEAR[gI];
        int gEnd = SRGB_TO_LINEAR[gJ];
        float fG = f * (gEnd - gStart);
        int iG = (int) fG;
        int g = LINEAR_TO_SRGB[gStart + (fG < iG ? iG - 1 : iG)] & 255;

        int bStart = SRGB_TO_LINEAR[bI];
        int bEnd = SRGB_TO_LINEAR[bJ];
        float fB = f * (bEnd - bStart);
        int iB = (int) fB;
        int b = LINEAR_TO_SRGB[bStart + (fB < iB ? iB - 1 : iB)] & 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Direct channel multiply with manual clamping, avoiding
     * float-based ColorHelper utilities and per-channel method dispatch.
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
     * eliminating division and float-to-int conversion overhead.
     */
    @Overwrite
    public static int scaleRGB(int i, int j) {
        int r = (((i >> 16) & 255) * j * 65794) >>> 24;
        int g = (((i >> 8) & 255) * j * 65794) >>> 24;
        int b = ((i & 255) * j * 65794) >>> 24;

        return (i & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /**
     * @author Starlev
     * @reason Early-outs for zero/fully-opaque, single channel multiply instead of
     * full ARGB decomposition.
     */
    @Overwrite
    public static int multiplyAlpha(int color, float alphaMultiplier) {
        if (color == 0 || alphaMultiplier <= 0.0F) return 0;
        if (alphaMultiplier >= 1.0F) return color;
        int newA = (int) ((color >>> 24) * alphaMultiplier);
        return (newA << 24) | (color & 0x00FFFFFF);
    }

    /**
     * @author Starlev
     * @reason Bitwise average of two ARGB colors without channel decomposition,
     * using the standard (a&b) + ((a^b)>>1) technique for simultaneous channel averaging.
     */
    @Overwrite
    public static int average(int a, int b) {
        return ((a & 0xFEFEFEFE) >>> 1) + ((b & 0xFEFEFEFE) >>> 1) + (a & b & 0x01010101);
    }

    @Overwrite
    public static int addRgb(int a, int b) {
        return (a & 0xFF000000)
             | Math.min((a & 0xFF0000) + (b & 0xFF0000), 0xFF0000)
             | Math.min((a & 0xFF00) + (b & 0xFF00), 0xFF00)
             | Math.min((a & 0xFF) + (b & 0xFF), 0xFF);
    }

    /**
     * @author Starlev
     * @reason Per-channel saturated subtract without full ARGB decomposition,
     * using bitmask extraction and Math.max for clamping.
     */
    @Overwrite
    public static int subtractRgb(int a, int b) {
        return (a & 0xFF000000)
             | Math.max((a & 0xFF0000) - (b & 0xFF0000), 0)
             | Math.max((a & 0xFF00) - (b & 0xFF00), 0)
             | Math.max((a & 0xFF) - (b & 0xFF), 0);
    }
}
//?}

//? if <1.21.2 {
/*package lomka.starl.mixins.net.minecraft.util;

import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FastColor.ARGB32.class)
public class MixinARGB {

    @Overwrite
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

    @Overwrite
    public static int average(int a, int b) {
        return ((a & 0xFEFEFEFE) >>> 1) + ((b & 0xFEFEFEFE) >>> 1) + (a & b & 0x01010101);
    }

    @Overwrite
    public static int lerp(float f, int i, int j) {
        if (f <= 0.0f) return i;
        if (f >= 1.0f) return j;
        int t = (int) (f * 256.0f);
        int s = 256 - t;
        int a = ((i >>> 24) * s + (j >>> 24) * t) >>> 8;
        int r = (((i >>> 16) & 0xFF) * s + ((j >>> 16) & 0xFF) * t) >>> 8;
        int g = (((i >>> 8) & 0xFF) * s + ((j >>> 8) & 0xFF) * t) >>> 8;
        int b = ((i & 0xFF) * s + (j & 0xFF) * t) >>> 8;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}*/
//?}