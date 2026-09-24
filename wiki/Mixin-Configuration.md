# Mixin Configuration

This page documents every optimization patch Lomka applies, and how to toggle them
at runtime via `config/lomka-mixins.properties`.

## How it works

The config file is created empty on first launch. Every line you add forces a single
patch on or off:

```properties
# fully qualified mixin name=true|false
net.minecraft.core.MixinCursor3D=false
```

Rules:

- **Missing keys always mean enabled.** You only need entries for patches you want to
  turn OFF. New mixins added by an update work automatically without editing the file.
- Key names are flexible: the fully qualified mixin name, the name without the
  `lomka.starl.mixins.` prefix, or just the simple class name (`MixinCursor3D`) all
  resolve to the same patch.
- Only `true` / `1` enable. Anything else (including typos like `flase`) disables the
  patch; non-boolean garbage is reported in the log.
- Keys that do not exist in your installed build are ignored and reported as unknown.
- Disabled patches are summarized at startup:
  `[Lomka/MixinConfig] Disabled 2 mixin(s): MixinCursor3D, MixinFont`
- Deleting the file restores the all-enabled default on next launch.

**Warning:** disabling patches reduces performance and may change behavior. The
`accessor.*` entries are internal access bridges used by other Lomka mixins keep
them enabled.

## Versions

Supported game versions: **1.19.2 · 1.20-1.20.1 · 1.21-1.21.1 · 1.21.4 · 1.21.6-1.21.8 · 1.21.9-1.21.10 · 1.21.11 · 26.1-26.1.2 · 26.2 · 26.3**

## Shared helpers

Only two helpers live outside mixins in `lomka.starl.utils`; since 0.5.x the former
pure-math/LUT classes (MthCalcs, ColorCalcs, WorldCalcs, VertexMaths, ColorsCache,
ClockWiseCache) were inlined directly into the mixins that used them:
- `cache/GlStateCache` — GL state dedup, shared by `MixinGlStateManager`, reset by `MixinWindow` on resize
- `math/AxisPoseRotate` — PoseStack scratch math for `MixinPoseStack` / `MixinPose`

In the tables below, **Versions** shows which builds contain the patch: `all` means
every supported version, `1.21.6+` means that version and newer, and ranges like
`1.21.6-1.21.11` are inclusive over the supported list above. A key outside your game
version simply does not exist in your jar the config loader ignores it with an
`unknown key` log line. Nothing breaks.

## Audio mixins

| Key | Description | Versions |
|---|---|---|
| `com.mojang.blaze3d.audio.MixinChannel` | Passes sound positions as primitives instead of allocating `float[3]` | all |
| `com.mojang.blaze3d.audio.MixinListener` | Reuses a cached orientation array for listener updates | 1.21+ |
| `net.minecraft.client.sounds.MixinJOrbisAudioStream` | Reuses ogg decode output containers during streamed audio | 1.21+ |
| `net.minecraft.client.sounds.MixinChunkedSampleByteBuf` | Single growing buffer for streamed sounds | 1.21+ |

## Blaze3D mixins

