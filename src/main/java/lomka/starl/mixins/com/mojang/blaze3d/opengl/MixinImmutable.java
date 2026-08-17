package lomka.starl.mixins.com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.BufferStorage;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "com.mojang.blaze3d.opengl.BufferStorage$Immutable")
public abstract class MixinImmutable {

    @Unique private static final Runnable lomka$NO_OP = () -> {};

    /**
     * @author Starlev
     * @reason Zero-allocation optimization for the persistent-mapped hot path.
     *         Vanilla allocates a lambda for the close action on EVERY mapBuffer call,
     *         even when flush is not needed ((k & 2) == 0). mapBuffer is called
     *         constantly during rendering (staging buffers, dynamic geometry).
     *         Each lambda = new object in heap = GC pressure.
     *         
     *         Pass static NO_OP when flush is not needed, avoiding lambda allocation.
     *         AccessWidener grants access to protected GlBuffer fields and GlMappedView constructor.
     */
    @Overwrite
    public GlBuffer.GlMappedView mapBuffer(DirectStateAccess directstateaccess, GlBuffer glbuffer, long i, long j, int k) {
        if (glbuffer.persistentBuffer == null) {
            throw new IllegalStateException("Somehow trying to map an unmappable buffer");
        }

        if (i >= 0L && j >= 0L && i <= Integer.MAX_VALUE && j <= Integer.MAX_VALUE) {
            Runnable closeAction;
            if ((k & 2) != 0) {
                //? if >=1.21.11 {
                closeAction = () -> directstateaccess.flushMappedBufferRange(glbuffer.handle, i, j, glbuffer.usage());
                /*? } else { */
                // closeAction = () -> directstateaccess.flushMappedBufferRange(glbuffer.handle, (int) i, (int) j);
                //? }
            } else {
                closeAction = lomka$NO_OP;
            }

            return new GlBuffer.GlMappedView(
                closeAction,
                glbuffer,
                MemoryUtil.memSlice(glbuffer.persistentBuffer, (int) i, (int) j)
            );
        } else {
            throw new IllegalArgumentException("Offset or length must be positive integer values");
        }
    }
}