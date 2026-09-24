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

package lomka.starl.mixins.com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.backend.opengl.FrameBufferCache;
import lomka.starl.duck.IFrameBufferCacheKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FrameBufferCache.CacheKey.class)
public abstract class MixinFrameBufferCacheKey implements IFrameBufferCacheKey {

    @Shadow @Mutable @Final private int[] data;
    @Shadow @Mutable @Final private int   hash;

    /**
     * Repoints this key's payload. Callers must supply the exact payload layout
     * produced by the vanilla constructor (interleaved glId/mip pairs) and a
     * hash identical to Arrays.hashCode over the same prefix, so probes match
     * keys created by vanilla code paths.
     */
    @Override
    @Unique
    public void lomka$reset(final int[] data, final int hash) {
        this.data = data;
        this.hash = hash;
    }
}
