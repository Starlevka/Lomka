# Lomka — Client Optimization Mod

## Base Parameters
- **Name:** Lomka
- **Type:** Client-side optimization mixin mod
- **MC versions:** 1.21.1, 1.21.10, 1.21.11, 26.1.2, 26.2
- **Loaders:** Fabric (`fabric-loom-remap` / `fabric-loom`), NeoForge (`neoforge moddev`)
- **Current version:** 0.3.0
- **JDK:** 21 (1.21.x), 25 (26.x)
- **Mappings:** Mojang official
- **Fabric Loader:** >=0.16.0 (no Fabric API dependency)
- **Build system:** Stonecutter 0.9.4 + Kotlin DSL

## Goals
1. **FPS & Frame Pacing** — Maximize average FPS, eliminate stutters via hot-path optimization.
2. **Memory Efficiency** — Reduce heap allocations, use optimized data structures.
3. **GC Pressure Reduction** — Eliminate unnecessary object creation in tick/render loops.
4. **Loading Speed** — Parallelize resource loading, optimize data structures.

## Project Structure
```
Lomka/
├── SOURCE CODE/                ← Decompiled/modified MC source per version + diff docs
│   ├── DIFFRENCE.md            ← Mixin presence matrix: 1.21.11 vs all versions (1.21.1–26.2)
│   ├── 1.20.1 SOURCE CODE/
│   ├── 1.21.1 SOURCE CODE/
│   ├── 1.21.11 SOURCE CODE/
│   ├── 1.21.9-1.21.10 SOURCE CODE/
│   ├── 26.1.X SOURCE CODE/
│   └── 26.2.X SOURCE CODE/
└── stonecutter/
├── src/main/java/
│   ├── lomka/Lomka.java              ← Main mod class (MOD_ID, init)
│   ├── lomka/fabric/LomkaFabric.java ← Fabric entrypoint
│   └── lomka/starl/
│       ├── mixins/                   ← All mixin classes (mirrors MC internal structure)
│       │   ├── accessor/             ← @Accessor (2 files: AccessorCameraNearPlane, InvokerQuadCollectionBuilder)
│       │   ├── com/mojang/audio/     ← Sound mixins (package decl: blaze3d.audio)
│       │   ├── com/mojang/blaze3d/   ← Blaze3D, buffers, platform, vertex mixins
│       │   ├── com/mojang/math/      ← Transformation, Quadrant
│       │   └── net/minecraft/        ← MC mixins (mirrors internal structure)
│       └── utils/                    ← Utility classes (2 files)
├── src/main/resources/
│   ├── lomka.mixins.json             ← Base mixin config (vcsVersion = 1.21.11)
│   └── aw/                           ← Access wideners per version
├── versions/
│   ├── <ver>/src/                    ← Branch-level: per-version mixin JSON
│   ├── <ver>-fabric/src/             ← Node-level overrides (Fabric)
│   └── <ver>-neoforge/src/           ← Node-level overrides + AT (NeoForge)
├── build.fabric-o.gradle.kts         ← Fabric 1.21.x (fabric-loom-remap)
├── build.fabric-m.gradle.kts         ← Fabric 26.x (fabric-loom)
├── build.neoforge.gradle.kts         ← NeoForge (neoforge moddev)
└── stonecutter.properties.toml       ← Mod metadata & dependencies
```

## Loader Entrypoints
- **Fabric:** `lomka.fabric.LomkaFabric` (ModInitializer) calls `Lomka.init()`
- **NeoForge:** `@Mod("lomka")` in `lomka.neoforge.LomkaNeoForge` calls `Lomka.init()`
  - Only exists in `versions/<ver>-neoforge/src/`, excluded from Fabric

## Stonecutter
- **VCS version:** 1.21.11-fabric (source compiles directly for this version)
- **10 variants:** `{1.21.1, 1.21.10, 1.21.11, 26.1.2, 26.2}` x `{fabric, neoforge}`
- **String replacements:**
  - `ResourceLocation` <-> `Identifier` (`>=1.21.11`)
  - `rendertype.RenderType` -> `renderer.RenderType` (`<1.21.11`)
