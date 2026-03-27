package set.starl.injection.mixins.gl;

import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import set.starl.LomkaCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GlBackend.class)
public class RenderSystemGlDebugOffMixin {
	@Unique
    private static final boolean LOMKA$DISABLE_GL_DEBUG = LomkaCore.CONFIG.render.openglDebugDisable;

	@ModifyArgs(
		method = "createDevice(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;)Lcom/mojang/blaze3d/systems/GpuDevice;",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/opengl/GlDevice;<init>(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;)V"
		)
	)
	private static void lomka$disableGlDebugWhenEnabled(final Args args) {
		if (!LOMKA$DISABLE_GL_DEBUG) {
			return;
		}
		args.set(2, new GpuDebugOptions(0, false, false));
	}
}