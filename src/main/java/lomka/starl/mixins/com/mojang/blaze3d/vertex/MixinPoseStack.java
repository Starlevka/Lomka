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
import lomka.starl.utils.AxisPoseRotate;
//? if >=1.21.6 {
import org.joml.Quaternionfc;
//?} else {
/*import org.joml.Quaternionf;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
//? if <1.21.5 {
/*import org.spongepowered.asm.mixin.Unique;
*///?}

/**
 * Pose-stack hot-path overwrites. Notes for branches that are pre-commented for the VCS version:
 * <ul>
 *   <li>1.21-1.21.4 {@code mulPose(Matrix4f)}: vanilla allocates a fresh Matrix3f on every
 *       orthonormal call; displays invoke this per entity per frame, so display-heavy worlds
 *       stream thousands of short-lived matrices into the heap every frame. A render-thread-
 *       confined scratch matrix ({@code lomka$scratchNormal}) replaces the allocation;
 *       everything else mirrors vanilla byte for byte.</li>
 * </ul>
 * So, the method overwrite confict with Sodium is okay for <=1.21.4.
 */
@Mixin(PoseStack.class)
public abstract class MixinPoseStack {

    //? if >=1.21.6 {
    @Shadow public abstract PoseStack.Pose last();
    //?} else {
    /*@Shadow @org.spongepowered.asm.mixin.Final private java.util.Deque<PoseStack.Pose> poseStack;
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

    //? if >=1.21 && <1.21.6 {
    /*@Overwrite
    public void mulPose(org.joml.Matrix4f m) {
        PoseStack.Pose pose = this.poseStack.getLast();
        pose.pose().mul(m);
        if (!com.mojang.math.MatrixUtil.isPureTranslation(m)) {
            if (com.mojang.math.MatrixUtil.isOrthonormal(m)) {
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

    @Unique private static final org.joml.Matrix3f lomka$scratchNormal = new org.joml.Matrix3f();
    *///?}

    //? if <1.21.5 {
    /*@Unique private final java.util.ArrayDeque<PoseStack.Pose> lomka$pool = new java.util.ArrayDeque<>();
    *///?}

    /**
     * @author Starlev
     * @reason Zero-allocation pushPose for <=1.21.4. Vanilla allocates a fresh Pose (two JOML matrix copies) on
     *         every pushPose call; rendering pushes/pops per entity and per part, so this is a steady heap-allocation
     *         stream. Reuse popped Pose objects from a per-stack pool instead. A reused Pose is fully overwritten
     *         (matrices + trustedNormals) before re-push, matching vanilla copy semantics; the same object-reuse
     *         model Mojang later adopted in 1.21.6+.
     */
    //? if <1.21.5 {
    /*@Overwrite
    public void pushPose() {
        PoseStack.Pose top    = this.poseStack.getLast();
        PoseStack.Pose reused = this.lomka$pool.pollLast();
        if (reused != null) {
            reused.pose().set(top.pose());
            reused.normal().set(top.normal());
            //? if >=1.21 {
            reused.trustedNormals = top.trustedNormals;
            //?}
            this.poseStack.addLast(reused);
        } else {
            this.poseStack.addLast(new PoseStack.Pose(new org.joml.Matrix4f(top.pose()), new org.joml.Matrix3f(top.normal())));
        }
    }
    *///?}

    /**
     * @author Starlev
     * @reason Return the popped Pose to the pool so the next pushPose can reuse it instead of allocating.
     */
    //? if <1.21.5 {
    /*@Overwrite
    public void popPose() {
        this.lomka$pool.addLast(this.poseStack.removeLast());
    }
    *///?}
}