- **Conditionals:** `//? if` / `//?}` blocks (files excluded from compilation; Loom compiles raw src, so conditionals are NOT processed for vcsVersion)

## Version Handling

### Node-level overrides
Fabric Loom deduplicates: when both `src/main/java/` and `versions/<ver>-fabric/src/` have the same class, Loom picks the node-level one. **NeoForge does NOT deduplicate** — base file must be excluded first.

### File exclusions (build scripts)
| Condition | Fabric | NeoForge |
|---|---|---|---|
| `<1.21.9` | Std140SizeCalculator, Std140Builder, GpuBuffer, NativeImage, MeshData, MeshDataSortState, BufferBuilder, ByteBufferBuilder, PoseStackPose, Quadrant, KeyframeAnimation, FontSet, Model, ModelPart, Material, DynamicUniformStorage, MipmapGenerator, PalettedPermutations, SoundEngine + RenderSystem, ModelBlockRenderer, DynamicUniforms, TextureAtlas, BakedQuad (block.model), QuadCollectionBuilder, InvokerQuadCollectionBuilder, ModelPartCube, SingleQuadParticle, MixinCamera | Same + MultiBufferSource, ARGB, VertexConsumer, FaceBakery, GameRenderer, MixinMinecraft |
| `>=1.21.9 && <1.21.11` | — | VertexConsumer, Quadrant, GameRenderer, DynamicUniforms, MultiBufferSource, TextureAtlas, BakedQuad (block.model), QuadCollectionBuilder, InvokerQuadCollectionBuilder, FaceBakery, Model, QuadrantRotationCache |
| `>=26.1` | ModelBlockRenderer, GameRenderer, BakedQuad, QuadCollectionBuilder, InvokerQuadCollectionBuilder, PoseStackPose, ModelPart, MixinCamera + RenderSystem | Same + VertexConsumer |
| `>=26.2` | MultiBufferSource, VertexFormat, MeshData, MeshDataSortState | Same + BufferBuilder, DynamicUniforms, TextureAtlas |

### Per-version mixin configs
| Path | Client | Main | Total |
|---|---|---|---|
| `src/main/resources/lomka.mixins.json` (base, 1.21.11) | 45 | 6 | 51 |
| `versions/1.21-fabric/src/main/resources/lomka.mixins.json` | 15 | 5 | 20 |
| `versions/1.21-neoforge/src/main/resources/lomka.mixins.json` | 14 | 5 | 19 |
| `versions/1.21.9-fabric/src/main/resources/lomka.mixins.json` | 30 | 6 | 36 |
| `versions/1.21.9-neoforge/src/main/resources/lomka.mixins.json` | 30 | 6 | 36 |
| `versions/26.1/src/main/resources/lomka.mixins.json` | 39 | 6 | 45 |
| `versions/26.1-fabric/src/main/resources/lomka.mixins.json` | 40 | 6 | 46 |
| `versions/26.1-neoforge/src/main/resources/lomka.mixins.json` | 38 | 6 | 44 |
| `versions/26.2/src/main/resources/lomka.mixins.json` | 38 | 6 | 44 |
| `versions/26.2-fabric/src/main/resources/lomka.mixins.json` | 39 | 6 | 45 |
| `versions/26.2-neoforge/src/main/resources/lomka.mixins.json` | 32 | 6 | 38 |

### Version-guarded mixins (file-level `//? if >=1.21.11`)
These 6 files are wrapped in Stonecutter conditionals and don't compile as classes on < 1.21.11:
- `MixinTextureUtil` — texture solidification BFS optimization
- `MixinTextureSetup` — GUI texture setup optimization
- `MixinPalettedPermutations` — palette permutation caching
- `MixinMipmapGenerator` — mipmap generation optimization
- `MixinGpuBuffer` — GPU buffer allocation optimization
- `MixinDynamicUniformStorage` — dynamic uniform storage optimization

