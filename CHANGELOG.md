# Changelog

Covers the modern variants (1.16.5 - 26.3).
The legacy Forge / Ornithe builds (1.7.10, 1.8.9, 1.12.2) are tracked separately in [CHANGELOG_LEGACY.md](CHANGELOG_LEGACY.md).

## [0.7.0] - 2026-10-10

### Performance
- New `DiscreteVoxelShape` cache: the greedy box decomposition is memoised as cell indices (`MixinDiscreteVoxelShape` + `IDiscreteVoxelShape`) and replayed by `VoxelShape#forAllBoxes`, instead of cloning the whole BitSet and re-decomposing it on every call. `VoxelShape#move` allocates a fresh wrapper each time, so a wrapper-keyed cache is cold on every call, while the decomposition itself is a pure function of the shape bits - boxes are recorded through vanilla's own `IntLineConsumer`, so the order is whatever upstream produces. Shapes whose decomposition is too fragmented to retain (>32 boxes) answer with the empty sentinel and keep vanilla's path. This is the per-touched-block path `Entity.move` walks. (all versions) **NEW**
- New `ModelPart#rotateBy` overwrite: reuses two scratch `Matrix3f` and one scratch `Vector3f` (thread-confined, same convention as `MixinPose`) instead of allocating per call. The Euler -> matrix -> Euler round trip is unchanged and bit-identical over 200k random rotations. Call sites are narrow (end crystals, player capes), so this is an allocation reduction, not a frame-time change. (1.21.4+) **NEW**
- New `DisplayRenderer` redirect: the billboarding quaternion comes from a reusable scratch instead of a fresh `Quaternionf` per display entity per frame. Every branch of `calculateOrientation` assigns rather than accumulates and the result is only read, so a shared instance is sound. (>=1.20) **NEW**
- New `BlockGetter#forEachBlockIntersectedBetween` overwrite plus the `BlockStepSweep` helper: replaces the per-cell `Iterable` + `AbstractIterator` + `MutableBlockPos` trio and the `Optional<Vec3>` box clip of the DDA sweep with one reused cursor and a primitive clip returning `t`. Vanilla's arithmetic, iteration order, deduplication and abort semantics are reproduced exactly - this runs on every entity move via `Entity#checkInsideBlocks`, and on `Ghast#tick`. (>=1.21.9) **NEW**
- `SoundEngine` handle pool now replenishes on hits as well as misses, gated on a live per-pool size counter and topping up below half the cap. A miss costs a full `createHandle().join()` - exactly what vanilla costs - so a miss-only pool can improve the median but never the tail: its hit rate settled at 50% (30% on 32-sound bursts) and p99 was vanilla's. Topping up on every call reaches a 100% hit rate. The win holds while a tick plays at most about cap/2 sounds and decays to vanilla beyond that; it is never worse. (all versions) **REWORK**
- `PalettedContainer`'s uniform-section cache moved into one immutable `UniformPaletteCache` `(Data, value)` holder behind a single volatile reference, so the hot path is still one volatile load, one identity check and one plain load. (all versions) **REWORK**
- New `BlockModelLighter` state-read memo: while a section's immutable render region is meshed, the repeated `BlockAndTintGetter#getBlockState` reads of ambient occlusion are answered from a 512-slot open-addressed table keyed on the packed block position - one block's six faces read 90 states over 19 distinct positions. The window belongs to the constructing thread and is opened/closed with vanilla's own `enableCaching`/`clearCache`, so a region change, a level change or a caller that forgets to close all invalidate it; collision-shape queries themselves stay vanilla, since ordinary states already cache their result. (>=26.1) **NEW**
- New `SpriteContents` mip-level release (`MixinSpriteContents` + `MixinTextureAtlasMip`, bridged by `ISpriteContents`): once the atlas has finished uploading, every non-animated sprite frees its generated mip levels above level 0 and truncates the chain, so they are not held for the lifetime of the pack. Level 0 is kept deliberately - it is what `isTransparent` (and therefore item model generation and ambient occlusion) reads, what a debug atlas dump reads, and what Axiom's `BlockColourComputation` holds; `NativeImage#close` is `nmemFree`, so closing it would be a native use-after-free. The hook is `TextureAtlas#upload` RETURN, not `SpriteContents#uploadFirstFrame`, which from 26.1 runs once per mip level inside the generation loop. Sodium and Iris already inject there, so coexistence is proven rather than assumed. (>=1.20) **NEW**
- New `BlockableEventLoop#doRunTask` redirect: the `SharedConstants.IS_RUNNING_IN_IDE` argument Tracy's `beginZone("Task", ...)` hangs on is folded to a constant, so no profiler zone is opened per executed task. Costs the Tracy per-task breakdown from this site - a small win, recorded so the trade is visible. (>=1.21.4) **NEW**
- New `WeightedList$Compact#get` overwrite: immutable cumulative weights and a branchless binary search replace the O(n) weight-subtraction scan - but only where it pays. The prefix table is built from 64 entries up, and only when a weighted draw would inspect at least 32 entries on average, so small and front-skewed selectors keep the vanilla scan, as do out-of-range selections, which still reproduce vanilla's overflow behaviour and exception text. (>=1.21.11) **NEW**
- New `UberGpuBuffer#uploadStagedAllocations` redirect: the staged-allocation walk uses fastutil's reuse-mode `fastIterator()` instead of `ObjectSet#iterator`, so the per-entry object is recycled rather than allocated per iteration. Starts after the preceding key iterator, which must still free the previous iteration's reuse objects. (>=26.2) **NEW**
- `DynamicUniformStorage#writeUniform` / `DynamicGpuDataStorageMapped#writeData`: the slow path builds its `GpuBufferSlice` directly instead of probing the one-slot cache - that probe could only ever miss, since the cached slice belongs to the previous block - and on 26.2+ the returned slice is the mapping's own rather than a second identical record. The fresh slice is still stored, which is what primes the next fast-path hit. (>=1.21.6) **REWORK**
- `InputConstants.Key#hashCode` overwrite: the two-element `Objects.hash` result is computed directly instead of allocating the temporary `Object[]` and boxing the key code, on every keybinding lookup. Null handling and the integer overflow of the original are preserved. This patch shipped before 0.5.0 and was dropped there as low-value; it is back and now widened down to every variant, 1.16.5 included. (all versions) **NEW from OLD**
- `SectionPos.betweenClosedStream`'s `forEachRemaining` steps the cursor directly instead of a `started` flag plus a separate sequential loop. (all versions)
- `MixinBufferBuilder` priority raised to 2000 so our `beginElement` store runs ahead of foreign patches on the same target.
- Enabled JOML's `sinLookup` (11-bit table) alongside `joml.fastmath`, both still overridable with `-D`.

