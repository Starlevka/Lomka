# Mixin Configuration (legacy)

This page documents the **legacy** patches for **Minecraft 1.7.10, 1.8.9 and 1.12.2**, on **Forge** and on **Ornithe**.
For Minecraft 1.16.5 - 26.3 see
[Mixin Configuration](https://github.com/Starlevka/Lomka/wiki/Mixin-Configuration).

These builds live in a separate source tree with their own Gradle setup and carry a much smaller
patch set. Forge: **4** patches on 1.7.10, **8** on 1.8.9, **12** on 1.12.2.
Ornithe: **6** patches on 1.7.10, **10** on 1.8.9, **11** on 1.12.2.

## Requirements

Lomka does not bundle a mixin runtime on these versions. Install one alongside it:

| Minecraft | Required |
|---|---|
| 1.7.10 | **UniMixins (1.7.10-0.3.1+)** |
| 1.8.9 | **MixinBooter 11.17+** |
| 1.12.2 | **MixinBooter 11.17+** |
| Ornithe, any version | nothing — the Ornithe loader ships sponge-mixin and the access widener itself |

The dependency is declared to the loader itself on 1.8.9 and 1.12.2 (`mixinbooter:[11.17,)` in
`mcmod.info`). On 1.7.10 nothing is declared, because UniMixins ships no mod id for FML to resolve —
set the requirement on the mod platform page instead.

No Fabric API, no library, no core mod.

## How the toggle works

The same mechanism as the modern builds: on first launch a comment-only
`config/lomka-mixins.properties` is created, and every line you add forces one patch on or off.

```properties
# fully qualified mixin name = true|false
net.minecraft.util.MixinEnumFacing = false
```

- **Missing keys always mean enabled.** Patches added by an update work without editing the file.
- The fully qualified name, the name without the `lomka.starl.mixins.` prefix, and the simple
  class name (`MixinEnumFacing`) all resolve to the same patch.
- Only `true` / `1` enable; anything else disables the patch.
- Keys that are not in your build are reported once at startup as `unknown key(s) ignored`, and
  disabled patches as `Disabled N mixin(s): ...`.
- Deleting the file restores the all-enabled default.

**Warning:** disabling patches reduces performance and may change behavior. Unlike the modern
tree there are no inter-dependent patch pairs here, so any single patch can be turned off safely.

The plugin is `lomka.starl.config.LomkaMixinPlugin`. It is a separate class from the modern one,
written for Java 8: it uses Log4j instead of SLF4J (SLF4J only arrives in Minecraft 1.17), and it
validates keys by reading the `lomka.mixins.json` inside its own jar without Gson, which would need
a version those games do not ship. It sets no JOML system properties — these versions use Mojang
math.

## Patches

The tables below are the **Forge** tree. Ornithe is a separate source tree with its own keys - see
**Ornithe patches** below.

### Forge client patches

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.MixinMinecraft` | Redirects away the `Thread.yield()` at the end of every `runGameLoop`. It is a scheduler hint rather than a real optimization — recorded so the trade (very slightly more CPU for lower input latency) is visible. | all |
| `net.minecraft.client.resources.MixinAbstractResourcePack` | `pack.mcmeta` is parsed once per pack instead of on every metadata query (three call sites per pack per resource reload), and `locationToName` loses its `String.format`. Loading path, not frame path. | all |
| `net.minecraft.client.shader.MixinFramebuffer` | The fullscreen blit quad and its half-dozen state toggles are replaced with one hardware `glBlitFramebuffer(NEAREST)` when blending is off. | 1.8.9, 1.12.2 |
| `net.minecraft.client.resources.MixinFallbackResourceManager` | The `.mcmeta` location is built only once a pack actually has metadata, instead of one string concat plus one `ResourceLocation` per lookup before any pack is consulted. | 1.12.2 |

### Forge common patches

These are registered in the common `mixins` list, so they apply on a dedicated server too.

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.util.math.MixinMathHelper` | `smallestEncompassingPowerOfTwo` / `log2`: the five-step bit spread and the De Bruijn table lookup replaced with a single `Integer.numberOfLeadingZeros` intrinsic, reproducing vanilla's edge cases exactly. | all |
| `net.minecraft.util.MixinAxisAlignedBB` | `calculateIntercept` with the candidate point kept in locals: vanilla allocates one `Vec3` per plane it tests, up to six per ray trace. Bit-identical over a million fuzzed ray/box pairs. | 1.7.10, 1.8.9 |
| `net.minecraft.util.math.MixinAxisAlignedBB` | The 1.12 half of the same patch. It is a separate file only because the `AxisAlignedBB` package itself moves at 1.12, so one file could not keep its own path consistent with its package across the three builds. | 1.12.2 |
| `net.minecraft.util.MixinEnumFacing` | `rotateAround` and its counter-clockwise sibling via flat lookup tables instead of nested enum dispatch. `getOpposite` and `random` are deliberately left alone — PolyPatcher already overwrites both with byte-identical bodies. | 1.8.9, 1.12.2 |
| `net.minecraft.util.math.MixinVec3i` | `hashCode` via phi-mix instead of vanilla's weak `(y + z * 31) * 31 + x`, which collapses a large share of a dense 64³ region onto few distinct values. `priority = 999`, so a foreign hashing mod lands on top. | 1.8.9, 1.12.2 |
| `net.minecraft.nbt.MixinNBTTagIntArray` | Bulk `load` on a `DataInputStream` — 8 KB blocks through a big-endian `ByteBuffer` instead of four `read()` calls per element. Disk path only (region files, player data, `level.dat`); network and buffer inputs keep vanilla's loop. | 1.8.9, 1.12.2 |
| `net.minecraft.nbt.MixinNBTTagLongArray` | The same for `long[]`. Chunk `Heightmaps` are long arrays, which is where it pays on a server. | 1.12.2 |
| `net.minecraft.network.MixinNettyCompressionEncoder` | The per-packet `new byte[readableBytes]` is reused, and only stored back when it stays under 64 KB so one oversized chunk packet cannot pin a permanent high-water buffer. | 1.12.2 |
| `net.minecraft.network.MixinNettyCompressionDecoder` | The same reuse for the compressed payload. The output array is still a fresh one, because it escapes into the pipeline. | 1.12.2 |

### Ornithe patches

Ornithe is a second source tree rather than a loader switch: the Feather mappings it compiles against
name classes the way 1.16+ does (`Box`, `Direction`, `NbtIntArray`), a different namespace from the
MCP names the Forge tree has to use. The two trees share only the pure-math kernels, so none of these
keys overlap with the Forge ones above.

**Versions** shows which Ornithe builds contain the patch.

| Key | Description | Versions |
|---|---|---|
| `net.minecraft.client.MixinMinecraft` | Redirects away the `Thread.yield()` at the end of every `runGameLoop`, as on Forge | all |
| `net.minecraft.client.resource.manager.MixinFallbackResourceManager` | The `.mcmeta` location is built only once a pack actually has metadata. Forge carries this patch on 1.12.2 only under its MCP name; here it is the Feather name and runs on all three versions | all |
| `net.minecraft.client.render.pipeline.MixinRenderTarget` | The fullscreen blit quad and its state toggles replaced with one hardware `glBlitFramebuffer(NEAREST)` — the `RenderTarget#draw(int, int, boolean)` present, which is the Forge `MixinFramebuffer` patch under the Feather name | 1.8.9, 1.12.2 |
| `net.minecraft.util.math.MixinMathHelper` | `smallestEncompassingPowerOfTwo` / `log2` through a single `Integer.numberOfLeadingZeros` intrinsic, as on Forge | all |
| `net.minecraft.util.math.MixinVec3i` | `hashCode` via phi-mix, `priority = 999` so a foreign hashing mod lands on top | all |
| `net.minecraft.util.math.MixinBox` | The ray trace of `Box#clip`, the Feather-named half of the Forge `MixinAxisAlignedBB` patch: the candidate point stays in locals instead of one `Vec3d` per plane tested | all |
| `net.minecraft.util.math.MixinDirection` | `Direction#clockwise(Axis)` through a flat lookup table anchored on the three private clockwise helpers | 1.8.9, 1.12.2 |
| `net.minecraft.nbt.MixinNbtIntArray` | Bulk `load` on a `DataInputStream` — 8 KB blocks through a big-endian `ByteBuffer` instead of four `read()` calls per element. Disk path only; network and buffer inputs keep vanilla's loop | all |
| `net.minecraft.nbt.MixinNbtLongArray` | The same for `long[]`; chunk `Heightmaps` are long arrays, which is where it pays on a server | 1.12.2 |
| `net.minecraft.network.MixinCompressionEncoder` | The per-packet buffer is reused and shrunk again past 64 KB, so one oversized chunk packet cannot pin a high-water buffer | 1.8.9, 1.12.2 |
| `net.minecraft.network.MixinCompressionDecoder` | The same reuse for the compressed payload; the output array stays fresh because it escapes into the pipeline | 1.8.9, 1.12.2 |

Ornithe requires nothing from the player: the loader ships sponge-mixin and the access widener
itself, so there is no MixinBooter / UniMixins dependency, no `mcmod.info` dependency block to get
the `modid:range` format right, and no core mod.

## Shared helpers

Two non-mixin classes, outside the mixin package for the same reason as the modern tree:

- `lomka.starl.utils.constants.EnumFacingTables` (Forge) and `lomka.starl.utils.constants.DirectionTables`
  (Ornithe) — the 18-entry clockwise table read, by `MixinEnumFacing` and `MixinDirection`.
  Kept outside the mixin package on purpose: a class inside a configured mixin
  package cannot be loaded by the mixin plugin loader.
- `lomka.starl.utils.nbt.NbtBulkReader` — the thread-local 8 KB scratch and the block-size
  thresholds shared by the two NBT array patches. Thread-local because chunk NBT is read on the
  server's IO workers; created lazily so a client that never reads a chunk pays nothing.