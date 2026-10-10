<div align="center">
  
<sub>![Lomka icon](https://cdn.modrinth.com/data/cached_images/5e22062d6564d104d3742aa7ec947aa2d394a2da_0.webp)

<i>The project code was created with help of Artificial Intelligence.</i></sub>
</div>
<div align="center">
<a href="https://modrinth.com/mod/lomka"><img alt="Available on Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/lomka"><img alt="Available on CurseForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/curseforge_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka"><img alt="GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka/issues"><img alt="Issues" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/issues_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka/wiki"><img alt="Wiki" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/gitbook_vector.svg"></a>
</div>

An open source mod with mix of small optimizations for your Minecraft. **Less stutters, slightly faster resource loading and world generation, less memory usage**.

The main thing Lomka does is reduce the number of redundant garbage (memory) allocations and improves game calcs.

## Optimizations
**Frame time**
- Fewer allocations in the render, tick and collision loops (vertex writes, pose stacks, entity block sweeps)
- Cached voxel shape boxes, block face-sturdiness and font glyph advances
- Cheaper math and color engine
- Faster texture atlas animation (lazily cycle)
- Playing a sound skips the wait for the sound thread
- Removing `Thread.yield()` from the client tick loop

**Loading and memory**
- Bulk NBT array reads for world, player and structure data on the server side (chunks, heightmaps and saves load in blocks instead of per-element stream calls)
- Cheaper resource scanning and cached pack metadata reads
- Generated mip levels releases after upload
- Fewer native buffer reallocations (audio streaming and mesh building)

**World and network**
- Faster block and section traversal, heightmapping for world generation, and in-place worldgen RNG reseeds (also, check the incompatibilities list first)
- Leaner network buffers and packet compression (without affecting your connection)

And some other small bugfixes in the game's code.

### Measured effect

Isolated microbenchmarks on JDK 25, one operation at a time.

That's **not** in game frame rates benchmarks:

| Patch | Measured | What you notice it in |
|---|---|---|
| Voxel shape decomposition | `toAabbs()`: 40 ns vs 8584 ns, **199x+** (14 ns vs 8879 ns on 368 cells) | Every block an entity touches **while moving** |
| `MixinLegacyRandomSource` | `next(int)`: **7.1x** | Every random draw a new chunk needs, on the client and dedicated server. |
| `MixinBlockPos.betweenClosed` | one 16³: 61 ms -> 11 ms, **5.61x** | `/fill` and other volume edits, the lighting pass after a large build, structure and feature placement, explosion block lists. |
| `MixinBlockStateBaseCache` | `isFaceSturdy`: **4.1-5.8x** | Checks such as walking on slabs and stairs, collision resolution between mobs and players, support recomputation when a neighbour changes. |
| `MixinCompressionDecoder` | `setupInflaterInput`: 37.5 ns -> 18.1 ns **and 128 B -> 0 B** per packet | Every incoming compressed packet |
| `MixinSoundEngine` | sound acquisition: 400 us -> 20 us, stall per tick 512 us -> 27 us | Client thread stops waiting for the sound thread. |
| `MixinAutoStorageIndexBuffer` | index buffer grows x4: 1.47-2.9x less time reallocating (85.4 ms -> 29.5 ms, 21 -> 11 reallocations; retains 40 MiB instead of 34) | Geometry spikes (for example, pulling the render distance) |
| `MixinFallbackResourceManager` | metadata lookup: **5.33x** (2.71 ms -> 0.51 ms for 20k files) | Startup and every resource reload (F3+T, still slightly improving) |
| `MixinMth` | `ceillog2` and `log2`: 3.93 ns -> 0.85 ns (**4.6x**), 4.83 ns -> 0.84 ns (**5.7x**) | Palette sizes, section and buffer counts, every place vanilla asks for a bit length |

## Versions
**1.21.11 is the main version.** It carries the full set of optimizations and gets the most testing.

Modern variants:

| Minecraft | Loaders |
|---|---|
| 1.16.5 | Fabric |
| 1.18.2 | Fabric, Forge |
| 1.19.2 | Fabric, Forge |
| 1.20-1.20.1 | Fabric, Forge |
| 1.21-1.21.1 | Fabric, NeoForge |
| 1.21.4 | Fabric, NeoForge |
| 1.21.6-1.21.8 | Fabric, NeoForge |
| 1.21.9-1.21.10 | Fabric, NeoForge |
| 1.21.11 | Fabric, NeoForge |
| 26.1-26.1.2 | Fabric, NeoForge |
| 26.2 | Fabric, NeoForge |
| 26.3 | Fabric, NeoForge |

Legacy builds cover **1.7.10**, **1.8.9** and **1.12.2**, on **Forge** and on **Ornithe**. These are a separate port with a much smaller patch set and their own patch list. The Forge variants need a mixin runtime installed alongside Lomka (**MixinBooter 11.17** on 1.8.9 and 1.12.2, **UniMixins** on 1.7.10); the Ornithe variants need nothing, because the Ornithe loader ships the mixin runtime itself. See
[Mixin Configuration (legacy)](https://github.com/Starlevka/Lomka/wiki/Configuration-Legacy).

### ❗ Everything outside 1.21.11 is a port of the same or other ideas with a smaller improvement.

## FAQ
**I see performance drops with Lomka installed**

Update to the latest build for your Minecraft version first.

Still bad? Report it (link below) with your game version, loader, mod list, and which config you had turned off.

**Does it change vanilla behaviour?**

Nope, of course, but if it does weird game changes reply in [the issue tracker](https://github.com/Starlevka/Lomka/issues).

**Is it server-side compatible?**

Yes, it is.

**Does it need any dependencies?**

No. The only external requirement is the mixin runtime, and only on the legacy **Forge** builds (1.7.10-1.12.2). The Ornithe ones ship their own.

**Can I add this mod in my modpack?**

Sure. Lomka is free and open source. Check out how it goes on your modpack first.

## Configuration
Lomka has no in-game GUI. Instead, on first launch it creates `config/lomka-mixins.properties` with basic mixin configuration.

The full list of mixins with descriptions lives in the wiki:

[Mixin Configuration](https://github.com/Starlevka/Lomka/wiki/Configuration) for 1.16.5-26.3.

[Mixin Configuration (legacy)](https://github.com/Starlevka/Lomka/wiki/Configuration-Legacy) for 1.7.10-1.12.2.

## Compatibility
This should work fine with most mods (like Sodium, Lithium, Iris, FerriteCore, ModernFix, ImmediatelyFast and even VulkanMod) but no guarantees at all.

Known conflicts are listed with Lomka versions in the [incompatibilities list](https://github.com/Starlevka/Lomka/wiki/Incompabilities-with-mods).

## Bug reports
Found something that only happens with Lomka?

1. Try reproducing it without Lomka first, and with the individual patches disabled.
2. Still there? Post it on [the issue tracker](https://github.com/Starlevka/Lomka/issues) with an optional log attached.

Please don't report Lomka-related crashes to other mod authors, because Lomka's mixins are cruel that could break unrelated systems.

## Credits

Lomka was created by Starlev and contributors.

Also, other mods credits for their's effecient work!
- `VecDeltaCodec` zero-delta path adapted from Collections Of Optimizations by Misanthropy (LGPL-3.0).
- `Vec3i` PhiMix hashing credits EfficientHashing by ZZZank (CC0-1.0).
- The legacy hardware-blit presentation path references from Tritium (MIT), but popularized in VulkanMod (LGPL-3.0-only).