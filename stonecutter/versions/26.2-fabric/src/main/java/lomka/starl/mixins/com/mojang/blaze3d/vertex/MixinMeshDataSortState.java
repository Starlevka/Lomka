package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.IndexType;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.blaze3d.vertex.MeshData$SortState")
public class MixinMeshDataSortState {

    @Shadow @Final private CompactVectorArray centroids;
    @Shadow @Final private IndexType indexType;

    @Overwrite
    public ByteBufferBuilder.Result buildSortedIndexBuffer(ByteBufferBuilder bytebufferbuilder, VertexSorting vertexsorting) {
        int[] sortedIndices = vertexsorting.sort(this.centroids);
        int quadCount = sortedIndices.length;
        long addr = bytebufferbuilder.reserve(quadCount * 6 * this.indexType.bytes);

        if (this.indexType == IndexType.SHORT) {
            for (int k = 0; k < quadCount; ++k) {
                int l = sortedIndices[k] * 4;
                MemoryUtil.memPutShort(addr,      (short) l);
                MemoryUtil.memPutShort(addr + 2,  (short) (l + 1));
                MemoryUtil.memPutShort(addr + 4,  (short) (l + 2));
                MemoryUtil.memPutShort(addr + 6,  (short) (l + 2));
                MemoryUtil.memPutShort(addr + 8,  (short) (l + 3));
                MemoryUtil.memPutShort(addr + 10, (short) l);
                addr += 12;
            }
        } else {
            for (int k = 0; k < quadCount; ++k) {
                int l = sortedIndices[k] * 4;
                MemoryUtil.memPutInt(addr,      l);
                MemoryUtil.memPutInt(addr + 4,  l + 1);
                MemoryUtil.memPutInt(addr + 8,  l + 2);
                MemoryUtil.memPutInt(addr + 12, l + 2);
                MemoryUtil.memPutInt(addr + 16, l + 3);
                MemoryUtil.memPutInt(addr + 20, l);
                addr += 24;
            }
        }

        return bytebufferbuilder.build();
    }
}