### Bug Fixes
- `VertexConsumer.addVertex` / `addVertexWith2DPose` / `setNormal` / `putBulkData` / `putBakedQuad` were **not** transcriptions of the JOML operation vanilla performs, in two separate ways. The first computed `w` and divided by it whenever `w != 1`, silently reimplementing perspective projection that vanilla never does. The second built the same sum from nested `java.lang.Math.fma`; JOML writes the identical expression through `org.joml.Math.fma`, and in every JOML release Minecraft ships `org.joml.Runtime.HAS_Math_fma` is false, so each of those is a plain `a * b + c` - the replica was off by up to one ulp on most vertices while looking like a faithful copy. Both the association (`m00 * x + (m10 * y + (m20 * z + m30))` for 3D, left-to-right for 2D) and `normalize()`'s nested length-squared are now reproduced exactly, and `putBulkData` + `putBakedQuad` gained the bit-comparison coverage they were already claiming. (1.21+)
- Fixed `ARGB.scaleRGB` corrupting out-of-range divisors: vanilla clamps the divisor to 0..255 before dividing, but the reciprocal form was applied to the raw value, so scaling white by `Integer.MAX_VALUE` overflowed the long product past `2^24 * Integer.MAX_VALUE`, wrapped negative and clamped to black. The reciprocal is now used inside vanilla's domain and the clamp is reproduced directly outside it, where no division is needed at all.
- Fixed the `Camera` fluid-in-camera cache being keyed on camera position alone, which is unsound: a stationary camera kept `FogType.WATER` after the water below it was removed and skipped the water query entirely once the cache held `FogType.NONE`. The caches are now dropped at the head of every `setup()`, which the game calls exactly once per rendered frame, so the memo is frame-scoped and a block, fluid, FOV or aspect change is always picked up.
- Fixed `BitSetDiscreteVoxelShape.join` writing contiguous runs with `BitSet.set(from, to)`. That overload edits the backing long words directly and never routes through the single-bit `set(int)`, so a `BitSet` subclass that keeps derived state in an overridden `set(int)` desynchronises - Palladium swaps this storage for a `FastBitSet` whose reader mirror only `set(int)`/`clear(int)` maintain, which silently emptied every shape built through `Shapes.join` (bed shapes, stairs, slabs) while its bounds stayed correct. The bulk write is now enabled only for a stock `java.util.BitSet`.
- Fixed the `PalettedContainer` uniform cache publishing a mismatched pair: with the value in a plain field next to a volatile `Data` field, two concurrent fillers can interleave and leave one section's `Data` paired with another section's value, handing back a wrong block. One immutable holder removes the interleaving via the JMM final-field guarantee.
- Fixed `AxisPoseRotate`'s axis fast path assuming a unit quaternion. `Matrix4f.rotate(Quaternionfc)` composes the rotation with a uniform scale of the norm squared, so a non-unit quaternion the axis path skipped left a different diagonal than JOML; anything that is not unit is now handed to JOML unchanged.
- Fixed `Mth.ceillog2`, `Mth.log2` and `Mth.smallestEncompassingPowerOfTwo` disagreeing with vanilla on `Integer.MIN_VALUE`. Vanilla's `isPowerOfTwo` accepts it, so `ceillog2` and `log2` answer 31 there while the leading-zero body answered 0 and -1, and the smallest-power-of-two guard disagreed on the sign of its result.
- Fixed `BlockStateBase#getCollisionShape` serving the cached per-state shape to *entity* collision. The cache is filled once from `(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())`, but the three-argument overload that entity collision uses delegates to `getBlockShape` unconditionally - so any block whose collision shape varies per entity got the wrong one. Twenty-one Create blocks override `getCollisionShape` without declaring `hasDynamicShape`, so the cache existed for all of them; `CrushingWheelControllerBlock` in particular returns an empty shape for the entity it is crushing, so with a solid controller the item could never be shoved past the intake threshold and the wheel spun and crushed nothing. The cache is now served only for contexts that carry no entity, which is exact on every version: the empty sentinels all have a null entity, and the 26.2+ `PositionCollisionContext` does not extend `EntityCollisionContext` at all, so a position-sensitive shape lands on the dispatch path anyway.
- Fixed `Heightmap#primeHeightmaps` carrying an unmatched pending-type mask between columns. The mask was initialised once for the whole chunk, so a water-only column that matched nothing left its bits set, and a later column could overwrite a height after vanilla's partial iterator rewind. Each column now starts from a fresh mask.
- Forge/NeoForge manifests serialise the Minecraft range through one `mcRange(ctx)` helper. The previous inline `when` emitted an open `[X,)` whenever `deps.minecraft_max` was absent or equal to the compiled version - true for *every* variant, since each block sets `deps.minecraft` to the version it builds - so the bounded branch was dead code and `1.21.4-neoforge`, `26.2-neoforge` and `26.3-neoforge` shipped unbounded, disagreeing with their own Fabric counterparts and letting an older patch set load on a version they were never built for.
- `MixinFallbackResourceManager.listResources` is a cancellable HEAD `@Inject` again rather than an `@Overwrite`. An overwrite deletes the target `MethodNode`, which breaks any foreign MixinExtras v2 injector anchored to it - owo's `FallbackResourceManagerMixin` uses `@WrapWithCondition` on the `PackResources.listResources` INVOKE, which died with `InvalidInjectionException` during `MultiPackResourceManager.<init>`, i.e. a boot crash. The price is one `CallbackInfoReturnable` per call, which is a data-reload path. (>=1.19.3)
- `MixinChannel.setSelfPosition` is a cancellable HEAD `@Inject` at default priority instead of a `priority = 500` `@Overwrite`, for the same reason.
- Fixed comment and javadoc accuracy across the touched mixins (`Mth.hsvToArgb` no longer claims to skip the modulo, `AutoStorageIndexBuffer` warmup counts indices rather than quads, `ResourceEntry` field alignment).

