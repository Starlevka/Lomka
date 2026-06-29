package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.Std140Builder;
import org.joml.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(Std140Builder.class)
public class MixinStd140Builder {
    
    @Shadow @Final private ByteBuffer buffer;
    @Shadow @Final private int start;

    /**
     * @author Starlev
     * @reason Replaced Mth.roundToward with bitwise alignment. Also avoids redundant 
     * ByteBuffer#position calls if the buffer is already correctly aligned.
     */
    @Overwrite
    public Std140Builder align(int align) {
        int currentPos = this.buffer.position();
        int offset = currentPos - this.start;
        int alignedOffset = (offset + (align - 1)) & -align;
        
        if (alignedOffset != offset) {
            this.buffer.position(this.start + alignedOffset);
        }
        return (Std140Builder) (Object) this;
    }
}