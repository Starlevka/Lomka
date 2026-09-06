# Contributing to Lomka

Thanks for your interest in Lomka. This guide targets the **0.5.x** release line (**0.5.2**) and explains how to report issues, propose optimizations and submit pull requests across all 17 build variants.

## Table of Contents
1. [Code of Conduct](#code-of-conduct)
2. [Before You Start](#before-you-start)
3. [Reporting Issues](#reporting-issues)
4. [Performance Contributions](#performance-contributions)
5. [AI-Assisted Contributions](#ai-assisted-contributions)
6. [Development Setup](#development-setup)
7. [Build Scripts & Loaders](#build-scripts--loaders)
8. [Project Structure & Stonecutter](#project-structure--stonecutter)
9. [Mixin Conventions](#mixin-conventions)
10. [Runtime Mixin Configuration](#runtime-mixin-configuration)
11. [Testing & Debugging](#testing--debugging)
12. [Versioning & Changelog](#versioning--changelog)
13. [Pull Request Process](#pull-request-process)
14. [License](#license)

## Code of Conduct

This project follows the spirit of the [Contributor Covenant 2.0](https://www.contributor-covenant.org/version/2/0/code_of_conduct.html). Be respectful, constructive and inclusive.

* Examples of positive behavior: empathy, respectful disagreement, gracefully accepting feedback, focusing on the community.
* Examples of unacceptable behavior: harassment, sexualized language, trolling, personal attacks, doxxing, other unprofessional conduct.

This applies in all community spaces (issues, PRs, discussions) and when representing the project. Violations may be reported via GitHub Issues or by contacting the maintainers through the repository. Reports are handled confidentially. Enforcement follows a ladder: correction → warning → temporary ban → permanent ban.

No dedicated conduct email is set up, use GitHub Issues for now. If you prefer a private channel, open an issue titled `CoC report` and request a private contact.

## Before You Start

* Lomka is `LGPL-3.0-only`, contributions use the same license.
* Lomka is built with **Stonecutter 0.9.7**: all sources live in one tree and compile directly for `1.21.11-fabric` (the VCS version). The other 16 variants are generated from the same sources by per-version swaps, constants and conditionals.
* Search existing [issues](https://github.com/Starlevka/Lomka/issues) to avoid duplicates.

## Reporting Issues

Use the templates in `.github/ISSUE_TEMPLATE/`:

* `bug_report.yml`: crashes, desyncs, visual glitches
* `performance_request.yml`: FPS / tick regressions, stutters
* `mod_incompatibility.yml`: conflicts with Sodium, Lithium, Iris, VulkanMod, etc.

Include:

1. Lomka version (`mod version` + `MC version` + `loader`, e.g. `0.5.2 1.21.11-fabric`).
2. Steps to reproduce, expected vs actual behavior.
3. Full log (`logs/latest.log`) and, for performance, a short Spark / `/debug` profile or `F3` screenshot.
4. If a patch is suspected, bisect it with `config/lomka-mixins.properties` (see [Runtime Mixin Configuration](#runtime-mixin-configuration)) and mention which toggles change the behavior.
5. Try reproducing without Lomka first (`README.md` FAQ). Do not report Lomka-related crashes to other mod authors.

## Performance Contributions

Lomka welcomes any optimization, from small cleanups to aggressive micro-optimizations (LUTs, branchless, SWAR, bit-twiddling, loop unrolling). Don't overthink it.

If you can, add a short note on why it's faster and that it keeps vanilla parity. For bigger changes, a quick proof or fuzz vs vanilla is nice to have but not required for small patches. If your change has edge cases, keep the mixin toggleable via `config/lomka-mixins.properties` (missing keys default to enabled, so no migration is needed for users).

Hot paths prefer `@Overwrite`; for Iris compat use cancellable `HEAD` `@Inject` at driver level (`GlStateManager`). Compat-sensitive targets should set a low mixin `priority` so other mods can win (see `MixinByteBufferBuilder` for the VulkanMod example).

## AI-Assisted Contributions

AI-assisted PRs are welcome. Just verify the change (quick test or `srcnav.py` check) and note that it was AI-assisted. Keep your `@author` on the optimization.

## Development Setup

* **JDK 25** must be installed and referenced in `gradle.properties` (`org.gradle.java.home`, currently `jdk-25.0.4`). Per-variant Java toolchains are auto-selected by the build scripts (Java 17 for `1.20.1`, Java 21 for `1.21.x`, Java 25 for `26.x`) via the foojay resolver.
* `gradle.properties` tuning: 2G G1GC heap, `org.gradle.parallel=false`, 2 workers, configuration cache off — keep these unless you have a reason.
* **Build:**
  ```bash
  ./gradlew buildAllVariants      # all 17 variants -> build/libs/<project>/
  ./gradlew :1.21.11-fabric:build # single variant
  ./gradlew licenseHeaders        # add/update the LGPL-3.0-only header
  ```
  `licenseHeaders` also runs automatically before every variant's `compileJava`, so headers self-heal on build.
* **Run:**
  ```bash
  ./gradlew runActiveClient       # client of the active Stonecutter version
  ./gradlew runActiveServer       # server of the active Stonecutter version
  ./gradlew :1.20.1-forge:runClient  # client of any specific variant
  ```
  The active version is tracked in `.sc_active_version` at the repo root; run directories live in `versions/<ver>-<loader>/run/`.
* **Inspect MC sources** (decompiled, all versions + the mod itself):
  ```bash
  py -3 scripts/srcnav.py find <name> [version]
  py -3 scripts/srcnav.py search <regex> [version] --max N --ctx N
  py -3 scripts/srcnav.py lines <name> [range] [version]
  py -3 scripts/srcnav.py method <name> <regex> [version]
  py -3 scripts/srcnav.py diff <name> <v1> <v2>
  py -3 scripts/srcnav.py dirs
  ```
  Version tokens: `1.20.1`, `1.21`, `1.21.4`, `1.21.6`, `1.21.9`, `1.21.11`, `26.1`, `26.2`, `mod` (Lomka sources), `compat` (COMPABILITY).

Base version is `1.21.11-fabric`. Sources compile directly for it; everything else is derived.

## Build Scripts & Loaders

The variant → build script mapping is defined in `settings.gradle.kts`:

| Variants | Build script | Pipeline |
|---|---|---|
| `1.20.1/1.21/1.21.4/1.21.6/1.21.9/1.21.11-fabric` | `build.fabric-o.gradle.kts` | `fabric-loom-remap`: compile (intermediary) → remap (Mojang), double pipeline |
| `26.1/26.2-fabric` | `build.fabric-m.gradle.kts` | `fabric-loom`: single compile on Mojang mappings |
| `1.20.1-forge` | `build.forge.gradle.kts` | `moddev.legacyforge` + explicit `annotationProcessor("org.mixinpowered:mixin:0.8.5:processor")` |
| all `*-neoforge` | `build.neoforge.gradle.kts` | `neoforge moddev`, single pass, Access Transformer based |

Fabric loader `>= 0.19.0`, no Fabric API dependency. Mappings: Mojang official everywhere.

## Project Structure & Stonecutter

```
src/main/java/lomka/
  Lomka.java            # unified entrypoint + loader adapters (fabric/forge/neoforge constants)
  starl/config/         # LomkaMixinPlugin (runtime toggles)
  starl/duck/           # duck interfaces (IBitSetDiscreteVoxelShape, IGlyphSource)
  starl/mixins/         # mirrors MC internal structure; accessor/ for @Accessor/@Invoker
  starl/utils/cache/    # GlStateCache
  starl/utils/math/     # AxisPoseRotate (shared helpers only; since 0.5.x math is inlined in mixins)
src/stonecutter/        # templates: lomka.mixins.json5, lomka.ct (Fabric AW), accesstransformer.ct (NeoForge/Forge AT)
buildSrc/               # build logic (LomkaPlatform.kt: excludeUnlistedMixins, AT/AW wiring)
versions/<ver>-<loader>/ # build + run dirs only, no sources
```

Stonecutter machinery (`stonecutter.gradle.kts`):

* **Swaps:** `mod_version`, `mod_id`, `mod_name`, `mod_group`, `minecraft` — used by templates and the entrypoint.
* **Constants:** `release`, and loader gates `fabric` / `forge` / `neoforge` (parsed from the variant name, used by `Lomka.java`).
* **String replacements** per version range: `ResourceLocation` ↔ `Identifier` and `location()` ↔ `identifier()` (`>=1.21.11`), `rendertype.RenderType` → `renderer.RenderType` (`<1.21.11`), `AtlasManager`/`Material` → `sprite.*` package (`>=26.1`).
* **Conditionals:** `//? if >=1.21.11 { ... //?}` blocks in base sources; else-branches are written as pre-commented `/* */` code. The VCS version treats `//?` lines as comments.
* **Mixin registration:** new mixins must be added to `src/stonecutter/lomka.mixins.json5` (`client` or `main`) with the right version guard — `excludeUnlistedMixins()` in `buildSrc` auto-excludes unlisted mixin files from each variant's compilation, so an unregistered mixin simply won't exist in that build.
* Mod metadata (id, version, description, deps per version) lives in `stonecutter.properties.toml`.

## Mixin Conventions

Keep it simple. The main thing is to credit the optimization:

* **`@Overwrite`**, add Javadoc with `@author YourName` and a short `@reason` why it's faster. If you improve an existing patch, keep the original `@author` and add yours.
  ```java
  /**
   * @author YourName
   * @reason Flat LUT; eliminates 2 cache misses per call.
   */
  @Overwrite
  public Direction getClockWise(Direction.Axis axis) {
      return CLOCKWISE_LUT[axis.ordinal() * 6 + ((Direction)(Object)this).ordinal()];
  }
  ```
  Strict mode (mandatory per-method `@author` + `@reason`, no exceptions): `mixins/net/minecraft/util/**` and `mixins/net/minecraft/core/**`.
* **`@Inject` / `@Redirect` / `@ModifyArg` / `@ModifyConstant`**, just a short `//` or `/* */` comment is enough, no `@author`/`@reason` needed.
* **Class-level shortcut:** for mixins with more than 3 annotated methods or heavy Stonecutter conditionals, a single `@author Starlev`-style comment above `@Mixin` is fine (no `@reason`).
* **English** for logs/comments, and run `./gradlew licenseHeaders` to add the `LGPL-3.0-only` header (or just build — it self-heals).
* Since 0.5.x, pure math lives **inline in the mixin**; put something into `starl/utils/` only when it is genuinely shared by several mixins (like `GlStateCache` or `AxisPoseRotate`).

## Runtime Mixin Configuration

`LomkaMixinPlugin` (`lomka.starl.config`) reads `config/lomka-mixins.properties`, generated with a notice-only stub on first launch:

```properties
net.minecraft.core.MixinCursor3D = false
```

* Keys resolve tolerantly: fully qualified name, name without the `lomka.starl.mixins.` prefix, or simple class name all work.
* Missing keys mean **enabled**; only `true`/`1` enable, anything else disables. Unknown keys are logged as stale and ignored.
* The plugin must never import Minecraft classes — `onLoad()` runs before class transformation. It only sets `joml.fastmath=true` (unless the user set it explicitly) and loads the properties file.
* `lomka.mixins.json` declares `minVersion: 0.8`.

## Testing & Debugging

* Run the affected variant in-game: `./gradlew :<ver>-<loader>:runClient`. For rendering changes, check at least one pre-1.21.6 (legacy pipeline) and one 26.x (new GPU-state pipeline) version.
* Bisect suspected patches with `config/lomka-mixins.properties` toggles instead of commenting code out.
* Compare vanilla semantics across versions with `srcnav.py diff` before rewriting shared logic.
* Big changes: run `./gradlew buildAllVariants` once before opening the PR so cross-version compile breaks surface early.

## Versioning & Changelog

* The mod version lives in `stonecutter.properties.toml` (`mod.version`) and is swapped into all templates — never hardcode it in Java.
* Add a `CHANGELOG.md` entry under a new `[x.y.z]` heading (sections in use: `Performance`, `Bug Fixes`, `New`, `Removed`, `Changed`), noting the affected version range per change like `(all versions)`, `(<1.21.4)`, `(>=26.1)`.
* Publishing is done by the maintainer via `py -3 scripts/publish.py --build --modrinth --curseforge` (supports `--dry-run`, `--variants`, `--skip-existing`, `--changelog`, tokens via `MODRINTH_TOKEN`/`CURSEFORGE_TOKEN`).

## Pull Request Process

1. **Fork** and create a branch from `dev`: `git checkout -b feat/short-name origin/dev` (or `git clone -b dev https://github.com/Starlevka/Lomka.git`).
2. **Implement** your change. Use `//? if` blocks for version-specific code and register new mixins in `src/stonecutter/lomka.mixins.json5`.
3. **Test** if you can: `./gradlew :1.21.11-fabric:build` or `buildAllVariants` for big changes; an in-game check is enough for small ones.
4. **Commit** with a short English message and push.
5. Open a PR to `Starlevka/Lomka:dev`, describe what it does and why it's faster. Link related issues if any. Don't worry about perfect formatting, we will help polish.

## License

By contributing you agree that your contributions will be licensed under `LGPL-3.0-only` (see `LICENSE`). The `LGPL-3.0-only` header must be present in every new Java file (`licenseHeaders` handles this automatically).

Thanks for making Lomka faster.