### Removed
- Removed three `LightCoordsUtil` overwrites (`max`, `lightCoordsWithEmission`, `smoothWeightedBlend`, 26.1+): the equality fast path and the nibble-max packing only duplicated what vanilla already does in three branches, and the "blend" replaced vanilla's per-channel `lerp` calls with a weighted accumulation, so the saved call overhead was not worth re-deriving the interpolation by hand.
- Removed `MixinGlStateManager`, `MixinWindow` and `utils/cache/GlStateCache`. Sodium already owns the vertex upload path these deduped, and the cache is an unverifiable claim about driver state: Accelerated Rendering, ImmediatelyFast and VulkanMod all mutate scissor/viewport without telling it, and Iris has its own cancellable HEAD injection on `_viewport` that can cancel before our cache sees the write. Every such case leaves the wrong rect or mode live. The `<1.21.2` FBO read/draw mirror backport that shared the file went with it. (all versions)
- Removed `MixinVertexBuffer`: Sodium already overwrites `ModelPart$Cube#compile` with an equally fast body, so ours was dead code under any popular rendering mod while adding conflict surface.
- Removed the `Util.growByHalf` overwrite (>=1.21.6): three branches around a `Math.min`/`Math.max` pair that HotSpot compiles better than the hand-rolled form.

### Changed
- New supported versions: **1.16.5** (Fabric) and **1.18.2** (Fabric + Forge). Both carry a real, reduced patch set rather than an empty config - 24 and 29 patches - and are the reason for most of the `//?` gates listed below. 1.16.5 + Forge has no working toolchain: `legacyforge` drives NeoForm Runtime, which refuses every pre-1.17 MCP config outright, so the `match` line stays commented out and the variant is left out of `stonecutter.properties.toml` entirely — declared, it would be a variant `publish.py` waits for a jar that can never be built.
- The legacy Forge / Ornithe builds (1.7.10, 1.8.9, 1.12.2) moved into the repository as two source trees, `src-legacy/` for Forge and `src-ornithe/` for Ornithe, plus a separate `legacy/` Gradle build driving six per-version sub-builds, and are tracked in [CHANGELOG_LEGACY.md](CHANGELOG_LEGACY.md) - they must not interleave with this file.
- Renamed the `Transformation#inverse` mixin back from `MixinTransformationInverse` to `MixinTransformation` (same target, same overwrite). The class name is its runtime config key, so an existing `com.mojang.math.MixinTransformation` entry works again.
- Mixin config gating tightened to what each patch actually needs: `MixinModel` moved from `>=1.21.6` to `>=1.21.4`, `MixinBuilder` from `>=1.21.11` to `>=1.21.6`, `MixinPoseStack` from `<26.3` to `>=1.19 && <=26.2`, `MixinVertexFormatElement` / `MixinMultiple` / `MixinCamera` gained `>=1.19` floors, and `MixinBlockStateBase` + `accessor.InvokerBlockBehaviour` moved into the `<1.19` block at `>=1.18`. `MixinAutoStorageIndexBuffer` now sits in the `>=1.18` block, which also serves 1.19+ - the nested class appears in 1.18.2, so the gate is the class, not the era, and a second `>=1.19` listing would duplicate the entry on every modern variant.
- The `plugin` key is now omitted below 1.17: 1.16.5 is a Java 8 target shipping Gson 2.8.0, and the modern `LomkaMixinPlugin` needs a text block, `var` and `JsonParser.parseReader`. The legacy tree ships its own Java 8 + Log4j + no-Gson plugin instead, so the runtime toggle is not lost there.
- Build outputs now stay clean: dev jars are deleted from `build/libs` as soon as the shippable artifact exists, skipped on the moddev-based loaders where the pre-reobf jar is a build input (and where `RemapJar.input` is wired as a plain `File`, so deleting it fails the build outright). Gradle heap raised 2G -> 3G.
- Source comments no longer cite the benchmark harness or the vendored competitor trees. Figures rot with every JDK change and are invisible to players reading the code, and the whole `scripts/` tree is gitignored, so such a path resolves for the author and for nobody else.
- README no longer advertises an FPS percentage, and the sound/attribution lines were reworded.

