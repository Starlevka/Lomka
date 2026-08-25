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

package lomka.starl.mixins.com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces vanilla's blit-shader present with a single hardware framebuffer copy.
 * Only fires for the {@code disableBlend == true} path - the hot one; the frame-end
 * present from {@code Minecraft} always passes {@code true}. The blended variant keeps
 * the vanilla shader pipeline because {@code glBlitFramebuffer} cannot blend.
 *
 * <p>Vanilla (&le;1.21.1) presents the main render target by drawing a fullscreen quad
 * through {@code gameRenderer.blitShader}: two ortho {@code Matrix4f} allocations per
 * frame (1.20.1), Tesselator &rarr; BufferBuilder &rarr; MeshData allocations, a mid-frame
 * shader program bind/unbind, and half a dozen state toggles that the driver then has to
 * verify. All of that collapses into {@code glBlitFramebuffer}: no rasterization, no
 * program switch, no allocations. Per GL spec a blit bypasses fragment processing, so the
 * surrounding colorMask/depth/blend toggles are dead weight around it and are skipped too.
 *
 * <p>Fidelity notes:
 * <ul>
 *   <li>Filter is NEAREST - matches the render target's MAG/MIN_FILTER and Mojang's own
 *       choice when they shipped this exact technique natively in 1.21.2+
 *       ({@code RenderTarget.blitToScreen} &rarr; {@code _glBlitFrameBuffer}); identical
 *       pixels in the common 1:1 case.</li>
 *   <li>Source rect mirrors each version's shader UVs: 1.20.1 samples the
 *       {@code viewWidth}&times;{@code viewHeight} sub-rect, 1.21.x samples the full
 *       texture - both reproduced here.</li>
 *   <li>Alpha is copied instead of masked off like vanilla's quad path does; the main
 *       target clears opaque so this is invisible outside exotic translucent-window
 *       compositors.</li>
 * </ul>
 *
 * <p>Origin: hardware-blit presentation popularized by VulkanMod's renderer and ported to
 * the GL pipeline by Tritium (craftamethyst); direction independently confirmed by Mojang
 * adopting the same replacement in 1.21.2+. Implemented as a cancellable HEAD inject at the
 * {@link RenderTarget} level per project compatibility rules - it composes with upstream
 * @Redirects instead of competing for instructions, unlike an exclusive encoder redirect.
 *
 * <p>Gated {@code <1.21.4}: from 1.21.2 vanilla performs this optimization natively and
 * {@code _blitToScreen} no longer exists to inject into.
 */
@Mixin(RenderTarget.class)
public abstract class MixinRenderTarget {

    /**
     * One driver blit call replaces the whole quad + blit-shader present path:
     * removes per-frame Matrix4f/Tesselator/MeshData allocations and the mid-frame
     * shader program switch on the hottest single draw of the frame.
     */
    @Inject(
            method = "_blitToScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lomka$fastBlit(int width, int height, boolean disableBlend, CallbackInfo ci) {
        if (!disableBlend) {
            return;
        }

        RenderTarget self = (RenderTarget) (Object) this;

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, self.frameBufferId);
        //? if <1.21 {
        GL30.glBlitFramebuffer(0, 0, self.viewWidth, self.viewHeight, 0, 0, width, height, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        //?} else {
        /*GL30.glBlitFramebuffer(0, 0, self.width, self.height, 0, 0, width, height, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        *///?}
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);

        ci.cancel();
    }
}
