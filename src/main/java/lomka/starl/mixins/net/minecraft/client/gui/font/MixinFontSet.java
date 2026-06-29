package lomka.starl.mixins.net.minecraft.client.gui.font;

import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(FontSet.class)
public class MixinFontSet {
    
    @Mutable @Shadow @Final private GlyphSource anyGlyphs;
    @Mutable @Shadow @Final private GlyphSource nonFishyGlyphs;

    /**
     * Wraps GlyphSource with array-based cache to skip HashMap lookup for common codepoints.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void wrapSourcesWithFastCache(CallbackInfo ci) {
        this.anyGlyphs = new FastGlyphSourceWrapper(this.anyGlyphs);
        this.nonFishyGlyphs = new FastGlyphSourceWrapper(this.nonFishyGlyphs);
    }

    /**
     * Invalidates glyph cache on texture reset to ensure stale references are cleared.
     */
    @Inject(method = "resetTextures", at = @At("RETURN"))
    private void clearFastCache(CallbackInfo ci) {
        ((FastGlyphSourceWrapper) this.anyGlyphs).clearCache();
        ((FastGlyphSourceWrapper) this.nonFishyGlyphs).clearCache();
    }

    @Unique
    private static final class FastGlyphSourceWrapper implements GlyphSource {
        private final GlyphSource delegate;

        private static final int CACHE_SIZE = 8192;
        private final BakedGlyph[] fastCache = new BakedGlyph[CACHE_SIZE];

        /**
         * Array-backed glyph cache that bypasses the slow HashMap lookup in the
         * default GlyphSource for codepoints in the ASCII/BMP range (0–8191).
         */

        FastGlyphSourceWrapper(GlyphSource delegate) {
            this.delegate = delegate;
        }

        public void clearCache() {
            Arrays.fill(this.fastCache, null);
        }

        @Override
        public BakedGlyph getGlyph(int codepoint) {
            if (codepoint >= 0 && codepoint < CACHE_SIZE) {
                BakedGlyph cached = this.fastCache[codepoint];
                if (cached != null) {
                    return cached;
                }

                BakedGlyph computed = this.delegate.getGlyph(codepoint);
                this.fastCache[codepoint] = computed;
                return computed;
            }
            
            return this.delegate.getGlyph(codepoint);
        }

        @Override
        public BakedGlyph getRandomGlyph(RandomSource randomsource, int i) {
            return this.delegate.getRandomGlyph(randomsource, i);
        }
    }
}