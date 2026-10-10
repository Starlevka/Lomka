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

package lomka.starl.mixins.net.minecraft.client.renderer.texture;

import lomka.starl.duck.ISpriteContents;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TextureAtlas.class)
public abstract class MixinTextureAtlasMip {

    @Shadow private Map<Identifier, TextureAtlasSprite> texturesByName;

    /**
     * Releases the generated mip levels of every non-animated sprite, once the atlas has finished
     * uploading. RETURN of {@code upload} is the first point that is after the upload on every
     * version this patch is enabled on, and it is a point other mods already inject at - Sodium
     * resets its sprite-finder cache there and Iris re-registers the texture with its tracker, so
     * coexistence is already proven rather than assumed.
     *
     * <p>{@code SpriteContents#uploadFirstFrame} looks like the tighter hook and is not: from 26.1
     * {@code TextureAtlas#uploadInitialContents} calls it once per mip level inside a
     * {@code for (level = 0; level <= maxMipLevel; ++level)} loop and it indexes
     * {@code byMipLevel[level]}, so truncating from its RETURN would throw on the second iteration.
     */
    @Inject(method = "upload", at = @At("RETURN"))
    private void lomka$releaseUnusedMipLevels(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        for (TextureAtlasSprite sprite : this.texturesByName.values()) {
            SpriteContents contents = sprite.contents();
            if (contents != null) {
                ((ISpriteContents) (Object) contents).lomka$releaseUnusedMipLevels();
            }
        }
    }
}