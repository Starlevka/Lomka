package lomka.starl.mixins.net.minecraft.client.renderer.block.model;

//? if >=26.1 {
/*import net.minecraft.client.resources.model.cuboid.FaceBakery;*/
//?} else {
import net.minecraft.client.renderer.block.model.FaceBakery;
//?}
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FaceBakery.class)
public abstract class MixinFaceBakery {

    /**
     * @author Starlev
     * @reason Y->Z->X sequential priority (matching Direction.values() ordinal grouping
     * DOWN,UP,NORTH,SOUTH,WEST,EAST with strict-greater semantics) instead of nested
     * if/else-if, which diverged from vanilla's true dot-product-loop tie-break on an
     * exact 3-way |x|==|y|==|z| tie (loop picks Y first; the old nested structure
     * structurally favored Z). No sqrt/normalize needed: uniform positive scaling
     * doesn't change which axis is largest or its sign.
     */
    @Overwrite
    private static @Nullable Direction findClosestDirection(Vector3f vector3f) {
        float x = vector3f.x;
        float y = vector3f.y;
        float z = vector3f.z;

        float ax = Math.abs(x);
        float ay = Math.abs(y);
        float az = Math.abs(z);

        if (!(ax > 0.0F || ay > 0.0F || az > 0.0F) || !Float.isFinite(ax + ay + az)) {
            return null;
        }

        float best = 0.0F;
        Direction result = null;

        if (ay > best) { best = ay; result = y > 0.0F ? Direction.UP : Direction.DOWN; }
        if (az > best) { best = az; result = z > 0.0F ? Direction.SOUTH : Direction.NORTH; }
        if (ax > best) { result = x > 0.0F ? Direction.EAST : Direction.WEST; }

        return result;
    }

    /**
     * @author Starlev
     * @reason Inline cross-product avoids GeometryUtils.normal's dest-param allocation
     * and flattens two method calls into one. FIX: return type must stay @Nullable and
     * the degenerate case must return null (not Direction.UP) - vanilla's calculateFacing
     * is @Nullable and bakeQuad's caller gates recalculateWinding() on "direction1 != null";
     * defaulting to UP here instead of null made that gate always-true, causing
     * recalculateWinding to run even on degenerate zero-normal quads where vanilla
     * skips it - a path that can throw "Can't find vertex to swap". Also fixes the same
     * Y->Z->X tie-break divergence as the >=26.1 branch above.
     */
    @Overwrite
    private static @Nullable Direction calculateFacing(Vector3fc[] vertices) {
        float x0 = vertices[0].x();
        float y0 = vertices[0].y();
        float z0 = vertices[0].z();
        float x1 = vertices[1].x();
        float y1 = vertices[1].y();
        float z1 = vertices[1].z();
        float x2 = vertices[2].x();
        float y2 = vertices[2].y();
        float z2 = vertices[2].z();

        float e1x = x0 - x1;
        float e1y = y0 - y1;
        float e1z = z0 - z1;
        float e2x = x2 - x1;
        float e2y = y2 - y1;
        float e2z = z2 - z1;

        float nx = e2y * e1z - e2z * e1y;
        float ny = e2z * e1x - e2x * e1z;
        float nz = e2x * e1y - e2y * e1x;

        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);

        if (!(ax > 0.0F || ay > 0.0F || az > 0.0F) || !Float.isFinite(ax + ay + az)) {
            return null;
        }

        float best = 0.0F;
        Direction result = null;

        if (ay > best) { best = ay; result = ny > 0.0F ? Direction.UP : Direction.DOWN; }
        if (az > best) { best = az; result = nz > 0.0F ? Direction.SOUTH : Direction.NORTH; }
        if (ax > best) { result = nx > 0.0F ? Direction.EAST : Direction.WEST; }

        return result;
    }

    /**
     * @author Starlev
     * @reason Eliminates up to 12 Vector3f allocations per call (6 for cross-product
     * setup + one per Direction.values() iteration in vanilla's dot-product loop) with
     * flat float arithmetic. This era's vanilla calculateFacing is already non-nullable
     * with an internal UP fallback, so the return-type/null-gate issue from the
     * >=1.21.11 branch above does not apply here - only the Y->Z->X tie-break fix does.
     */
    /*@Overwrite
    public static Direction calculateFacing(int[] aint) {
        float x0 = Float.intBitsToFloat(aint[0]);
        float y0 = Float.intBitsToFloat(aint[1]);
        float z0 = Float.intBitsToFloat(aint[2]);
        float x1 = Float.intBitsToFloat(aint[8]);
        float y1 = Float.intBitsToFloat(aint[9]);
        float z1 = Float.intBitsToFloat(aint[10]);
        float x2 = Float.intBitsToFloat(aint[16]);
        float y2 = Float.intBitsToFloat(aint[17]);
        float z2 = Float.intBitsToFloat(aint[18]);

        float e1x = x0 - x1;
        float e1y = y0 - y1;
        float e1z = z0 - z1;
        float e2x = x2 - x1;
        float e2y = y2 - y1;
        float e2z = z2 - z1;

        float nx = e2y * e1z - e2z * e1y;
        float ny = e2z * e1x - e2x * e1z;
        float nz = e2x * e1y - e2y * e1x;

        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);

        if (!(ax > 0.0F || ay > 0.0F || az > 0.0F) || !Float.isFinite(ax + ay + az)) {
            return Direction.UP;
        }

        float best = 0.0F;
        Direction result = Direction.UP;

        if (ay > best) { best = ay; result = ny > 0.0F ? Direction.UP : Direction.DOWN; }
        if (az > best) { best = az; result = nz > 0.0F ? Direction.SOUTH : Direction.NORTH; }
        if (ax > best) { result = nx > 0.0F ? Direction.EAST : Direction.WEST; }

        return result;
    }*/

}