| Key | Description | Versions |
|---|---|---|
| `com.mojang.blaze3d.buffers.MixinGpuBuffer` | Caches the full-buffer slice instead of allocating per call | 1.21.6-26.1 |
| `com.mojang.blaze3d.buffers.MixinStd140Builder` | Bitwise std140 alignment and absolute writes when building uniform blocks | 1.21.6+ |
| `com.mojang.blaze3d.buffers.MixinStd140SizeCalculator` | Bitwise std140 alignment for uniform size math | 1.21.6+ |
| `com.mojang.blaze3d.opengl.MixinGlProgram` | Flat uniform-location table for faster per-draw uniform lookup | 1.21.6-26.2 |
| `com.mojang.renderpearl.backend.opengl.MixinFrameBufferCache` | Reuses a probe CacheKey + int[] to resolve FBO hits with zero allocation | 26.3 |
| `com.mojang.renderpearl.backend.opengl.MixinFrameBufferCacheKey` | Duck for in-place probe key reset (`lomka$reset`) | 26.3 |
| `com.mojang.blaze3d.opengl.MixinGlStateManager` | Skips redundant viewport/scissor/polygonMode driver calls via `cache/GlStateCache` | all |
| `com.mojang.blaze3d.pipeline.MixinRenderTarget` | Hardware blit-to-screen present instead of the blit-shader quad; zero allocations, no mid-frame program switch | <1.21.4 |
| `com.mojang.blaze3d.platform.MixinWindow` | Invalidates `cache/GlStateCache` on window/framebuffer resize | all |
| `com.mojang.blaze3d.platform.MixinLighting` | Caches light UBO slices; skips unchanged GPU uploads | 1.21.6+ |
| `com.mojang.blaze3d.systems.MixinAutoStorageIndexBuffer` | Grows the shared index buffer x4 with a warm start; fewer mid-frame stalls | all |
| `com.mojang.blaze3d.vertex.MixinPoseStack` | Pooled matrix-stack push/pop and sparse rotate via `math/AxisPoseRotate`; on 1.19.2 (Mojang math) per-stack scratch matrices instead of per-call allocations | 1.19.2-26.2 |
| `com.mojang.blaze3d.vertex.MixinPose` | Scratch matrix reuse in mulPose for display entities | 1.21-1.21.11 |
| `com.mojang.blaze3d.vertex.MixinBufferBuilder` | Fast ARGB-to-ABGR color packing while feeding vertices | 1.21+ |
| `com.mojang.blaze3d.vertex.MixinByteBufferBuilder` | Inlined `reserve` fast path and 8 MB growth steps for mesh building buffers (low priority, stays compatible with VulkanMod) | 1.21+ |
| `com.mojang.blaze3d.vertex.MixinVertexConsumer` | Direct FMA pose transforms; on 1.21.11+, bulk quad writes hoist matrix loads and unroll all four vertices | 1.21+ |
| `com.mojang.blaze3d.vertex.MixinVertexFormat` | Reuses precomputed mask/offset arrays on the hot path | 1.21-26.1 |
| `com.mojang.blaze3d.vertex.MixinMultiple` | Direct indexed loops for multi-consumer fan-out instead of per-vertex lambdas | 1.19.2-26.1 |
| `com.mojang.blaze3d.vertex.MixinVertexBuffer` | Caches the GL primitive mode per buffer | 1.19.2-1.21.4 |
| `com.mojang.blaze3d.vertex.MixinVertexFormatElement` | Precomputed hashCode for format map lookups | 1.19.2-1.20.1 |
| `com.mojang.blaze3d.vulkan.MixinDirect` | Persistent mapping for host-visible Vulkan buffers | 26.2+ |

## GUI mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.gui.font.MixinFontSet` | Clears cached glyph advances on font rebuild so widths never go stale | 1.21.9+ |
| `net.minecraft.client.gui.font.MixinSource` | Int2Float advance cache for glyph sources | 1.21.9+ |
| `net.minecraft.client.gui.MixinFont` | Routes text width lookups through cached glyph advances | 1.21.9+ |
| `net.minecraft.client.gui.MixinGlyphSource` | Adds a cached per-codepoint advance lookup to glyph sources | 1.21.9+ |
| `net.minecraft.client.gui.MixinStringRenderOutput` | One-slot FontSet memo per laid-out string on the legacy text sink | 1.19.2-1.21.8 |

## Models mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.resources.model.MixinBuilder` | Fewer allocations and map lookups during block-model baking | 1.21.11+ |
| `net.minecraft.client.resources.model.MixinMaterial` | Caches the material hash instead of allocating an Object[] per sprite lookup | 1.19.2-1.21.11 |
| `net.minecraft.client.model.MixinModel` | Indexed loop instead of Iterator in resetPose; no per-entity allocation | 1.21.9+ |

## Animation mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.animation.MixinKeyframeAnimation` | Reuses the scratch vector that 1.21.9 made per-call (24 B/frame, measured) and skips zero-scale clips on the per-frame animation path; the iterator, the `ifStarted` Consumer and the search callback all measure 0 B/frame, so they stay vanilla | 1.21.6+ |
| `net.minecraft.client.animation.MixinEntry` | Bridge to the private `Entry` record through the `IKeyframeAnimationEntry` duck, so `MixinKeyframeAnimation` can hand it its own scratch vector; the search is vanilla's (an inlined replica bought 0 B/frame and was ~9% slower per search) | 1.21.6+ |

