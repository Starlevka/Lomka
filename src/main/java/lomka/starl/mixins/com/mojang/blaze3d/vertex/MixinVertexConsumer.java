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
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
//? if >=1.21 && <26.1 {
import net.minecraft.client.renderer.LightTexture;
//?}
import net.minecraft.core.Direction;
//? if >=1.21 && <26.1 {
import net.minecraft.client.renderer.block.model.BakedQuad;
//?}
//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.resources.model.geometry.BakedQuad;*/
//?}
//? if >=1.21.4 {
import net.minecraft.util.ARGB;
//?}
import org.joml.Matrix3fc;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VertexConsumer.class, priority = 999) // Sodium/VulkanMod compability
public interface MixinVertexConsumer {

    @Shadow VertexConsumer addVertex(float f, float f1, float f2);
    @Shadow VertexConsumer setNormal(float f, float f1, float f2);
    @Shadow void addVertex(float f, float f1, float f2, int i, float f3, float f4, int j, int k, float f5, float f6, float f7);

    //? if >=1.21.11 {
    /**
     * @author Starlev
     * @reason Eliminates the Vector3f allocation of Matrix4fc.transformPosition via scalar 4x4 fma (Pose
     *         matrices are affine; a (0,0,0,1) w-row makes the perspective divide identity).
     *         1.21.6/1.21.9 declared Matrix4f instead of Matrix4fc, so this overwrite is gated >=1.21.11.
     */
    @Overwrite
    default VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float tz = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));
        return this.addVertex(tx, ty, tz);
    }
    //?} else {
    /*@Overwrite
    default VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float tz = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));
        return this.addVertex(tx, ty, tz);
    }
    *///?}

    //? if >=1.21.6 {
    //? if >=1.21.9 {
    //? if >=1.21.11 {
    /**
     * @author Starlev
     * @reason Eliminates the Vector2f allocation of Matrix3x2fc.transformPosition in the hot 2D GUI/font
     *         path via scalar 3x2 fma. 1.21.6 had an extra z param and 1.21.9 used Matrix3x2f, so this
     *         overwrite is gated >=1.21.11.
     */
    @Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2fc matrix, float x, float y) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, matrix.m20()));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, matrix.m21()));
        return this.addVertex(tx, ty, 0.0F);
    }
    //?} else {
    /*@Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2f matrix, float x, float y) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, matrix.m20()));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, matrix.m21()));
        return this.addVertex(tx, ty, 0.0F);
    }
    *///?}
    //?} else {
    /*@Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2f matrix, float x, float y, float z) {
        float tx = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, matrix.m20()));
        float ty = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, matrix.m21()));
        return this.addVertex(tx, ty, z);
    }
    *///?}
    //?}

    //? if >=1.21 {
    /**
     * @author Starlev
     * @reason Eliminates the Vector3f allocation of Pose.transformNormal and skips redundant
     *         normalization when pose.trustedNormals is true. The method exists on 1.21+
     *         only (1.20.1 has no Pose normal-trust concept), and the trustedNormals field is
     *         widened by the AW/AT templates across that whole range.
     */
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

    //? if >=1.21 && <1.21.4 {
    /*@Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light, int overlay) {
        int[] verts         = quad.getVertices();
        Direction direction = quad.getDirection();
        Matrix3fc normalMat = pose.normal();
        float dx     = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx     = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny     = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz     = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));
        float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;
        Matrix4f poseMat = pose.pose();
        int alpha        = (int) (a * 255.0F);
        int color        = net.minecraft.util.FastColor.ARGB32.color(alpha, (int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F));
        for (int vi = 0, count = verts.length / 8; vi < count; ++vi) {
            int base = vi * 8;
            float px = Float.intBitsToFloat(verts[base]);
            float py = Float.intBitsToFloat(verts[base + 1]);
            float pz = Float.intBitsToFloat(verts[base + 2]);
            float u  = Float.intBitsToFloat(verts[base + 4]);
            float v  = Float.intBitsToFloat(verts[base + 5]);
            float vx = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            this.addVertex(vx, vy, vz, color, u, v, overlay, light, nx, ny, nz);
        }
    }

    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, float a, int[] lights, int overlay, boolean flag) {
        int[] verts         = quad.getVertices();
        Direction direction = quad.getDirection();
        Matrix3fc normalMat = pose.normal();
        float dx     = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx     = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny     = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz     = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));
        float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;
        Matrix4f poseMat = pose.pose();
        int alpha = (int) (a * 255.0F);
        for (int vi = 0, count = verts.length / 8; vi < count; ++vi) {
            int base = vi * 8;
            float px = Float.intBitsToFloat(verts[base]);
            float py = Float.intBitsToFloat(verts[base + 1]);
            float pz = Float.intBitsToFloat(verts[base + 2]);
            float u  = Float.intBitsToFloat(verts[base + 4]);
            float v  = Float.intBitsToFloat(verts[base + 5]);
            float cr, cg, cb;
            if (flag) {
                int colorInt = verts[base + 3];
                cr = (colorInt & 0xFF) * brightness[vi] * r;
                cg = ((colorInt >> 8) & 0xFF) * brightness[vi] * g;
                cb = ((colorInt >> 16) & 0xFF) * brightness[vi] * b;
            } else {
                cr = brightness[vi] * r * 255.0F;
                cg = brightness[vi] * g * 255.0F;
                cb = brightness[vi] * b * 255.0F;
            }
            int color = net.minecraft.util.FastColor.ARGB32.color(alpha, (int) cr, (int) cg, (int) cb);
            int light = lights[vi];
            float vx  = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy  = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz  = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            this.addVertex(vx, vy, vz, color, u, v, overlay, light, nx, ny, nz);
        }
    }
    *///?} else if >=1.21.4 && <1.21.6 {
    /*@Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light, int overlay) {
        int[] verts         = quad.getVertices();
        Direction direction = quad.getDirection();
        Matrix3fc normalMat = pose.normal();
        float dx     = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx     = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny     = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz     = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));
        float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;
        Matrix4f poseMat = pose.pose();
        int alpha    = (int) (a * 255.0F);
        int emission = quad.getLightEmission();
        int color    = ARGB.color(alpha, (int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F));
        int l        = LightTexture.lightCoordsWithEmission(light, emission);
        for (int vi = 0, count = verts.length / 8; vi < count; ++vi) {
            int base = vi * 8;
            float px = Float.intBitsToFloat(verts[base]);
            float py = Float.intBitsToFloat(verts[base + 1]);
            float pz = Float.intBitsToFloat(verts[base + 2]);
            float u  = Float.intBitsToFloat(verts[base + 4]);
            float v  = Float.intBitsToFloat(verts[base + 5]);
            float vx = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            this.addVertex(vx, vy, vz, color, u, v, overlay, l, nx, ny, nz);
        }
    }

    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, float a, int[] lights, int overlay, boolean flag) {
        int[] verts         = quad.getVertices();
        Direction direction = quad.getDirection();
        Matrix3fc normalMat = pose.normal();
        float dx     = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx      = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny     = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz     = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));
        float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;
        Matrix4f poseMat = pose.pose();
        int alpha    = (int) (a * 255.0F);
        int emission = quad.getLightEmission();
        for (int vi = 0, count = verts.length / 8; vi < count; ++vi) {
            int base = vi * 8;
            float px = Float.intBitsToFloat(verts[base]);
            float py = Float.intBitsToFloat(verts[base + 1]);
            float pz = Float.intBitsToFloat(verts[base + 2]);
            float u  = Float.intBitsToFloat(verts[base + 4]);
            float v  = Float.intBitsToFloat(verts[base + 5]);
            float cr, cg, cb;
            if (flag) {
                int colorInt = verts[base + 3];
                cr = (colorInt & 0xFF) * brightness[vi] * r;
                cg = ((colorInt >> 8) & 0xFF) * brightness[vi] * g;
                cb = ((colorInt >> 16) & 0xFF) * brightness[vi] * b;
            } else {
                cr = brightness[vi] * r * 255.0F;
                cg = brightness[vi] * g * 255.0F;
                cb = brightness[vi] * b * 255.0F;
            }
            int color = ARGB.color(alpha, (int) cr, (int) cg, (int) cb);
            int light = LightTexture.lightCoordsWithEmission(lights[vi], emission);
            float vx  = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy  = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz  = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            this.addVertex(vx, vy, vz, color, u, v, overlay, light, nx, ny, nz);
        }
    }
    *///?} else if >=1.21.6 && <1.21.11 {
    /*@Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light, int overlay) {
        int[] verts         = quad.vertices();
        Direction direction = quad.direction();
        Matrix3fc normalMat = pose.normal();
        float dx     = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx     = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny     = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz     = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));
        float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;
        Matrix4f poseMat = pose.pose();
        int alpha    = (int) (a * 255.0F);
        int emission = quad.lightEmission();
        int color    = ARGB.color(alpha, (int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F));
        int l        = LightTexture.lightCoordsWithEmission(light, emission);
        for (int vi = 0, count = verts.length / 8; vi < count; ++vi) {
            int base = vi * 8;
            float px = Float.intBitsToFloat(verts[base]);
            float py = Float.intBitsToFloat(verts[base + 1]);
            float pz = Float.intBitsToFloat(verts[base + 2]);
            float u  = Float.intBitsToFloat(verts[base + 4]);
            float v  = Float.intBitsToFloat(verts[base + 5]);
            float vx = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            this.addVertex(vx, vy, vz, color, u, v, overlay, l, nx, ny, nz);
        }
    }

    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, float a, int[] lights, int overlay, boolean flag) {
        int[] verts         = quad.vertices();
        Direction direction = quad.direction();
        Matrix3fc normalMat = pose.normal();
        float dx     = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx     = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny     = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz     = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));
        float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;
        Matrix4f poseMat = pose.pose();
        int alpha    = (int) (a * 255.0F);
        int emission = quad.lightEmission();
        for (int vi = 0, count = verts.length / 8; vi < count; ++vi) {
            int base = vi * 8;
            float px = Float.intBitsToFloat(verts[base]);
            float py = Float.intBitsToFloat(verts[base + 1]);
            float pz = Float.intBitsToFloat(verts[base + 2]);
            float u  = Float.intBitsToFloat(verts[base + 4]);
            float v  = Float.intBitsToFloat(verts[base + 5]);
            float cr, cg, cb;
            if (flag) {
                int colorInt = verts[base + 3];
                cr = (colorInt & 0xFF) * brightness[vi] * r;
                cg = ((colorInt >> 8) & 0xFF) * brightness[vi] * g;
                cb = ((colorInt >> 16) & 0xFF) * brightness[vi] * b;
            } else {
                cr = brightness[vi] * r * 255.0F;
                cg = brightness[vi] * g * 255.0F;
                cb = brightness[vi] * b * 255.0F;
            }
            int color = ARGB.color(alpha, (int) cr, (int) cg, (int) cb);
            int light = LightTexture.lightCoordsWithEmission(lights[vi], emission);
            float vx  = Math.fma(poseMat.m00(), px, Math.fma(poseMat.m10(), py, Math.fma(poseMat.m20(), pz, poseMat.m30())));
            float vy  = Math.fma(poseMat.m01(), px, Math.fma(poseMat.m11(), py, Math.fma(poseMat.m21(), pz, poseMat.m31())));
            float vz  = Math.fma(poseMat.m02(), px, Math.fma(poseMat.m12(), py, Math.fma(poseMat.m22(), pz, poseMat.m32())));
            this.addVertex(vx, vy, vz, color, u, v, overlay, light, nx, ny, nz);
        }
    }
    *///?} else if >=1.21.11 && <26.1 {
    /**
     * @author Starlev
     * @reason Zero-allocation bulk quad transfer: vanilla allocates one normal Vector3f plus
     *         four position Vector3f per quad (5 objects) and re-derives UVs through the same
     *         path. Scalar fma transforms against the affine pose matrix and direct BakedQuad
     *         record accessors (position(i)/packedUV(i)/lightEmission()) emit identical
     *         vertices with no heap traffic. Face normal uses Direction's unit vector through
     *         the pose normal matrix, normalized only when !trustedNormals (field widened by
     *         the AW/AT templates). 26.1+ removed putBulkData — hence the <26.1 gate.
     */
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
        int color             = ARGB.colorFromFloat(a, r, g, b);
        int lightWithEmission = LightTexture.lightCoordsWithEmission(light, quad.lightEmission());

        for (int k = 0; k < 4; ++k) {
            Vector3fc p = quad.position(k);
            long packedUV = quad.packedUV(k);

            float vx = Math.fma(poseMat.m00(), p.x(), Math.fma(poseMat.m10(), p.y(), Math.fma(poseMat.m20(), p.z(), poseMat.m30())));
            float vy = Math.fma(poseMat.m01(), p.x(), Math.fma(poseMat.m11(), p.y(), Math.fma(poseMat.m21(), p.z(), poseMat.m31())));
            float vz = Math.fma(poseMat.m02(), p.x(), Math.fma(poseMat.m12(), p.y(), Math.fma(poseMat.m22(), p.z(), poseMat.m32())));

            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, lightWithEmission, nx, ny, nz);
        }
    }

    /**
     * @author Starlev
     * @reason Same zero-allocation optimizations as the non-brightness putBulkData:
     *         trustedNormals fast-path + per-vertex scalar transforms against the record API.
     */
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

        for (int k = 0; k < 4; ++k) {
            Vector3fc p = quad.position(k);
            long packedUV = quad.packedUV(k);
            float br = brightness[k];

            int color = ARGB.colorFromFloat(a, br * r, br * g, br * b);
            int light = LightTexture.lightCoordsWithEmission(lights[k], emission);

            float vx = Math.fma(poseMat.m00(), p.x(), Math.fma(poseMat.m10(), p.y(), Math.fma(poseMat.m20(), p.z(), poseMat.m30())));
            float vy = Math.fma(poseMat.m01(), p.x(), Math.fma(poseMat.m11(), p.y(), Math.fma(poseMat.m21(), p.z(), poseMat.m31())));
            float vz = Math.fma(poseMat.m02(), p.x(), Math.fma(poseMat.m12(), p.y(), Math.fma(poseMat.m22(), p.z(), poseMat.m32())));

            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, light, nx, ny, nz);
        }
    }
    //?} else if >=26.1 {
    /*@Overwrite
    default void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        Direction direction = quad.direction();
        Matrix3fc normalMat = pose.normal();
        float dx     = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx     = Math.fma(normalMat.m00(), dx, Math.fma(normalMat.m10(), dy, normalMat.m20() * dz));
        float ny     = Math.fma(normalMat.m01(), dx, Math.fma(normalMat.m11(), dy, normalMat.m21() * dz));
        float nz     = Math.fma(normalMat.m02(), dx, Math.fma(normalMat.m12(), dy, normalMat.m22() * dz));
        float invLen = org.joml.Math.invsqrt(Math.fma(nx, nx, Math.fma(ny, ny, nz * nz)));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;
        Matrix4f poseMat = pose.pose();
        int emission = quad.materialInfo().lightEmission();
        for (int vertex = 0; vertex < 4; ++vertex) {
            Vector3fc pos = quad.position(vertex);
            float vx  = Math.fma(poseMat.m00(), pos.x(), Math.fma(poseMat.m10(), pos.y(), Math.fma(poseMat.m20(), pos.z(), poseMat.m30())));
            float vy  = Math.fma(poseMat.m01(), pos.x(), Math.fma(poseMat.m11(), pos.y(), Math.fma(poseMat.m21(), pos.z(), poseMat.m31())));
            float vz  = Math.fma(poseMat.m02(), pos.x(), Math.fma(poseMat.m12(), pos.y(), Math.fma(poseMat.m22(), pos.z(), poseMat.m32())));
            long uv   = quad.packedUV(vertex);
            float u   = UVPair.unpackU(uv);
            float v   = UVPair.unpackV(uv);
            int color = instance.getColor(vertex);
            int light = instance.getLightCoordsWithEmission(vertex, emission);
            this.addVertex(vx, vy, vz, color, u, v, instance.overlayCoords(), light, nx, ny, nz);
        }
    }

    @Overwrite
    default void putBlockBakedQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
        Direction direction = quad.direction();
        float nx = direction.getStepX(), ny = direction.getStepY(), nz = direction.getStepZ();
        int emission = quad.materialInfo().lightEmission();
        for (int vertex = 0; vertex < 4; ++vertex) {
            Vector3fc pos = quad.position(vertex);
            long uv   = quad.packedUV(vertex);
            float u   = UVPair.unpackU(uv);
            float v   = UVPair.unpackV(uv);
            int color = instance.getColor(vertex);
            int light = instance.getLightCoordsWithEmission(vertex, emission);
            this.addVertex(pos.x() + x, pos.y() + y, pos.z() + z, color, u, v, instance.overlayCoords(), light, nx, ny, nz);
        }
    }
    *///?}
}