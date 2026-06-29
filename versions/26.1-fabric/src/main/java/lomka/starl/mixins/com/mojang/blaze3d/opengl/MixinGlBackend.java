package lomka.starl.mixins.com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GlBackend.class)
public class MixinGlBackend {
    @ModifyVariable(
        method = "createDevice(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;)Lcom/mojang/blaze3d/systems/GpuDevice;",
        at = @At("HEAD"),
        argsOnly = true
    )
    private static GpuDebugOptions lomka$modifyDebugOptions(GpuDebugOptions debugOptions) {
        return new GpuDebugOptions(0, false, false);
    }
}