The zero-scale skip is value-identical to vanilla for every input; two bit-level corners are
pinned by fuzzing and accepted. A pose component already holding `-0.0F` is normalized to
`+0.0F` by vanilla's additive zero write (sign of zero only, the skip keeps `-0.0F`), and
duplicate head keyframe timestamps hit exactly (`seconds == ts[0] == ts[1]`) let vanilla write
NaN where the skip does not; such content is broken at any scale, so it is unreachable on
well-formed animations. Nothing else diverges, not at the bit level.

## Client rendering and logic mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.multiplayer.MixinClientLevel` | Caches entity-type names used by the tick profiler | 1.19.2-1.21.11 |
| `net.minecraft.client.renderer.MixinDynamicUniformStorage` | Caches ring-buffer getter and reuses uniform slice records | 1.21.6-26.2 |
| `net.minecraft.client.renderer.MixinDynamicGpuDataStorageMapped` | `renderpearl` successor: caches `currentBuffer()` and dedupes `GpuBufferSlice` for `DynamicGpuData` | 26.3 |
| `net.minecraft.client.renderer.MixinGameRenderer` | Reuses the camera render-state quaternion instead of per-frame allocation | 1.21.9-1.21.11 |
| `net.minecraft.client.renderer.MixinItemInHandRenderer` | Reuses one render state per hand slot instead of allocating every frame | 1.21.11-26.2 |
| `net.minecraft.client.renderer.MixinLightmap` | Zero-allocation direct UBO writing for lightmap updates | 26.1+ |
| `net.minecraft.client.renderer.MixinLightmapRenderStateExtractor` | Cached vectors avoid per-frame allocations in lightmap state extraction | 26.1+ |
| `net.minecraft.client.renderer.MixinLightTexture` | Zero-allocation lightmap texture updates | 1.20.1-1.21 · 1.21.6-1.21.11 |
| `net.minecraft.client.renderer.texture.MixinTextureAtlas` | Animation ticking and batched render-pass uploads with a cached GPU device on 1.21.11+; registered but has no active patches on 1.21.6-1.21.10 | 1.21.6+ |
| `net.minecraft.client.MixinMinecraft` | Removes `Thread.yield()` from the frame loop | 1.19.2-1.21.11 |
| `net.minecraft.client.MixinCamera` | Vector-based camera math without temporary allocations | 1.19.2-1.21.11 |