## [0.6.0] - 2026-09-27

### Performance
- New `IntArrayTag` and `LongArrayTag` mixins, where vanilla reads these arrays element by element, so every int costs four and every long eight `read()` calls on a `DataInputStream`. The array is now read in 8 KB blocks through a big-endian `ByteBuffer`. The bulk path only triggers on `DataInputStream` inputs, so in practice it accelerates server-side disk reads (chunk NBT with its `Heightmaps` long arrays, player data, `level.dat`, saved data, structure templates). **NEW**
- New `Vec3#normalize` mixin: replaces three divisions with one reciprocal and three multiplications. Results may differ from vanilla by up to 1 ULP. (1.21.4+) **NEW**
- New `ARGB#multiply` overwrite that folds the four per-channel `/255` integer divisions into exact reciprocal multiplies, which runs on every entity's model tint each frame. Results are bit-identical to vanilla. (all versions) **NEW**
- New `MixinSoundEngine` channel-handle pool. Playing a sound no longer makes the client thread `join()` a hand-off to the sound executor, so a sound plays as soon as its handle is already acquired. (all versions) **NEW**

### Bug Fixes
- Register common optimizations under the standard `mixins` key so they actually run. The key was spelled `main`, which Mixin does not read, so every common patch (`Mth`, `ARGB`, `BlockPos`, `Direction`, `AABB`, `PalettedContainer`, `Heightmap`, NBT tags, ...) **was silently dropped in every release from 0.3.0 to 0.5.5**. Only the `client` section was live, so the mod has been running at roughly half its intended patch set for its entire published history. **CRITICAL**
- Fixed the 1.19.2 manifests advertising Minecraft up to `1.19.4`, which the 1.19.2 artifacts cannot run on: the 1.19.3 JOML transition is binary-incompatible. Both loaders now declare `[1.19,1.19.2]`, and the range generator collapses to the single-element form when the minimum equals the maximum, because Forge's version parser rejects a range with identical boundaries.
- Fixed a crash of the Forge mod list on 1.19.2, where the generated manifest pointed `logoFile` at `assets/lomka/icon.png`, and 1.19.2 feeds that value to `AbstractPackResources#getRootResource(String)`, which throws `IllegalArgumentException` on any name containing `/`, so clicking the mod in the mod list always crashed. The Forge manifest now uses a bare `logo.png` at the jar root, which both 1.19.2 and 1.20.1 resolve.
- Fixed missing mixin visibility and 26.3 API drift that aborted the launch on several versions (`MixinBitSetDiscreteVoxelShape`, `MixinPalettedContainer`, `MixinArrayListDeque`, `MixinUtil`, `MixinDirection`, `MixinFriendlyByteBuf`).
- Fixed `MixinTextureAtlas` on 1.21.6-1.21.10: the texture field is inherited from `AbstractTexture`, so `@Shadow` could not resolve it and aborted the launch. A new `accessor.AbstractTextureAccessor` bridge reads it through the superclass. (1.21.6-1.21.10)
- Fixed `MixinCompressionDecoder` dropping the connection: `ByteBuf#internalNioBuffer` is absent from `AbstractByteBuf` and from `AbstractUnpooledSlicedByteBuf`, and the only available guard is `nioBufferCount()`, so a refused buffer would throw inside the packet decoder. The call is now wrapped in a non-throwing `try`/`catch` that falls back to `nioBuffer`. (1.21+)
- Fixed a Forge/NeoForge-only `IllegalClassLoadError`: helper types nested inside mixins produce classes in the `lomka.starl.mixins.*` package, which ModLauncher's Mixin plugin refuses to load.
- Fix version-specific NBT overwrite signatures and returnable PalettedContainer callbacks.
- Restore vanilla defensive translation copies to preserve display interpolation.
- Restrict the 1.19.2 artifacts to Minecraft 1.19.2; later versions use incompatible math APIs.
- Hardened control-character escaping in generated Fabric JSON and Forge/NeoForge TOML metadata.

