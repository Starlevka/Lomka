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

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * <p>Origin: last-font memo ported from Collections Of Optimizations by Misanthropy. (LGPLv3)
 * Reimplemented here as a plain {@code @Redirect} without the MixinExtras/config
 * wrapper so the per-codepoint path stays branch-lean.
 *
 * <p>One-slot cache on the caller instance: consecutive codepoints of a styled run
 * share the same font id, so an identity hit skips the font map walk entirely and a
 * font switch falls through to the vanilla lookup once. Instances are short-lived
 * (one per laid-out string), so the slot never grows. {@code ResourceLocation} ids
 * are not guaranteed interned across mods, so an identity miss falls back to
 * {@code equals} before re-resolving.
 *
 * <p>Vanilla renames the render sink at {@code 1.21.6} ({@code StringRenderOutput}
 * to {@code PreparedTextBuilder}); both bodies resolve one {@code FontSet} per
 * codepoint through {@code Font#getFontSet} inside {@code accept}, so the memo
 * targets whichever sink the version ships. From {@code 1.21.9} the lookup moved to
 * {@code GlyphSource}/{@code FontDescription} and is covered by the
 * {@code MixinFont}/{@code MixinSource} advance cache instead.
 */
//? if <1.21.6 {
@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
//?} else {
/*@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
*///?}
public abstract class MixinStringRenderOutput {

    @Unique private ResourceLocation lomka$lastFontName;
    @Unique private FontSet          lomka$lastFontSet;

    /*
     * Memoizes the FontSet for the active font id: styled text runs resolve one
     * ResourceLocation per codepoint, and vanilla re-walks the font function map
     * on every lookup. Single-slot identity memo hits on the common single-font
     * path and falls through to vanilla on a font switch.
     */
    @Redirect(
        method = "accept",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;getFontSet(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/gui/font/FontSet;",
            require = 0
        )
    )
    private FontSet lomka$memoFontSet(Font font, ResourceLocation name) {
        FontSet cached = this.lomka$lastFontSet;
            if (cached == null || (this.lomka$lastFontName != name && !name.equals(this.lomka$lastFontName))) {
                cached = font.getFontSet(name);
                this.lomka$lastFontName = name;
                this.lomka$lastFontSet  = cached;
        }
        return cached;
    }
}
