package lomka.starl.mixins.com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    //? if >=1.21.11 {
    /**
     * Forces debug verbosity to 0 to skip OpenGL debug message overhead during init.
     */
    @ModifyVariable(
        method = "initRenderer(JIZLcom/mojang/blaze3d/shaders/ShaderSource;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static int lomka$modifyDebugVerbosity(int debugVerbosity) {
        return 0;
    }

    /**
     * Disables debug synchronization to avoid GPU pipeline stalls during render system init.
     */
    @ModifyVariable(
        method = "initRenderer(JIZLcom/mojang/blaze3d/shaders/ShaderSource;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static boolean lomka$modifyDebugSync(boolean debugSync) {
        return false;
    }

    /**
     * Disables OpenGL state capture to eliminate unnecessary glGet* calls during init.
     */
    @ModifyVariable(
        method = "initRenderer(JIZLcom/mojang/blaze3d/shaders/ShaderSource;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private static boolean lomka$modifyCaptureGl(boolean captureGL) {
        return false;
    }
    //?} else {
    /*@ModifyVariable(
        method = "initRenderer(JIZLjava/util/function/BiFunction;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static int lomka$modifyDebugVerbosity(int debugVerbosity) {
        return 0;
    }

    @ModifyVariable(
        method = "initRenderer(JIZLjava/util/function/BiFunction;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static boolean lomka$modifyDebugSync(boolean debugSync) {
        return false;
    }

    @ModifyVariable(
        method = "initRenderer(JIZLjava/util/function/BiFunction;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private static boolean lomka$modifyCaptureGl(boolean captureGL) {
        return false;
    }*/
    //?}
}