### Removed
- Removed `MixinStringRenderOutput` and its one-slot `FontSet` memo. (1.19.2-1.21.8)
- Removed `MixinLevel`, `MixinBlockableEventLoop`, `MixinClientLevel` and the Vulkan buffers `MixinDirect` patch.
- Removed the reentrant `FriendlyByteBuf.writeMap` helper and unsafe `Util`/`Mth` overwrites.
- Removed `MixinBlockStateBase` and `MixinMultiPackResourceManager` on 1.19.2.
- Removed `MixinVanillaPackResources` on 1.19.2 and 26.3.
- Removed the `Direction#getApproximateNearest` on 1.21.4+ overwrite: Sodium (Fabric) and Embeddium (Forge/NeoForge) already replace the vanilla dot-product loop with the same three-abs-compare chain. Our version was bit-exact where theirs is not (NaN / $\pm\infty$ / lone-`Float.MIN_VALUE` inputs), but paid for that exactness with extra ALU ops on the finite path.
- Removed `MixinLeveledPriorityQueue`: it swapped the priority-queue sets for fastutil `SpatialLongSet`, whose 4x4x4 outer key packing only holds for light positions, but the same queue backs `DynamicGraphMinFixedPoint` with `ChunkPos` keys. Distinct chunks collapsed into one bucket, the distance-update fixpoint never converged, and world generation or saving hung with the server thread at 100% CPU in `DistanceManager.runAllUpdates`. All versions affected.

### Changed
- Reorganized the shared helper packages: `DirectionTables` moved to `utils/constants/` and `ResourceEntry` to `utils/pack/`, next to `GlStateCache` in `utils/cache/`, replacing the catch-all `utils/misc/`. Internal only, no behavior change. (all versions)
- `MixinPalettedContainer` caches the uniform-section value in a `volatile Data` reference plus a plain value field, published by the volatile store, and invalidates it in `read` / `onResize`. The `set` / `getAndSet` / `getAndSetUnchecked` hooks were dropped as provably redundant, since zero-bit storage validates writes as no-ops.
- `MixinBuilder` restores the 26.1+ `addAll` culled-face transfer.
- `MixinPose` uses thread-local scratch matrices.
- Renamed the `Transformation#inverse` mixin class from `MixinTransformation` to `MixinTransformationInverse` (same target, same overwrite). This is also its runtime config key, so an existing `com.mojang.math.MixinTransformation` entry is now reported as an unknown key - rename it to `com.mojang.math.MixinTransformationInverse`.
- Expanded generated Fabric, Forge, and NeoForge metadata with CurseForge and attribution credits; Forge/NeoForge now also expose the Modrinth page and issue tracker, while Fabric keeps its Modrinth homepage.
- Updated README attribution and runtime mixin-configuration documentation for the added and removed patches.

## [0.5.5] - 2026-09-24

