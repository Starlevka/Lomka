package lomka.starl.mixins.net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DynamicUniformStorage.class)
public abstract class MixinDynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> {

    @Shadow private T lastUniform;
    @Shadow private MappableRingBuffer ringBuffer;
    @Shadow private int nextBlock;
    @Shadow @Final private int blockSize;

    /**
     * @author Starlev
     * @reason Fast-path reference check for identical uniform objects. 
     *         Bypasses vanilla's method call overhead and directly returns the cached GPU buffer slice.
     */
    @Inject(method = "writeUniform(Lnet/minecraft/client/renderer/DynamicUniformStorage$DynamicUniform;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", at = @At("HEAD"), cancellable = true)
    private void lomka$fastReferenceCheck(DynamicUniformStorage.DynamicUniform t0, CallbackInfoReturnable<GpuBufferSlice> cir) {
        if (t0 != null && t0 == this.lastUniform && this.nextBlock > 0) {
            //? if >=1.21.11 {
            cir.setReturnValue(this.ringBuffer.currentBuffer().slice((long) ((this.nextBlock - 1) * this.blockSize), (long) this.blockSize));
            //?} else {
            /*cir.setReturnValue(this.ringBuffer.currentBuffer().slice((this.nextBlock - 1) * this.blockSize, this.blockSize));*/
            //?}
        }
    }
}