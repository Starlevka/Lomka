@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.kotlin.dsl.*

fun File.stripHashComments(): File {
	writeText(readText().lineSequence().filter { !it.startsWith("#") }.joinToString("\n") + "\n")
	return this
}

fun File.stripBlockComments(): File {
	writeText(Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL)).replace(readText(), ""))
	return this
}

private val MIXIN_SOURCE_ROOT = "lomka/starl/mixins"

/**
 * Shared roots whose only consumers are mixins, excluded per version range.
 *
 * `duck` + non-record `utils` below 1.19: the shared helpers target 1.19+ classes, but the
 * 1.16.5 / 1.18.2 `<1.19` port block in `lomka.mixins.json5` lists real mixins again
 * (Mth/ARGB/Util/BlockPos/Cursor3D/Vec3i/Direction/SectionPos/NBT/CompressionEncoder/
 * BlockStateBaseCache/PalettedContainer/Heightmap/LayerLight/VoxelShapes/AABB/ChunkStatus/
 * Channel/SoundEngine/Material/Minecraft/RenderTarget) - and those mixins import the duck
 * interfaces plus DirectionTables/UniformPaletteCache. Excluding the whole roots alongside
 * `excludeUnlistedMixins` therefore produces "cannot find symbol" at the mixin call sites,
 * which is the opposite of the empty-variant honesty the block was written for.
 *
 * Records are still excluded below 1.18 (class-file 55): `UniformPaletteCache` and
 * `ResourceEntry` cannot load on a 1.16.5 JVM, and the 1.16.5 PalettedContainer has no
 * `Data` holder to key that memo on anyway (palette + BitStorage live in separate fields).
 * `AxisPoseRotate`/`BlockStepSweep` stay excluded: their only consumers (PoseStack,
 * BlockGetter) are gated >=1.19+ in the json5. `config` below 1.17: unchanged - the modern
 * `LomkaMixinPlugin` needs a text block and `var` (Java 15 / 10) and `JsonParser.parseReader`
 * (Gson 2.8.6). 1.16.5 is a Java 8 target shipping **Gson 2.8.0** and has neither, so the
 * class cannot even compile there - the legacy tree solves this with its own Java 8 + Log4j
 * + no-Gson plugin. The json5 drops the `"plugin"` key for the same range.
 *
 * When narrowing per file, keep this list in sync with the json5 `<1.19` block: whatever a
 * listed mixin imports has to compile, and whatever no listed mixin imports must stay out
 * (notably the JOML helpers on the <1.19.3 classpath shim).
 */
private val MIXIN_ONLY_RECORDS_BELOW_1_18 = listOf(
    "lomka/starl/utils/cache/UniformPaletteCache.java",
    "lomka/starl/utils/pack/ResourceEntry.java"
)
private val MIXIN_ONLY_JOML_HELPERS_BELOW_1_19 = listOf(
    "lomka/starl/utils/math/AxisPoseRotate.java",
    "lomka/starl/utils/math/BlockStepSweep.java"
)
private val MIXIN_PLUGIN_BELOW_1_17 = listOf("lomka/starl/config")

