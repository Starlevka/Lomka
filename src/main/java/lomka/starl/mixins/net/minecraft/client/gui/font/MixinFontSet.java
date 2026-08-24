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

    /**
     * Clears the cached glyph advances when the font set is rebuilt
     * so stale width data never outlives a resource reload.
     */
    @Inject(method = "resetTextures", at = @At("HEAD"))
    private void lomka$clearAdvanceCaches(CallbackInfo ci) {
        ((IGlyphSource) (Object) this.anyGlyphs).lomka$clear();
        ((IGlyphSource) (Object) this.nonFishyGlyphs).lomka$clear();
    }
}