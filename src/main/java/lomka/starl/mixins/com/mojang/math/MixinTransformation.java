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

package lomka.starl.mixins.com.mojang.math;

import com.mojang.math.Transformation;
import org.joml.Matrix4f;
//? if >=1.21.6 {
import org.joml.Matrix4fc;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Transformation.class)
public abstract class MixinTransformation {

    @Shadow @Final private static Transformation IDENTITY;

    //? if >=1.21.6 {
    @Shadow @Final private Matrix4fc matrix;
    //? } else {
    /*@Shadow @Final private Matrix4f matrix;
    *///?}

    /**
     * @author Starlev
     * @reason Inverts the stored matrix directly into a fresh destination matrix,
     *         avoiding the defensive matrix copy made before inversion.
     */
    @Overwrite
    public Transformation inverse() {
        if ((Object) this == IDENTITY) {
            return (Transformation) (Object) this;
        }

        //? if <1.21.6 {
        Matrix4f result = this.matrix.invert(new Matrix4f());
        //? } else {
        /*Matrix4f result = this.matrix.invertAffine(new Matrix4f());
        *///?}
        return result.isFinite() ? new Transformation(result) : null;
    }
}
