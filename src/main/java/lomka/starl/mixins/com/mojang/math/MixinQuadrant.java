package lomka.starl.mixins.com.mojang.math;

import com.mojang.math.Quadrant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Quadrant.class)
public abstract class MixinQuadrant {

    @Shadow @Final public int shift;

    /**
     * @author Starlev
     * @reason Replace modulo with bitwise AND; shift is always in [0,3] so masking by 3 is correct.
     */
    @Overwrite
    public int rotateVertexIndex(int i) {
        return (i + this.shift) & 3;
    }
}
