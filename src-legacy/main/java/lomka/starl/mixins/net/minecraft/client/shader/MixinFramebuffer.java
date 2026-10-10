//$ if >=1.8

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

package lomka.starl.mixins.net.minecraft.client.shader;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

/**
 * Port of the modern {@code MixinRenderTarget} to 1.12.2, where the render target class is
 * {@link Framebuffer} and the present is {@code framebufferRenderExt(int, int, boolean)}, reached
 * once per frame from {@code Minecraft#runTick} through the two-argument
 * {@code framebufferRender(int, int)}.
 *
 * <p>Vanilla draws the main render target to the window as a fullscreen quad through the blit
 * shader: two ortho matrices, a Tessellator -&gt; BufferBuilder draw, a mid-frame program bind and
 * half a dozen state toggles, every frame. {@code glBlitFramebuffer} replaces all of it with one
 * driver copy - no rasterisation, no program switch, no allocations - which is the substitution
 * Mojang later shipped natively once the main target stopped being the default framebuffer.
 *
 * <p>Fidelity notes, all transcribed from this version's own body rather than from the modern
 * one:
 * <ul>
 *   <li>The source rect is the {@code framebufferWidth}x{@code framebufferHeight} sub-rect, not
 *       the whole texture. 1.12.2 scales the quad's UVs by
 *       {@code framebufferWidth / framebufferTextureWidth}, so that sub-rect is exactly what the
 *       shader samples; the two are equal in practice but not by construction.</li>
 *   <li>Filter is NEAREST, matching what {@code createFramebuffer} installs via
 *       {@code setFramebufferFilter(9728)}.</li>
 *   <li>Alpha is copied rather than masked off the way the quad path's
 *       {@code colorMask(true, true, true, false)} leaves it, which is the same trade the native
 *       blit makes.</li>
 *   <li>Only the {@code disableBlend == true} path is taken. It is the hot one - the frame-end
 *       present always passes true - and {@code glBlitFramebuffer} cannot blend, so the flag-false
 *       variant keeps the shader pipeline.</li>
 * </ul>
 *
 * <p>It stays a cancellable HEAD inject rather than an overwrite, for the same reason as the
 * modern patch: {@code framebufferRenderExt} is the seam every shader pack and rendering mod on
 * this version reaches for, and cancelling leaves the vanilla body in the bytecode for their
 * injectors to anchor to.
 */
@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {

	@Inject(method = "framebufferRenderExt", at = @At("HEAD"), cancellable = true)
	private void lomka$fastBlit(int width, int height, boolean disableBlend, CallbackInfo ci) {
		if (!disableBlend) {
			return;
		}

		if (!OpenGlHelper.isFramebufferEnabled()) {
			return;
		}

		// OpenGlHelper picks its FBO entry point once, in its static initialiser: GL 3.0 first,
		// then ARB_framebuffer_object, then EXT_framebuffer_object. Only the first two export
		// glBlitFramebuffer and accept GL_READ_FRAMEBUFFER as a bind target, so on the EXT path
		// the bind below would raise GL_INVALID_ENUM and leave the source on the default
		// framebuffer - turning the blit into an overlapping copy onto itself. Fall back instead.
		ContextCapabilities caps = GLContext.getCapabilities();
		if (!caps.OpenGL30 && !caps.GL_ARB_framebuffer_object) {
			return;
		}

		Framebuffer self = (Framebuffer) (Object) this;

		OpenGlHelper.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, self.framebufferObject);
				GL30.glBlitFramebuffer(0, 0, self.framebufferWidth, self.framebufferHeight, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
		OpenGlHelper.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);

		ci.cancel();
	}
}
//$ end
