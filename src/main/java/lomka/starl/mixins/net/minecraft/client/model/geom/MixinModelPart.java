package lomka.starl.mixins.net.minecraft.client.model.geom;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelPart.class)
public class MixinModelPart {

    @Shadow public float zRot;
    @Shadow public float yRot;
    @Shadow public float xRot;
    @Shadow public void setRotation(float x, float y, float z) {}

    @Unique private final Matrix3f lomka$mat = new Matrix3f();
    @Unique private final Vector3f lomka$vec = new Vector3f();

    /**
     * @author Starlev
     * @reason Eliminates two per-call heap allocations (Matrix3f, Vector3f) by
     * reusing per-instance scratch buffers. rotationZYX() and rotate() both
     * return 'this', so lomka$mat serves as the sole working matrix throughout.
     */
    @Overwrite
    public void rotateBy(Quaternionf quaternionf) {
        Matrix3f m = this.lomka$mat.rotationZYX(this.zRot, this.yRot, this.xRot);
        m.rotate(quaternionf);
        m.getEulerAnglesZYX(this.lomka$vec);
        this.setRotation(this.lomka$vec.x, this.lomka$vec.y, this.lomka$vec.z);
    }
}
