# Changelog

## [Unreleased]

## [0.4.0] - 2026-08-17

### Performance
- Font glyph widths cached per codepoint/bold pair — text rendering does fewer glyph source lookups. (1.21.11+) **NEW**
- Resource pack loading overhaul:
  - FallbackResourceManager listResources fast-path for `.mcmeta` — eliminates thousands of useless Identifier allocations and map lookups. **NEW**
  - MultiPackResourceManager bypasses redundant secondary TreeMap allocation when only one namespace manager is present. **NEW** 
  - VanillaPackResources fast-path namespace check, zero-allocation path resolution, and cached parsed `pack.mcmeta`. (<26.1) **NEW**
- Block collision shapes now served from the cached shape instead of virtual `getCollisionShape` dispatch. (non-dynamic blocks) **NEW**
- Light queues swapped from `LongLinkedOpenHashSet` to bit-packed `SpatialLongSet` — less heap churn during chunk loading. **NEW**
- Shader uniform lookup via fastutil open-addressing map instead of HashMap. (1.21.6+) **NEW**
- ChunkedSampleByteBuf no longer allocates a fresh native buffer per chunk. (1.21+) **NEW**
- Lighting precomputes a buffer slice per entry and skips redundant GPU UBO writes when the cardinal light type is unchanged. (1.21.6+)
- NativeImage fillRect/copyRect rewritten with bulk MemoryUtil.memCopy — whole-image and whole-row transfers in a single call.
- Std140 builder and size calculator put* methods rewritten with bitwise alignment and absolute indexed writes. (1.21.6+)
- ModelPart rotateBy composes rotations through a single quaternion instead of a full 3x3 matrix multiply.
- ArrayListDeque removes from the front half by advancing head instead of shifting the tail.
- ARGB reciprocal lookup table computed in O(1) per entry instead of an inner loop.

### Bug Fixes
- NativeImage fillRect/copyRect now validate format and bounds explicitly instead of silently corrupting memory.
- Fixed Lightmap and LightmapRenderStateExtractor 26.2 API calls (`mapBuffer`, `getMainCamera`, `getBossOverlay`, `getNightVisionScale`).
- MixinMth clampedLerp is now version-conditional so the correct argument order is used on 1.21.11+ and older.
- Camera 26.1+ near-plane path kept on the lightweight custom structure.

### New
- New duck interfaces: `IBitSetDiscreteVoxelShape`, `IGlyphSource`.
- New accessors: `AccessorCache`, `InvokerBlockBehaviour`.

### Compatibility
- Per-version mixin configs, access wideners and access transformers are now generated from Stonecutter templates (`src/stonecutter/`) instead of committed per-version files.
- Mixin source exclusions are auto-derived from the processed mixin config — no more hand-maintained exclude tables in build scripts.

### Removed
- Removed low-impact mixins: MixinSamplerCache, MixinSortState, MixinQuadrant, MixinItemTransform, MixinMipmapGenerator.
- Removed DiscreteVoxelShapeHelper — replaced with the IBitSetDiscreteVoxelShape duck interface.
- Removed per-version committed `lomka.mixins.json`, `*.accesswidener` and `accesstransformer.cfg` files.

### Changed
- Build system refactored: `build-logic` composite build → `buildSrc` with auto-derived mixin exclusions.
- 16 build variants now (added `1.20.1-fabric` and `1.20.1-forge`).

## [0.3.3] - 2026-08-03

### Performance
- Sampler cache lookup rewritten — no per-call enum comparisons.
- Cursor3D advance rewritten — no divisions/modulos per voxel.
- Tiny holder sets use linear scan instead of Set allocation.
- Lighting coordinate calculations now applied. (26.1+)
- BlockableEventLoop submit runs inline with no future allocations.
- GameRenderer extractCamera removed null-conditional.
- Key inputs hashCode caching simplified.
- Removed Thread.yield() from runTick for lower frame latency. (before it was just commented I dunno why, bruh)

