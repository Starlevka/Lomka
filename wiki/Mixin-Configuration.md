# Mixin Configuration

This page documents every optimization patch Lomka applies to **Minecraft 1.16.5 - 26.3**, and how to
toggle them at runtime via `config/lomka-mixins.properties`.

For the legacy Forge builds (1.7.10 - 1.12.2) see
[Mixin Configuration (legacy)](https://github.com/Starlevka/Lomka/wiki/Mixin-Configuration-Legacy).

## How it works

The config file is created on first launch with a short notice and no entries. Every line
you add forces a single patch on or off:

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
- Disabled patches are summarized at startup (a warning, like the unknown-key report):
  `[Lomka/MixinConfig] Disabled 2 mixin(s): MixinCursor3D, MixinFont`
- Deleting the file restores the all-enabled default on next launch.

**Warning:** disabling patches reduces performance and may change behavior.

### Patches that have to be toggled as a group

`LomkaMixinPlugin` resolves each patch independently, so disabling one half of these pairs throws
at runtime rather than degrading. Keep both members enabled, or disable both.

| Patch | Requires | Failure if only the first is enabled |
|---|---|---|
| `net.minecraft.client.animation.MixinKeyframeAnimation` | `net.minecraft.client.animation.MixinEntry` | `ClassCastException` when applying an animation |
| `com.mojang.renderpearl.backend.opengl.MixinFrameBufferCache` | `com.mojang.renderpearl.backend.opengl.MixinFrameBufferCacheKey` | `ClassCastException` on any framebuffer cache lookup (26.3) |
| `net.minecraft.world.level.block.state.MixinBlockStateBase` | `accessor.InvokerBlockBehaviour` | `ClassCastException` on the collision-shape fallback |
| `net.minecraft.client.renderer.texture.MixinTextureAtlas` | `accessor.AbstractTextureAccessor` | `ClassCastException` on the texture read (1.21.6 - 1.21.10 only) |
| `net.minecraft.client.renderer.texture.MixinTextureAtlasMip` | `net.minecraft.client.renderer.texture.MixinSpriteContents` | `ClassCastException` after an atlas upload |
| `net.minecraft.client.gui.MixinFont` | `net.minecraft.client.gui.MixinGlyphSource` (or `font.MixinSource`) | `ClassCastException` on every text width lookup |
| `net.minecraft.client.gui.font.MixinFontSet` | `net.minecraft.client.gui.MixinGlyphSource` (or `font.MixinSource`) | `ClassCastException` on font reload |

`MixinHeightmap` is the counter-example that works: it probes `IPalettedContainer` with an
`instanceof` and keeps a correct vanilla fallback, so the dependency there is optional.

`accessor.*` entries are internal access bridges used by other Lomka patches — keep them enabled
unless the row above tells you otherwise.

## Versions

Supported game versions: **1.16.5 · 1.18.2 · 1.19.2 · 1.20-1.20.1 · 1.21-1.21.1 · 1.21.4 · 1.21.6-1.21.8 · 1.21.9-1.21.10 · 1.21.11 · 26.1-26.1.2 · 26.2 · 26.3**

In the tables below, **Versions** shows which builds contain the patch: `all` means
every supported version, `1.21.6+` means that version and newer, and ranges like
`1.21.6-1.21.11` are inclusive over the supported list above. A key outside your game
version simply does not exist in your jar — the config loader ignores it with an
`unknown key` log line. Nothing breaks.

**1.16.5** (Fabric only) and **1.18.2** (Fabric + Forge) carry a reduced patch set - **24** and **29**
patches. What they ship, in full:

- **1.16.5** — 24 patches. Client: `MixinChannel`, `MixinKey`, `MixinSoundEngine`,
  `MixinMaterial`, `MixinMinecraft`, `MixinRenderTarget`. Common: `MixinMth`, `MixinARGB`,
  `MixinUtil`, `MixinBlockPos`, `MixinCursor3D`, `MixinVec3i`, `MixinSectionPos`,
  `MixinIntArrayTag`, `MixinLongArrayTag`, `MixinCompressionEncoder`, `MixinBlockStateBaseCache`,
  `MixinHeightmap`, `MixinLayerLightSectionStorage`, `MixinBitSetDiscreteVoxelShape`,
  `MixinDiscreteVoxelShape`, `MixinVoxelShape`, `MixinAABB`, `MixinChunkStatus`.
- **1.18.2** — the 1.16.5 set plus `MixinAutoStorageIndexBuffer`, `MixinDirection`,
  `MixinBlockStateBase`, `accessor.InvokerBlockBehaviour` and `MixinPalettedContainer` (29 total).

## Shared helpers

Non-mixin code lives in three places:

- `lomka.starl.utils` — five shared helpers outside mixins (they live outside the mixin
  package on purpose: a class inside a configured mixin package cannot be loaded by the
  loader). The former pure-math/LUT classes (MthCalcs, ColorCalcs, WorldCalcs, VertexMaths,
  ColorsCache, ClockWiseCache) were inlined directly into the mixins that used them back in 0.5.x.
  - `cache/UniformPaletteCache` — immutable `(Data, value)` holder behind `MixinPalettedContainer`'s uniform fast path
  - `constants/DirectionTables` — flat clockwise/counter-clockwise `Direction` LUTs, read by `MixinDirection`
  - `math/AxisPoseRotate` — PoseStack scratch math for `MixinPoseStack` / `MixinPose`
  - `math/BlockStepSweep` — the DDA block sweep behind `MixinBlockGetter`
  - `pack/ResourceEntry` — resolved pack entry (source, byte supplier, fallback index) for `MixinFallbackResourceManager`
- `lomka.starl.duck` — bridge interfaces implemented by one patch so another can
  call into the vanilla class without an accessor:
  - `IBitSetDiscreteVoxelShape` (`lomka$storage` / `lomka$index` / `lomka$setBounds`) — implemented and consumed by `MixinBitSetDiscreteVoxelShape`
  - `IDiscreteVoxelShape` (`lomka$boxes`) — the cached box decomposition by `MixinDiscreteVoxelShape`, replayed by `MixinVoxelShape`
  - `IFrameBufferCacheKey` (`lomka$reset`) — implemented by `MixinFrameBufferCacheKey`, consumed by `MixinFrameBufferCache` for the zero-alloc probe key
  - `IGlyphSource` (`getAdvance` + `lomka$clear`) — advance cache behind `MixinSource` / `MixinGlyphSource`, read by `MixinFont`, cleared by `MixinFontSet`
  - `IHeightmap` (`lomka$setHeightAt`) — implemented and consumed by `MixinHeightmap` (`primeHeightmaps`)
  - `IKeyframeAnimationEntry` (`lomka$apply` with caller scratch vector) — implemented by `MixinEntry`, consumed by `MixinKeyframeAnimation`
  - `IPalettedContainer` (`lomka$uniformValue`) — implemented by `MixinPalettedContainer`, consumed by `MixinHeightmap` for the uniform-section check
  - `ISpriteContents` (`lomka$releaseUnusedMipLevels`) — implemented by `MixinSpriteContents`, consumed by `MixinTextureAtlasMip`
- `lomka.starl.config.LomkaMixinPlugin` — the `plugin` from `lomka.mixins.json5`; reads
  `config/lomka-mixins.properties` and answers `shouldApplyMixin` (never imports Minecraft classes).
  It also sets `joml.fastmath`, `joml.sinLookup` and `joml.sinLookup.bits=11` before JOML
  initializes, unless you set them yourself with `-D`.

## Audio mixins

| Key | Description | Versions |
|---|---|---|
| `com.mojang.blaze3d.audio.MixinChannel` | Passes sound positions as primitives instead of allocating `float[3]` | all |
| `com.mojang.blaze3d.audio.MixinListener` | Reuses cached orientation buffers for listener updates | 1.21+ |
| `net.minecraft.client.sounds.MixinJOrbisAudioStream` | Reuses the two ogg decode output containers (`float[1][][]`, `int[channels]`) instead of allocating them per decoded chunk | 1.21+ |
| `net.minecraft.client.sounds.MixinChunkedSampleByteBuf` | One growing native direct buffer instead of per-chunk reallocations during audio streaming | 1.21+ |
| `net.minecraft.client.sounds.MixinSoundEngine` | Serves an already acquired channel handle from a small pool so playing a sound no longer waits for the sound thread | all |

## Blaze3D mixins

| Key | Description | Versions |
|---|---|---|
| `com.mojang.blaze3d.buffers.MixinGpuBuffer` | Caches the full-buffer slice instead of allocating one per `slice()` call | 1.21.6-26.1 |
| `com.mojang.blaze3d.buffers.MixinStd140Builder` | Bitwise std140 alignment (power-of-two fast path) instead of a division chain, when writing uniform blocks | 1.21.6+ |
| `com.mojang.blaze3d.buffers.MixinStd140SizeCalculator` | The same alignment shortcut for the size-calculating half of the uniform builder | 1.21.6+ |
| `com.mojang.blaze3d.opengl.MixinGlProgram` | Replaces the per-draw uniform-name map with a fastutil open-addressing map | 1.21-26.2 |
| `com.mojang.renderpearl.backend.opengl.MixinFrameBufferCache` | Reuses a probe CacheKey + exact-length `int[]` to resolve FBO hits with zero allocation | 26.3 |
| `com.mojang.renderpearl.backend.opengl.MixinFrameBufferCacheKey` | Duck for in-place probe key reset (`lomka$reset`) | 26.3 |
| `com.mojang.blaze3d.pipeline.MixinRenderTarget` | Hardware `glBlitFramebuffer(NEAREST)` present instead of the blit-shader fullscreen quad; zero allocations, no mid-frame program switch | <1.21.4 |
| `com.mojang.blaze3d.platform.MixinLighting` | Caches one UBO slice per light entry and skips the redundant GPU upload when the light level did not change | 1.21.6+ |
| `com.mojang.blaze3d.platform.MixinKey` | Computes the two-element `Objects.hash` result directly, without the temporary `Object[]` and the boxed key code, for keybinding lookups | all |
| `com.mojang.blaze3d.systems.MixinAutoStorageIndexBuffer` | Grows the shared index buffer x4 with a 16k-index warm start; fewer mid-frame stalls | 1.18.2+ |
| `com.mojang.blaze3d.vertex.MixinPoseStack` | Sparse X/Y/Z quaternion rotate via `math/AxisPoseRotate` instead of a full matrix multiply; on 1.19.2 (Mojang math) per-stack scratch matrices instead of per-call allocations | 1.19.2-26.2 |
| `com.mojang.blaze3d.vertex.MixinPose` | Thread-local scratch matrix reuse in `mulPose` for orthonormal poses (display entities) | 1.21-1.21.11 |
| `com.mojang.blaze3d.vertex.MixinBufferBuilder` | Wider per-vertex stores while feeding vertices (BSWAP/ROR color packing, `memPutInt` color, `memPutLong` UV) | 1.21+ |
| `com.mojang.blaze3d.vertex.MixinByteBufferBuilder` | Inlined `reserve` fast path and 8 MB growth steps for mesh building buffers (low priority, stays compatible with VulkanMod) | 1.21+ |
| `com.mojang.blaze3d.vertex.MixinVertexConsumer` | Scalar transcription of the JOML pose transforms (no temporary vectors); on 1.21.11+, bulk quad writes hoist matrix loads out of the four-vertex loop | 1.21+ |
| `com.mojang.blaze3d.vertex.MixinVertexFormat` | Caches `hashCode` once per instance instead of recomputing the element-mask/offset hash | 1.21-26.1 |
| `com.mojang.blaze3d.vertex.MixinMultiple` | Direct indexed loops for multi-consumer fan-out instead of per-vertex lambdas | 1.19.2-26.1 |
| `com.mojang.blaze3d.vertex.MixinVertexFormatElement` | Direct `switch` for vertex attribute buffer state instead of the per-usage lambda dispatch | 1.19.2-1.20.1 |
| `com.mojang.blaze3d.vertex.MixinUberGpuBuffer` | Walks staged allocations with fastutil's reuse-mode iterator, so the per-entry object is recycled instead of allocated per iteration | 26.2+ |

## GUI mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.gui.font.MixinFontSet` | Clears cached glyph advances on font rebuild so widths never go stale | 1.21.9+ |
| `net.minecraft.client.gui.font.MixinSource` | Synchronized `Int2Float` advance cache for glyph sources (16384 entries, cleared when full) | 1.21.9+ |
| `net.minecraft.client.gui.MixinFont` | Routes text width lookups through cached glyph advances, cutting a lookup layer | 1.21.9+ |
| `net.minecraft.client.gui.MixinGlyphSource` | Adds the cached per-codepoint advance lookup to all glyph sources | 1.21.9+ |

## Models mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.resources.model.MixinBuilder` | Array buckets instead of Guava `ArrayListMultimap` during block model baking, plus a bulk `addAll` transfer on 26.1+ | 1.21.6+ |
| `net.minecraft.client.resources.model.MixinMaterial` | Caches the material hash instead of allocating an `Object[]` per sprite lookup | 1.16.5-1.21.11 |
| `com.mojang.math.MixinTransformation` | Inverts the stored matrix straight into the destination instead of taking the defensive copy first | 1.20+ |
| `net.minecraft.client.model.MixinModel` | Indexed loop instead of `Iterator` in `resetPose`; no per-entity allocation | 1.21.4+ |
| `net.minecraft.client.model.geom.MixinModelPart` | Thread-local scratch matrices for the Euler → matrix → Euler round trip in `rotateBy` (end crystals, player capes) | 1.21.4+ |

## Client rendering and logic mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.renderer.MixinDynamicUniformStorage` | Caches the ring-buffer getter and dedupes immutable `GpuBufferSlice` records for per-draw uniform writes | 1.21.6-26.2 |
| `net.minecraft.client.renderer.MixinDynamicGpuDataStorageMapped` | `renderpearl` successor: same caching for `DynamicGpuData` + `MappableRingBuffer` | 26.3 |
| `net.minecraft.client.renderer.MixinGameRenderer` | Reuses the camera render-state quaternion instead of allocating one per frame | 1.21.9-1.21.11 |
| `net.minecraft.client.renderer.MixinItemInHandRenderer` | Reuses one render state per hand slot instead of allocating one or two every frame | 1.21.11-26.2 |
| `net.minecraft.client.renderer.MixinLightmap` | Direct absolute-offset UBO writes for the lightmap update, no intermediate objects | 26.1+ |
| `net.minecraft.client.renderer.block.MixinBlockModelLighter` | Chunk meshing only: memoises the block reads of smooth lighting (one block's six faces read 90 states over 19 distinct positions) and answers the full-block collision-shape query once per block instead of once per face | 26.1+ |
| `net.minecraft.client.renderer.MixinLightmapRenderStateExtractor` | Short-circuits the vanilla extractor with cached tint vectors, so the lightmap refresh allocates no objects per update | 26.1+ |
| `net.minecraft.client.renderer.MixinLightTexture` | Lightmap update without vanilla's per-column allocations: baked straight into the shared texture on the older path, into the UBO render pass on 1.21.6+ | 1.20.1-1.21 · 1.21.6-1.21.11 |
| `net.minecraft.client.renderer.texture.MixinTextureAtlas` | Indexed animation-frame ticking instead of the per-frame iterator; on 1.21.11+ a batched render-pass upload with a cached GPU device, and no pass created on idle frames | 1.21.6+ |
| `net.minecraft.client.renderer.texture.MixinSpriteContents` | Releases generated sprite mip levels above level 0 after the atlas upload, so they are not kept alive forever | 1.20+ |
| `net.minecraft.client.renderer.texture.MixinTextureAtlasMip` | Triggers that mip release once per atlas upload; the hook point is shared with Sodium and Iris on purpose | 1.20+ |
| `net.minecraft.client.renderer.entity.MixinDisplayRenderer` | Thread-local scratch quaternion for the billboarding math, one allocation saved per display entity per frame | 1.20+ |
| `net.minecraft.client.MixinMinecraft` | Removes `Thread.yield()` from the tick loop | 1.16.5-1.21.11 |
| `net.minecraft.client.MixinCamera` | Frame-scoped near-plane/fluid caches and reused corner buffers, no `NearPlane`/`Vec3`/iterator allocation per frame | 1.19.2-1.21.11 |
| `net.minecraft.client.animation.MixinKeyframeAnimation` | Reuses one scratch `Vector3f` per animation instead of one per `apply()` call (24 B/frame, measured), walks entries by index, and skips zero-scale clips; the search stays vanilla | 1.21.6+ |
| `net.minecraft.client.animation.MixinEntry` | Bridge to the private `Entry` record through the `IKeyframeAnimationEntry` duck, so `MixinKeyframeAnimation` can hand it its own scratch vector | 1.21.6+ |

## World logic mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.core.MixinCursor3D` | Incremental voxel stepping instead of div/mod in AABB scans | all |
| `net.minecraft.core.MixinDirect` | Linear scan for holder sets of 8 or fewer elements instead of building a `Set` | 1.19.2+ |
| `net.minecraft.core.MixinSectionPos` | Fast packed section offsets, single-section fast path in `aroundAndAtBlockPos`, and a stream whose `forEachRemaining` steps `Cursor3D` directly | all |
| `net.minecraft.core.MixinVec3i` | `get(Axis)` via a switch instead of `Axis#choose` dispatch, int-exact `distManhattan` (no float round trip), and phi-mix `hashCode` (ZZZank's EfficientHashing) | all |
| `net.minecraft.core.MixinDirection` | Direction rotation via a flat LUT in `constants/DirectionTables` instead of nested enum-switch | 1.18.2+ |
| `net.minecraft.core.MixinBlockPos` | Incremental stepped counters and one reused cursor in `betweenClosed` instead of div/mod plus a fresh `BlockPos` per block; byte-identical X→Y→Z order | all |
| `net.minecraft.world.level.MixinBlockGetter` | Entity block sweep with one reused cursor and no `Optional<Vec3>` box clip; runs on every entity move | 1.21.9+ |
| `net.minecraft.world.level.block.state.MixinBlockStateBase` | Returns the pre-cached collision shape for non-dynamic blocks, bypassing virtual dispatch | 1.18.2+ |
| `net.minecraft.world.level.block.state.MixinBlockStateBaseCache` | Caches face-sturdiness checks in a `volatile long` bitmask (rebuilds itself if another mod swaps the backing array, e.g. FerriteCore) | all |
| `net.minecraft.world.level.chunk.status.MixinChunkStatus` | Caches the static status progression list, which vanilla rebuilds once per generation layer | all |
| `net.minecraft.world.level.chunk.MixinPalettedContainer` | Uniform-section fast path through one volatile-published `(Data, value)` holder, skipping the palette dispatch and bit arithmetic; also answers the `IPalettedContainer` probe used by `MixinHeightmap` | 1.18.2+ |
| `net.minecraft.world.level.levelgen.MixinHeightmap` | Chunk priming without the per-Y `ChunkAccess#getBlockState` walk and per-column scratch state: flat arrays, active-type bitmask, section-local scans and one predicate test per uniform section | all |
| `net.minecraft.world.level.levelgen.MixinLegacyRandomSource` | Plain-field LCG draw instead of a CAS per call; `setSeed` keeps the vanilla thread guard, so only the draw fast path changes. Concurrent misuse of `next` no longer throws. | 1.19.2+ |
| `net.minecraft.world.level.levelgen.MixinXoroshiroRandomSource` | Reseeds the live Xoroshiro generator in place instead of installing a fresh object on every `setFeatureSeed`/`setDecorationSeed`; the drawn sequence stays identical | 1.19.2+ |
| `net.minecraft.world.phys.shapes.MixinBitSetDiscreteVoxelShape` | Reusable capture arrays in shape merge loops, and contiguous bit runs written with one bulk `BitSet.set(from, to)` (only for a stock `BitSet` — see the note below) | all |
| `net.minecraft.world.phys.shapes.MixinDiscreteVoxelShape` | Memoizes the greedy box decomposition as cell indices, computed once per shape through vanilla's own `IntLineConsumer` | all |
| `net.minecraft.world.phys.shapes.MixinVoxelShape` | Replays that cached decomposition in `forAllBoxes` instead of re-decomposing, and caches the `toAabbs` list so raycasts and collision queries stop allocating per call | all |
| `net.minecraft.world.phys.MixinAABB` | Allocation-free `clip` paths: no `double[1]` box, no `getDirection` indirection, no per-iteration `AABB.move` in the `Iterable` variant | all |
| `net.minecraft.world.phys.MixinVec3` | Reciprocal-multiply `normalize` (one division + three multiplications instead of three divisions, near-zero guard preserved) | 1.21.4+ |
| `net.minecraft.world.level.lighting.MixinLayerLightSectionStorage` | One reusable `LongConsumer` instead of a per-block capturing lambda allocation | all |
| `net.minecraft.util.thread.MixinBlockableEventLoop` | Folds the `IS_RUNNING_IN_IDE` guard of `doRunTask` to a constant, so Tracy stops opening a profiler zone per executed task | 1.21.4+ |

## Network mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.network.MixinFriendlyByteBuf` | Indexed list writes and sparse EnumSet bit packing/unpacking without `BitSet` temporaries, plus a pre-sized id list | 1.19.2+ |
| `net.minecraft.network.MixinCompressionEncoder` | Reuses the input buffer between packet compressions, shrinking it again past 64 KB so one huge packet cannot pin memory | all |
| `net.minecraft.network.MixinCompressionDecoder` | Hands the inflater Netty's cached internal NIO buffer view instead of allocating a fresh `ByteBuffer` per compressed packet | 1.21+ |
| `net.minecraft.network.protocol.game.MixinVecDeltaCodec` | Zero-delta fast path returning `Vec3.ZERO` instead of subtract + allocation (CoO port, `priority = 999` defers to CoO) | 1.19.2+ |

## Resource packs mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.server.packs.resources.MixinFallbackResourceManager` | Builds a base-to-metadata index after pack filtering, avoiding a per-file `.mcmeta` string concat and identifier allocation | 1.20+ |
| `net.minecraft.server.packs.resources.MixinMultiPackResourceManager` | Returns the single namespace manager's map directly instead of copying it into a `TreeMap` | 1.19.2+ |
| `net.minecraft.server.packs.MixinVanillaPackResources` | One path resolve per pack root instead of one per segment, cached empty `LinkOption[]`, and cached parsed `pack.mcmeta` metadata | 1.20-26.2 |

## Utils mixins

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.nbt.MixinIntArrayTag` | Bulk array reads on `DataInputStream` (server-side world/player/structure data from disk); vanilla loop for buffer-backed and network inputs | all |
| `net.minecraft.nbt.MixinLongArrayTag` | Bulk array reads on `DataInputStream` (server-side chunk heightmaps, player and structure data from disk); vanilla loop for buffer-backed and network inputs | all |
| `net.minecraft.util.MixinLightCoordsUtil` | Quick exit for blocks that emit no smooth light, without the clamp/repack round trip | 26.1+ |
| `net.minecraft.util.MixinByIdMap` | Leaner sparse/continuous id maps (no wrapper lambda, no per-lookup null check, power-of-two wrap) | 1.20+ |
| `net.minecraft.util.MixinMth` | `numberOfLeadingZeros`-based `ceillog2`/`log2`/smallest-power-of-two, unsigned midpoint in `binarySearch`, exact degree and HSV helpers | all |
| `net.minecraft.util.MixinARGB` | Flat LUT lookup instead of float division in color helpers, exact reciprocal multiply in `alphaBlend`/`multiply`/`scaleRGB` instead of per-channel `/255`, bitwise color averaging | all |
| `net.minecraft.util.MixinUtil` | Zero-alloc `getEpochMillis`/`sanitizeName`, fastutil shuffle specializations, identity index lookups, single-step codepoint offsets and cached `allOf`/`anyOf` | all |
| `net.minecraft.util.MixinArrayListDeque` | Power-of-two deque indexing without modulo, direct O(1) head/tail ops | 1.21+ |
| `net.minecraft.util.random.MixinCompact` | Binary search over cumulative weights instead of `Compact`'s O(n) weight-subtraction scan; 1.3x at 32 entries, 1.9x at 64, 3.2x at 128+ | 1.21.11+ |

## Accessor mixins

| Key | Description | Versions |
|---|---|---|
| `accessor.InvokerBlockBehaviour` | Internal access bridge used by `MixinBlockStateBase` | 1.18.2+ |
| `accessor.AbstractTextureAccessor` | Reads the texture field `MixinTextureAtlas` inherits from `AbstractTexture` | 1.21.6-1.21.10 |
