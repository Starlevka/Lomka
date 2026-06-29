package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GpuBuffer.class)
public abstract class MixinGpuBuffer {

    //? if >=1.21.11 {
    @Shadow public abstract long size();
    //?} else {
    /*@Shadow public abstract int size();*/
    //?}

    @Unique private GpuBufferSlice lomka$fullSlice;

    /**
     * @author Starlev
     * @reason Lazy-cache the full-buffer GpuBufferSlice to avoid allocating a new slice object on every call.
     */
    @Overwrite
    public GpuBufferSlice slice() {
        if (this.lomka$fullSlice == null) {
            //? if >=1.21.11 {
            this.lomka$fullSlice = new GpuBufferSlice((GpuBuffer) (Object) this, 0L, this.size());
            //?} else {
            /*this.lomka$fullSlice = new GpuBufferSlice((GpuBuffer) (Object) this, 0, (int) this.size());*/
            //?}
        }
        return this.lomka$fullSlice;
    }
}