@OptIn(StonecutterExperimentalAPI::class)
fun Project.lomkaPlatform(loader: Loader) {
	val sc = this.sc
	val (version, loaderTag) = sc.current.project.split('-', limit = 2)
	sc.properties.tags(version, loaderTag)

	configureModPlatform(loader) {
		dependencies {
			when (loader) {
				Loader.FabricO, Loader.FabricM -> {
				required("minecraft") {
					val mcMin = runCatching { prop("deps.minecraft_min") }.getOrNull() ?: sc.current.version
					val mcMax = runCatching { prop("deps.minecraft_max") }.getOrNull()
					fabricLikeVersionRange = if (mcMax != null) ">=$mcMin <=$mcMax" else ">=$mcMin"
				}
					required("fabricloader") {
						fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
					}
				}
				else -> required("minecraft") {
					fabricLikeVersionRange = prop("deps.minecraft")
				}
			}
		}
	}

	val mainSourceSet = the<JavaPluginExtension>().sourceSets.getByName("main")
	val mainSources = mainSourceSet.java
	val parsed = sc.current.parsed

	val mixinsJson = sc.process(
		rootProject.file("src/stonecutter/lomka.mixins.json5"),
		"build/processed/${sc.current.project}/mixins/lomka.mixins.json"
	).stripBlockComments()
	stripUtf8Bom(mixinsJson)
	if (loader == Loader.Forge) {
		// Production Forge remaps member names to SRG. The packaged lomka.refmap.json
		// (wired by the legacyforge mixin extension) is only loaded when the config
		// references it explicitly - without this key Mixin logs "No refMap loaded"
		// and every @Inject/@ModifyConstant fails on obfuscated targets.
		// The json5 template already declares the key for <1.21, so only fill it in when absent -
		// writing it unconditionally emitted the key twice in the packaged config.
		val text = mixinsJson.readText()
		if (!text.contains("\"refmap\"")) {
			mixinsJson.writeText(text.replaceFirst("{", "{\n  \"refmap\": \"lomka.refmap.json\","))
		}
	}
	mainSourceSet.resources.srcDir(mixinsJson.parentFile)

	// Every variant build refreshes license headers on the shared source tree before compiling,
	// so new classes and template edits converge without manual steps (see root licenseHeaders).
	tasks.named("compileJava") {
		dependsOn(rootProject.tasks.named("licenseHeaders"))
	}

	tasks.named<ProcessResources>("processResources") {
		// Ship the project license text inside every jar so Modrinth/CurseForge scanners
		// and end users can verify the terms without visiting the repository.
		from(rootProject.file("LICENSE"))

if (loader == Loader.Forge && parsed >= "1.19") {
			// mods.toml's logoFile has to be a bare filename: 1.19.2 feeds it to
			// AbstractPackResources#getRootResource(String), which throws IllegalArgumentException
			// on any name containing '/', crashing the Forge mod list on selection. The root-level
			// copy is what 1.19.2 resolves; assets/lomka/icon.png stays for Fabric/NeoForge.
			from(rootProject.file("src/main/resources/assets/${prop("mod.id")}/icon.png")) {
				into("")
				rename { "logo.png" }
			}
		}
	}

	excludeUnlistedMixins(mainSources, mixinsJson)

	// Port helpers below 1.19 are narrowed per file, not per root: the json5 `<1.19` block
	// lists real mixins that import the duck interfaces plus DirectionTables (MixinDirection)
	// and UniformPaletteCache (MixinPalettedContainer, 1.18.2 only), so excluding the whole
	// roots breaks those mixins with "cannot find symbol". What stays out:
	// - records below 1.18 (class-file 55): UniformPaletteCache + ResourceEntry cannot load
	//   on a 1.16.5 JVM; 1.16.5 has no PalettedContainer.Data to key the uniform memo on anyway.
	// - JOML helpers below 1.19: AxisPoseRotate/BlockStepSweep are consumed only by the
	//   >=1.19+ mixins (PoseStack/BlockGetter), which the json5 never lists there.
	if (parsed < "1.19") {
		logger.lifecycle("Excluding JOML-only helpers below 1.19: {}", MIXIN_ONLY_JOML_HELPERS_BELOW_1_19)
		mainSources.exclude(MIXIN_ONLY_JOML_HELPERS_BELOW_1_19)
	}
	if (parsed < "1.18") {
		logger.lifecycle("Excluding record helpers below 1.18: {}", MIXIN_ONLY_RECORDS_BELOW_1_18)
		mainSources.exclude(MIXIN_ONLY_RECORDS_BELOW_1_18)
	}
	// The modern plugin cannot compile on 1.16.5's Java 8 + Gson 2.8.0 target.
	if (parsed < "1.17") {
		logger.lifecycle("Excluding the modern mixin plugin below 1.17: {}", MIXIN_PLUGIN_BELOW_1_17)
		mainSources.exclude(MIXIN_PLUGIN_BELOW_1_17.map { "$it/**" })
	}

	if (parsed < "1.20.2") {
		afterEvaluate {
			the<JavaPluginExtension>().toolchain {
				languageVersion = JavaLanguageVersion.of(17)
			}
		}
	}

	// Dev artifacts are deleted as soon as the shipped jar exists - but **not** on the moddev-based
	// loaders, where the pre-reobf jar is a build input.
	//
	// `devlibs` is the plugins' scratch area: Loom puts the dev jar *and* a pre-remap sources jar
	// there, moddev puts the pre-reobf jar there, and the last of those carries the very same file
	// name as the shippable jar - a worse trap than a `-dev` suffix. Deleting it breaks the Forge
	// builds outright, though: `RemapJar.input` is wired as a plain `File`, not a provider, so Gradle
	// infers no task dependency, and the build then fails with "Input file does not exist" instead of
	// re-running `jar`. Loom passes a provider, so there the deletion is safe.
	//
	// Either way nothing dev-ish is left in `build/libs`, which is the directory anything (a person,
	// an AI, `publish.py`) is supposed to take an artifact from.
	val dropDevArtifacts = tasks.register("dropDevArtifacts") {
		group = "build"
		description = "Deletes dev jars so build/libs holds only shippable artifacts"
		// The files are removed from outside Gradle's up-to-date bookkeeping (a previous run, a clean
		// IDE, a manual delete), so there is no input to track: always run.
		outputs.upToDateWhen { false }
		doLast {
			val devlibs = layout.buildDirectory.dir("devlibs").get().asFile
			if (loader is Loader.FabricLike) {
				devlibs.listFiles()?.forEach {
					if (it.isFile) {
						delete(it)
						logger.lifecycle("Removed dev artifact: devlibs/{}", it.name)
					}
				}
			}
			layout.buildDirectory.dir("libs").get().asFile
				.listFiles { file -> file.isFile && file.name.endsWith("-dev.jar") }
				?.forEach {
					delete(it)
					logger.lifecycle("Removed dev artifact: libs/{}", it.name)
				}
		}
	}
	tasks.named(loader.jarTask) { finalizedBy(dropDevArtifacts) }

	tasks.register<Copy>("buildAndCollect") {
		group = "build"
		description = "Builds mod jars and copies results to `build/libs/{project}/`"
		from(tasks.named<Jar>(loader.jarTask).flatMap { it.archiveFile })
		from(tasks.named<Jar>(loader.sourcesJarTask).flatMap { it.archiveFile })
		into(rootProject.layout.buildDirectory.dir("libs/${sc.current.project}"))
	}
}

