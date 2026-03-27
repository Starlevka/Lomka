package set.starl.injection.mixins.gl;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cache glScissor — skip if scissor box hasn't changed.
 * GlStateManager._scissorBox has no caching unlike _bindTexture,
 * _depthMask, _colorMask etc. Saves 1 GL call per scissor box set
 * when the region stays the same (common during consecutive clears
 * or same-size render passes).
 *
 * Note: passes through to GlStateManager._scissorBox to maintain
 * internal state consistency, rather than calling GL11 directly.
 */
@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderScissorCacheMixin {

    @Unique
    private static int lomka$lastScissorX = Integer.MIN_VALUE;

    @Unique
    private static int lomka$lastScissorY = Integer.MIN_VALUE;

    @Unique
    private static int lomka$lastScissorW = Integer.MIN_VALUE;

    @Unique
    private static int lomka$lastScissorH = Integer.MIN_VALUE;

    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_scissorBox(IIII)V"
        )
    )
    private static void lomka$cacheScissorBox(int x, int y, int width, int height) {
        if (lomka$lastScissorX == x && lomka$lastScissorY == y
                && lomka$lastScissorW == width && lomka$lastScissorH == height) {
            return;
        }
        lomka$lastScissorX = x;
        lomka$lastScissorY = y;
        lomka$lastScissorW = width;
        lomka$lastScissorH = height;
        GlStateManager._scissorBox(x, y, width, height);
    }
}