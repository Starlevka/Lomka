package lomka.starl.mixins.net.minecraft.client.gui.font;

import lomka.starl.duck.IGlyphSource;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontSet.class)
public abstract class MixinFontSet {

    @Shadow private GlyphSource anyGlyphs;
    @Shadow private GlyphSource nonFishyGlyphs;

    @Inject(method = "resetTextures", at = @At("HEAD"))
    private void lomka$clearAdvanceCaches(CallbackInfo ci) {
        ((IGlyphSource) (Object) this.anyGlyphs).lomka$clear();
        ((IGlyphSource) (Object) this.nonFishyGlyphs).lomka$clear();
    }
}