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

//? if >=1.21 {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.QuadInstance;
import org.joml.Vector3f;
*///?}
//? if >=1.21.11 {
import net.minecraft.client.model.geom.builders.UVPair;
//?}
//? if >=1.21.11 && <26.1 {
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
//?}
//? if >=1.21 && <26.1 {
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
//?}
//? if >=1.21.11 {
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
//?}
//? if >=1.21.6 && <1.21.11 {
import org.joml.Matrix3x2f;
//?}

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Starlev
 * Scalar replacements for the allocating default transforms of VertexConsumer.
 *
 * Coverage by era (file is config-gated >=1.21):
 * - addVertex(Matrix4fc/Matrix4f): all eras. The fma chain mirrors JOML
 *   transformPosition term-for-term; w is computed alongside (it pipelines
 *   with the x/y/z chains) and applied via DIRECT division whenever
 *   w != 1.0F - never on vanilla's affine pose matrices - reproducing JOML's
 *   dx/dw form bit-for-bit for non-affine callers.
 * - addVertexWith2DPose: >=1.21.6 only (Matrix3x2f 3-arg on 1.21.6-1.21.8,
 *   Matrix3x2f 2-arg on 1.21.9-1.21.10, Matrix3x2fc 2-arg >=1.21.11). The 2D
 *   affine transform has no w - plain fma, unconditionally bit-identical.
 * - setNormal(Pose, ...): >=1.21 && <26.1. Reads pose.trustedNormals through
 *   the AW/AT-widened field (gate >=1.21 in lomka.ct / accesstransformer.ct);
 *   on 26.1+ the field stays private and un-widened, so vanilla remains.
 * - putBulkData: >=1.21.11 && <26.1 ONLY. Pre-1.21.11 vanilla differs too much
 *   to replicate blind (boolean flag color branches, getUnitVec3i normals,
 *   MemoryStack raw-int extraction at a different vertex stride) - those eras
 *   keep vanilla. 26.1+ replaced putBulkData with putBakedQuad(QuadInstance),
 *   which is optimized below: per-vertex position transform via scalar fma with
 *   the affine w-check, normal delegated to vanilla transformNormal (the private
 *   trustedNormals field is left untouched on 26.x).
 *
 * putBulkData additionally inlines vanilla's 8->9-arg delegation; the float[4]
 * and int[4] bridge arrays are real per-quad allocations whenever the large
 * 9-arg default fails to inline into the meshing caller.
 */
@Mixin(value = VertexConsumer.class, priority = 999) // Sodium/VulkanMod compability
public interface MixinVertexConsumer {

    @Shadow VertexConsumer addVertex(float f, float f1, float f2);
    @Shadow VertexConsumer setNormal(float f, float f1, float f2);
    @Shadow void addVertex(float f, float f1, float f2, int i, float f3, float f4, int j, int k, float f5, float f6, float f7);