@OptIn(StonecutterExperimentalAPI::class)
fun Project.lomkaAwFile(): File {
	val sc = this.sc
	return sc.process(
		rootProject.file("src/stonecutter/lomka.ct"),
		"build/processed/${sc.current.project}/aw/${sc.current.version}.accesswidener"
	).stripHashComments().also { stripUtf8Bom(it) }
}

@OptIn(StonecutterExperimentalAPI::class)
fun Project.lomkaAtFile(): File {
	val sc = this.sc
	return sc.process(
		rootProject.file("src/stonecutter/accesstransformer.ct"),
		"build/processed/${sc.current.project}/at/META-INF/accesstransformer.cfg"
	).stripHashComments().also { stripUtf8Bom(it) }
}

/**
 * Stonecutter copies processed template bytes verbatim, so a UTF-8 BOM saved by an
 * editor flows into every generated resource and crashes Gson-based mixin config
 * parsing at launch ("Expected BEGIN_OBJECT but was STRING"). Strips it defensively.
 */
private fun stripUtf8Bom(file: File) {
	val bytes = file.readBytes()
	if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
		file.writeBytes(bytes.copyOfRange(3, bytes.size))
	}
}

private fun Project.excludeUnlistedMixins(mainSources: SourceDirectorySet, mixinsJson: File) {
	val listed = parseMixinNames(mixinsJson)
	val srcDir = rootProject.file("src/main/java")
	val packagePattern = Regex("package\\s+([\\w.]+)\\s*;")
	val excluded = mutableListOf<String>()

	srcDir.resolve(MIXIN_SOURCE_ROOT).walkTopDown().forEach { file ->
		if (!file.isFile || file.extension != "java") return@forEach
		val pkg = packagePattern.find(file.readText())?.groupValues?.get(1) ?: return@forEach
		val configName = "$pkg.${file.nameWithoutExtension}".removePrefix("lomka.starl.mixins.")
		if (configName !in listed) {
			excluded.add(file.toRelativeString(srcDir).replace('\\', '/'))
		}
	}

	if (excluded.isNotEmpty()) {
		logger.lifecycle("Excluding mixins absent from {}: {}", mixinsJson.name, excluded)
		mainSources.exclude(excluded)
	}
}

private fun parseMixinNames(configFile: File): Set<String> {
	val pattern = Regex("\"((?:accessor|com\\.mojang|net\\.minecraft)\\.[\\w.]+)\"")
	return pattern.findAll(configFile.readText()).map { it.groupValues[1] }.toSet()
}
