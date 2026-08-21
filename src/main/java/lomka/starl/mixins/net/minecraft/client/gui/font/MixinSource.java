package lomka.starl.mixins.net.minecraft.client.gui.font;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import lomka.starl.duck.IGlyphSource;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FontSet.Source.class)
public abstract class MixinSource implements IGlyphSource {

    @Unique private final Int2FloatOpenHashMap lomka$advanceCache = new Int2FloatOpenHashMap(16, 0.75F);

    @Shadow protected abstract BakedGlyph getGlyph(int codepoint);

    @Override
    public void lomka$clear() {
        synchronized (this.lomka$advanceCache) {
            this.lomka$advanceCache.clear();
        }
    }

    @Override
    public float getAdvance(int codepoint, boolean bold) {
        int key = codepoint << 1 | (bold ? 1 : 0);
        synchronized (this.lomka$advanceCache) {
            float cached = this.lomka$advanceCache.getOrDefault(key, Float.NaN);
            if (!Float.isNaN(cached)) {
                return cached;
            }
            float advance = this.getGlyph(codepoint).info().getAdvance(bold);
            this.lomka$advanceCache.put(key, advance);
            return advance;
        }
    }
}