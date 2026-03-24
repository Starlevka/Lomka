package set.starl.injection.mixins.render;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderViewportCacheMixin {

    @Unique
    private static int lomka$lastVpX = Integer.MIN_VALUE;

    @Unique
    private static int lomka$lastVpY = Integer.MIN_VALUE;

    @Unique
    private static int lomka$lastVpW = Integer.MIN_VALUE;

    @Unique
    private static int lomka$lastVpH = Integer.MIN_VALUE;

    /**
     * Cache glViewport — skip if viewport hasn't changed.
     * GlStateManager._viewport has no caching unlike _bindTexture,
     * _depthMask, _colorMask etc. Saves 1 GL call per frame when
     * viewport stays the same (most common case).
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_viewport(IIII)V"
        )
    )
    private static void lomka$cacheViewport(int x, int y, int width, int height) {
        if (lomka$lastVpX == x && lomka$lastVpY == y && lomka$lastVpW == width && lomka$lastVpH == height) {
            return;
        }
        lomka$lastVpX = x;
        lomka$lastVpY = y;
        lomka$lastVpW = width;
        lomka$lastVpH = height;
        GL11.glViewport(x, y, width, height);
    }
}