## World logic mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.core.MixinCursor3D` | Incremental voxel stepping instead of div/mod in AABB scans | all |
| `net.minecraft.core.MixinDirect` | Linear scan for tiny holder sets instead of building a Set | all |
| `net.minecraft.core.MixinSectionPos` | Fast packed section offsets, direct around-and-at-block traversal, and allocation-free stream traversal | all |
| `net.minecraft.core.MixinVec3i` | Raw-Vec3i `get(Axis)` via enum switch and int-exact `distManhattan` | all |
| `net.minecraft.core.MixinDirection` | Direction rotation via an inlined flat LUT instead of nested enum-switch; branch-free `getApproximateNearest` on 1.21.4+ | all |
| `net.minecraft.core.MixinBlockPos` | Incremental stepped counters in `betweenClosed` instead of div/mod per block; byte-identical X→Y→Z order | all |
| `net.minecraft.world.level.block.state.MixinBlockStateBase` | Returns cached collision shapes, bypassing virtual dispatch | all |
| `net.minecraft.world.level.block.state.MixinBlockStateBaseCache` | Caches face-sturdiness checks in a volatile long bitmask, refreshing it when the backing array is replaced | all |
| `net.minecraft.world.level.chunk.status.MixinChunkStatus` | Caches the status progression list with volatile publication for worldgen threads | 1.21+ |
| `net.minecraft.world.level.chunk.MixinPalettedContainer` | Uniform-section fast path via `volatile Data` identity (zero-bit palette); also answers the `IPalettedContainer` uniform-value probe used by `MixinHeightmap` | all |
| `net.minecraft.world.level.levelgen.MixinHeightmap` | Chunk priming without the per-Y `ChunkAccess#getBlockState` walk and the per-column scratch state (a fresh iterator per column in vanilla up to 26.2; 26.3 allocates nothing per column but still walks Y by Y): flat arrays, active-type bitmask, section-local scans and O(1) resolution of uniform sections | all |
| `net.minecraft.world.level.MixinLevel` | `ThreadLocal` scratch list for `getEntities(Entity,AABB,Predicate)` — hottest entity query | all |
| `net.minecraft.world.level.levelgen.MixinLegacyRandomSource` | Plain-field RNG draw instead of a CAS per call; seed installation keeps the vanilla thread guard. Concurrent misuse of `next` no longer throws. | all |
| `net.minecraft.world.level.levelgen.MixinXoroshiroRandomSource` | Reseeds the live Xoroshiro generator in place instead of installing a fresh object on every `setFeatureSeed`/`setDecorationSeed` (decoration reseeds once per structure and per placed feature, per chunk); the drawn sequence stays identical | all |
| `net.minecraft.world.phys.shapes.MixinBitSetDiscreteVoxelShape` | Reusable index lists in shape merge loops; `join` accumulates contiguous bit runs into bulk `BitSet.set(from, to)` writes | all |
| `net.minecraft.world.phys.shapes.MixinVoxelShape` | Caches the computed AABB list so raycasts (crosshair `clip()` runs every frame) stop allocating per call | all |
| `net.minecraft.world.phys.MixinAABB` | Allocation-free `clip` paths: no `double[1]` box, no per-iteration `AABB.move` in the `Iterable` variant | all |
| `net.minecraft.world.level.lighting.MixinLeveledPriorityQueue` | Packs light queue entries into SpatialLongSet | 1.20+ |
| `net.minecraft.world.level.lighting.MixinLayerLightSectionStorage` | Single reusable consumer instead of per-block lambda allocation | 1.20+ |

## Network mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.network.MixinFriendlyByteBuf` | Indexed list writes, reusable BiConsumer for HashMap writes, and sparse EnumSet bit packing/unpacking without BitSet temporaries | all |
| `net.minecraft.network.MixinCompressionEncoder` | Reuses the input buffer between packet compressions | all |
| `net.minecraft.network.MixinCompressionDecoder` | Direct-buffer inflate path for compressed packets | 1.21+ |

## Resource packs mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.server.packs.resources.MixinFallbackResourceManager` | Builds a base-to-metadata index after pack filtering to avoid per-file metadata identifier allocations; caches the logger | 1.20+ |
| `net.minecraft.server.packs.resources.MixinMultiPackResourceManager` | Skips redundant TreeMap copies with one namespace manager | all |
| `net.minecraft.server.packs.MixinVanillaPackResources` | Fast namespace/path resolution and cached pack.mcmeta | 1.20+ |

## Utils mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.util.thread.MixinBlockableEventLoop` | No captured-lambda allocation on async task submission (`main` - all environments) | all |
| `net.minecraft.util.MixinLightCoordsUtil` | Early-out for zero-emission light coordinates | 26.1+ |
| `net.minecraft.util.MixinByIdMap` | Leaner sparse/continuous id maps | 1.20+ |
| `net.minecraft.util.MixinMth` | numberOfLeadingZeros-based ceillog2 and similar math shortcuts | all |
| `net.minecraft.util.MixinARGB` | Flat LUT lookup instead of float division in color helpers | all |
| `net.minecraft.util.MixinUtil` | Cached OS/arch detection, array-backed shuffles, allocation-free URI scheme checks and single-step codepoint offsets | all |
| `net.minecraft.util.MixinArrayListDeque` | Power-of-two deque indexing without modulo | 1.21+ |


## Accessor mixin

| Key | Description | Versions |
|---|---|---|
| `accessor.InvokerBlockBehaviour` | Internal access bridge used by MixinBlockStateBase | all |
