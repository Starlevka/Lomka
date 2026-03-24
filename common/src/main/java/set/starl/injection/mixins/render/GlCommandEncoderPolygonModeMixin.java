package set.starl.injection.mixins.render;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderPolygonModeMixin {
    @Unique
    private static int lomka$lastPolygonMode = -1;

    /**
     * Cache glPolygonMode calls — skip if mode hasn't changed.
     * In 99.9% of cases the mode is GL_FILL (6914), so this saves
     * one GL call per pipeline switch.
     */
    @Redirect(
        method = "applyPipelineState",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_polygonMode(II)V"
        )
    )
    private static void lomka$cachePolygonMode(int face, int mode) {
        if (lomka$lastPolygonMode == mode) {
            return;
        }
        lomka$lastPolygonMode = mode;
        GlStateManager._polygonMode(face, mode);
    }
}