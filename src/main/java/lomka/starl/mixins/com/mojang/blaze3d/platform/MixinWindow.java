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

package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.Window;
import lomka.starl.utils.GlRenderStateCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Safety net for the GlStateManager viewport/scissor dedup: a resize is the one event
 * where external code (raw LWJGL calls from other mods, driver tooling, launcher
 * overlays) most plausibly touches GL state behind our back. Vanilla itself routes its
 * viewport updates through GlStateManager, so this reset is normally redundant - it
 * just guarantees the cache starts re-learning from a clean slate whenever the window
 * or framebuffer geometry changes.
 */
@Mixin(Window.class)
public class MixinWindow {

    /**
     * Resets the GL state cache so viewport/scissor dedup re-learns from a clean slate after a resize.
     */
    @Inject(
            method = "onResize",
            at = @At("TAIL")
    )
    private void lomka$resetGlCacheOnResize(long handle, int width, int height, CallbackInfo ci) {
        GlRenderStateCache.reset();
    }

    /**
     * Resets the GL state cache after a framebuffer resize.
     */
    @Inject(
            method = "onFramebufferResize",
            at = @At("TAIL")
    )
    private void lomka$resetGlCacheOnFramebufferResize(long handle, int width, int height, CallbackInfo ci) {
        GlRenderStateCache.reset();
    }
}
