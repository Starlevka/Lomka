package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Mixin(MeshData.class)
public class MixinMeshData {

    @Overwrite
    private static Vector3f[] unpackQuadCentroids(ByteBuffer bytebuffer, int vertexCount, VertexFormat format) {
        int posOffset = format.getOffset(VertexFormatElement.POSITION);
        if (posOffset == -1) {
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");
        }

        int vertexSize = format.getVertexSize();
        int quadSize   = vertexSize * 4;
        int quadCount  = vertexCount / 4;
        Vector3f[] result  = new Vector3f[quadCount];
        long base      = MemoryUtil.memAddress(bytebuffer);

        for (int q = 0; q < quadCount; q++) {
            long addr0 = base + (long) q * quadSize + posOffset;
            long addr2 = addr0 + (long) vertexSize * 2L;
            result[q] = new Vector3f(
                (MemoryUtil.memGetFloat(addr0)      + MemoryUtil.memGetFloat(addr2))      * 0.5f,
                (MemoryUtil.memGetFloat(addr0 + 4L) + MemoryUtil.memGetFloat(addr2 + 4L)) * 0.5f,
                (MemoryUtil.memGetFloat(addr0 + 8L) + MemoryUtil.memGetFloat(addr2 + 8L)) * 0.5f
            );
        }

        return result;
    }
}
