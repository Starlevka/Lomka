package set.starl.injection.mixins.render;

import com.mojang.blaze3d.systems.RenderSystem;
import set.starl.LomkaCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(RenderSystem.class)
public class RenderSystemGlDebugOffMixin {
	@Unique
    private static final boolean LOMKA$DISABLE_GL_DEBUG = LomkaCore.CONFIG.render.openglDebugDisable;

    @ModifyArgs(
        method = "initRenderer(JIZLjava/util/function/BiFunction;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlDevice;<init>(JIZLjava/util/function/BiFunction;Z)V"
        )
    )
	
	private static void lomka$disableGlDebugWhenEnabled(final Args args) {
		if (!LOMKA$DISABLE_GL_DEBUG) {
			return;
		}
		args.set(1, Integer.valueOf(0));
		args.set(2, Boolean.FALSE);
		args.set(4, Boolean.FALSE);
	}
}