# Performance Presets

Guide for advanced users. Ready-to-paste blocks for `config/lomka-mixins.properties` copy the whole block you need, no extra files required. Missing keys stay `enabled`, unknown keys are ignored (wrong version or typo).

## FPS Preset

```properties
# Preset higher FPS for config/lomka-mixins.properties (just copy and paste)
# Effect of disabling: -static memory/caches/DirectBuffer, BUT +allocations/+GC stutter

# 1.21.6+ | Animation ticking/batched uploads on 1.21.11+; no active patches on 1.21.6-1.21.10. Disable = vanilla animation path
net.minecraft.client.renderer.texture.MixinTextureAtlas=false
# all | x4 growth + warm 16384 indices - holds extra VRAM. Disable = -VRAM, +mid-frame reallocs
com.mojang.blaze3d.systems.MixinAutoStorageIndexBuffer=false
# 1.21+ | 8MB mesh growth steps. Disable = -reserve, +frequent resizes
com.mojang.blaze3d.vertex.MixinByteBufferBuilder=false
# 1.21.6-26.1 | Caches full-buffer slice. Disable = +alloc per uniform
com.mojang.blaze3d.buffers.MixinGpuBuffer=false
# 1.20.1-26.2 | Pose pool + AxisPoseRotate. Disable = +new Pose per push
com.mojang.blaze3d.vertex.MixinPoseStack=false
# 1.21-1.21.11 | Scratch Matrix3f in mulPose. Disable = +new Matrix3f per call
com.mojang.blaze3d.vertex.MixinPose=false
# 1.21+ | Growing DirectBuffer for streaming. Disable = +buffer recreations
net.minecraft.client.sounds.MixinChunkedSampleByteBuf=false
# 1.21+ | Reuses ogg containers. Disable = +alloc on decode
net.minecraft.client.sounds.MixinJOrbisAudioStream=false
# all | Passes sound positions as primitives via alSource3f. Disable = +float[3] per position update
com.mojang.blaze3d.audio.MixinChannel=false
# 1.21+ | Caches listener orientation. Disable = +arrays per frame
com.mojang.blaze3d.audio.MixinListener=false

# all | Caches AABB list for raycast every frame. Disable = +alloc in clip()
net.minecraft.world.phys.shapes.MixinVoxelShape=false
# all | Reuses index lists + bulk BitSet.set. Disable = +lists
net.minecraft.world.phys.shapes.MixinBitSetDiscreteVoxelShape=false
# all | No double[1] box + no AABB.move() allocs. Disable = +garbage on traces
net.minecraft.world.phys.MixinAABB=false
# 1.21+ | Caches the status progression list with volatile publication. Disable = vanilla list construction
net.minecraft.world.level.chunk.status.MixinChunkStatus=false
# all | SpatialLongSet 4x4x4 instead of LongLinkedOpenHashSet 512. Disable = +rehash on chunk load
net.minecraft.world.level.lighting.MixinLeveledPriorityQueue=false
# all | Reuses LongConsumer instead of per-block lambda. Disable = +lambdas
net.minecraft.world.level.lighting.MixinLayerLightSectionStorage=false

# 1.21.11+ | Int2Float cache (codepoint<<1|bold). Disable = +glyph lookups
net.minecraft.client.gui.font.MixinSource=false
# 1.21.11+ | Cached advance per glyph
net.minecraft.client.gui.MixinGlyphSource=false
# 1.21.11+ | Routes text width via cache
net.minecraft.client.gui.MixinFont=false
# 1.21.11+ | Invalidates cache on rebuild
net.minecraft.client.gui.font.MixinFontSet=false

# all | Reuses inputBuf 64KB between packets. Disable = +alloc per packet, but -64KB hold per channel
net.minecraft.network.MixinCompressionEncoder=false
# 1.21+ | Direct inflate path. Disable = +heap ByteBuffer duplicates
net.minecraft.network.MixinCompressionDecoder=false
# all | Indexed list writes, reusable HashMap consumer, sparse EnumSet packing. Disable = vanilla serialization
net.minecraft.network.MixinFriendlyByteBuf=false
# all | Base-to-metadata index after pack filtering + cached logger. Disable = vanilla metadata lookup
net.minecraft.server.packs.resources.MixinFallbackResourceManager=false
# all | Skips TreeMap copies. Disable = +copies
net.minecraft.server.packs.resources.MixinMultiPackResourceManager=false
# all | Caches pack.mcmeta. Disable = +parsing
net.minecraft.server.packs.MixinVanillaPackResources=false
# all | No capturing lambda on submitAsync. Disable = +lambdas (also server)
net.minecraft.util.thread.MixinBlockableEventLoop=false
# all | Lean sparse/continuous maps. Disable = +maps
net.minecraft.util.MixinByIdMap=false
# 1.21+ | Pow2 deque without modulo. Disable = +mod
net.minecraft.util.MixinArrayListDeque=false
# all | LUT[256] FROM_8_BIT + RECIPROCALS. Disable = +divisions, but -1KB static
net.minecraft.util.MixinARGB=false
# 1.21.6-26.2 | Caches ring-buffer getter and uniform slices. Disable = vanilla buffer/slice lookup
net.minecraft.client.renderer.MixinDynamicUniformStorage=false
# 1.21.6+ | Bitwise std140. Disable = +compute
com.mojang.blaze3d.buffers.MixinStd140Builder=false
# 1.21.6+ | Bitwise size calc
com.mojang.blaze3d.buffers.MixinStd140SizeCalculator=false
```

