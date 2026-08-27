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

/**
 * Quaternion pose rotation fast path. Matrix {@code mulPose} scratch optimization
 * lives in {@code MixinPose} (outer/inner split by version).
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

}