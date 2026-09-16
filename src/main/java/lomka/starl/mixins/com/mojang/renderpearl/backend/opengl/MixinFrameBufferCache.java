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

import com.mojang.renderpearl.backend.opengl.DirectStateAccess;
import com.mojang.renderpearl.backend.opengl.FrameBufferAttachment;
import com.mojang.renderpearl.backend.opengl.FrameBufferCache;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import lomka.starl.duck.IFrameBufferCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FrameBufferCache.class)
public abstract class MixinFrameBufferCache {

    @Shadow @Final private Object2IntMap<FrameBufferCache.CacheKey> cache;

    @Shadow
    protected abstract int createFbo(
            final FrameBufferCache.CacheKey key,
            final DirectStateAccess dsa,
            final List<FrameBufferAttachment> colorAttachments,
            final FrameBufferAttachment depthAttachment,
            final int mipOffset
    );

    /*
     * Render-thread confined probe key: vanilla GL paths (GlCommandEncoder, texture
     * teardown) all execute on the single render thread, so one instance shared by
     * the cache is safe. It carries an empty attachment list - CacheKey#equals only
     * inspects the flat payload and the precomputed hash, never the list - and is
     * repointed in place before every lookup. Only real keys, freshly constructed on
     * a miss, ever reach createFbo's addAssociatedFbo bookkeeping or destroyFbo.
     */
    @Unique private FrameBufferCache.CacheKey lomka$probeKey = new FrameBufferCache.CacheKey(List.of(), null, 0);
    @Unique private int[] lomka$probeData                    = new int[0];

    /**
     * @author Starlev
     * @reason Vanilla unconditionally allocates a CacheKey (int[] payload, copied
     *         attachment list, Arrays.hashCode) on every lookup, yet the cache is
     *         hit-bound: the set of distinct FBO topologies is tiny and stable per
     *         frame graph, so nearly every call pays full construction cost to
     *         discard the result. This overwrite fills a reused exact-length int[]
     *         and repoints a single probe key, resolving steady-state hits with one
     *         hash-map probe and zero allocation. Object2IntOpenHashMap#get returns
     *         0 on a miss and glCreateFramebuffers never yields 0, so 0 is a safe
     *         miss sentinel; misses fall through to the exact vanilla path (real
     *         key + computeIfAbsent), preserving FBO creation, attachment
     *         bookkeeping and eviction semantics byte for byte.
     */
    @Overwrite
    public int getFbo(
            final DirectStateAccess dsa,
            final List<FrameBufferAttachment> colorTextures,
            final FrameBufferAttachment depthTexture,
            final int mipOffset
    ) {
        int count  = colorTextures.size();
        int needed = (count + (depthTexture != null ? 1 : 0)) * 2;
        int[] data = this.lomka$probeData;

        if (data.length != needed) {
            data = this.lomka$probeData = new int[needed];
        }

        int hash   = 1;
        int cursor = 0;

        for (int i = 0; i < count; ++i) {
            FrameBufferAttachment attachment = colorTextures.get(i);
            int glId = attachment != null ? attachment.glId() : 0;
            int mip  = attachment != null ? attachment.fboMipLevel() + mipOffset : 0;

            data[cursor]     = glId;
            data[cursor + 1] = mip;
                 cursor += 2;

            hash = hash * 31 + glId;
            hash = hash * 31 + mip;
        }

        if (depthTexture != null) {
            int glId = depthTexture.glId();
            int mip  = depthTexture.fboMipLevel() + mipOffset;

            data[cursor]     = glId;
            data[cursor + 1] = mip;

            hash = hash * 31 + glId;
            hash = hash * 31 + mip;
        }

        ((IFrameBufferCache) (Object) this.lomka$probeKey).lomka$reset(data, hash);
        int fbo = this.cache.getInt(this.lomka$probeKey);

        if (fbo != 0) {
            return fbo;
        }

        FrameBufferCache.CacheKey realKey = new FrameBufferCache.CacheKey(colorTextures, depthTexture, mipOffset);
        return this.cache.computeIfAbsent(realKey, object -> this.createFbo(realKey, dsa, colorTextures, depthTexture, mipOffset));
    }
}
