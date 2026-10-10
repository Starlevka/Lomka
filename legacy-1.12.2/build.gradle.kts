// Legacy Forge build for Minecraft 1.7.10 - 1.12.2.
//
// Deliberately a SEPARATE Gradle build from the repository root, and the reason is structural
// rather than stylistic:
//
//   * One build means one Gradle version. RetroFuturaGradle 2.0.6 requires Gradle 9.2+, and its
//     UserDevPlugin is compiled for Java 25 (class file 69.0), so the daemon runs on 25 while the
//     compile toolchain stays on 8. If 1.7.10 ever needs a different RFG/Gradle line, a shared
//     build would have no way to express that.
//   * The root build's buildSrc is written against moddev and loom. A legacy loader needs a
//     separate Loader branch, its own Context.javaVersion rule (8, not the `parsed < "1.20.2"`
//     -> 17 the modern tree uses), and no access widener at all - 1.12.2 Forge has AT only.
//   * gradle.properties at the root caps the heap at 2G with 2 workers. RFG decompiles Minecraft
//     per version, which does not fit alongside twenty loom-resolved modern variants.
//
// What this replaced, and why (see scripts/legacy/README.md for the full archaeology):
//
//   * The hand-written notch -> SRG -> MCP pipeline (SpecialSource + tools/SrgMcpRemapper.java +
//     tools/build-mapping-tables.py + the generated mc-artifacts/ tree) is gone. RFG does all of
//     it and reobfs the result.
//   * Embedding Mixin 0.8.5 in the jar is gone. One MixinBooter coordinate carries the Mixin API,
//     the annotation processor and the runtime, and it ships the loader FML needs.
//   * lomka.starl.coremod.LomkaMixinBootstrap is gone. It existed because FML only accepts an
//     IClassTransformer from IFMLLoadingPlugin#getASMTransformerClass while Mixin's MixinTweaker
//     is an ITweaker. MixinBooter reads the `MixinConfigs` manifest attribute directly, so no
//     loader class is needed at all - and leaving the bootstrap in place would install a second,
//     conflicting Mixin.
//
// Mapping namespace is MCP, and that is the same choice the modern tree makes with Mojang
// mappings: the MCP identifier written in source is stable across legacy versions, and the refmap
// absorbs the SRG difference. Verified rather than assumed - func_176734_d is `getOpposite` in
// both 1.8.9 and 1.12.2, so one source file compiles on both with no string replacement.
//
// The MCP name is load-bearing in the source. mcp-srg.srg maps readable -> SRG
// (`rotateAround` -> `func_176732_a`) and never the reverse, so writing the `func_` form makes the
// annotation processor fail with "Unable to locate obfuscation mapping for @Overwrite method".
//
// NOT applied: org.spongepowered.mixin (mixingradle). Its newest release is 0.7.38 from
// 2023-08-03, it references org.gradle.util.VersionNumber, and Gradle 9 removed that class, so it
// cannot be applied here at all. `modUtils.enableMixins` below is RTG's own refmap generator and
// replaces both the mixingradle plugin and its `mixin { add sourceSets.main, ... }` block.

import org.gradle.jvm.toolchain.JavaLanguageVersion

// Which legacy variant to build, so a second version can be tried without a second copy of this
// file. Defaults to 1.12.2. Forge coordinates are `mc-build`, a versioning scheme of their own with
// nothing to do with the modern tree's loader versions.
//
// Deliberately NOT named `mcVersion` / `forgeVersion`: inside `minecraft { }` those names already
// resolve to the extension's own properties, and shadowing them makes the right-hand side of
// `mcVersion.set(...)` self-referential - Gradle reports it as a circular evaluation.
val targetMcVersion: String = (findProperty("lomkaMcVersion") as String?) ?: "1.12.2"
val targetForgeVersion: String = (findProperty("lomkaForgeVersion") as String?) ?: "1.12.2-14.23.5.2860"

plugins {
	java
	id("com.gtnewhorizons.retrofuturagradle") version "2.0.6"
}

version = "0.6.0"
group = "lomka.starl"
base {
	archivesName = "lomka-legacy-$targetMcVersion"
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(8)
	}
	withSourcesJar()
}

sourceSets {
	main {
		java.setSrcDirs(listOf(rootProject.file("../src-legacy/main/java")))
		resources.setSrcDirs(listOf(rootProject.file("../src-legacy/main/resources")))
	}
}

minecraft {
	mcVersion.set(targetMcVersion)
	javaCompatibilityVersion.set(8)
	extraRunJvmArguments.addAll(listOf("-ea:lomka", "-Dmixin.checks.interfaces=true"))
}

repositories {
	maven {
		name = "GTNH Maven"
		url = uri("https://nexus.gtnewhorizons.com/repository/public/")
	}
	maven("https://maven.cleanroommc.com")
	maven("https://repo.spongepowered.org/repository/maven-public/")
	maven("https://libraries.minecraft.net/")
	maven("https://maven.minecraftforge.net")
	mavenCentral()
}

val mixinbooter = modUtils.enableMixins("zone.rong:mixinbooter:11.17", "lomka.refmap.json").toString()

dependencies {
	patchedMcImplementation("net.minecraftforge:forge:$targetForgeVersion")

	implementation(mixinbooter) { isTransitive = false }
	annotationProcessor(mixinbooter)

	// IMixinConfigPlugin in CleanMix 0.7.2 declares preApply/postApply against the UNSHADED
	// org.objectweb.asm.tree.ClassNode, while the library only ships ASM relocated to
	// org.spongepowered.asm.lib. Our plugin never calls those two methods, but the descriptors still
	// have to resolve, so the type is needed at compile time. Whether it is present at RUNTIME comes
	// from Forge/launchwrapper's own ASM and is proven by booting, not by compiling - see
	// LomkaMixinPlugin's javadoc.
	compileOnly("org.ow2.asm:asm-tree:5.2")
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("mcversion", targetMcVersion)

	filesMatching("mcmod.info") {
		expand(
			mapOf(
				"version" to project.version.toString(),
				"mcversion" to targetMcVersion
			)
		)
	}

	// Forge 1.12.2's FML looks for this at the jar root before it will accept a mod's version, and
	// warns on every boot without it:
	//   "Mod lomka is missing the required element 'version' and a version.properties file could not
	//    be found. Falling back to metadata version 0.6.0"
	// The fallback works, so this is hygiene rather than a defect - but a mod should not print a
	// WARN on every launch.
	filesMatching("version.properties") {
		expand(mapOf("version" to project.version.toString()))
	}
}

tasks.jar {
	manifest {
		attributes(
			"MixinConfigs" to "lomka.mixins.json",
			"FMLAT" to "accesstransformer.cfg",
			"Implementation-Title" to "lomka",
			"Implementation-Version" to project.version.toString()
		)
	}
	archiveClassifier = "dev"
}