### Performance
- New KeyframeAnimation mixin: scratch `Vector3f` reused per animation instead of `new Vector3f()` per `apply()` (24 B/frame, measured), indexed entry walk instead of iterator, exact zero-scale early-out. (1.21.6+) **NEW**
- New Entry bridge mixin: `IKeyframeAnimationEntry` duck to the private `KeyframeAnimation$Entry` record so the caller scratch vector flows into vanilla `apply()` search/interpolation stay vanilla. (1.21.6+) **NEW**
- New Heightmap mixin: `primeHeightmaps` rewritten without the per-Y `ChunkAccess#getBlockState` walk flat `Heightmap[]`/`Predicate[]` pair, int bitmask for pending types, section-local scans, `O(1)` skip of null/`hasOnlyAir` sections and one predicate test per uniform section. Bit-identical, no per-column alloc. (all versions) **NEW**
- New PalettedContainer probe: `lomka$uniformValue()` answers the Heightmap uniform-section check via the same `(value, volatile Data)` pair as the hot `get()` path, heterogeneous containers return null and are never cached. (all versions) **NEW**
- New XoroshiroRandomSource mixin: `setSeed` reseeds the live `Xoroshiro128PlusPlus` in place (`seedLo`/`seedHi` writes + `gaussianSource.reset()`) instead of `new Xoroshiro128PlusPlus(...)` per `setFeatureSeed`/`setDecorationSeed` one generator alloc saved per structure/feature per chunk, sequence bit-identical. (all versions) **NEW**
- New StringRenderOutput mixin: one-slot `FontSet` memo per laid-out string consecutive codepoints of a styled run skip the font-map walk, identity hit with `equals` fallback. Ported from Collections Of Optimizations by Misanthropy (LGPL v3). (1.19.2-1.21.8) **NEW**
- New VecDeltaCodec mixin: zero-delta fast path returns `Vec3.ZERO` instead of `subtract` + new `Vec3` one alloc saved per unchanged tracker delta. Ported from Collections Of Optimizations by Misanthropy (LGPLv3, with authority), priority 999 for CoO compat. (all versions) **NEW**
- PoseStack on 1.19.2 (Mojang math): per-stack scratch `Matrix4f`/`Matrix3f` reuse instead of vanilla per-call allocations in `mulPose(Quaternion)`; bit-identical. (1.19.2) **NEW**
- Font mixins (`MixinFont`, `MixinGlyphSource`, `MixinFontSet`, `MixinSource`) widened down to 1.21.9+ (were 1.21.11+). (1.21.9+) **NEW from OLD**
- GlStateCache rework: `int[] STATE` + `get`/`set` replaced with plain public static fields; viewport/scissor/polygon compare via single XOR-OR, FBO read/write via direct field compare no array bounds checks / interface dispatch on the dedup path. (all versions) **REWORK**

### Bug Fixes
- Util `sanitizeName` parity fix: now runs `toLowerCase(ROOT)` on the whole string first (handles length-changing lowercases like U+0130) and only then patches rejected chars previous version lowercased per-char. Zero-alloc fast path for all-valid input kept.
- Util `offsetByCodepoints` bounds fix: out-of-range single steps return `i` unchanged instead of clamping to `len`/`0`.

### Removed
- Removed low-value/parity-risky Util overwrites: `getPlatform`/`isAarch64` OS-arch cache, `makeDescriptionId`, `findNextInIterable`/`findPreviousInIterable`, `parseAndValidateUntrustedUri`.
- Removed `wiki/Performance-Presets.md` and its README/wiki links (stale RAM/FPS preset blocks).
- Renamed `IFrameBufferCache` -> `IFrameBufferCacheKey`.

### Changed
- New supported version: **1.19.2** (Fabric + Forge) `stonecutter.properties.toml`, `settings.gradle.kts`, `deps.minecraft_min`/`max` ranges, `[20,)` Forge range for `<1.21`, JOML 1.10.5 shim, `MixinCamera`/`MixinClientLevel`/`MixinFriendlyByteBuf`/`MixinPoseStack` version branches, AT/CT gating.

## [0.5.4] - 2026-09-17

### Performance
- VertexConsumer `putBulkData`/`putBakedQuad`: pose-matrix loads hoisted out of the per-vertex loop and the 4-vertex loop fully unrolled — no per-vertex interface dispatch, friendlier to auto-vectorization. (1.21.11+ / 26.1+) **REWORK**
- FriendlyByteBuf `writeMap`: cached reusable `BiConsumer` keeps `HashMap.forEach` internal iteration (measured faster than an entry-set loop) with zero per-packet lambda allocations; `writeEnumSet`/`readEnumSet` rewritten as sparse bit packing (`O(set size)` instead of `O(enum length)`), no `BitSet` temporaries. (all versions) **REWORK**
- FallbackResourceManager `listResources`: derived base->meta index built after pack filtering — zero-alloc metadata attach per file with exact vanilla filter semantics; logger handle cached instead of a registry lookup per call. (all versions) **REWORK**

### Bug Fixes
- Mod dev version -> Normal mod version

### Removed
- Removed MixinDataLayer and MixinSoundEngine cause the rare scenarios.
- Removed Frustum `offsetToFullyIncludeCameraCube` (branchless int casts measured slower than `Math.floor`/`ceil` intrinsics).
- Removed SectionPos `asLong`/`x`/`blockToSection` overwrites.

### Changed
- ChunkStatus status-list cache is now published via `volatile` — safe for worldgen worker threads at no steady-state cost. (1.21+)

## [0.5.3] - 2026-09-16

### Performance
- New Level mixin: `ThreadLocal` scratch for `getEntities(Entity,AABB,Predicate)` — hottest entity query. (all) **NEW**
- New PalettedContainer mixin: uniform-section fast path via `volatile Data` identity. (all) **NEW**
- New FrameBufferCache mixin: zero-alloc probe `CacheKey` for `getFbo`. (26.3) **NEW**
- DynamicGpuDataStorageMapped mixin: `renderpearl` successor of `DynamicUniformStorage`. (26.3)

### Bug Fixes
- Refmaps fix for 1.20.1 Forge.
- Lightmap profiler/pipeline + `RenderStateExtractor` vectors (probably Axiom compability)

