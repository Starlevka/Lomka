![Lomka icon](https://cdn.modrinth.com/data/cached_images/5e22062d6564d104d3742aa7ec947aa2d394a2da_0.webp)

<sub><i>The project code was created with help of Artificial Intelligence.</i></sub>

An open source mod with mix of small optimizations for your Minecraft. **Fewer stutters, more FPS, faster resource loading and slightly reduces memory usage**.

## Optimizations
- Faster hot-path math and color engine
- Less garbage in render and tick loops
- Caching where vanilla repeats work sometimes
- Faster block collision scans
- Lightning and textures light engine and chunk tracking speedups
- Cheaper resource scanning and metadata reads
- Leaner network buffers (without affecting on your connection)
- Fewer redundant OpenGL calls
- And more other small bugfixes in the game's code

## Versions
**1.21.11 is the main version.** It carries the full set of optimizations and gets the most testing.

Other supported versions (1.20.1-26.2) are ports of the same or with the less of features set.

## FAQ
**I see performance drops with Lomka installed**

Update to the latest build for your MC version first. Try out Lomka's best versions: **0.2.0**, **0.2.1**, **0.4.x**

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