package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Std140SizeCalculator.class)
public abstract class MixinStd140SizeCalculator {

    @Shadow private int size;

    /**
     * @author Starlev
     * @reason Replaces slow division Mth.roundToward with single-cycle bitwise power-of-two alignment.
     */
    @Overwrite
    public Std140SizeCalculator align(int align) {
        this.size = (this.size + align - 1) & -align;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(4) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putFloat() {
        this.size = (this.size + 7) & -4;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(4) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putInt() {
        this.size = (this.size + 7) & -4;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(8) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putVec2() {
        this.size = (this.size + 15) & -8;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(8) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putIVec2() {
        this.size = (this.size + 15) & -8;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(16) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putVec3() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(16) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putIVec3() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(16) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putVec4() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(16) + size increment into a single atomic calculation.
     */
    @Overwrite
    public Std140SizeCalculator putIVec4() {
        this.size = (this.size + 31) & -16;
        return (Std140SizeCalculator) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Inlines bitwise align(16) + matrix size increment into a single calculation.
     */
    @Overwrite
    public Std140SizeCalculator putMat4f() {
        this.size = ((this.size + 15) & -16) + 64;
        return (Std140SizeCalculator) (Object) this;
    }
}