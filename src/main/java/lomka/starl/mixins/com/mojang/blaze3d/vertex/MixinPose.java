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
 * @reason Vanilla allocates a fresh Matrix3f on every orthonormal mulPose(Matrix4fc) call.
 *         Display entities invoke this per entity per frame to apply their interpolated
 *         transforms, so display-heavy maps stream thousands of short-lived matrices into the
 *         heap every frame. A render-thread-confined scratch matrix replaces the allocation;
 *         everything else mirrors vanilla exactly, including the non-orthonormal fallback that
 *         invalidates trustedNormals. Exists only while Pose#mulPose(Matrix4fc) does:
 *         absent before 1.21.6 (outer PoseStack did the work inline with a raw Matrix4f
 *         parameter - see MixinPoseStack) and removed again in 26.x.
 */
@Mixin(PoseStack.Pose.class)
public abstract class MixinPose {

    @Shadow @Final private Matrix4f pose;
    @Shadow @Final private Matrix3f normal;
    @Shadow private boolean trustedNormals;

    @Shadow abstract void computeNormalMatrix();

    @Unique private static final Matrix3f lomka$SCRATCH_NORMAL = new Matrix3f();

    /**
     * @author Starlev
     * @reason See class comment - only the orthonormal-path allocation is replaced; semantics
     *         are byte-for-byte vanilla.
     */
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
