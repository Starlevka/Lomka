
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion

val targetMcVersion: String = (findProperty("lomkaMcVersion") as String?) ?: "1.12.2"
val targetForgeVersion: String = (findProperty("lomkaForgeVersion") as String?) ?: "1.12.2-14.23.5.2860"

plugins {
	java
	id("com.gtnewhorizons.retrofuturagradle") version "2.0.6"
}

version = "0.7.0"
group = "lomka.starl"

val artifactStem = "lomka-$version-forge+$targetMcVersion"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(8)
	}
	withSourcesJar()
}

apply(from = "../../gradle/preprocess.gradle")
apply(from = "../dependencies.gradle")

extra["lomkaMcVersion"] = targetMcVersion
extra["lomkaPreprocessTask"] = "preprocessLegacyResources"
extra["lomkaRefmap"] = "lomka.refmap.json"
extra["lomkaMixinConfigs"] = "lomka.mixins.json"
extra["lomkaConfigCopy"] = null
extra["lomkaReobfTask"] = "reobfJar"
extra["lomkaNoAnnotationProcessing"] = false

apply(from = "../common.gradle")

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

val legacyVersions = project.extensions.extraProperties.get("lomkaVersions") as Map<*, *>
val mixinbooter = modUtils.enableMixins(legacyVersions["mixinBooter"] as String, "lomka.refmap.json").toString()

dependencies {
	patchedMcImplementation("net.minecraftforge:forge:$targetForgeVersion")

	implementation(mixinbooter) { isTransitive = false }
	annotationProcessor(mixinbooter)
	compileOnly(legacyVersions["asmTree"] as String)
}

val devLibsDir = layout.buildDirectory.dir("devlibs")
val shipLibsDir = layout.buildDirectory.dir("libs")

tasks.jar {
	archiveFileName.set("$artifactStem-dev.jar")
	destinationDirectory = devLibsDir
	manifest {
		attributes(
			"Implementation-Title" to "lomka",
			"Implementation-Version" to project.version.toString()
		)
	}
}

tasks.named<Jar>("reobfJar") {
	archiveFileName.set("$artifactStem.jar")
	destinationDirectory.set(shipLibsDir)
}

tasks.named<Jar>("sourcesJar") {
	archiveFileName.set("$artifactStem-sources.jar")
}
