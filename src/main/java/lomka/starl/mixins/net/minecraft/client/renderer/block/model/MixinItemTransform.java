package lomka.starl.mixins.net.minecraft.client.renderer.block.model;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26.1 {
/*import net.minecraft.client.resources.model.cuboid.ItemTransform;*/
//?} else {
import net.minecraft.client.renderer.block.model.ItemTransform;
//?}
import org.joml.Quaternionf;
//? if >=1.21.9 {
import org.joml.Vector3fc;
//?} else {
/*import org.joml.Vector3f;
*///?}
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemTransform.class)
public abstract class MixinItemTransform {

    //? if >=1.21.9 {
    @Shadow @Final private Vector3fc rotation;
    @Shadow @Final private Vector3fc translation;
    @Shadow @Final private Vector3fc scale;
    //?} else {
    /*@Shadow @Final private Vector3f rotation;
    @Shadow @Final private Vector3f translation;
    @Shadow @Final private Vector3f scale;
    *///?}

    @Unique private @Nullable Quaternionf lomka$quatNormal;
    @Unique private @Nullable Quaternionf lomka$quatFlipped;

    /**
     * @author Starlev
     * @reason Caches the fully-computed rotation quaternion per instance
     * instead of recomputing rotationXYZ() (sin/cos calls) and allocating a
     * fresh Quaternionf on every apply() call. ItemTransform is an immutable
     * record baked once per item model at resource-load time and reused
     * every frame the item is rendered (in hand, dropped, in item frames,
     * GUI slots with 3D rendering), so both possible outputs are genuinely
     * constant for the object's entire lifetime.
     *
     * Safe to hand out the same cached instance repeatedly: PoseStack.Pose
     * .rotate(Quaternionfc) passes the quaternion to Matrix4f.rotate and
     * Matrix3f.rotate using JOML's read-only Quaternionfc interface for
     * both calls, confirming neither mutates it.
     */
    //? if >=1.21.9 {
    @Overwrite
    public void apply(boolean flag, PoseStack.Pose pose) {
        ItemTransform self = (ItemTransform) (Object) this;
        if (self == ItemTransform.NO_TRANSFORM) {
            pose.translate(-0.5F, -0.5F, -0.5F);
            return;
        }

        float tx = flag ? -this.translation.x() : this.translation.x();
        pose.translate(tx, this.translation.y(), this.translation.z());

        Quaternionf q = flag ? this.lomka$quatFlipped : this.lomka$quatNormal;
        if (q == null) {
            float ry = flag ? -this.rotation.y() : this.rotation.y();
            float rz = flag ? -this.rotation.z() : this.rotation.z();
            q = new Quaternionf().rotationXYZ(
                this.rotation.x() * 0.017453292F, ry * 0.017453292F, rz * 0.017453292F);
            if (flag) {
                this.lomka$quatFlipped = q;
            } else {
                this.lomka$quatNormal = q;
            }
        }
        pose.rotate(q);

        pose.scale(this.scale.x(), this.scale.y(), this.scale.z());
        pose.translate(-0.5F, -0.5F, -0.5F);
    }
    //?} else {
    /*@Overwrite
    public void apply(boolean flag, PoseStack poseStack) {
        ItemTransform self = (ItemTransform) (Object) this;
        if (self == ItemTransform.NO_TRANSFORM) {
            return;
        }

        int i = flag ? -1 : 1;
        poseStack.translate((float) i * this.translation.x(), this.translation.y(), this.translation.z());

        Quaternionf q = flag ? this.lomka$quatFlipped : this.lomka$quatNormal;
        if (q == null) {
            float f = this.rotation.x();
            float f1 = flag ? -this.rotation.y() : this.rotation.y();
            float f2 = flag ? -this.rotation.z() : this.rotation.z();
            q = new Quaternionf().rotationXYZ(f * 0.017453292F, f1 * 0.017453292F, f2 * 0.017453292F);
            if (flag) {
                this.lomka$quatFlipped = q;
            } else {
                this.lomka$quatNormal = q;
            }
        }
        poseStack.mulPose(q);

        poseStack.scale(this.scale.x(), this.scale.y(), this.scale.z());
    }
    *///?}
}