    //? if >=1.21.11 {
    @Overwrite
    default VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float tz = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));
        float w = Math.fma(matrix.m03(), x, Math.fma(matrix.m13(), y, Math.fma(matrix.m23(), z, matrix.m33())));
        if (w != 1.0F) {
            tx /= w;
            ty /= w;
            tz /= w;
        }
        return this.addVertex(tx, ty, tz);
    }
    //?}
    //? if >=1.21 && <1.21.11 {
    /*@Overwrite
    default VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float tz = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));
        float w = Math.fma(matrix.m03(), x, Math.fma(matrix.m13(), y, Math.fma(matrix.m23(), z, matrix.m33())));
        if (w != 1.0F) {
            tx /= w;
            ty /= w;
            tz /= w;
        }
        return this.addVertex(tx, ty, tz);
    }
    *///?}

    //? if >=1.21.6 && <1.21.9 {
    /*@Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2f matrix, float x, float y, float z) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, matrix.m20()));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, matrix.m21()));
        return this.addVertex(tx, ty, z);
    }
    *///?}
    //? if >=1.21.9 && <1.21.11 {
    /*@Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2f matrix, float x, float y) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, matrix.m20()));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, matrix.m21()));
        return this.addVertex(tx, ty, 0.0F);
    }
    *///?}
    //? if >=1.21.11 {
    @Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2fc matrix, float x, float y) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, matrix.m20()));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, matrix.m21()));
        return this.addVertex(tx, ty, 0.0F);
    }
    //?}

    //? if >=1.21 && <26.1 {
    @Overwrite
    default VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        Matrix3fc normal = pose.normal();
        float nx = Math.fma(normal.m00(), x, Math.fma(normal.m10(), y, normal.m20() * z));
        float ny = Math.fma(normal.m01(), x, Math.fma(normal.m11(), y, normal.m21() * z));
        float nz = Math.fma(normal.m02(), x, Math.fma(normal.m12(), y, normal.m22() * z));

        if (!pose.trustedNormals) {
            float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;
        }

        return this.setNormal(nx, ny, nz);
    }
    //?}

    //? if >=1.21.11 && <26.1 {
    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light, int overlay) {
        Matrix3fc normalMat = pose.normal();
        Vector3fc unit = quad.direction().getUnitVec3f();
        float dx = unit.x(), dy = unit.y(), dz = unit.z();
        float nx = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));

        if (!pose.trustedNormals) {
            float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;
        }

        Matrix4f poseMat = pose.pose();
        int color = ARGB.colorFromFloat(a, r, g, b);
        int lightWithEmission = LightTexture.lightCoordsWithEmission(light, quad.lightEmission());
        boolean affine = poseMat.m03() == 0.0F && poseMat.m13() == 0.0F && poseMat.m23() == 0.0F && poseMat.m33() == 1.0F;

        for (int k = 0; k < 4; ++k) {
            Vector3fc p = quad.position(k);
            long packedUV = quad.packedUV(k);
            float px = p.x(), py = p.y(), pz = p.z();

            float vx = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            if (!affine) {
                float w = Math.fma(poseMat.m03(), px, Math.fma(poseMat.m13(), py, Math.fma(poseMat.m23(), pz, poseMat.m33())));
                vx /= w;
                vy /= w;
                vz /= w;
            }

            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, lightWithEmission, nx, ny, nz);
        }
    }

    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, float a, int[] lights, int overlay) {
        Matrix3fc normalMat = pose.normal();
        Vector3fc unit = quad.direction().getUnitVec3f();
        float dx = unit.x(), dy = unit.y(), dz = unit.z();
        float nx = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));

        if (!pose.trustedNormals) {
            float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;
        }

        Matrix4f poseMat = pose.pose();
        int emission = quad.lightEmission();
        boolean affine = poseMat.m03() == 0.0F && poseMat.m13() == 0.0F && poseMat.m23() == 0.0F && poseMat.m33() == 1.0F;

        for (int k = 0; k < 4; ++k) {
            Vector3fc p = quad.position(k);
            long packedUV = quad.packedUV(k);
            float px = p.x(), py = p.y(), pz = p.z();
            float br = brightness[k];

            int color = ARGB.colorFromFloat(a, br * r, br * g, br * b);
            int light = LightTexture.lightCoordsWithEmission(lights[k], emission);

            float vx = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            if (!affine) {
                float w = Math.fma(poseMat.m03(), px, Math.fma(poseMat.m13(), py, Math.fma(poseMat.m23(), pz, poseMat.m33())));
                vx /= w;
                vy /= w;
                vz /= w;
            }

            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, light, nx, ny, nz);
        }
    }
    //?}

    //? if >=26.1 {
    /*@Overwrite
    default void putBakedQuad(PoseStack.Pose pose, net.minecraft.client.resources.model.geometry.BakedQuad quad, QuadInstance instance) {
        Matrix4fc matrix = pose.pose();
        Vector3fc unit = quad.direction().getUnitVec3f();
        Vector3f normal = pose.transformNormal(unit, new Vector3f());
        int emission = quad.materialInfo().lightEmission();
        boolean affine = matrix.m03() == 0.0F && matrix.m13() == 0.0F && matrix.m23() == 0.0F && matrix.m33() == 1.0F;

        for (int v = 0; v < 4; ++v) {
            Vector3fc p = quad.position(v);
            long packedUV = quad.packedUV(v);
            float px = p.x(), py = p.y(), pz = p.z();

            float vx = Math.fma(matrix.m00(), px, Math.fma(matrix.m10(), py, Math.fma(matrix.m20(), pz, matrix.m30())));
            float vy = Math.fma(matrix.m01(), px, Math.fma(matrix.m11(), py, Math.fma(matrix.m21(), pz, matrix.m31())));
            float vz = Math.fma(matrix.m02(), px, Math.fma(matrix.m12(), py, Math.fma(matrix.m22(), pz, matrix.m32())));
            if (!affine) {
                float w = Math.fma(matrix.m03(), px, Math.fma(matrix.m13(), py, Math.fma(matrix.m23(), pz, matrix.m33())));
                vx /= w;
                vy /= w;
                vz /= w;
            }

            this.addVertex(vx, vy, vz, instance.getColor(v), UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), instance.overlayCoords(), instance.getLightCoordsWithEmission(v, emission), normal.x(), normal.y(), normal.z());
        }
    }
    *///?}
}
