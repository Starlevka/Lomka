/*
 * This file is part of Lomka (https://github.com/Starlevka/Lomka)
 * Copyright (C) 2026 Starlev (a.k.a. Starlevka) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.MatrixUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Starlev
 * Matrix mulPose hot path - scratch allocation removal.
 * <p>
 * The allocation site moved between classes: outer {@code PoseStack} with raw
 * {@code Matrix4f} on 1.21-1.21.4, inner {@code PoseStack.Pose} with
 * {@code Matrix4fc} on 1.21.6-26.x (removed again in 26.x). Both share one fix:
 * render-thread confined scratch {@code Matrix3f} on the orthonormal path.
 * Quaternion path stays in {@code MixinPoseStack}.
 */
//? if >=1.21.6 && <26.1 {
@Mixin(PoseStack.Pose.class)
public abstract class MixinPose {

    @Shadow @Final private Matrix4f pose;
    @Shadow @Final private Matrix3f normal;

    @Shadow abstract void computeNormalMatrix();

    @Unique private static final Matrix3f lomka$SCRATCH_NORMAL = new Matrix3f();

    @Overwrite
    public void mulPose(Matrix4fc m) {
        this.pose.mul(m);
        if (!MatrixUtil.isPureTranslation(m)) {
            if (MatrixUtil.isOrthonormal(m)) {
                lomka$SCRATCH_NORMAL.set(
                        m.m00(), m.m01(), m.m02(),
                        m.m10(), m.m11(), m.m12(),
                        m.m20(), m.m21(), m.m22());
                this.normal.mul(lomka$SCRATCH_NORMAL);
            } else {
                this.computeNormalMatrix();
            }
        }
    }
}
//?} else if >=1.21 && <1.21.6 {
/*@Mixin(PoseStack.class)
public abstract class MixinPose {

    @Shadow @Final private java.util.Deque<PoseStack.Pose> poseStack;

    @Unique private static final Matrix3f lomka$scratchNormal = new Matrix3f();

    @Overwrite
    public void mulPose(Matrix4f m) {
        PoseStack.Pose pose = this.poseStack.getLast();
        pose.pose().mul(m);
        if (!MatrixUtil.isPureTranslation(m)) {
            if (MatrixUtil.isOrthonormal(m)) {
                lomka$scratchNormal.set(
                        m.m00(), m.m01(), m.m02(),
                        m.m10(), m.m11(), m.m12(),
                        m.m20(), m.m21(), m.m22());
                pose.normal().mul(lomka$scratchNormal);
            } else {
                pose.normal().set(pose.pose()).invert().transpose();
                pose.trustedNormals = false;
            }
        }
    }
}
*///?}