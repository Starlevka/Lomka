# Changelog (Legacy)

Release notes for the legacy builds: Minecraft **1.7.10**, **1.8.9** and **1.12.2**, on **Forge**
and on **Ornithe**. The modern variants (1.16.5 - 26.3) are tracked in [CHANGELOG.md](CHANGELOG.md).

## [0.7.0] - 2026-10-10

First legacy release, on both loaders: **4** patches on 1.7.10 Forge, **8** on 1.8.9, **12** on 1.12.2;
**6** on 1.7.10 Ornithe, **10** on 1.8.9, **11** on 1.12.2.

### New
- Forge support for **1.7.10**, **1.8.9** and **1.12.2**, in a separate tree with its own Gradle builds.
  Stonecutter cannot run there (it needs Gradle 7.4+ and the Kotlin DSL, and 1.8.9 sits on Gradle 2.14.1),
  so a patch is version-gated by a `//$ if >=1.12` marker on line 1, processed by `gradle/preprocess.gradle`
  - the same directive syntax the modern `json5` config uses. Each build's mixin config is **generated**
  from the filtered tree, so a patch that does not exist for a version cannot appear in that version's
  config.
- **Ornithe support for the same three versions**, alongside the Forge ones:
  `lomka-<version>-ornithe+<mc>.jar`. It is a second source tree rather than a loader switch, because
  Feather - the mapping set Ornithe compiles against - names classes the way 1.16+ does (`Direction`,
  `Box`, `NbtIntArray`, `Identifier`), a different namespace from the MCP names the Forge tree has to
  use. The two trees share only the pure-math kernels; every `@Mixin` shell is per-namespace.
  One toolchain covers all three (`fabric-loom` + Ploceus), where Forge needs three: ForgeGradle 1.2
  on Gradle 6.9.1 for 1.7.10, ForgeGradle 2.1.6 on Gradle 2.14.1 for 1.8.9 (the only line that serves
  1.8.9 `userdev`), and RetroFuturaGradle 2.0.6 on Gradle 9 for 1.12.2 - and RetroFuturaGradle
  supports 1.7.10 and 1.12.2 only, never 1.8.9.
- **No external mixin runtime on Ornithe.** The Ornithe loader ships sponge-mixin and
  access-widener itself, so there is no MixinBooter / UniMixins dependency to resolve, no
  `mcmod.info` dependency block to get the `modid:range` format right, and nothing to ask players to
  install. Access widening is an access widener on all three versions, where 1.12.2 Forge has none.
  The Ornithe entrypoint is OSL's `net.ornithemc.osl.entrypoints.api.ModInitializer` - the Fabric API
  classes do not exist on these Minecraft versions - and the same runtime toggle and `//$ if` gating
  run through the same preprocessor.
- The legacy Forge build directories are now `legacy/legacy-forge-<mc>` rather than
  `legacy/legacy-<mc>`, so they read as a pair with `legacy/legacy-ornithe-<mc>`.
- Runtime per-mixin toggle (`config/lomka-mixins.properties`) through a legacy-specific
  `LomkaMixinPlugin`: Log4j instead of SLF4J (which only arrives in 1.17), no Gson (`parseString` needs
  2.8.6, 1.12.2 ships 2.8.0), and no JOML system properties (these versions use Mojang math).
- Mixin runtime is an external mod, never shaded: **MixinBooter 11.17** on 1.8.9 and 1.12.2,
  **UniMixins 1.7.10-0.3.1** on 1.7.10. Registration is the `MixinConfigs` manifest attribute, so there
  is no coremod / `IFMLLoadingPlugin` class. 1.12.2 ships `lomka.refmap.json`; 1.7.10 and 1.8.9 remap in
  the reobf step and need no refmap.
- Per-version `mcmod.info`, with `logoFile` as a bare `logo.png` in the jar root - 1.19.2's
  `AbstractPackResources#getRootResource(String)` throws on any path containing `/`, and the same rule
  applies here. The `credits` field is gone: the ports it named are not in this tree, and Forge shows it
  only on the mod list entry.
- 1.8.9 and 1.12.2 declare their mixin runtime to the loader itself, `mixinbooter:[11.17,)`. FML parses
  `mcmod.info` dependencies as `modid:range` with a lowercase id, so the display-name form is a load
  error. 1.7.10 declares nothing: UniMixins ships no `mcmod.info`, so there is no id for FML to resolve
  and declaring one aborts the load. The 1.7.10 requirement is a platform-level one instead.
