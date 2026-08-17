package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import lomka.starl.utils.AxisPoseRotate;
//? if >=1.21.6 {
import org.joml.Quaternionfc;
//?} else {
/*import org.joml.Quaternionf;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PoseStack.class)
public abstract class MixinPoseStack {

    //? if >=1.21.6 {
    @Shadow
    public abstract PoseStack.Pose last();
    //?} else {
    /*@Shadow @org.spongepowered.asm.mixin.Final
    private java.util.Deque<PoseStack.Pose> poseStack;
    *///?}

    /**
     * @author Starlev
     * @reason Pure X/Y/Z quaternions (Axis.XP/YP/ZP path) recover sin/cos via double-angle identities
     *         sin(θ)=2xw, cos(θ)=w²-x² and apply sparse column updates matching JOML rotateX/Y/Z, avoiding full
     *         3D quaternion matrix multiply. General quaternions fall back to JOML rotate.
     */
    //? if >=1.21.6 {
    @Overwrite
    public void mulPose(Quaternionfc q) {
        PoseStack.Pose pose = this.last();
        AxisPoseRotate.mulPose(pose.pose(), pose.normal(), q);
    }
    //?} else {
    /*@Overwrite
    public void mulPose(Quaternionf q) {
        PoseStack.Pose pose = this.poseStack.getLast();
        AxisPoseRotate.mulPose(pose.pose(), pose.normal(), q);
    }
    *///?}
}
