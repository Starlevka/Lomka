//? if >=1.21.11 {
package lomka.starl.mixins.net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MipmapGenerator.class)
public class MixinMipmapGenerator {

    @Unique private static final float[] lomka$C3 = new float[16];
    @Unique private static final float[] lomka$C4 = new float[16];
    @Unique private static final float[] lomka$C5 = new float[16];
    @Unique private static final float[] lomka$C6 = new float[16];

    static {
        int idx = 0;
        for (int r = 0; r < 4; ++r) {
            float fy = ((float) r + 0.5F) / 4.0F;
            for (int c = 0; c < 4; ++c) {
                float fx = ((float) c + 0.5F) / 4.0F;
                lomka$C3[idx] = (1.0F - fx) * (1.0F - fy);
                lomka$C4[idx] = fx * (1.0F - fy);
                lomka$C5[idx] = (1.0F - fx) * fy;
                lomka$C6[idx] = fx * fy;
                idx++;
            }
        }
    }

    @Overwrite
    private static float alphaTestCoverage(NativeImage image, float threshold, float scale) {
        int w = image.getWidth();
        int h = image.getHeight();
        float invScale = scale / 255.0F;
        int count = 0;

        float[] topRow = new float[w];
        float[] botRow = new float[w];

        for (int x = 0; x < w; ++x) {
            topRow[x] = Math.min((image.getPixel(x, 0) >>> 24) * invScale, 1.0F);
        }

        for (int y = 0; y < h - 1; ++y) {
            for (int x = 0; x < w; ++x) {
                botRow[x] = Math.min((image.getPixel(x, y + 1) >>> 24) * invScale, 1.0F);
            }

            for (int x = 0; x < w - 1; ++x) {
                float tl = topRow[x];
                float tr = topRow[x + 1];
                float bl = botRow[x];
                float br = botRow[x + 1];
                int hits = 0;

                for (int s = 0; s < 16; ++s) {
                    if (tl * lomka$C3[s] + tr * lomka$C4[s] + bl * lomka$C5[s] + br * lomka$C6[s] > threshold) {
                        ++hits;
                    }
                }

                count += hits;
            }

            float[] tmp = topRow;
            topRow = botRow;
            botRow = tmp;
        }

        return (float) count / (float) ((w - 1) * (h - 1) * 16);
    }

    @Overwrite
    private static int darkenedAlphaBlend(int i, int j, int k, int l) {
        short[] lin  = ARGB.SRGB_TO_LINEAR;
        byte[]  srgb = ARGB.LINEAR_TO_SRGB;
        int aSum = 0, rSum = 0, gSum = 0, bSum = 0;

        int a0 = i >>> 24;
        if (a0 != 0) { aSum  = lin[a0]; rSum  = lin[(i >> 16) & 255]; gSum  = lin[(i >> 8) & 255]; bSum  = lin[i & 255]; }
        int a1 = j >>> 24;
        if (a1 != 0) { aSum += lin[a1]; rSum += lin[(j >> 16) & 255]; gSum += lin[(j >> 8) & 255]; bSum += lin[j & 255]; }
        int a2 = k >>> 24;
        if (a2 != 0) { aSum += lin[a2]; rSum += lin[(k >> 16) & 255]; gSum += lin[(k >> 8) & 255]; bSum += lin[k & 255]; }
        int a3 = l >>> 24;
        if (a3 != 0) { aSum += lin[a3]; rSum += lin[(l >> 16) & 255]; gSum += lin[(l >> 8) & 255]; bSum += lin[l & 255]; }

        return ((srgb[aSum >> 2] & 255) << 24)
             | ((srgb[rSum >> 2] & 255) << 16)
             | ((srgb[gSum >> 2] & 255) << 8)
             |  (srgb[bSum >> 2] & 255);
    }
}
//?}

//? if <1.21.11 {
/*package lomka.starl.mixins.net.minecraft.client.renderer.texture;

import net.minecraft.client.renderer.texture.MipmapGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MipmapGenerator.class)
public class MixinMipmapGenerator {

    @Unique
    private static final byte[] lomka$LINEAR_TO_SRGB = new byte[4096];

    static {
        for (int i = 0; i < 4096; i++) {
            float v = i / 4095.0F;
            lomka$LINEAR_TO_SRGB[i] = (byte) (int) (Math.pow(v, 0.45454545454545453D) * 255.0D);
        }
    }

    @Shadow
    private static float getPow22(int i) {
        throw new AssertionError();
    }

    @Overwrite
    private static int gammaBlend(int i, int j, int k, int l, int i1) {
        float f  = getPow22(i >> i1);
        float f1 = getPow22(j >> i1);
        float f2 = getPow22(k >> i1);
        float f3 = getPow22(l >> i1);

        int idx = (int) ((f + f1 + f2 + f3) * 1023.75F);
        if (idx > 4095) idx = 4095;

        return lomka$LINEAR_TO_SRGB[idx] & 255;
    }

}
*///?}