package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.MatrixUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PoseStack.Pose.class)
public abstract class MixinPoseStackPose {

    @Shadow private Matrix4f pose;
    @Shadow private Matrix3f normal;

    @Shadow
    private void computeNormalMatrix() { throw new UnsupportedOperationException(); }

    @Unique private static final Matrix3f lomka$mat = new Matrix3f();

    /**
     * @author Starlev
     * @reason Reuse ThreadLocal Matrix3f for normal matrix extraction to eliminate per-pose-multiply allocation.
     */
    @Overwrite
    public void mulPose(Matrix4fc matrix4fc) {
        this.pose.mul(matrix4fc);
        if (!MatrixUtil.isPureTranslation(matrix4fc)) {
            if (MatrixUtil.isOrthonormal(matrix4fc)) {
                this.normal.mul(lomka$mat.set(matrix4fc));
            } else {
                this.computeNormalMatrix();
            }
        }
    }
}