- An access transformer is shipped and declared (`FMLAT: accesstransformer.cfg`) but has no entries yet:
  a declared-but-missing transformer fails Forge at load, an empty one does not. Entries become necessary
  when an `@Overwrite` body calls a private method, or when an `@Invoker`/`@Accessor` is added.

### Performance
- `MathHelper.smallestEncompassingPowerOfTwo` / `log2`: the five-step bit spread and the De Bruijn table
  lookup replaced with a single `Integer.numberOfLeadingZeros` intrinsic, reproducing vanilla's edge
  cases exactly (`0` for `n <= 0` on the first, `-1` on the second). (all versions)
- `MixinMinecraft`: the per-frame `Thread.yield()` in `runGameLoop` is redirected away. It is a scheduler
  hint rather than an optimization, recorded so the trade is visible. (all versions)
- `AbstractResourcePack`: `pack.mcmeta` is parsed once per pack instead of on every metadata query - three
  call sites per pack per resource reload - and `locationToName` loses its `String.format`. Loading path,
  not frame path. (all versions)
- `AABB#calculateIntercept` on 1.12.2, and the equivalent `EnumFacing`/int-returning body on 1.7.10 and
  1.8.9: vanilla allocates one `Vec3d` per plane it tests, up to six per ray trace. The candidate point
  is kept in locals and the planes are tested directly. (all versions)
- `EnumFacing#rotateAround` and its counter-clockwise sibling: flat lookup tables instead of nested enum
  dispatch. `getOpposite` and `random` are deliberately left alone - PolyPatcher already overwrites both
  on 1.12.2 with byte-identical bodies. (1.8.9, 1.12.2)
- `Vec3i#hashCode`: phi-mix (from `efficient_hashing` by ZZZank, CC0) instead of vanilla's weak
  `(y + z * 31) * 31 + x`, which collapses a large share of a dense 64^3 region onto few distinct values.
  1.12.2 carries the identical degree-2 polynomial, so the body is a transcription. (1.8.9, 1.12.2)
- `NBTTagIntArray` / `NBTTagLongArray` bulk `load`: 8 KB blocks through a big-endian `ByteBuffer` instead
  of four/eight `read()` calls per element. Taken only for a `DataInputStream`, i.e. the disk path (region
  files, player data, `level.dat`); `Heightmaps` long arrays are where it pays on a server. (1.8.9 int
  arrays, 1.12.2 both)
- `NettyCompressionEncoder` / `NettyCompressionDecoder`: the per-packet `new byte[readableBytes]` is
  reused, capped so one oversized chunk packet cannot pin a permanent high-water buffer. (1.12.2)
- `FallbackResourceManager`: the `.mcmeta` location is built only once a pack actually has metadata,
  instead of one string concat plus one `ResourceLocation` per lookup before any pack is consulted. (1.12.2)
- `Framebuffer` present: the fullscreen blit quad and its half-dozen state toggles are replaced with one
  hardware `glBlitFramebuffer(NEAREST)`. (1.8.9, 1.12.2)
- On Ornithe the same patches carry Feather names: `RenderTarget#draw(int, int, boolean)` is the
  present, `Box#clip` the ray trace, `FallbackResourceManager` unchanged. `RenderTarget` and
  `Direction#clockwise(Axis)` also ship on Ornithe, where the rotation is a flat lookup table
  anchored on the three private `clockwiseX/Y/Z` helpers rather than on `clockwise` itself.
  (Ornithe, 1.8.9 and 1.12.2)
- `Vec3i#hashCode` phi-mix also runs on Ornithe on all three versions, at `priority = 999` so a
  foreign body lands on top. (Ornithe, all versions)

### Notes
- `MixinDirection` and the Ornithe `MixinMathHelper` are cancellable HEAD `@Inject`s rather than
  `@Overwrite`s. On those classes `@Overwrite` cannot resolve a target at all - `Direction` fails
  because it is first loaded *through* `Direction$Axis`, `MathHelper` fails on 1.7.10 - while the
  identical bodies expressed as injectors apply. The cost is one `CallbackInfoReturnable` per call.