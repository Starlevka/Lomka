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

(AI generated changelog)