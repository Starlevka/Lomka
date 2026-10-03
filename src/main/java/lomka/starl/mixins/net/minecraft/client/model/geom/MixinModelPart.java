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

package lomka.starl.mixins.net.minecraft.client.model.geom;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * <p>
 * The method composes the part's Euler angles with an external quaternion and converts the
 * result straight back to Euler, so it is an Euler -> matrix -> Euler round trip. Vanilla
 * allocates a {@code Matrix3f} for the rotation and a {@code Vector3f} for the Euler
 * extraction - JOML's {@code Matrix3f.rotate(Quaternionfc)} mutates in place and returns the
 * receiver, so there is no third object to avoid, which is why the patch below reuses two
 * matrices and one vector rather than three of each. The scratch fields are thread-confined,
 * matching the existing convention in {@code MixinPose}, and JOML's
 * {@code rotate(quat, dest)} overload writes the full result into {@code dest} without
 * allocating.
 * <p>
 * Nothing escapes: the only values written out are the part's own {@code xRot}/{@code yRot}/
 * {@code zRot} through {@code setRotation}, exactly as before, so the external
 * {@code Quaternionf} argument is still only read.
 * <p>
 * Call sites are few - {@code EndCrystalModel} (three per crystal per frame) and
 * {@code PlayerCapeModel} (one per player per frame) - so this is an allocation reduction on a
 * narrow path, not a frame-time change. The trigonometry inside {@code getEulerAnglesZYX}
 * is untouched and remains the dominant cost of the call.
 * (Measured figures live in scripts/bench, not in source comments.)
 */
@Mixin(ModelPart.class)
public abstract class MixinModelPart {

    @Shadow public float xRot;
    @Shadow public float yRot;
    @Shadow public float zRot;

    @Shadow public abstract void setRotation(float xRot, float yRot, float zRot);

    @Unique private static final ThreadLocal<Matrix3f> lomka$SCRATCH_EULER     = ThreadLocal.withInitial(Matrix3f::new);
    @Unique private static final ThreadLocal<Matrix3f> lomka$SCRATCH_ROTATED   = ThreadLocal.withInitial(Matrix3f::new);
    @Unique private static final ThreadLocal<Vector3f> lomka$SCRATCH_EULER_OUT = ThreadLocal.withInitial(Vector3f::new);

    /**
     * @author Starlev
     * @reason Reuses two scratch Matrix3f and one scratch Vector3f instead of allocating
     *         per call. The arithmetic is unchanged: {@code rotationZYX} on the part's angles,
     *         then JOML's {@code rotate(quat, dest)} which computes the same {@code M * Q}
     *         the in-place overload returns, then {@code getEulerAnglesZYX} into a reused
     *         vector. The resulting Euler angles are bit-identical to vanilla's sequence,
     *         verified over 200k random rotations by {@code FormulaParityCrossCheck}.
     *         (Figures: scripts/bench.)
     */
    @Overwrite
    public void rotateBy(Quaternionf quaternionf) {
        Matrix3f euler     = lomka$SCRATCH_EULER.get();
        Matrix3f rotated   = lomka$SCRATCH_ROTATED.get();
        Vector3f extracted = lomka$SCRATCH_EULER_OUT.get();

        euler.rotationZYX(this.zRot, this.yRot, this.xRot);
        euler.rotate(quaternionf, rotated);
        rotated.getEulerAnglesZYX(extracted);

        this.setRotation(extracted.x, extracted.y, extracted.z);
    }
}