## RAM Preset

```properties
# Preset less RAM and compability for config/lomka-mixins.properties (just copy and paste)
# Effect of disabling: -FPS / +microstutter / +tick time, BUT +compatibility -render artifacts / -hidden logic broke

# all | Dedup viewport/scissor/polygonMode via GlStateCache. Disable = +redundant gl calls
com.mojang.blaze3d.opengl.MixinGlStateManager=false
# all | Invalidates GlStateCache on resize - pair to GlStateManager
com.mojang.blaze3d.platform.MixinWindow=false
# <1.21.4 | glBlitFramebuffer instead of blit-shader quad. Disable = +shader switches
com.mojang.blaze3d.pipeline.MixinRenderTarget=false
# 1.21+ | Direct FMA pose transforms; 1.21.11+ hoists matrix loads and unrolls quad writes. Disable = vanilla transforms
com.mojang.blaze3d.vertex.MixinVertexConsumer=false
# 1.21+ | Fast ARGB->ABGR packing. Disable = +branches
com.mojang.blaze3d.vertex.MixinBufferBuilder=false
# 1.20.1-26.1 | Direct indexed loops instead of lambda fan-out. Disable = +Consumer.accept
com.mojang.blaze3d.vertex.MixinMultiple=false
# 1.20.1-1.21.4 | Caches GL primitive mode (== vs equals). Disable = +VertexFormat.equals
com.mojang.blaze3d.vertex.MixinVertexBuffer=false
# 1.21-26.1 | Reuses mask/offset arrays
com.mojang.blaze3d.vertex.MixinVertexFormat=false
# 1.20.1 only | Caches hashCode
com.mojang.blaze3d.vertex.MixinVertexFormatElement=false
# 1.21.6-26.2 | Flat uniform-location table
com.mojang.blaze3d.opengl.MixinGlProgram=false
# 1.21.6+ | Caches UBO slices
com.mojang.blaze3d.platform.MixinLighting=false
# 1.20.1-1.21, 1.21.6-1.21.11 | Zero-alloc lightmap update
net.minecraft.client.renderer.MixinLightTexture=false
# 26.1+ | Direct UBO write
net.minecraft.client.renderer.MixinLightmap=false
# 26.1+ | Cached vectors
net.minecraft.client.renderer.MixinLightmapRenderStateExtractor=false
# 1.20.1-1.21.11 | Removes Thread.yield in frame loop - stutter vs battery
net.minecraft.client.MixinMinecraft=false
# 1.20.1-1.21.11 | Vector math without allocs
net.minecraft.client.MixinCamera=false
# 1.21.9-1.21.11 | Reuses quaternion
net.minecraft.client.renderer.MixinGameRenderer=false
# 1.21.11-26.2 | Reuses render state per hand
net.minecraft.client.renderer.MixinItemInHandRenderer=false

# all | Flat 1D LUT axis*6+ord + branch-free getApproximateNearest
net.minecraft.core.MixinDirection=false
# all | Stepped counters instead of div/mod in betweenClosed (fill 16^3)
net.minecraft.core.MixinBlockPos=false
# all | Packed section offsets, around-and-at-block traversal + alloc-free spliterator
net.minecraft.core.MixinSectionPos=false
# all | Incremental stepping instead of mod/div in AABB scans
net.minecraft.core.MixinCursor3D=false
# all | int-exact distManhattan instead of float
net.minecraft.core.MixinVec3i=false
# all | Linear scan <=8 instead of Set.copyOf
net.minecraft.core.MixinDirect=false
# all | Caches collisionShape without virtual dispatch
net.minecraft.world.level.block.state.MixinBlockStateBase=false
# all | Volatile face-sturdiness bitmask; refreshes on backing-array replacement
net.minecraft.world.level.block.state.MixinBlockStateBaseCache=false
# all | Inline t + epsilon 1e-7 (both RAM and FPS)
net.minecraft.world.phys.MixinAABB=false
# all | SpatialLongSet - fewer light rehashes
net.minecraft.world.level.lighting.MixinLeveledPriorityQueue=false
# 1.20.1-1.21.11 | Caches entity-type names for profiler
net.minecraft.client.multiplayer.MixinClientLevel=false
# 1.21.11+ | Fewer maps on block-model bake
net.minecraft.client.resources.model.MixinBuilder=false
# 1.20.1-1.21.11 | Caches material hashCode
net.minecraft.client.resources.model.MixinMaterial=false
# 1.21.9+ | Indexed loop instead of Iterator
net.minecraft.client.model.MixinModel=false
# all | Caches AABB - both RAM and FPS (crosshair each frame)
net.minecraft.world.phys.shapes.MixinVoxelShape=false
# all | Bulk BitSet.set
net.minecraft.world.phys.shapes.MixinBitSetDiscreteVoxelShape=false

# all | numberOfLeadingZeros ceillog2, fma lerp, rotLerpRad O(1), hsvToArgb
net.minecraft.util.MixinMth=false
# all | LUT + reciprocal multiply (both RAM and FPS)
net.minecraft.util.MixinARGB=false
# all | Caches OS/arch, array shuffle, URI checks
net.minecraft.util.MixinUtil=false
# all | Plain-field LCG without CAS/volatile
net.minecraft.world.level.levelgen.MixinLegacyRandomSource=false
# all | Lean id maps (both RAM and FPS)
net.minecraft.util.MixinByIdMap=false
```