package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import java.nio.ByteOrder;

@Mixin(value = BufferBuilder.class, priority = 1100)
public abstract class MixinBufferBuilder implements VertexConsumer {

    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    //? if >=26.2 {
    /*@Shadow
    private long beginElement(int semanticID) {
        return 0;
    }
    *///?} else {
    @Shadow
    private long beginElement(VertexFormatElement element) {
        return 0;
    }
    //?}

    /**
     * @author Starlev
     * @reason Replace 7 bitwise operations with 2 native hardware instructions (BSWAP and ROR).
     * Converts ARGB to ABGR in 2 CPU cycles instead of standard masking.
     */
    @Overwrite
    private static void putRgba(long pointer, int argb) {
        int abgr = Integer.rotateRight(Integer.reverseBytes(argb), 8);

        MemoryUtil.memPutInt(pointer, IS_LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr));
    }

    /**
     * @author Starlev
     * @reason Batch 4 independent byte-writes into a single 32-bit integer memory write.
     * Prevents CPU pipeline stalls and heavily reduces memory access overhead during chunk generation.
     */
    @Overwrite
    public VertexConsumer setColor(int r, int g, int b, int a) {
        //? if >=26.2 {
        /*long pointer = this.beginElement(1);
        *///?} else {
        long pointer = this.beginElement(VertexFormatElement.COLOR);
        //?}

        if (pointer != -1L) {
            int packedColor;
            
            if (IS_LITTLE_ENDIAN) {
                packedColor = (r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | ((a & 0xFF) << 24);
            } else {
                packedColor = (a & 0xFF) | ((b & 0xFF) << 8) | ((g & 0xFF) << 16) | ((r & 0xFF) << 24);
            }

            MemoryUtil.memPutInt(pointer, packedColor);
        }

        return this;
    }
}