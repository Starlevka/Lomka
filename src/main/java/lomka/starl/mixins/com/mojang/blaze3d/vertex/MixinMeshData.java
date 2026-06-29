package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Mixin(MeshData.class)
public class MixinMeshData {

    //? if >=26.2 {
    /*@Overwrite
    public static void decodeQuadCentroids(ByteBuffer bytebuffer, int i, VertexFormat vertexformat, CompactVectorArray output, int outputIndex) {
        VertexFormatElement posElement = vertexformat.getElement("Position");
        if (posElement == null) {
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");
        }

        int posOffset = bytebuffer.position() + posElement.offset();
        int vertexSize = vertexformat.getVertexSize();
        int quadSize = vertexSize * 4;
        int quadsCount = i / 4;

        if (bytebuffer.isDirect()) {
            long address = MemoryUtil.memAddress(bytebuffer);
            for (int quad = 0; quad < quadsCount; ++quad) {
                long addr0 = address + (long) quad * quadSize + posOffset;
                long addr2 = addr0 + (long) vertexSize * 2L;

                float f0 = MemoryUtil.memGetFloat(addr0);
                float f1 = MemoryUtil.memGetFloat(addr0 + 4L);
                float f2 = MemoryUtil.memGetFloat(addr0 + 8L);

                float f3 = MemoryUtil.memGetFloat(addr2);
                float f4 = MemoryUtil.memGetFloat(addr2 + 4L);
                float f5 = MemoryUtil.memGetFloat(addr2 + 8L);

                output.set(outputIndex + quad, (f0 + f3) * 0.5F, (f1 + f4) * 0.5F, (f2 + f5) * 0.5F);
            }
        } else {
            FloatBuffer floatbuffer = bytebuffer.asFloatBuffer();
            int floatsPerVertex = vertexSize / 4;
            int floatsPerQuad = floatsPerVertex * 4;
            int posFloatOffset = posOffset / 4;

            for (int quad = 0; quad < quadsCount; ++quad) {
                int index0 = quad * floatsPerQuad + posFloatOffset;
                int index2 = index0 + floatsPerVertex * 2;

                float f0 = floatbuffer.get(index0);
                float f1 = floatbuffer.get(index0 + 1);
                float f2 = floatbuffer.get(index0 + 2);
                float f3 = floatbuffer.get(index2);
                float f4 = floatbuffer.get(index2 + 1);
                float f5 = floatbuffer.get(index2 + 2);

                output.set(outputIndex + quad, (f0 + f3) * 0.5F, (f1 + f4) * 0.5F, (f2 + f5) * 0.5F);
            }
        }
    }*/
    //?} else {
    /**
     * @author Starlev
     * @reason Uses direct MemoryUtil memory access for direct buffers instead of
     * ByteBuffer.getFloat() overhead, and uses pre-calculated position offsets
     * instead of per-quad vertex format lookups.
     */
    @Overwrite
    private static CompactVectorArray unpackQuadCentroids(ByteBuffer bytebuffer, int i, VertexFormat vertexformat) {
        int posOffset = vertexformat.getOffset(VertexFormatElement.POSITION);
        if (posOffset == -1) {
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");
        }
        int vertexSize = vertexformat.getVertexSize();
        int quadSize = vertexSize * 4;
        int quadsCount = i / 4;
        CompactVectorArray compactvectorarray = new CompactVectorArray(quadsCount);
        if (bytebuffer.isDirect()) {
            long address = MemoryUtil.memAddress(bytebuffer);
            for (int quad = 0; quad < quadsCount; quad++) {
                long addr0 = address + (long) quad * (long) quadSize + (long) posOffset;
                long addr2 = addr0 + (long) vertexSize * 2L;
                float f0 = MemoryUtil.memGetFloat(addr0);
                float f1 = MemoryUtil.memGetFloat(addr0 + 4L);
                float f2 = MemoryUtil.memGetFloat(addr0 + 8L);
                float f3 = MemoryUtil.memGetFloat(addr2);
                float f4 = MemoryUtil.memGetFloat(addr2 + 4L);
                float f5 = MemoryUtil.memGetFloat(addr2 + 8L);
                compactvectorarray.set(quad, (f0 + f3) * 0.5f, (f1 + f4) * 0.5f, (f2 + f5) * 0.5f);
            }
        } else {
            FloatBuffer floatbuffer = bytebuffer.asFloatBuffer();
            int floatsPerVertex = vertexSize / 4;
            int floatsPerQuad = floatsPerVertex * 4;
            int posFloatOffset = posOffset / 4;
            for (int quad = 0; quad < quadsCount; quad++) {
                int index0 = quad * floatsPerQuad + posFloatOffset;
                int index2 = index0 + floatsPerVertex * 2;
                float f0 = floatbuffer.get(index0);
                float f1 = floatbuffer.get(index0 + 1);
                float f2 = floatbuffer.get(index0 + 2);
                float f3 = floatbuffer.get(index2);
                float f4 = floatbuffer.get(index2 + 1);
                float f5 = floatbuffer.get(index2 + 2);
                compactvectorarray.set(quad, (f0 + f3) * 0.5f, (f1 + f4) * 0.5f, (f2 + f5) * 0.5f);
            }
        }
        return compactvectorarray;
    }
    //?}
}