### Cross-version conditionals (`//? if` for API compatibility)
| File | Condition | What changes |
|---|---|---|
| MixinResource | `>=1.21.11` | `@Mixin` target: `Identifier.class` vs `ResourceLocation.class` (class name avoids bidirectional replacement) |
| MixinSoundEngine | `>=1.21.11` | Camera API: `position()`/`xRot()`/`yRot()` vs `getPosition()`/`getXRot()`/`getYRot()` |
| MixinRenderSystem | `>=1.21.11` | Method desc: `ShaderSource` vs `BiFunction`; `@Unique` config field + return-original check |
| MixinARGB | `>=1.21.2` | Uses `ARGB` class (>=1.21.2) vs `FastColor.ARGB32` (<1.21.2) |
| MixinMth | `>=1.21.10` | Adds `packDegrees`/`unpackDegrees` overwrites |
| MixinMinecraft | `>=1.21.9` | `InputConstants.isKeyDown(window, key)` vs `InputConstants.isKeyDown(handle, key)` (Window API) |
| MixinGameRenderer | `<26.1` | `//? if <26.1` activates `extractCamera` overwrite (26.x uses different rendering pipeline) |
| MixinVertexConsumer | `>=26.1` | Entire file (needs `putBulkData` signature from 26.x) |
| MixinFaceBakery | `>=26.1` | Import: `cuboid.FaceBakery` vs `block.model.FaceBakery` |
| MixinItemTransform | `>=26.1` | Import: `cuboid.ItemTransform` vs `block.model.ItemTransform` |
| MixinMaterial | `>=26.1` | Record fields: `sprite`/`forceTranslucent` vs `atlasLocation`/`texture` |
| MixinBufferBuilder | `>=26.2` | `beginElement(int)` vs `beginElement(VertexFormatElement)` |
| MixinMeshData | `>=26.2` | `decodeQuadCentroids` vs `unpackQuadCentroids` |
| MixinMeshDataSortState | `>=26.2` | `IndexType` top-level vs `VertexFormat.IndexType` inner class |
| MixinDynamicUniforms | `>=26.2` | Optional fields/API adaptation |
| MixinTextureAtlas | `>=26.2` | `createRenderPass` depth stencil: `Optional.empty()` vs `OptionalInt.empty()` |

### Per-version access wideners/transformers
- **Fabric:** `src/main/resources/aw/<version>.accesswidener`
- **NeoForge:** `versions/<ver>-neoforge/src/main/resources/META-INF/accesstransformer.cfg`
  - 26.1.2: `public net.minecraft.client.animation.KeyframeAnimation$Entry`

## Current Mixin Count (1.21.11 base: 51 total)

### Client (45)
| Category | Mixins |
|---|---|
| **Audio** | Channel, Listener, SoundBuffer, SoundEngine |
| **Blaze3D Buffers** | Std140SizeCalculator, Std140Builder, GpuBuffer |
| **Blaze3D Platform** | InputConstants, NativeImage, TextureUtil |
| **Blaze3D Vertex** | MeshData, MeshDataSortState, VertexFormat, VertexConsumer, BufferBuilder, ByteBufferBuilder, PoseStackPose |
| **Math** | Transformation, Quadrant |
| **Camera** | Camera |
| **Animation** | KeyframeAnimation |
| **GUI** | TextureSetup, FontSet |
| **Model** | Model, ModelPart, ModelPartCube |
| **Particle** | SingleQuadParticle |
| **Resources** | Material, QuadCollectionBuilder |
| **Renderer** | GameRenderer, DynamicUniformStorage, DynamicUniforms, MultiBufferSource |
| **Block** | ModelBlockRenderer, FaceBakery, ItemTransform, BakedQuad |
| **Texture** | SpriteContents, TextureAtlas |
| **Minecraft** | Minecraft |
| **Accessors** | CameraNearPlane, InvokerQuadCollectionBuilder |

### Main (6) — client + server
BlockableEventLoop, ArrayListDeque, ByIdMap, Mth, ARGB, MixinResource

