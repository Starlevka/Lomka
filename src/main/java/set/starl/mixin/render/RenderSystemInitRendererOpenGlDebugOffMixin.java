package set.starl.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import set.starl.Lomka;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(RenderSystem.class)
public class RenderSystemInitRendererOpenGlDebugOffMixin {
	@Unique
	private static final boolean LOMKA$DISABLE_GL_DEBUG = Lomka.JAVA_22_25_OPTIMIZATIONS
		&& !"false".equalsIgnoreCase(System.getProperty("lomka.opengl.debug.disable", "true"));

	@ModifyArgs(
		method = "initRenderer(JIZLcom/mojang/blaze3d/shaders/ShaderSource;Z)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/opengl/GlDevice;<init>(JIZLcom/mojang/blaze3d/shaders/ShaderSource;Z)V"
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

