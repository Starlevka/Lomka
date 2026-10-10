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
import org.joml.Matrix3fc;
*///?}
//? if >=1.21.11 {
import net.minecraft.client.model.geom.builders.UVPair;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
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
//? if >=1.21.6 && <1.21.11 {
/*import org.joml.Matrix3x2f;
*///?}

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Starlev
 * Scalar replacements for the allocating default transforms of VertexConsumer.
 *
 * Coverage by era (file is config-gated >=1.21):
 * - addVertex(Matrix4fc/Matrix4f): all eras. Vanilla routes through
 *   {@code Matrix4f.transformPosition}, which is {@code Vector3f#mulPosition}: the 3x3
 *   block plus translation, with NO w term and no perspective divide anywhere. The bodies
 *   below reproduce that sum exactly and drop the temporary Vector3f. Two earlier
 *   revisions got it wrong, and both looked harmless on every pose matrix the game builds:
 *   one computed w and divided by it whenever w != 1, silently reimplementing perspective
 *   projection that vanilla never performs; the other built the same sum from nested
 *   {@code java.lang.Math.fma}. The second one is the subtle one. JOML writes the identical
 *   expression, but through {@code org.joml.Math.fma}, and in every JOML release Minecraft
 *   ships {@code org.joml.Runtime.HAS_Math_fma} is false - each of those calls is a plain
 *   {@code a * b + c}. A replica that uses the fused JDK intrinsic is therefore off by up to
 *   one ulp on most vertices while looking like a faithful transcription. The same applies to
 *   the association: vanilla nests as {@code m00 * x + (m10 * y + (m20 * z + m30))}, while the
 *   2D transform is a left-to-right sum - both verified bit-identical by a fuzz comparison
 *   against the vanilla operation.
 * - addVertexWith2DPose: >=1.21.6 only (Matrix3x2f 3-arg on 1.21.6-1.21.8,
 *   Matrix3x2f 2-arg on 1.21.9-1.21.10, Matrix3x2fc 2-arg >=1.21.11).
 * - setNormal(Pose, ...): >=1.21 && <26.1. Mirrors {@code Pose#transformNormal}:
 *   {@code normal.transform(...)} and, when the pose flags untrusted normals,
 *   {@code normalize()} - which is {@code invsqrt} of the nested length-squared, not a
 *   reciprocal square root of a different sum.
 * - putBulkData: >=1.21.11 && <26.1 ONLY. Pre-1.21.11 vanilla differs too much
 *   to replicate blind (boolean flag color branches, getUnitVec3i normals,
 *   MemoryStack raw-int extraction at a different vertex stride) - those eras
 *   keep vanilla. 26.1+ replaced putBulkData with putBakedQuad(QuadInstance),
 *   which is optimized below.
 * - putBakedQuad: >=26.1. The same two JOML expressions on the shape 26.1 introduced.
 *   Vanilla allocates one {@code Vector3f} for the normal plus one per vertex, five per
 *   quad; this body allocates none, and {@code BakedQuad#position(int)} /
 *   {@code #packedUV(int)} are switch dispatches over record fields, so nothing else in
 *   the loop allocates either.
 *
 * putBulkData additionally inlines vanilla's 8->9-arg delegation; the float[4]
 * and int[4] bridge arrays are real per-quad allocations whenever the large
 * 9-arg default fails to inline into the meshing caller.
 *
 * Both quad overwrites and both scalar transforms are verified bit-identical to the vanilla
 * JOML operation by a fuzz comparison, including the untrusted-normal branch
 * and the 8-arg delegation.
 *
 * Deliberately left vanilla on 26.x:
 * - {@code putBlockBakedQuad} allocates nothing to begin with - it adds the block offset per
 *   component instead of transforming a position.
 * - {@code putBakedQuadWithGlint} (>=26.3 only) has a single call site, the item-foil branch
 *   in {@code ItemFeatureRenderer}, and cannot be written allocation-free:
 *   {@code SheetedDecalTextureGenerator#setSheetedDecalUv} takes the position and the normal
 *   as {@code Vector3f} arguments, and it allocates two more per vertex itself. Removing the
 *   four position transforms would not change the shape of that path.
 */
@Mixin(value = VertexConsumer.class, priority = 999) // Sodium/VulkanMod compability
public interface MixinVertexConsumer {

    @Shadow VertexConsumer addVertex(float f, float f1, float f2);
    @Shadow VertexConsumer setNormal(float f, float f1, float f2);
    @Shadow void addVertex(float f, float f1, float f2, int i, float f3, float f4, int j, int k, float f5, float f6, float f7);

    //? if >=1.21.11 {
    @Overwrite
    default VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z) {
        return this.addVertex(
                matrix.m00() * x + (matrix.m10() * y + (matrix.m20() * z + matrix.m30())),
                matrix.m01() * x + (matrix.m11() * y + (matrix.m21() * z + matrix.m31())),
                matrix.m02() * x + (matrix.m12() * y + (matrix.m22() * z + matrix.m32())));
    }
    //?}
    //? if >=1.21 && <1.21.11 {
    /*@Overwrite
    default VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        return this.addVertex(
                matrix.m00() * x + (matrix.m10() * y + (matrix.m20() * z + matrix.m30())),
                matrix.m01() * x + (matrix.m11() * y + (matrix.m21() * z + matrix.m31())),
                matrix.m02() * x + (matrix.m12() * y + (matrix.m22() * z + matrix.m32())));
    }
    *///?}

    //? if >=1.21.6 && <1.21.9 {
    /*@Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2f matrix, float x, float y, float z) {
        return this.addVertex(
                matrix.m00() * x + matrix.m10() * y + matrix.m20(),
                matrix.m01() * x + matrix.m11() * y + matrix.m21(),
                z);
    }
    *///?}
    //? if >=1.21.9 && <1.21.11 {
    /*@Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2f matrix, float x, float y) {
        return this.addVertex(
                matrix.m00() * x + matrix.m10() * y + matrix.m20(),
                matrix.m01() * x + matrix.m11() * y + matrix.m21(),
                0.0F);
    }
    *///?}
    //? if >=1.21.11 {
    @Overwrite
    default VertexConsumer addVertexWith2DPose(Matrix3x2fc matrix, float x, float y) {
        return this.addVertex(
                matrix.m00() * x + matrix.m10() * y + matrix.m20(),
                matrix.m01() * x + matrix.m11() * y + matrix.m21(),
                0.0F);
    }
    //?}

    //? if >=1.21 && <26.1 {
    /**
     * @author Starlev
     * @reason Reproduces {@code Pose#transformNormal} in scalars - the 3x3 normal matrix
     *         sum plus the {@code normalize()} that an untrusted pose demands - without
     *         the Vector3f vanilla allocates on every call.
     */
    @Overwrite
    default VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        Matrix3fc normal = pose.normal();
        float nx = normal.m00() * x + (normal.m10() * y + normal.m20() * z);
        float ny = normal.m01() * x + (normal.m11() * y + normal.m21() * z);
        float nz = normal.m02() * x + (normal.m12() * y + normal.m22() * z);

        if (!pose.trustedNormals) {
            float inv = org.joml.Math.invsqrt(nx * nx + (ny * ny + nz * nz));
            nx *= inv;
            ny *= inv;
            nz *= inv;
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
        float nx = normalMat.m00() * dx + (normalMat.m10() * dy + normalMat.m20() * dz);
        float ny = normalMat.m01() * dx + (normalMat.m11() * dy + normalMat.m21() * dz);
        float nz = normalMat.m02() * dx + (normalMat.m12() * dy + normalMat.m22() * dz);

        if (!pose.trustedNormals) {
            float invLen = org.joml.Math.invsqrt(nx * nx + (ny * ny + nz * nz));
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;
        }

        Matrix4f poseMat = pose.pose();
        int color = ARGB.colorFromFloat(a, r, g, b);
        int lightWithEmission = LightTexture.lightCoordsWithEmission(light, quad.lightEmission());

        {
            Vector3fc p = quad.position(0);
            long packedUV = quad.packedUV(0);
            float px = p.x(), py = p.y(), pz = p.z();
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, lightWithEmission, nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(1);
            long packedUV = quad.packedUV(1);
            float px = p.x(), py = p.y(), pz = p.z();
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, lightWithEmission, nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(2);
            long packedUV = quad.packedUV(2);
            float px = p.x(), py = p.y(), pz = p.z();
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, lightWithEmission, nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(3);
            long packedUV = quad.packedUV(3);
            float px = p.x(), py = p.y(), pz = p.z();
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, lightWithEmission, nx, ny, nz);
        }
    }

    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, float a, int[] lights, int overlay) {
        Matrix3fc normalMat = pose.normal();
        Vector3fc unit = quad.direction().getUnitVec3f();
        float dx = unit.x(), dy = unit.y(), dz = unit.z();
        float nx = normalMat.m00() * dx + (normalMat.m10() * dy + normalMat.m20() * dz);
        float ny = normalMat.m01() * dx + (normalMat.m11() * dy + normalMat.m21() * dz);
        float nz = normalMat.m02() * dx + (normalMat.m12() * dy + normalMat.m22() * dz);

        if (!pose.trustedNormals) {
            float invLen = org.joml.Math.invsqrt(nx * nx + (ny * ny + nz * nz));
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;
        }

        Matrix4f poseMat = pose.pose();
        int emission = quad.lightEmission();

        {
            Vector3fc p = quad.position(0);
            long packedUV = quad.packedUV(0);
            float px = p.x(), py = p.y(), pz = p.z();
            float br = brightness[0];
            int color = ARGB.colorFromFloat(a, br * r, br * g, br * b);
            int light = LightTexture.lightCoordsWithEmission(lights[0], emission);
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, light, nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(1);
            long packedUV = quad.packedUV(1);
            float px = p.x(), py = p.y(), pz = p.z();
            float br = brightness[1];
            int color = ARGB.colorFromFloat(a, br * r, br * g, br * b);
            int light = LightTexture.lightCoordsWithEmission(lights[1], emission);
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, light, nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(2);
            long packedUV = quad.packedUV(2);
            float px = p.x(), py = p.y(), pz = p.z();
            float br = brightness[2];
            int color = ARGB.colorFromFloat(a, br * r, br * g, br * b);
            int light = LightTexture.lightCoordsWithEmission(lights[2], emission);
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, light, nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(3);
            long packedUV = quad.packedUV(3);
            float px = p.x(), py = p.y(), pz = p.z();
            float br = brightness[3];
            int color = ARGB.colorFromFloat(a, br * r, br * g, br * b);
            int light = LightTexture.lightCoordsWithEmission(lights[3], emission);
            float vx = poseMat.m00() * px + (poseMat.m10() * py + (poseMat.m20() * pz + poseMat.m30()));
            float vy = poseMat.m01() * px + (poseMat.m11() * py + (poseMat.m21() * pz + poseMat.m31()));
            float vz = poseMat.m02() * px + (poseMat.m12() * py + (poseMat.m22() * pz + poseMat.m32()));
            this.addVertex(vx, vy, vz, color, UVPair.unpackU(packedUV), UVPair.unpackV(packedUV), overlay, light, nx, ny, nz);
        }
    }
    //?}

    //? if >=26.1 {
    /*@Overwrite
    default void putBakedQuad(PoseStack.Pose pose, net.minecraft.client.resources.model.geometry.BakedQuad quad, QuadInstance instance) {
        Matrix4fc matrix = pose.pose();
        Matrix3fc normalMat = pose.normal();
        Vector3fc unit = quad.direction().getUnitVec3f();
        float dx = unit.x(), dy = unit.y(), dz = unit.z();
        float nx = normalMat.m00() * dx + (normalMat.m10() * dy + normalMat.m20() * dz);
        float ny = normalMat.m01() * dx + (normalMat.m11() * dy + normalMat.m21() * dz);
        float nz = normalMat.m02() * dx + (normalMat.m12() * dy + normalMat.m22() * dz);

        if (!pose.trustedNormals) {
            float inv = org.joml.Math.invsqrt(nx * nx + (ny * ny + nz * nz));
            nx *= inv;
            ny *= inv;
            nz *= inv;
        }

        int emission = quad.materialInfo().lightEmission();
        int overlay = instance.overlayCoords();

        {
            Vector3fc p = quad.position(0);
            long packedUV = quad.packedUV(0);
            float px = p.x(), py = p.y(), pz = p.z();
            this.addVertex(matrix.m00() * px + (matrix.m10() * py + (matrix.m20() * pz + matrix.m30())),
                    matrix.m01() * px + (matrix.m11() * py + (matrix.m21() * pz + matrix.m31())),
                    matrix.m02() * px + (matrix.m12() * py + (matrix.m22() * pz + matrix.m32())),
                    instance.getColor(0), UVPair.unpackU(packedUV), UVPair.unpackV(packedUV),
                    overlay, instance.getLightCoordsWithEmission(0, emission), nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(1);
            long packedUV = quad.packedUV(1);
            float px = p.x(), py = p.y(), pz = p.z();
            this.addVertex(matrix.m00() * px + (matrix.m10() * py + (matrix.m20() * pz + matrix.m30())),
                    matrix.m01() * px + (matrix.m11() * py + (matrix.m21() * pz + matrix.m31())),
                    matrix.m02() * px + (matrix.m12() * py + (matrix.m22() * pz + matrix.m32())),
                    instance.getColor(1), UVPair.unpackU(packedUV), UVPair.unpackV(packedUV),
                    overlay, instance.getLightCoordsWithEmission(1, emission), nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(2);
            long packedUV = quad.packedUV(2);
            float px = p.x(), py = p.y(), pz = p.z();
            this.addVertex(matrix.m00() * px + (matrix.m10() * py + (matrix.m20() * pz + matrix.m30())),
                    matrix.m01() * px + (matrix.m11() * py + (matrix.m21() * pz + matrix.m31())),
                    matrix.m02() * px + (matrix.m12() * py + (matrix.m22() * pz + matrix.m32())),
                    instance.getColor(2), UVPair.unpackU(packedUV), UVPair.unpackV(packedUV),
                    overlay, instance.getLightCoordsWithEmission(2, emission), nx, ny, nz);
        }
        {
            Vector3fc p = quad.position(3);
            long packedUV = quad.packedUV(3);
            float px = p.x(), py = p.y(), pz = p.z();
            this.addVertex(matrix.m00() * px + (matrix.m10() * py + (matrix.m20() * pz + matrix.m30())),
                    matrix.m01() * px + (matrix.m11() * py + (matrix.m21() * pz + matrix.m31())),
                    matrix.m02() * px + (matrix.m12() * py + (matrix.m22() * pz + matrix.m32())),
                    instance.getColor(3), UVPair.unpackU(packedUV), UVPair.unpackV(packedUV),
                    overlay, instance.getLightCoordsWithEmission(3, emission), nx, ny, nz);
        }
    }
    *///?}
}