### Utils (2)
SpriteContentsHelper (interface), VertexConsumerHelper

## Key Cross-Version Notes
- **RenderType package:** `net.minecraft.client.renderer.rendertype.RenderType` (1.21.11+) vs `net.minecraft.client.renderer.RenderType` (< 1.21.11). Source uses 1.21.11 name; Stonecutter replacement converts for older versions.
- **Audio package:** Files live at `com/mojang/audio/` on disk but declare `package lomka.starl.mixins.com.mojang.blaze3d.audio`. JSON entries must use the declared package (`blaze3d.audio`), NOT the filesystem path.
- **Camera API:** `position()`/`xRot()`/`yRot()` (1.21.11+) vs `getPosition()`/`getXRot()`/`getYRot()` (< 1.21.11). MixinSoundEngine uses `//? if >=1.21.11` conditional.
- **MixinDynamicUniforms:** Class doesn't exist in 1.21.1; different `writeTransform` signature in 1.21.10 (extra `float lineWidth`). Excluded for < 1.21.11.
- **MixinTextureAtlas:** `uploadInitialContents()` doesn't exist < 1.21.11. Excluded.
- **defaultRequire: 1** — all listed mixins must exist in JAR at runtime or crash with `ClassNotFoundException`.

## Optimization Implementations

### Render Pipeline
- **DynamicUniforms** (`writeTransform`): skips redundant Vector4f/Vector3f copies for common constants
- **MultiBufferSource**: cached RenderType array + pre-sized `startedBuilders` HashMap (capacity 16)
- **TextureAtlas**: closes all sprite contents after `uploadInitialContents()` RETURN

### Texture Memory
- **SpriteContents**: implements `SpriteContentsHelper` with `lomka$releaseUseuselessMipmaps()` (closes mipmap images except [0])
- **TextureUtil**: replaces ArrayList-based BFS with IntArrayFIFOQueue and packed int coordinates for texture solidification

### Resources
- **MixinResource**: caches `toString()` (namespace:path string) and `hashCode()` on Identifier/ResourceLocation — avoids recomputation on every call

### Sound
- **SoundEngine**: skips `updateSource` when camera position/rotation unchanged

## Jar Sizes (current)
| Variant | Size |
|---|---|
| 1.21.1-fabric | 42.2 KB |
| 1.21.1-neoforge | 37.9 KB |
| 1.21.10-fabric | 84.5 KB |
| 1.21.10-neoforge | 81.1 KB |
| 1.21.11-fabric | 99.1 KB |
| 1.21.11-neoforge | 95.9 KB |
| 26.1.2-fabric | 94.2 KB |
| 26.1.2-neoforge | 74.8 KB |

Note: 26.1.2-neoforge is smaller because API-changed mixins are excluded (NeoForge lacks Loom's node-level deduplication).

## Optimization Philosophy
- **No Compromises:** `@Overwrite` in hot-paths to bypass Mixin CallbackInfo overhead.
- **Zero Allocations:** Frame/tick methods must not allocate new objects.
- **Hardware Acceleration:** Use JVM intrinsics and hardware features where possible.

## SOURCE CODE Directory

`SOURCE CODE/` contains decompiled and modified Minecraft source code for each supported version, plus cross-version documentation:
- `DIFFRENCE.md` — Deep cross-version comparison: mixin presence matrix, per-method/field/annotation analysis, version-specific overrides (71 files), Stonecutter conditionals, API breakpoints, utility class internals. Covers all 49 base mixins + 6 extras across 1.21.1–26.2. Intended as reference for AI models and developers.
- Version subdirectories (`1.21.1 SOURCE CODE/`, etc.) — Decompiled MC sources used for reference when writing mixins or debugging version-specific issues.

## Development Rules
- **No Comments:** Code must be self-documenting.
- **Deletion rule:** Never delete directly; use `@Deprecated` or move to `legacy/`.
- Logs must be in English.
- Scope: Only features with direct FPS/RAM/TPS impact.
