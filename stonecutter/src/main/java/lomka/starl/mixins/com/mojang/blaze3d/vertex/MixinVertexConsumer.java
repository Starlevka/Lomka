package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import net.minecraft.client.model.geom.builders.UVPair;
//? if >=26.1 {
/*import net.minecraft.client.resources.model.geometry.BakedQuad;
import com.mojang.blaze3d.vertex.QuadInstance;
import org.joml.Matrix3f;*/
//?} else {
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
import org.joml.Matrix3f;
//?}
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VertexConsumer.class, priority = 1100)
public interface MixinVertexConsumer {

    //? if >=26.1 {
    /*@Overwrite
    default void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        Vector3fc dir = quad.direction().getUnitVec3f();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float dx = dir.x(), dy = dir.y(), dz = dir.z();
        float nx = Math.fma(normalMatrix.m00(), dx, Math.fma(normalMatrix.m10(), dy, normalMatrix.m20() * dz));
        float ny = Math.fma(normalMatrix.m01(), dx, Math.fma(normalMatrix.m11(), dy, normalMatrix.m21() * dz));
        float nz = Math.fma(normalMatrix.m02(), dx, Math.fma(normalMatrix.m12(), dy, normalMatrix.m22() * dz));

        int lightEmission = quad.materialInfo().lightEmission();

        for (int vertex = 0; vertex < 4; ++vertex) {
            Vector3fc position = quad.position(vertex);
            long packedUv = quad.packedUV(vertex);
            int vertexColor = instance.getColor(vertex);
            int light = instance.getLightCoordsWithEmission(vertex, lightEmission);

            float px = position.x(), py = position.y(), pz = position.z();
            float tx = Math.fma(matrix.m00(), px, Math.fma(matrix.m10(), py, Math.fma(matrix.m20(), pz, matrix.m30())));
            float ty = Math.fma(matrix.m01(), px, Math.fma(matrix.m11(), py, Math.fma(matrix.m21(), pz, matrix.m31())));
            float tz = Math.fma(matrix.m02(), px, Math.fma(matrix.m12(), py, Math.fma(matrix.m22(), pz, matrix.m32())));

            float u = UVPair.unpackU(packedUv);
            float v = UVPair.unpackV(packedUv);

            ((VertexConsumer) this).addVertex(tx, ty, tz, vertexColor, u, v, instance.overlayCoords(), light, nx, ny, nz);
        }
    }*/
    //?} else {
    @Shadow
    void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz);

    /**
     * @author Starlev
     * @reason Eliminates per-quad allocation of a temporary VertexConsumer,
     * replacing lambda-based element iteration with inlined direct vertex emission.
     */
    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad,
        float[] brightness, float r, float g, float b, float a,
        int[] lights, int overlay) {

        Vector3fc dir = quad.direction().getUnitVec3f();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float dx = dir.x(), dy = dir.y(), dz = dir.z();
        float nx = Math.fma(normalMatrix.m00(), dx, Math.fma(normalMatrix.m10(), dy, normalMatrix.m20() * dz));
        float ny = Math.fma(normalMatrix.m01(), dx, Math.fma(normalMatrix.m11(), dy, normalMatrix.m21() * dz));
        float nz = Math.fma(normalMatrix.m02(), dx, Math.fma(normalMatrix.m12(), dy, normalMatrix.m22() * dz));

        int emission = quad.lightEmission();

        for (int k = 0; k < 4; k++) {
            Vector3fc pos = quad.position(k);
            long uv = quad.packedUV(k);
            float br = brightness[k];

            float px = pos.x(), py = pos.y(), pz = pos.z();
            float tx = Math.fma(matrix.m00(), px, Math.fma(matrix.m10(), py, Math.fma(matrix.m20(), pz, matrix.m30())));
            float ty = Math.fma(matrix.m01(), px, Math.fma(matrix.m11(), py, Math.fma(matrix.m21(), pz, matrix.m31())));
            float tz = Math.fma(matrix.m02(), px, Math.fma(matrix.m12(), py, Math.fma(matrix.m22(), pz, matrix.m32())));

            int color = ARGB.colorFromFloat(a, br * r, br * g, br * b);
            int light = LightTexture.lightCoordsWithEmission(lights[k], emission);

            ((VertexConsumer) this).addVertex(tx, ty, tz, color,
                UVPair.unpackU(uv), UVPair.unpackV(uv),
                overlay, light, nx, ny, nz);
        }
    }
    //?}
}