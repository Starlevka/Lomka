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
						val mcMin = sc.current.version
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
		mixinsJson.writeText(
			mixinsJson.readText().replaceFirst("{", "{\n  \"refmap\": \"lomka.refmap.json\",")
		)
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
	}

	excludeUnlistedMixins(mainSources, mixinsJson)

	if (parsed < "1.20.2") {
		afterEvaluate {
			the<JavaPluginExtension>().toolchain {
				languageVersion = JavaLanguageVersion.of(17)
			}
		}
	}

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
