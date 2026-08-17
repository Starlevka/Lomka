package lomka.starl.mixins.com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.BufferStorage;
import lomka.starl.utils.GlDriver;
import org.lwjgl.opengl.GLCapabilities;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BufferStorage.class)
public abstract class MixinBufferStorage {

    /**
     * @author Starlev
     * @reason Prefer the mutable buffer path on drivers where the immutable one
     *         (glBufferStorage + persistent mapping) is known to cause GPU-side
     *         lag spikes or outright persistent-map failures.
     *         Everywhere else immutable storage is kept, since it is a net win.
     */
    @Redirect(
        method = "create",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lorg/lwjgl/opengl/GLCapabilities;GL_ARB_buffer_storage:Z"
        )
    )
    private static boolean lomka$useImmutableStorage(GLCapabilities capabilities) {
        return capabilities.GL_ARB_buffer_storage && !lomka$forceMutable();
    }

    /**
     * @author Starlev
     * @reason Vendor detection is cached inside GlDriver; first call happens
     *         here during GlDevice init when a GL context is current.
     */
    @Unique
    private static boolean lomka$forceMutable() {
        return GlDriver.isNVIDIA() 
            || GlDriver.isIntel();
    }
}