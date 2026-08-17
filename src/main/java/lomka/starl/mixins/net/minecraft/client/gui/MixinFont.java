package lomka.starl.mixins.net.minecraft.client.gui;

import lomka.starl.duck.IGlyphSource;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Font.class)
public abstract class MixinFont {

    @Shadow
    private GlyphSource getGlyphSource(FontDescription fontdescription) {
        throw new AssertionError();
    }

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/StringSplitter;<init>(Lnet/minecraft/client/StringSplitter$WidthProvider;)V"),
        index = 0
    )
    private StringSplitter.WidthProvider lomka$cachedWidthProvider(StringSplitter.WidthProvider original) {
        return (codepoint, style) -> ((IGlyphSource) (Object) this.getGlyphSource(style.getFont()))
                .getAdvance(codepoint, style.isBold());
    }
}