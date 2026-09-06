![Lomka icon](https://cdn.modrinth.com/data/cached_images/5e22062d6564d104d3742aa7ec947aa2d394a2da_0.webp)
<div align="center">
  
<sub><i>The project code was created with help of Artificial Intelligence.</i></sub>
</div>
<div align="center">
<a href="https://modrinth.com/mod/lomka"><img alt="Available on Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/lomka"><img alt="Available on CurseForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/curseforge_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka"><img alt="GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka/issues"><img alt="Issues" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/issues_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka/wiki"><img alt="Wiki" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/gitbook_vector.svg"></a>
</div>

An open source mod with mix of small optimizations for your Minecraft. **Fewer stutters, slightly improvements: more FPS (up to 4.58% increase depends on your hardware), faster resource loading, world generation and reduces memory usage**.

## Optimizations
- Faster hot-path math and color engine
- Less garbage in render and tick loops
- Caching where vanilla repeats work sometimes
- Faster block collision scans
- Cached voxel shape AABBs and allocation-free raycasts
- Cached font glyph advances for faster text rendering
- Fewer native buffer reallocations (audio streaming and mesh building)
- Lightning and textures light engine and chunk tracking speedups
- Cheaper resource scanning and metadata reads
- Leaner network buffers (without affecting on your connection)
- Fewer redundant OpenGL calls
- Removing thread.yield() from render thread
- And more other small bugfixes in the game's code

## Versions
**1.21.11 is the main version.** It carries the full set of optimizations and gets the most testing.

Other supported versions (1.20.1-26.2) are ports of the same or with the less of features set.

## FAQ
**I see performance drops with Lomka installed**

Update to the latest build for your MC version first. Try out Lomka's best versions: **0.2.0**, **0.2.1**, **0.4.x** and **0.5.x**

Still bad? Report it (link below).

**Is it server-side compatible?**

Yes, it is.

**Will be added in the future more forge support?**

No, only for 1.20.1 at the moment.

**Will be mod backports below Minecraft version 1.20.1?**

No. The mod closely follows modern Minecraft internals. Surely, it possible but not too needly.

**Can I add this mod in my modpack?**

Sure. Lomka is free and open source. Check out how it goes on your modpack first.

## Configuration
Lomka has no in-game GUI. Instead, on first launch it creates `config/lomka-mixins.properties` with basic mixin configuration.

The full list of mixins with descriptions lives in the [wiki](https://github.com/Starlevka/Lomka/wiki/Configuration).

## Compatibility
This should work fine with most of mods (like Sodium, Lithium, Iris, FerriteCore, ModernFix, ImmediatelyFast and even VulkanMod) but no guarantees at all.

Known conflicts are listed with Lomka versions in the [incompabilities list](https://github.com/Starlevka/Lomka/wiki/Incompabilities-with-mods).

## Bug reports
Found something that only happens with Lomka?

1. Try reproducing it without Lomka first.
2. Still there? Post it on [the issue tracker](https://github.com/Starlevka/Lomka/issues) with an optional log attached.

Please don't report Lomka-related crashes to other mod authors, because Lomka's mixins are cruel that could break unrelated systems.

![Lomka icon](https://cdn.modrinth.com/data/cached_images/5e22062d6564d104d3742aa7ec947aa2d394a2da_0.webp)
<div align="center">
  
<sub><i>The project code was created with help of Artificial Intelligence.</i></sub>
</div>
<div align="center">
<a href="https://modrinth.com/mod/lomka"><img alt="Available on Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/lomka"><img alt="Available on CurseForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/curseforge_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka"><img alt="GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka/issues"><img alt="Issues" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/issues_vector.svg"></a>
<a href="https://github.com/Starlevka/Lomka/wiki"><img alt="Wiki" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/gitbook_vector.svg"></a>
</div>

An open source mod with mix of small optimizations for your Minecraft. **Fewer stutters, slightly improvements: more FPS (up to 4.58% increase depends on your hardware), faster resource loading, world generation and reduces memory usage**.

## Optimizations
- Faster hot-path math and color engine
- Less garbage in render and tick loops
- Caching where vanilla repeats work sometimes
- Faster block collision scans
- Cached voxel shape AABBs and allocation-free raycasts
- Cached font glyph advances for faster text rendering
- Fewer native buffer reallocations (audio streaming and mesh building)
- Lightning and textures light engine and chunk tracking speedups
- Cheaper resource scanning and metadata reads
- Leaner network buffers (without affecting on your connection)
- Fewer redundant OpenGL calls
- Removing thread.yield() from render thread
- And more other small bugfixes in the game's code

## Versions
**1.21.11 is the main version.** It carries the full set of optimizations and gets the most testing.

Other supported versions (1.20.1-26.2) are ports of the same or with the less of features set.

## FAQ
**I see performance drops with Lomka installed**

Update to the latest build for your MC version first. Try out Lomka's best versions: **0.2.0**, **0.2.1**, **0.4.x** and **0.5.x**

Still bad? Report it (link below).

**Is it server-side compatible?**

Yes, it is.

**Will be added in the future more forge support?**

No, only for 1.20.1 at the moment.

**Will be mod backports below Minecraft version 1.20.1?**

No. The mod closely follows modern Minecraft internals. Surely, it possible but not too needly.

**Can I add this mod in my modpack?**

Sure. Lomka is free and open source. Check out how it goes on your modpack first.

## Configuration
Lomka has no in-game GUI. Instead, on first launch it creates `config/lomka-mixins.properties` with basic mixin configuration.

The full list of mixins with descriptions lives in the [wiki](https://github.com/Starlevka/Lomka/wiki/Configuration).

## Compatibility
This should work fine with most of mods (like Sodium, Lithium, Iris, FerriteCore, ModernFix, ImmediatelyFast and even VulkanMod) but no guarantees at all.

Known conflicts are listed with Lomka versions in the [incompabilities list](https://github.com/Starlevka/Lomka/wiki/Incompabilities-with-mods).

## Bug reports
Found something that only happens with Lomka?

1. Try reproducing it without Lomka first.
2. Still there? Post it on [the issue tracker](https://github.com/Starlevka/Lomka/issues) with an optional log attached.

Please don't report Lomka-related crashes to other mod authors, because Lomka's mixins are cruel that could break unrelated systems.

## Contributing
See [the contibuting guide](https://github.com/Starlevka/Lomka/blob/main/CONTRIBUTING.md).
