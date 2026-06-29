//? if >=1.21.11 {
package lomka.starl.mixins.net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import java.io.IOException;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

@Mixin(PalettedPermutations.class)
public class MixinPalettedPermutations {

    @Mixin(targets = "net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations$PalettedSpriteSupplier")
    public static class PalettedSpriteSupplier {

        @Shadow @Final private LazyLoadedImage baseImage;
        @Shadow @Final private Supplier<IntUnaryOperator> palette;
        @Shadow @Final private Identifier permutationLocation;

        @Overwrite
        public @Nullable SpriteContents get(SpriteResourceLoader spriteresourceloader) {
            SpriteContents spritecontents;
            try {
                NativeImage nativeImageBase = this.baseImage.get();
                IntUnaryOperator originalPalette = this.palette.get();

                IntUnaryOperator cachedPalette = new IntUnaryOperator() {
                    private int lastIn = -1;
                    private int lastOut = -1;

                    @Override
                    public int applyAsInt(int k) {
                        if (k == this.lastIn) {
                            return this.lastOut;
                        }
                        int out = originalPalette.applyAsInt(k);
                        this.lastIn = k;
                        this.lastOut = out;
                        return out;
                    }
                };

                NativeImage nativeimage = nativeImageBase.mappedCopy(cachedPalette);
                spritecontents = new SpriteContents(this.permutationLocation, new FrameSize(nativeimage.getWidth(), nativeimage.getHeight()), nativeimage);
                return spritecontents;
            } catch (IllegalArgumentException | IOException ioexception) {
                PalettedPermutations.LOGGER.error("unable to apply palette to {}", this.permutationLocation, ioexception);
                spritecontents = null;
            } finally {
                this.baseImage.release();
            }
            return spritecontents;
        }
    }

    @Mixin(PalettedPermutations.class)
    public static class PalettedPermutationsMath {

        @Shadow @Final private static Logger LOGGER;

        @Overwrite
        private static IntUnaryOperator createPaletteMapping(int[] aint, int[] aint1) {
            if (aint1.length != aint.length) {
                LOGGER.warn("Palette mapping has different sizes: {} and {}", aint.length, aint1.length);
                throw new IllegalArgumentException();
            }
            it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap int2intopenhashmap = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap(aint1.length);
            for (int i = 0; i < aint.length; ++i) {
                int j = aint[i];
                if (((j >>> 24) & 255) != 0) {
                    int2intopenhashmap.put(j & 16777215, aint1[i]);
                }
            }
            return (k) -> {
                int l = k >>> 24;
                if (l == 0) {
                    return k;
                }
                int i1 = k & 16777215;
                int j1 = int2intopenhashmap.getOrDefault(i1, i1 | -16777216);
                int k1 = j1 >>> 24;
                int outA = (l * k1 * 65794) >>> 24;
                return (outA << 24) | (j1 & 16777215);
            };
        }
    }
}
//?}