### Bug Fixes
- Fixed NativeImage region fill writing wrong color byte order.
- Fixed SoundEngine skipping camera updates before sound engine loads.
- Reverted Mth power-of-two calculation to vanilla.
- Raised ItemInHandRenderer and LightTexture(Lightmaps) mixins priority to avoid conflicts. [#18](https://github.com/Starlevka/Lomka/issues/18)

### Compatibility
- Light texture and light state extraction now use cancellable injects instead of overwrites.

### Removed
- Removed strict mixins: FaceBakery, SpriteContents, PalettedPermutations.
- Removed commented AccessorQuadCollection.


## [0.3.2] - 2026-07-27

### Performance
- Lightmap pipeline rewritten — less GPU/CPU allocations per frame.
- PoseStack uses pure `mulPose` rotation with cached axis transforms.
- Sound buffer library optimized.
- VertexFormat `hashCode()` cached.
- Lighting coordinate calculations optimized.
- TextureAtlas ported to new rendering API.
- ARGB and Mth math minor optimizations.

### Bug Fixes
- Fixed item in hand rendering desync. ([#15](https://github.com/Starlevka/Lomka/issues/15))
- Fixed mipmap generation issues.
- Fixed Mth math edge cases.

### Removed
- Removed RAM-heavy optimizations (Material and Resource caching).
- Removed incompatible mixins: TextureUtil, MeshData, KeyframeAnimation.


## [0.3.1] - 2026-07-18

### Performance
- Chunks load smoother — faster lighting and biome data access.
- FaceSturdy block state caching.
- Bitset shapes create slightly less garbage.
- Refactored more perfomously game calculations.
- Network packets create less garbage.
- No more per-frame allocation for items held by entity.
- Resource packs now free unused mip levels after loading. Saves 10-50 MB RAM.
- Improved faces building (1.21.11+).

### Bug Fixes
- Fixed a rare crash (`NegativeArraySizeException`) in world loading.
- Fixed lava fog appearing wrong in some camera positions.
- Fixed sound updates being skipped. 
- Fixed game crash with Create Fly ([#10](https://github.com/Starlevka/Lomka/issues/10))

### New
- Added **1.21.4** and **1.21.6-1.21.8** support.

### Removed
- Old low-impact and uncareful optimizations to keep the mod cleaner.

## [0.3.0] - 2026-06-29

Main version: 1.21.11.

All optimizations below apply to it. On other versions (1.21, 1.21.9, 26.1, 26.2) some changes are absent due to Minecraft code differences.

### Removed

- **Entity culling** — the mod no longer checks which entities are off-screen and skips their rendering/ticks. Removed entirely (EntityCulling, BlockEntityCulling, EntityTickCull, Frustum, Octree, SmartLeaves + utilities). Too many bugs, and the FPS gain was inconsistent.
- **Config** — config file, in-game config screen, ModMenu integration. The mod now works out of the box with no settings.
- **OpenGL command cache** — three mixins that cached viewport/scissor/polygonMode calls. Negligible gain.
- **NBT throttling** — thread limiting for NBT read/write + file cache. Complexity wasn't worth it.
- **Platform abstraction layer** — ServiceLoader and PlatformHelper removed. Simpler now.
- **Chunk log spam suppression, crash report guard, translucency resort throttle** — minor stuff.

### Added

The mod was rewritten from scratch. Focus shifted from entity culling to **hot-path acceleration** and **garbage elimination** (less GC pressure = fewer stutters):

- **Block/model rendering** — faster face geometry calculations, model rotations, quaternion/matrix work. Some less allocations and less GC.
- **Buffers & vertices** — direct memory writes via MemoryUtil, compact color packing (2 instructions instead of 7), no more Math.addExact, cached vertex format hashes. All of these run every frame.
- **Camera** — fully reworked: reusable objects, unrolled loops, pre-computed near-plane vectors, cached fluid-in-camera check. Smoother movement.
- **Math** — bitwise operations instead of division, FMA (fused multiply-add) for distance/slerp, pre-computed rotation tables, fast rounding. Small gains that add up every frame.
- **ARGB (colors)** — all color operations (blending, greyscale, alpha) rewritten: no division, pre-computed lookup tables, bit shifts. Helps particles, banners, maps, etc.
- **Audio** — optimized buffer creation, source positioning, OGG decoding. Fewer micro-stutters when sounds load.
- **Textures** — unused mipmap levels get closed (frees VRAM), pre-computed filter weights for MipmapGenerator, palette caching for atlases.
- **Fonts** — first 8192 characters cached in a flat array instead of HashMap. Every time the game renders text — faster.
- **Collections** — ArrayListDeque rewritten from scratch (power-of-two capacity, bitwise masking, fixes a vanilla NegativeArraySizeException bug), ByIdMap uses Int2ObjectOpenHashMap. Faster, fewer allocations.
- **Hashing** — cached toString/hashCode on Identifier/ResourceLocation, Material, Transformation, BakedQuad, InputConstants, VertexFormat, TextureSetup.
- **Modifier keys** — Shift/Ctrl/Alt checked once per tick instead of every hasShiftDown() call.
- **RenderSystem** — forces GL debug flags off (debug mode hurts FPS significantly).
- **EventLoop** — CompletableFuture + LockSupport instead of Thread.yield().
- **Shader uniforms** — cached GPU buffer slices, static constants for white/zero uniforms, skip redundant writes.

### Changed

- **Build system** — old multi-module Gradle (common/fabric/neoforge) → Stonecutter. Shared codebase with version-specific overrides.
- **Package** — `set.starl` → `lomka.starl`.
- **Entry points** — separate LomkaFabric and LomkaNeoForge.
- **Versions** — 1.21.11/Fabric+NeoForge (primary), plus additional variants for 1.21-1.21.1, 1.21.9-1.21.10, 26.1-26.1.2, 26.2.