package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Optimizes std140 uniform size calculation by replacing division-based Mth.roundToward
 * with single-cycle power-of-two bitwise alignment masks and inlining size increments
 * directly into atomic arithmetic expressions.
 */
@Mixin(Std140SizeCalculator.class)
public abstract class MixinStd140SizeCalculator {

    @Shadow private int size;

    @Overwrite
    public Std140SizeCalculator align(int align) {
        this.size = (this.size + align - 1) & -align;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putFloat() {
        this.size = (this.size + 7) & -4;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putInt() {
        this.size = (this.size + 7) & -4;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putVec2() {
        this.size = (this.size + 15) & -8;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putIVec2() {
        this.size = (this.size + 15) & -8;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putVec3() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putIVec3() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putVec4() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putIVec4() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    @Overwrite
    public Std140SizeCalculator putMat4f() {
        this.size = ((this.size + 15) & -16) + 64;
        return (Std140SizeCalculator) (Object) this;
    }
}