package lomka.starl.mixins.com.mojang.math;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import lomka.starl.utils.QuadrantRotationCache;
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
    @Overwrite(remap = false)
    public int rotateVertexIndex(int i) {
        return (i + this.shift) & 3;
    }

    /**
     * @author Starlev
     * @reason Cache pre-computed OctahedralGroup compositions in a flat array,
     * eliminating per-call quadrants[x].rotationY.compose(quadrants[y].rotationX).
     */
    @Overwrite(remap = false)
    public static OctahedralGroup fromXYAngles(Quadrant quadrant, Quadrant quadrant1) {
        int index = (quadrant.ordinal() << 2) | quadrant1.ordinal();
        return QuadrantRotationCache.getXY(index);
    }

    /**
     * @author Starlev
     * @reason Cache pre-computed OctahedralGroup compositions in a flat array,
     * eliminating per-call matrix composition math.
     */
    @Overwrite(remap = false)
    public static OctahedralGroup fromXYZAngles(Quadrant quadrant, Quadrant quadrant1, Quadrant quadrant2) {
        int index = (quadrant.ordinal() << 4) | (quadrant1.ordinal() << 2) | quadrant2.ordinal();
        return QuadrantRotationCache.getXYZ(index);
    }
}