### Changed
- 26.3 `renderpearl` replacements (`GpuBuffer`, `GlProgram`, etc.) — 19 variants, `PoseStack`/`GlProgram` gated `<26.3`, `neoforge-moddev` 2.0.147, `VERSION` 0.5.3.
- 26.3 `ResourceManager.Selector` compat for pack managers, Window `onResize` signature.

## [0.5.2] - 2026-09-06

### Performance
- VoxelShape.toAabbs is computed once per shape and cached — `clip()` no longer allocates an ArrayList + AABBs on every raycast miss (crosshair raycast runs every frame). (all versions) **NEW**
- BitSetDiscreteVoxelShape.join accumulates contiguous bit runs and flushes them with bulk `BitSet.set(from, to)` word writes instead of one `set()` per voxel. (all versions)
- New AABB mixin: allocation-free `clip` paths — no `double[1]` box, no per-iteration `AABB.move` in the `Iterable` variant, direct face tests. Keeps Lithium's AABB mixin intact. (all versions) **NEW**
- New BlockPos mixin: `betweenClosed` replaces `2 div + 2 mod` per block with incremental stepped counters, preserving the vanilla X→Y→Z order. (all versions) **NEW**
- AutoStorageIndexBuffer warmup lands the first allocation at 16384 indices (~32 KB SHORT) with the x4 growth no early mid-frame regen hitches. (all versions) **NEW**
- New Vec3i mixin: exhaustive switch for `get(Axis)`, int-exact `distManhattan`, phi-mix `hashCode` (from `efficient_hashing` by ZZZank, CC0). (all versions) **NEW**
- New LegacyRandomSource mixin: plain-field LCG draws instead of volatile + CAS per call; the vanilla seed-install guard stays, worldgen parity is bit-for-bit. (all versions) **NEW**
- New Util mixin: cached OS/arch detection, array-backed shuffles, allocation-free URI scheme checks, single-step `offsetByCodepoints`, shared predicate constants. (all versions) **NEW**
- New Direction mixin: `getClockWise`/`getCounterClockWise` via a flat 1D LUT instead of nested enum-switch dispatch; branch-free `getApproximateNearest`. (all versions, nearest optimization one: 1.21.4+) **NEW**
- New ByteBufferBuilder mixin: inlined `reserve` fast path, max growth step raised from 2 MB to 8 MB — no repeated native reallocations on chunk/batch surges. (1.21+) **NEW**
- BufferBuilder per-vertex writes (`setColor(IIII)`, `setUv(FF)`, `uvShort`) collapsed to single int/long stores; the packed-color path stays on BSWAP + ROR. (1.21+) **NEW**
- VertexConsumer transforms rewritten with per-era coverage: scalar fma `addVertex` (1.21+), 2D `addVertexWith2DPose` (1.21.6+), `setNormal` via the widened `trustedNormals` field (1.21-1.21.11), `putBulkData` without bridge arrays (1.21.11+), fma `putBakedQuad` (26.1+). **NEW**
- Mth: `smallestEncompassingPowerOfTwo` via a single LZCNT instruction, fma `lerp` (float/double) with single rounding — ported from SuperFastMath (elias, permission granted). (all versions) **NEW**
- GlStateManager state dedup (viewport/scissor/polygonMode, cancellable HEAD injections) extended to all versions; GlStateCache now tracks bound read/write FBOs and dedups `_glBindFramebuffer` back to 1.20.1, replicating vanilla's own 1.21.2+ mirror logic. (all versions) **NEW from OLD**
- Pose scratch-matrix optimization covers the outer `PoseStack.mulPose(Matrix4f)` era (1.21-1.21.4) too, unified in MixinPose. (1.21+, not 26.x) **NEW from OLD**
- Std140Builder / Std140SizeCalculator: blanket per-`put*` overwrites replaced with a single power-of-two `align()` fast path, vanilla fallback for exotic alignments. (1.21.6+)
- BitSetDiscreteVoxelShape.join accumulates contiguous bit runs and flushes them with bulk `BitSet.set(from, to)` word writes instead of one `set()` per voxel (all versions).

### Bug Fixes
- Supplier fix in VulkanGpuBuffer the `<init>` handler's `label` parameter is typed `Supplier<String>` now, fixing `InvalidInjectionException` at apply time.
- MixinClientLevel causes crash.
- Fixed `betweenClosedStream` int overflow in the spliterator estimate and duplicate emission after a partial `tryAdvance` (SectionPos).
- `FallbackResourceManager` keeps the `wrapForDebug` wrapper when debug logging is on vanilla `LeakedResourceWarning` parity.
- VanillaPackResources resolves root identifiers (empty path) to the namespace root instead of a malformed `namespace/` path.
- CompressionEncoder shrinks runaway retained buffers (>64 KB and 4x the current packet) back to a power-of-two size no 8 MB held forever after one huge chunk packet.
- BlockStateBaseCache publishes the face-sturdy mask through `volatile` fields with a `@Mutable` array no torn reads.
- Mth.hsvToArgb clamps channels and throws exactly like vanilla on out-of-range hues.

