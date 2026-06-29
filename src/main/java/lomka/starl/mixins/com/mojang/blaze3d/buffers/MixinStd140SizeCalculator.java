package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Std140SizeCalculator.class)
public class MixinStd140SizeCalculator {
    
    @Shadow private int size;

    /**
     * @author Starlev
     * @reason Replaced Mth.roundToward with fast bitwise operations for power-of-two alignment.
     * Division and multiplication are unnecessary here since OpenGL std140 alignment rules 
     * strictly use powers of two (4, 8, 16).
     */
    @Overwrite
    public Std140SizeCalculator align(int align) {
        this.size = (this.size + (align - 1)) & -align;
        return (Std140SizeCalculator) (Object) this;
    }
}