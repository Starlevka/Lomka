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

package lomka.starl.mixins.com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.GlStateManager;
import lomka.starl.utils.cache.GlStateCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Deduplicates redundant context-global GL state writes ({@code _viewport},
 * {@code _scissorBox}, {@code _polygonMode}). Vanilla re-issues these calls on every
 * render pass setup, render-target bind and pipeline switch even when the values equal
 * the ones already on the GPU - post chains and stacked offscreen passes hit this dozens
 * of times per frame. All three states are context-global (binding another framebuffer
 * never resets them) and are written exclusively through GlStateManager by vanilla, so
 * skipping an equal-value driver call is semantically transparent; MIN_VALUE sentinels
 * guarantee the first call of a session always executes. Viewport/scissor are the
 * measurable win; polygon mode fires rarely and exists mostly for consistency.
 *
 * <p>The cache lives at the GlStateManager level instead of redirecting GlCommandEncoder
 * call sites: @Redirect is exclusive - one redirect per instruction - and shader mods
 * (Iris) redirect those same encoder call sites, which crashes whichever mixin applies
 * second. A cancellable HEAD inject here composes with any upstream redirects and covers
 * every current and future caller. State is stored in {@link GlStateCache} so
 * MixinWindow can invalidate it on window/framebuffer resize - external mods using raw
 * LWJGL calls would otherwise desynchronize the cache until the next value change.
 *
 * Method = "_viewport" is also used by Sodium. I added and saved it just as a must-have
 * with dedup system.
 * 
 * <p>Backport (&lt;1.21.2): vanilla deduplicates read/write framebuffer binds through its
 * internal READ/DRAW_FRAMEBUFFER mirrors only from 1.21.2 up; on 1.20.1 and 1.21.x
 * {@code _glBindFramebuffer} is an unconditional passthrough while post chains, render-target
 * pipelines and GUI passes re-issue identical binds every frame. The gated handlers below
 * replicate Mojang's own later implementation (including resetting mirrors to 0 when a deleted
 * id matches, as vanilla does from 26.x onward), so a bind is skipped iff vanilla itself would
 * skip it on 1.21.2+.
 */
@Mixin(GlStateManager.class)
public class MixinGlStateManager {

    /**
     * Caches the incoming viewport rect and cancels the native call when unchanged.
     */
    @Inject(
            method = "_viewport",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void lomka$cacheViewport(int x, int y, int width, int height, CallbackInfo ci) {
        if (x != GlStateCache.get(GlStateCache.VIEWPORT_X)
                || y      != GlStateCache.get(GlStateCache.VIEWPORT_Y)
                || width  != GlStateCache.get(GlStateCache.VIEWPORT_W)
                || height != GlStateCache.get(GlStateCache.VIEWPORT_H)) {
            GlStateCache.set(GlStateCache.VIEWPORT_X, x);
            GlStateCache.set(GlStateCache.VIEWPORT_Y, y);
            GlStateCache.set(GlStateCache.VIEWPORT_W, width);
            GlStateCache.set(GlStateCache.VIEWPORT_H, height);
        } else {
            ci.cancel();
        }
    }

    /**
     * Caches the incoming scissor rect and cancels the native call when unchanged.
     */
    @Inject(
            method = "_scissorBox",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void lomka$cacheScissorBox(int x, int y, int width, int height, CallbackInfo ci) {
        if (x != GlStateCache.get(GlStateCache.SCISSOR_X)
                || y      != GlStateCache.get(GlStateCache.SCISSOR_Y)
                || width  != GlStateCache.get(GlStateCache.SCISSOR_W)
                || height != GlStateCache.get(GlStateCache.SCISSOR_H)) {
            GlStateCache.set(GlStateCache.SCISSOR_X, x);
            GlStateCache.set(GlStateCache.SCISSOR_Y, y);
            GlStateCache.set(GlStateCache.SCISSOR_W, width);
            GlStateCache.set(GlStateCache.SCISSOR_H, height);
        } else {
            ci.cancel();
        }
    }

    /**
     * Caches the polygon mode and cancels the native call when unchanged.
     */
    @Inject(
            method = "_polygonMode",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void lomka$cachePolygonMode(int face, int mode, CallbackInfo ci) {
        if (face != GlStateCache.get(GlStateCache.POLYGON_FACE)
         || mode != GlStateCache.get(GlStateCache.POLYGON_MODE)) {
            GlStateCache.set(GlStateCache.POLYGON_FACE, face);
            GlStateCache.set(GlStateCache.POLYGON_MODE, mode);
        } else {
            ci.cancel();
        }
    }

    //? if <1.21.2 {
    /**
     * Mirrors Mojang's 1.21.2+ framebuffer-bind dedup onto older versions; cancels unchanged binds.
     */
    @Inject(
            method = "_glBindFramebuffer",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void lomka$cacheBindFramebuffer(int target, int framebuffer, CallbackInfo ci) {
        boolean known = false;
        boolean changed = false;

        if (target == 36008 || target == 36160) {
            known = true;
            if (GlStateCache.get(GlStateCache.FBO_READ) != framebuffer) {
                GlStateCache.set(GlStateCache.FBO_READ, framebuffer);
                changed = true;
            }
        }

        if (target == 36009 || target == 36160) {
            known = true;
            if (GlStateCache.get(GlStateCache.FBO_WRITE) != framebuffer) {
                GlStateCache.set(GlStateCache.FBO_WRITE, framebuffer);
                changed = true;
            }
        }

        if (known && !changed) {
            ci.cancel();
        }
    }

    /**
     * Resets the cached FBO mirrors when a framebuffer is deleted.
     */
    @Inject(
            method = "_glDeleteFramebuffers",
            at = @At("TAIL")
    )
    private static void lomka$clearFboCache(int framebuffer, CallbackInfo ci) {
        if (GlStateCache.get(GlStateCache.FBO_READ) == framebuffer) {
            GlStateCache.set(GlStateCache.FBO_READ, 0);
        }

        if (GlStateCache.get(GlStateCache.FBO_WRITE) == framebuffer) {
            GlStateCache.set(GlStateCache.FBO_WRITE, 0);
        }
    }
    //?}
}
