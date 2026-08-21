package lomka.starl.mixins.net.minecraft.client.gui;

import lomka.starl.duck.IGlyphSource;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlyphSource.class)
public interface MixinGlyphSource extends IGlyphSource {

    @Shadow BakedGlyph getGlyph(int codepoint);

    @Override
    default float getAdvance(int codepoint, boolean bold) {
        return this.getGlyph(codepoint).info().getAdvance(bold);
    }
}