/*
 * This file is part of Lomka (https://github.com/Starlevka/Lomka)
 * Copyright (C) 2026 Starlev (a.k.a. Starlevka) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 */

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

    /**
     * Routes every StringSplitter width lookup through the font's glyph source advance
     * (with bold scaling) instead of the vanilla lambda that re-walks glyph resolution,
     * cutting a per-codepoint lookup layer on the text layout hot path.
     */
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