### Removed
- Removed MixinStatType.
- Removed the Camera `getMaxZoom` overwrite (vanilla restored, possibly Sable compability yea).
- Removed the BlockableEventLoop `submit()` shared-future overwrite — only the leaner `submitAsync` remains.
- Removed low-value Mth overwrites: `clampedLerp` (float/double), `lerpInt`, `wrapDegrees` (int/float/double/long), `packDegrees`.
- Removed the VertexFormatElement cached `hashCode` overwrite. (1.20.1)
- Removed the PoseStack `pushPose`/`popPose` Pose pooling (1.21-1.21.4) — superseded by vanilla's own pooled Pose model.

### Changed
- Unified entrypoint: `lomka.Lomka` with nested `Fabric`/`Forge`/`NeoForge` adapters gated by Stonecutter loader constants; `VERSION` is swapped from `stonecutter.properties.toml` automatically.
- `LomkaMixinPlugin.onLoad()` sets `joml.fastmath=true` (unless the user set it) before JOML classes initialize.
- `GlRenderStateCache` renamed to `GlStateCache` and moved to `utils/cache/`; `AxisPoseRotate` moved to `utils/math/`.
- ARGB LUTs are built inside the mixin; AT/AW entries for vanilla `SRGB_TO_LINEAR`/`LINEAR_TO_SRGB` removed.
- Mixin priorities lowered where foreign patches are expected (BufferBuilder, ByteBufferBuilder, VulkanGpuBuffer.Direct, Minecraft, lightmap mixins).

## [0.5.1] - 2026-08-25

### Performance
- Frame-end present replaced with a single hardware `glBlitFramebuffer(NEAREST)` call instead of the blit-shader fullscreen quad — no per-frame Matrix4f/Tesselator/MeshData allocations, no mid-frame shader program bind. (<1.21.4) **NEW**

### Bug Fixes
- Fixed entity tick crash (`ClassCastException` in `tickNonPassenger`) — the cached profiler name path now casts to the `Level` superclass directly instead of relying on a separate Level mixin being applied.
- Mixin config (`lomka-mixins.properties`) keys now match full/short/simple mixin names. Toggles are no longer silently ignored or falsely reported as unknown.

### Removed
- Removed MixinLevel and the ILevelProfiler duck interface (superseded by the direct superclass cast).

### Changed
- Removed duplicated license headers and stale javadoc tags in several mixins.

## [0.5.0] - 2026-08-24

### Performance
- Shared sequential index buffer grows x4 instead of x2, with an 8192-index warmup floor — no more mid-frame regen hitches on chunk/GUI/cloud spikes. (all versions) **NEW**
- GlStateManager skips redundant viewport/scissor/polygonMode driver calls, cache resets on resize. (1.21.6+) **NEW from OLD**
- Frustum setup uses branchless integer floor/ceil instead of Math calls. (all versions) **NEW**
- PoseStack push/pop pooled and mulPose runs through a scratch matrix. Zero allocations per entity part. (mulPose scratch: 1.21.6–1.21.11)
- VertexBuffer caches its GL primitive mode, format check by reference. (<1.21.6) **NEW**
- VertexFormatElement caches hashCode and drops Usage lambdas in attribute setup. (<1.21) **NEW**
- Multi-consumer fan-out walks delegates with direct indexed loops instead of per-vertex lambdas. (1.20.1–26.1) **NEW**
- VertexConsumer gains an affine fast path for applying pose matrices. (1.21+) **NEW**
- Material hash cached instead of allocating an Object[] per sprite lookup. (1.20.1–1.21.11) **NEW from OLD**
- SectionPos cheaper packed math with allocation-free stream traversal. (all versions) **NEW**
- StatType registration is a lambda-free get-then-put. (all versions) **NEW**
- ChunkStatus progression list cached instead of rebuilt per chunk layer. (1.21+) **NEW**
- Light storage reuses one consumer instead of allocating a lambda per block change. (all versions) **NEW**
- CompressionEncoder reuses its input buffer between packets. (all versions) **NEW**
- Entity tick profiler names cached per entity type, removing vanilla's capturing lambda. (<26.1) **NEW**
- Vulkan host-visible buffers map through a persistent address, no driver round-trip per frame. (26.2) **NEW**

### New
- Mixin configuration is REAL in `config/lomka-mixins.properties`.
- Mixin configs now declare minVersion: 0.8.

### Removed
- Removed low-value mixins: MixinKey, MixinNativeImage, MixinSoundBufferLibrary, MixinIdMapper.
- Removed accessors: AccessorCache, AccessorNearPlane, InvokerBuilder. (changed by aw/at)

### Changed
- License changed from **MIT** to **LGPL-3.0-only**.
- Mod icon, mod description.
- LomkaFabric, LomkaNeoForge and LomkaForge all-in-one in just Lomka with conditionals.
- GpuBuffer patch disabled on 26.2, vanilla caches the full-buffer slice natively.
- Numerous existing mixins received further tweaks and version-condition adjustments.

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