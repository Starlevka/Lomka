package lomka.starl.mixins.net.minecraft.client.renderer.block.model;

//? if >=26.1 {
/*import net.minecraft.client.resources.model.cuboid.FaceBakery;*/
//?} else {
import net.minecraft.client.renderer.block.model.FaceBakery;
//?}
import net.minecraft.core.Direction;
//? if <26.1 {
import org.joml.Vector3fc;
//?} else {
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FaceBakery.class)
public abstract class MixinFaceBakery {

    //? if >=26.1 {
    /*@Overwrite
    private static @Nullable Direction findClosestDirection(Vector3f vector3f) {
        if (!vector3f.isFinite()) {
            return null;
        }

        float x = vector3f.x;
        float y = vector3f.y;
        float z = vector3f.z;

        float ax = Math.abs(x);
        float ay = Math.abs(y);
        float az = Math.abs(z);

        float max = ax > ay ? (ax > az ? ax : az) : (ay > az ? ay : az);

        if (max <= 0.0F) {
            return null;
        }

        if (max == ax) {
            return x > 0.0F ? Direction.EAST : Direction.WEST;
        } else if (max == ay) {
            return y > 0.0F ? Direction.UP : Direction.DOWN;
        } else {
            return z > 0.0F ? Direction.SOUTH : Direction.NORTH;
        }
    }*/
    //?} else {
    //? if >=1.21.11 {
    /**
     * @author Starlev
     * @reason Direct cross-product normal computation with early-exit for
     * degenerate/zero-length normals, avoiding helper method dispatch overhead.
     */
    @Overwrite
    private static Direction calculateFacing(Vector3fc[] vertices) {
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

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0.0F || !Float.isFinite(len)) {
            return Direction.UP;
        }

        nx /= len;
        ny /= len;
        nz /= len;

        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);

        if (ax > ay) {
            if (ax > az) {
                return nx > 0.0F ? Direction.EAST : Direction.WEST;
            }
        } else if (ay > az) {
            return ny > 0.0F ? Direction.UP : Direction.DOWN;
        }

        return nz > 0.0F ? Direction.SOUTH : Direction.NORTH;
    }
    //?} else {
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

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0.0F || !Float.isFinite(len)) {
            return Direction.UP;
        }

        nx /= len;
        ny /= len;
        nz /= len;

        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);

        if (ax > ay) {
            if (ax > az) {
                return nx > 0.0F ? Direction.EAST : Direction.WEST;
            }
        } else if (ay > az) {
            return ny > 0.0F ? Direction.UP : Direction.DOWN;
        }

        return nz > 0.0F ? Direction.SOUTH : Direction.NORTH;
    }*/
    //?}
    //?}

}
